# 모니터링 초기 환경 적용

이 문서는 이미 완료한 초기 환경 설치 기록이다. 현재 prometheus.yml에는 develop 수집 대상이 추가되어 있으므로, 백엔드 연동은 [develop-connection.md](develop-connection.md)의 배포 순서를 따른다. 초기 설치 당시에는 자체 지표만 수집했다.

이번 기능은 기존 EC2에 Prometheus와 Grafana를 추가하는 것이다. 백엔드 Actuator 의존성·설정, API 대시보드, 알림은 다음 작업이다. 기존 CI/CD·프론트엔드·API·application-local.yml은 수정하지 않는다.

## 구성

- 접속 주소: https://www.midpoint.my/grafana/
- Prometheus v3.13.3 LTS, Grafana OSS 13.1.2: 버전 고정.
- Prometheus 512MiB, Grafana 384MiB: 초기 상한이며 실제 부하에서 조정한다. 기존 컨테이너 포함 상한 합계 3,124MiB. 호스트 메모리와 컨테이너 외 사용량도 확인한다.
- 수집 주기 30초, 보관 7일 또는 저장 블록 2GB 중 먼저 도달하는 조건. WAL 등으로 실제 디스크 사용량은 2GB를 넘을 수 있다.
- Prometheus는 호스트 포트를 공개하지 않는다. Grafana만 3000을 사용한다.
- 기존 Compose 기본 네트워크를 사용한다. 이는 웹 컨테이너도 참여하는 네트워크이며, 전용 보안 격리를 의미하지 않는다. 백엔드 관리 포트 접근 제어는 다음 작업에서 검증한다.
- 이번에는 Prometheus 자체 지표만 수집한다. Grafana Explore에서 `up{job="prometheus"}` 결과가 1이면 연결 성공이다.

## 1. 파일 전송

먼저 EC2 SSH 터미널에서 실행한다. 기존 monitoring 폴더가 있다면 덮어쓰지 말고 내용을 먼저 확인한다.

```bash
mkdir -p /opt/midpoint/monitoring-incoming
test ! -e /opt/midpoint/monitoring
```

위 test가 성공한 경우 진행한다. 로컬 PowerShell에서 저장소 루트로 이동한 다음 실행한다. SSH 키 경로와 호스트는 사용자 입력이며 비밀키 자체를 공유하지 않는다.

```powershell
$monitoringKey = Read-Host 'SSH 개인 키 파일 경로'
$monitoringHost = Read-Host 'EC2 접속 주소'
scp -i "$monitoringKey" deploy/docker-compose.yml "ubuntu@${monitoringHost}:/opt/midpoint/monitoring-incoming/"
scp -i "$monitoringKey" -r deploy/monitoring "ubuntu@${monitoringHost}:/opt/midpoint/monitoring-incoming/"
```

## 2. 백업과 비밀번호 준비

EC2 SSH 터미널에서 실행한다. 오류가 나면 다음 단계로 넘어가지 않는다.

```bash
cd /opt/midpoint
cp -p docker-compose.yml "docker-compose.yml.before-monitoring.$(date +%Y%m%d-%H%M%S)"
cp -R monitoring-incoming/monitoring ./monitoring
mkdir -p monitoring/secrets
chmod 700 monitoring/secrets
read -r -s -p 'Grafana admin 비밀번호: ' monitoring_password
printf '\n'
```

빈 비밀번호가 아닌지 확인한 후 저장한다. 상위 secrets 디렉터리는 ubuntu만 접근하며, 파일은 컨테이너의 Grafana 사용자가 읽을 수 있도록 설정한다. 로컬 Compose file secret은 암호화 저장소가 아니다.

```bash
test -n "$monitoring_password"
(umask 077; printf '%s' "$monitoring_password" > monitoring/secrets/grafana_admin_password)
unset monitoring_password
chmod 444 monitoring/secrets/grafana_admin_password
```

이 비밀번호 설정은 Grafana 최초 DB 생성 시 적용된다. 이후 파일 변경만으로 기존 admin 비밀번호가 바뀌지 않으며 Grafana에서 변경해야 한다.

## 3. 검증 후 모니터링 서비스만 시작

각 명령 성공을 확인한 후 다음 명령을 실행한다. 기존 서비스와 볼륨 삭제 명령은 사용하지 않는다.

```bash
cd /opt/midpoint
docker compose --project-directory /opt/midpoint -f monitoring-incoming/docker-compose.yml config --quiet
docker compose --project-directory /opt/midpoint -f monitoring-incoming/docker-compose.yml pull prometheus grafana
docker compose --project-directory /opt/midpoint -f monitoring-incoming/docker-compose.yml run --rm --no-deps --entrypoint /bin/promtool prometheus check config /etc/prometheus/prometheus.yml
diff -u docker-compose.yml monitoring-incoming/docker-compose.yml
```

