# 배포 구성

EC2 1대에 컨테이너 4개를 띄우고, 앞단은 ALB 가 Host 헤더로 갈라준다.

```
midpoint.my          → main-web      :8081   nginx + dist 마운트
api.midpoint.my      → main-api      :8080   temurin + jar 마운트
dev.midpoint.my      → develop-web   :8083
dev-api.midpoint.my  → develop-api   :8082
```

커스텀 이미지를 만들지 않는다. stock 이미지에 산출물을 볼륨으로 넣기 때문에
Dockerfile 도, 컨테이너 레지스트리도 없다. 배포는 **scp 로 jar 를 덮고 컨테이너를 다시 띄우는 것**이 전부다.

---

## 설정 파일이 어떻게 나뉘는가

| 파일 | git | 어디에 있나 | 무엇이 들어가나 |
|---|---|---|---|
| `application.yml` | **커밋** | jar 안 | 프로파일 순서만 (7줄) |
| `application-constant.yml` | **커밋** | jar 안 | 비밀이 아닌 상수. 공개 API URL, 타임아웃, enum 문자열, 게임 상수 |
| `application-local.yml` | 제외 | 서버에 마운트 | DB 접속 정보, 카카오/Tmap 키, 환경별 CORS 주소 |

`application.yml` 의 프로파일 순서는 `constant` → `local` 이다. Spring 은 뒤에 오는 프로파일이
이기므로, **마운트한 `application-local.yml` 이 jar 안의 상수를 덮어쓴다.**
덕분에 커밋되는 파일을 건드리지 않고 환경별 값만 서버에서 바꿀 수 있다.

컨테이너는 `SPRING_CONFIG_ADDITIONAL_LOCATION=file:/config/` 로 그 파일을 읽는다.
`SPRING_CONFIG_LOCATION` 이 아니다 — 그건 기본 탐색 위치를 통째로 대체해서
jar 안의 두 파일이 안 읽히고 41개 `@Value` 가 전부 실패한다.

---

## 서버 최초 세팅 (1회)

### 1. 기본 패키지와 사용자

```bash
sudo apt update
sudo apt install -y docker.io docker-compose-v2 fail2ban
sudo useradd -m -s /bin/bash deploy
sudo usermod -aG docker deploy
```

`docker` 그룹은 사실상 root 권한과 같다. 그래서 `deploy` 계정에 별도 sudo 권한은 주지 않는다.

### 2. SSH 하드닝

`/etc/ssh/sshd_config`:

```
PasswordAuthentication no
PermitRootLogin no
```

```bash
sudo systemctl restart ssh
```

22번은 전 세계에 열려 있어야 한다. Actions 러너 IP 는 수천 CIDR 이라 보안그룹으로 못 막는다.
게다가 이 레포가 public 이라 `deploy` 라는 계정명과 경로가 공개되어 있으니 비밀번호 인증은 반드시 끈다.

### 3. 배포 키

로컬에서 만들고 공개키만 서버에 넣는다.

```bash
ssh-keygen -t ed25519 -f ~/.ssh/midpoint_deploy -N "" -C "gh-actions midpoint"
```

서버:

```bash
sudo -u deploy mkdir -p /home/deploy/.ssh
sudo -u deploy chmod 700 /home/deploy/.ssh
# 공개키(midpoint_deploy.pub) 내용을 붙여넣는다
sudo -u deploy tee -a /home/deploy/.ssh/authorized_keys
sudo -u deploy chmod 600 /home/deploy/.ssh/authorized_keys
```

GitHub Secret `SSH_PRIVATE_KEY` 에는 **개인키 전문**(`-----BEGIN`부터 `-----END`까지)을 넣는다.
Variable `SSH_KNOWN_HOSTS` 에는 `ssh-keyscan -H <EC2 IP>` 출력을 그대로 넣는다.

### 4. 디렉터리

```bash
sudo mkdir -p /opt/midpoint/{main,develop}/{incoming,config,dist}
sudo chown -R deploy:deploy /opt/midpoint
```

