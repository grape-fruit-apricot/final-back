# develop 서버 상태·수집 연결

이번 범위는 health 응답과 Prometheus 수집 연결 확인이다. API·JVM·DB 풀 전용 지표 설정·대시보드, main 수집, 테스트 코드는 추가하지 않는다. Actuator/Micrometer가 자동 제공하는 기본 지표는 존재할 수 있다.

## 1. 서버 외부 설정 추가

EC2에서 `/opt/midpoint/develop/config/application-local.yml`을 먼저 백업한다. 기존 DB·외부 API·cleanup 설정은 유지하고 아래 최상위 management 블록만 추가한다. 해당 키가 이미 생겼다면 중복 작성하지 않는다.

```yaml
management:
  server:
    port: 9091
    address: 0.0.0.0
  endpoints:
    web:
      base-path: /actuator
      exposure:
        include: health,prometheus
      discovery:
        enabled: false
    jmx:
      exposure:
        exclude: '*'
  endpoint:
    health:
      show-details: never
      show-components: never
  health:
    redis:
      enabled: false
```

내부 관리 포트 9091은 Compose ports·ALB·EC2 보안 그룹에 추가하지 않는다. Docker 기본 네트워크의 컨테이너에서는 접근 가능하며 Prometheus 전용 인증을 의미하지 않는다. 관리 포트 미설정 시 보안 코드는 /actuator 경로를 차단한다. 포트를 분리하지 않고 업무 포트에서 허용하는 대체 설정을 하지 않는다.

## 2. develop 배포

build.gradle과 SecurityConfig 변경을 기존 PR/CI/CD 절차로 develop에 배포한다. 기존 배포 확인 경로 /v3/api-docs는 유지한다. 이 문서만으로 push·merge를 실행하지 않는다. main 설정은 수정하지 않는다.

배포 성공 후 EC2의 `/opt/midpoint`에서 실행한다.

```bash
docker compose exec -T prometheus wget -qO- http://develop-api:9091/actuator/health
docker compose exec -T prometheus wget -qO- http://develop-api:9091/actuator/prometheus
curl -s -o /dev/null -w '%{http_code}\n' http://127.0.0.1:8082/actuator/health
curl -s -o /dev/null -w '%{http_code}\n' http://127.0.0.1:8082/actuator/prometheus
```

내부 health 정상 응답은 `{"status":"UP"}`이며 Oracle 등 활성 상태 검사가 실패하면 503/DOWN이 나올 수 있다. Redis만 검사에서 제외한다. 내부 prometheus는 지표 텍스트가 나와야 한다. 업무 포트의 두 요청은 403으로 차단되어야 한다. 실패 시 공개 포트를 추가하지 말고 배포 로그와 설정을 확인한다.

## 3. 수집 설정 반영

기존 초기 설정용 Prometheus 파일을 백업한다. 로컬 PowerShell에서 기존 monitoringKey·monitoringHost 변수가 있는 창을 사용한다. 없으면 다시 입력한다.

```powershell
scp -i "$monitoringKey" deploy/monitoring/prometheus.yml "ubuntu@${monitoringHost}:/opt/midpoint/monitoring-incoming/prometheus-develop.yml"
```

EC2에서 후보 파일을 검사한다. 다음 명령은 한 줄 전체를 실행한다.

```bash
cd /opt/midpoint
docker compose run --rm --no-deps -v /opt/midpoint/monitoring-incoming/prometheus-develop.yml:/tmp/prometheus-develop.yml:ro --entrypoint /bin/promtool prometheus check config /tmp/prometheus-develop.yml
```

검사가 성공한 경우에만 다음 명령을 순서대로 실행한다.

```bash
cp -p monitoring/prometheus.yml "monitoring/prometheus.yml.before-develop.$(date +%Y%m%d-%H%M%S)"
cp monitoring-incoming/prometheus-develop.yml monitoring/prometheus.yml
docker compose up -d --no-deps --force-recreate prometheus
```

Prometheus만 재생성한다. 저장 볼륨·Grafana·API·웹은 유지한다.

## 4. 확인과 복구

Grafana Explore에서 `up{job="midpoint-develop",environment="develop"}`가 1인지 확인한다. 이는 수집 성공이며 DB 정상 여부는 health로 따로 확인한다. backend 종료 재현을 위해 운영 프로세스를 임의로 중지하지 않는다.

외부 `https://dev-api.midpoint.my/actuator/health`와 `/actuator/prometheus`도 403인지 확인한다. 기존 develop 업무 API·채팅과 컨테이너 메모리를 확인한다. main은 이번 수집 대상이 아니다.

수집 설정 문제가 있으면 백업 Prometheus 파일을 복원하고 Prometheus만 재생성한다. 애플리케이션 변경 문제가 있으면 기존 CI/CD 롤백 절차와 develop 외부 설정 백업을 함께 사용한다. 전체 Compose 종료·볼륨 삭제는 하지 않는다.