diff는 차이가 있으면 종료 코드 1을 반환하는 것이 정상이다. 기존 네 서비스가 바뀌지 않고 모니터링 항목만 추가됐는지 직접 확인한다. 서버 파일이 달라졌다면 통째로 덮어쓰지 말고 변경점을 다시 검토한다.

```bash
cp monitoring-incoming/docker-compose.yml docker-compose.yml
docker compose up -d --no-deps prometheus grafana
docker compose ps
docker compose logs --tail 50 prometheus grafana
docker compose exec -T prometheus wget -qO- http://localhost:9090/-/ready
curl -fsS http://127.0.0.1:3000/grafana/api/health
```

컨테이너 기동 직후에는 준비가 끝날 때까지 잠시 기다린 뒤 상태 확인을 다시 실행한다. Grafana 상태 JSON에서 database가 ok인지 확인한다. 비밀번호 입력은 HTTPS 접속에서만 진행한다.

## 4. 기존 ALB 연결

1. EC2 보안 그룹 `sg-0f91b2f2c4d8817cc`에 TCP 3000, 소스 `sg-0512e9a2ad7f72fb7` 추가. 3000의 인터넷 전체 허용 규칙을 만들지 않는다.
2. ALB 보안 그룹 `sg-0512e9a2ad7f72fb7`의 아웃바운드에서 EC2의 3000 접근이 가능한지 확인. 제한돼 있다면 대상 EC2 보안 그룹으로 TCP 3000 허용.
3. 기존 EC2를 대상으로 HTTP 3000 대상 그룹 추가. 상태 확인 경로 `/grafana/api/health`, 성공 코드 200. Healthy 확인.
4. 기존 ALB HTTPS 443에 Host `www.midpoint.my` AND Path `/grafana`, `/grafana/*` 조건의 전달 규칙 추가. 두 경로 값은 OR이며 Host와는 AND다.
5. 위 규칙이 기존 www 전체 전달 규칙보다 먼저 평가되도록 사용하지 않는 우선순위 선택. URL 재작성은 하지 않는다. Grafana가 하위 경로를 처리한다.
6. 기존 www 인증서 사용. DNS·기존 대상 그룹·프론트 nginx 변경 불필요.

## 5. 검증

- https://www.midpoint.my/grafana/ 로그인 화면이 뜨고 admin과 준비한 비밀번호로 로그인되는지 확인.
- 새로고침과 로그인 후에도 URL이 /grafana/ 아래에 있는지 확인.
- Grafana Explore에서 Prometheus 데이터 소스를 선택하고 `up{job="prometheus"}` 실행. 1인지 확인. 이 결과는 백엔드 상태를 뜻하지 않는다.
- www, api, dev, dev-api 기존 기능과 STOMP 연결에 영향이 없는지 확인.
- EC2 공인 주소:3000에 외부에서 직접 접근되지 않는지 확인.
- `docker stats --no-stream`, `free -m`, `df -h`로 적용 후 자원 확인. OOM·재시작·메모리 압박이 있으면 main API 상한을 줄이지 말고 모니터링 배치를 재검토.

기존 CI/CD는 JAR와 api-deploy.sh만 보내므로 모니터링 설정은 유지된다. API 배포 성공 검사는 기존 /v3/api-docs를 유지한다.

## 되돌리기

ALB에 추가한 Grafana 규칙을 제거하고, 다음 명령으로 추가 서비스만 중지한다. 저장 볼륨을 삭제하지 않는다.

```bash
cd /opt/midpoint
docker compose stop grafana prometheus
```

백업 Compose 파일명을 확인하여 원본 위치로 복사한다. 기존 API·웹 재시작은 필요 없다. 추가한 대상 그룹과 3000 허용 규칙만 제거한다. 전체 `docker compose down`이나 `down -v`는 실행하지 않는다.

## 다음 기능

Actuator 의존성, 내부 관리 포트·보안, 미사용 Redis 상태 검사 제외, main/develop 수집 설정을 함께 구현·검증한다. 그때까지 application-local.yml에 관리 설정을 추가하거나 백엔드 수집 대상을 활성화하지 않는다.

## 검증 근거

- https://prometheus.io/download/ (3.13.3 LTS)
- https://prometheus.io/docs/prometheus/latest/storage/ (보관과 WAL)
- https://grafana.com/grafana/download (13.1.2)
- https://grafana.com/tutorials/run-grafana-behind-a-proxy/ (하위 경로)
- https://grafana.com/docs/grafana/latest/setup-grafana/configure-docker/ (파일 기반 비밀번호)

로컬에서는 기존 네 서비스 설정 보존, Prometheus 포트 미공개, 서비스 의존성 미추가와 공백 오류를 확인했다. 로컬에 Docker와 YAML 파서가 없어 Compose 구문·promtool·실제 기동 검증은 수행하지 못했다. 위 서버 검증 명령이 통과한 뒤에만 적용한다.
