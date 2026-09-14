# develop API·JVM·DB 풀 대시보드 적용

사용자가 제공한 실제 지표와 기존 데이터 소스 UID midpoint-prometheus를 사용한다. 모든 쿼리에 job="midpoint-develop", environment="develop"를 고정한다. main, Java 코드, 테스트, 서버 application-local.yml, Compose, Prometheus 수집 설정 변경은 없다.

## 화면 구성

- API: 요청률, 요청 가중 평균 응답 시간, 4xx·5xx 비율, 상태 코드별 요청률, 경로·메서드별 평균 응답 시간.
- JVM: heap 사용·확보·최대 메모리, non-heap 사용·확보 메모리, GC 빈도·평균 정지 시간, 전체·데몬·최대 스레드, 상태별 스레드.
- DB 풀: active·idle·max 연결, 사용률, pending 요청, 최근 5분 timeout 증가, 평균 연결 획득·사용 시간.
- 3개 영역, 총 18개 그래프. 기본 1시간 조회, 30초 새로고침.

HTTP는 Actuator·Swagger·API 문서 경로를 제외하지만 UNKNOWN·/**는 포함한다. 제공된 표본에는 업무 API 경로가 아직 없었으므로 실제 develop 화면을 평소처럼 사용한 뒤 경로별 그래프를 확인한다. 요청을 임의로 생성하거나 오류를 유발할 필요는 없다.

평균값은 요청 또는 이벤트가 없는 구간에서 No data가 정상이다. rate 계산에는 2회 이상 수집이 필요하며 $__rate_interval을 사용한다. 오류 시계열이 없는 경우에도 전체 요청이 관측될 때만 0으로 보완한다. 히스토그램이 확인되지 않아 p95/p99는 만들지 않았다. Hikari 사용 시간은 SQL 한 건의 실행 시간이 아니며, 풀 30개 전체가 idle인 상태는 사용률 0%다.

## 1. 파일 전송

EC2에서 임시 디렉터리 생성:

```bash
mkdir -p /opt/midpoint/monitoring-incoming/dashboard-update
```

로컬 PowerShell에서 기존 monitoringKey·monitoringHost 변수를 사용한다. 값이 없으면 키 파일 전체 경로와 EC2 퍼블릭 IP를 다시 입력한다.

```powershell
cd C:\260312_501_workspace\mp-workspace\back\MidPointAPI
scp -i "$monitoringKey" -r deploy/monitoring/grafana/provisioning/dashboards "ubuntu@${monitoringHost}:/opt/midpoint/monitoring-incoming/dashboard-update/"
```

## 2. 검증·백업·반영

EC2에서 한 줄씩 실행한다. 오류가 나오면 중단한다.

```bash
cd /opt/midpoint
python3 -m json.tool monitoring-incoming/dashboard-update/dashboards/definitions/develop-runtime.json > /dev/null
cp -a monitoring/grafana/provisioning "monitoring/grafana/provisioning.before-dashboard.$(date +%Y%m%d-%H%M%S)"
```

기존 dashboards 디렉터리에 같은 develop.yml 또는 definitions/develop-runtime.json이 있다면 먼저 내용을 비교한다. 최초 적용 기준 명령:

```bash
mkdir -p monitoring/grafana/provisioning/dashboards/definitions
cp monitoring-incoming/dashboard-update/dashboards/develop.yml monitoring/grafana/provisioning/dashboards/develop.yml
cp monitoring-incoming/dashboard-update/dashboards/definitions/develop-runtime.json monitoring/grafana/provisioning/dashboards/definitions/develop-runtime.json
docker compose restart grafana
docker compose logs --tail 80 grafana
```

기존 provisioning 디렉터리 마운트를 재사용한다. Grafana만 잠시 재시작하며 API·웹·Prometheus는 재시작하지 않는다. 로그에 provisioning 오류가 없어야 한다.

## 3. 확인

https://www.midpoint.my/grafana/d/midpoint-develop-runtime 에서 확인한다. 또는 Dashboards → Midpoint Develop 폴더를 연다.

1. 제목이 Midpoint · Develop · API / JVM / DB이고 영역 3개·패널 18개인지 확인.
2. JVM 메모리와 Hikari 연결에 값이 표시되는지 확인. 제공 표본의 풀 max는 30이며 실시간 값은 달라질 수 있다.
3. develop 앱을 평소처럼 사용하고 수집 간격이 지난 후 HTTP 요청·경로 데이터 확인.
4. 데이터가 없으면 먼저 Explore의 `up{job="midpoint-develop",environment="develop"}`가 1인지 확인. 수집 성공과 HTTP 이벤트 존재 여부는 별개다.
5. 빨간 쿼리 오류가 있으면 오류 문구를 전달. 0건 구간의 평균 No data를 장애로 단정하지 않는다.

대시보드는 파일을 기준으로 관리하며 UI 저장을 허용하지 않는다. 파일 제거가 DB 대시보드를 자동 삭제하지 않도록 disableDeletion을 사용한다. 복구 시 기존 파일이 있었다면 백업에서 해당 두 파일만 복원 후 Grafana를 재시작한다. 신규 대시보드를 완전히 제거하려면 provider 제거·재시작 후 Grafana에서 해당 UID 대시보드만 삭제한다. 기존 대시보드·데이터 소스·볼륨은 삭제하지 않는다.

## 로컬 검증 범위

JSON 파싱, 패널 ID·좌표 중복, 모든 쿼리의 develop 고정 필터와 입력 지표 이름 일치 여부를 확인한다. 서버의 PromQL 실행과 Grafana 렌더링은 적용 후 위 절차로 확인해야 한다. 테스트 코드는 추가하지 않는다.

공식 참고: https://grafana.com/docs/grafana/latest/administration/provisioning/