### 5. 설정 파일 배치

`src/main/resources/application-local.yml.example` 을 참고해 두 개를 만든다.

```bash
sudo -u deploy vi /opt/midpoint/main/config/application-local.yml
sudo -u deploy vi /opt/midpoint/develop/config/application-local.yml
sudo chmod 600 /opt/midpoint/{main,develop}/config/application-local.yml
```

환경별로 반드시 다르게 넣을 값:

| 키 | main | develop |
|---|---|---|
| `cors.allowed-origin` | `https://midpoint.my` | `https://dev.midpoint.my` |
| `spring.datasource.hikari.maximum-pool-size` | `10` | `5` |

풀 크기를 낮추는 이유: 학원 Oracle XE 는 전체 세션 수에 한계가 있는데,
기본값 30 을 두 환경에 그대로 쓰면 60 개를 잡는다. 여기에 개발자 로컬까지 붙으면
`ORA-00018` 이 난다.

### 6. compose 와 nginx.conf

- `deploy/docker-compose.yml` → `/opt/midpoint/docker-compose.yml`
- 프론트 레포의 `deploy/nginx.conf` → `/opt/midpoint/{main,develop}/nginx.conf`

첫 `up -d` 전에 `app.jar` 와 `dist/` 가 없으면 컨테이너가 뜨지 않는다.
**첫 배포를 먼저 돌리고 나서 `up -d` 하는 편이 간단하다.**

### 7. ALB

- 타깃 그룹 4개. 전부 같은 인스턴스, 포트만 다르다
- 헬스체크: web 은 `/`, api 는 `/v3/api-docs` (둘 다 200)
- 리스너 `:443` 에 ACM 인증서(`midpoint.my` + `*.midpoint.my`), `:80` 은 `:443` 으로 리다이렉트
- **ALB 는 2개 이상 AZ 의 서브넷이 필요하다.** 인스턴스는 한 AZ 에 있어도 된다
- 인스턴스 보안그룹은 8080~8083 을 **ALB 의 보안그룹에서만** 허용한다. 인터넷에 직접 열지 않는다
- WebSocket 은 ALB 가 기본 지원한다. idle timeout 은 기본 60초 그대로 두면 된다
  (`chat.heartbeat-interval` 이 10초라 여유가 크다)

---

## 배포 흐름

```
feature 브랜치 → PR → develop → (자동) develop-api 배포 → dev 환경에서 확인
                          ↓
                        main → (승인 후) main-api 배포
```

`production` 환경에 Required reviewers 를 걸어두면 main 머지 후 배포가 대기 상태로 멈춘다.

---

## 확인 명령

```bash
# 컨테이너 4개가 다 살아있나
docker compose ps

# 각각 응답하나
curl -sI localhost:8080/v3/api-docs    # main-api
curl -sI localhost:8082/v3/api-docs    # develop-api
curl -sI localhost:8081/               # main-web
curl -sI localhost:8083/               # develop-web

# ALB 를 통해서 — CORS 가 맞는지가 배포 성공의 진짜 신호다
curl -sI -H "Origin: https://dev.midpoint.my" \
  https://dev-api.midpoint.my/api/rooms/00000000-0000-0000-0000-000000000000 \
  | grep -i "^access-control-allow-origin"

# 시각 (UTC 로 나오면 TZ 설정이 안 먹은 것)
docker compose exec main-api date

# 메모리 — 같은 호스트에 4개가 사는 구조라 이게 중요하다
docker stats --no-stream
free -h
```

## 문제가 생겼을 때

```bash
docker compose logs --tail 100 main-api

# 이전 버전으로 되돌리기
mv /opt/midpoint/main/app.jar.prev /opt/midpoint/main/app.jar
docker compose up -d --force-recreate main-api
```

기동 실패가 `Could not resolve placeholder ...` 라면 마운트한
`application-local.yml` 에 그 키가 없는 것이다. 자바 쪽에 기본값을 두지 않기 때문에
키 하나만 빠져도 기동하지 않는다.
