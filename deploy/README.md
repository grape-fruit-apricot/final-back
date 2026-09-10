# 배포 구성

EC2 1대에 컨테이너 4개를 띄우고, 앞단은 ALB 가 Host 헤더로 갈라준다.

```
midpoint.my          → main-web      :80     nginx + dist 마운트
api.midpoint.my      → main-api      :8080   temurin + jar 마운트
dev.midpoint.my      → develop-web   :8081
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

### 1. 기본 패키지

```bash
sudo apt update
sudo apt install -y docker.io docker-compose-v2 rsync

# 기본 계정을 그대로 쓴다. docker 그룹에 넣지 않으면
# 배포 스크립트의 docker compose 가 permission denied 로 죽는다.
sudo usermod -aG docker ubuntu
```

적용하려면 재로그인하거나 `newgrp docker` 를 한 번 실행한다.

배포 전용 계정(`deploy`)을 따로 만들지 않는다. 어차피 컨테이너를 재시작해야 해서
`docker` 그룹에 넣어야 하고, **docker 그룹은 사실상 root 와 같아서** 계정만 나누는 것은
권한을 낮추지 못한다. 대신 아래 3번처럼 **키를 분리**한다.

> Amazon Linux 면 `ubuntu` 자리가 `ec2-user` 이고 `apt` 대신 `dnf` 다.

### 2. SSH 확인

22번은 전 세계에 열어야 한다. Actions 러너 IP 는 수천 CIDR 이라 보안그룹으로 못 막는다.
키 인증만 열려 있으면 비밀번호 브루트포스는 성립하지 않으므로, 별도 설정 없이
**최종 적용값만 확인**하면 된다.

```bash
sudo sshd -T | grep -iE "passwordauthentication|permitrootlogin"
# passwordauthentication no
# permitrootlogin no
```

파일이 아니라 `sshd -T` 로 본다. Ubuntu 클라우드 이미지는 `/etc/ssh/sshd_config.d/` 안의
파일이 메인 설정보다 우선하므로, 메인 파일만 고치면 실제로는 안 먹을 수 있다.
`yes` 가 나오면 그때 `/etc/ssh/sshd_config.d/` 안을 고친다.

### 3. 배포 키

계정은 기본 계정을 쓰되 **키는 CI 전용으로 하나 더 만든다.** GitHub Secret 은
워크플로를 고칠 수 있는 사람이면 꺼내 볼 수 있다(마스킹은 base64 로 우회된다).
개인 키를 넣으면 그게 유출됐을 때 본인 접속 수단까지 넘어가고, 갈아끼우면 본인 접속도 끊긴다.
전용 키면 서버 `authorized_keys` 에서 그 한 줄만 지워 CI 만 차단할 수 있다.

로컬에서:

```bash
ssh-keygen -t ed25519 -f ~/.ssh/midpoint_ci -N "" -C "gh-actions midpoint"
ssh-copy-id -i ~/.ssh/midpoint_ci.pub ubuntu@<탄력적IP>
```

GitHub Secret `SSH_PRIVATE_KEY` 에는 `cat ~/.ssh/midpoint_ci` 출력 **전문**
(`-----BEGIN` 부터 `-----END` 까지, 마지막 줄바꿈 포함)을 넣는다. 한 줄로 뭉치면
`Load key: invalid format` 으로 죽는다.

Variable `SSH_KNOWN_HOSTS` 에는 `ssh-keyscan -H <탄력적IP>` 출력을 그대로 넣는다.

### 4. 디렉터리

```bash
sudo mkdir -p /opt/midpoint/{main,develop}/{incoming,config,dist}
sudo chown -R ubuntu:ubuntu /opt/midpoint
```

### 5. 설정 파일 배치

`src/main/resources/application-local.yml.example` 을 참고해 두 개를 만든다.

```bash
vi /opt/midpoint/main/config/application-local.yml
vi /opt/midpoint/develop/config/application-local.yml
chmod 600 /opt/midpoint/{main,develop}/config/application-local.yml
```

환경별로 반드시 다르게 넣을 값:

| 키 | main | develop |
|---|---|---|
| `spring.datasource.hikari.maximum-pool-size` | `10` | `5` |

CORS 는 여기에 넣지 않는다. `application-constant.yml` 의 `cors.allowed-origins` 에
로컬 주소와 배포 주소가 모두 들어 있다. 특정 환경만 더 좁히고 싶을 때만 같은 키로 덮어쓴다.

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
- 인스턴스 보안그룹은 80, 8080~8082 를 **ALB 의 보안그룹에서만** 허용한다. 인터넷에 직접 열지 않는다
- WebSocket 은 ALB 가 기본 지원한다. idle timeout 은 기본 60초 그대로 두면 된다
  (`chat.heartbeat-interval` 이 10초라 여유가 크다)

---

## 배포 흐름

```
feature 브랜치 → PR → develop → (자동) develop-api 배포 → dev 환경에서 확인
                          ↓
                        main → (즉시) main-api 배포
```

**main 머지 = 즉시 운영 배포다.** 승인 게이트를 두지 않는다.
GitHub Environment 를 쓰지 않으므로 Required reviewers 도 없다.
`develop -> main` 은 팀장만 올리고 머지하는 것으로 통제한다.

---

## 확인 명령

```bash
# 컨테이너 4개가 다 살아있나
docker compose ps

# 각각 응답하나
curl -sI localhost:8080/v3/api-docs    # main-api
curl -sI localhost:8082/v3/api-docs    # develop-api
curl -sI localhost:80/                 # main-web
curl -sI localhost:8081/               # develop-web

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

---

## GitHub 에 넣는 값

Settings → Secrets and variables → Actions. **Repository 단위**로 넣는다
(Environment 를 쓰지 않는다 — 개인 계정 레포라 Environment 생성은 소유자만 가능하고,
환경별로 다른 값은 브랜치에서 계산되는 것 둘뿐이라 상자를 나눌 실익이 없다).

| 탭 | Name | Value |
|---|---|---|
| Variables | `DEPLOY_HOST` | EC2 탄력적 IP |
| Variables | `DEPLOY_USER` | `ubuntu` (Amazon Linux 면 `ec2-user`) |
| Variables | `SSH_KNOWN_HOSTS` | `ssh-keyscan -H <탄력적IP>` 출력 전체 |
| Secrets | `SSH_PRIVATE_KEY` | `cat ~/.ssh/midpoint_ci` 전문 |

`DEPLOY_ENV` 는 넣지 않는다. 워크플로가 브랜치에서 계산한다
(`main` 브랜치 -> `main`, 그 외 -> `develop`).

**GitHub 에 절대 넣지 않는 것:** Oracle 접속정보, `kakao.rest-api-key`, `tmap.app-key`.
이건 서버의 `/opt/midpoint/{main,develop}/config/application-local.yml` 에 손으로 넣는다.
CI 가 DB 비밀번호를 들고 있으면 GitHub 이 털렸을 때 DB 까지 같이 털린다.
