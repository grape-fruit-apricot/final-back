#!/usr/bin/env bash
#
# 서버에서 실행되는 백엔드 배포 스크립트. 매 배포마다 jar 와 함께 새로 전송된다.
#
#   사용법: api-deploy.sh <main|develop> <git-sha>
#
# 하는 일: 새 jar 를 원자적으로 갈아끼우고 컨테이너를 다시 띄운 뒤,
# 응답이 돌아올 때까지 기다린다. 응답이 없으면 실패로 끝낸다(자동 롤백은 하지 않는다).
set -euo pipefail

ENV_NAME="${1:?첫 번째 인자로 main 또는 develop 이 필요합니다}"
SHA="${2:-unknown}"

case "$ENV_NAME" in
  main)    SERVICE=main-api;    PORT=8080 ;;
  develop) SERVICE=develop-api; PORT=8082 ;;
  *) echo "알 수 없는 환경: $ENV_NAME" >&2; exit 1 ;;
esac

BASE="/opt/midpoint/${ENV_NAME}"
COMPOSE_DIR="/opt/midpoint"
HEALTH_PATH="/v3/api-docs"   # springdoc 이 200 을 준다. actuator 가 없어 이걸 쓴다
TIMEOUT_SEC=120

echo "[1/4] 새 jar 배치 (${ENV_NAME}, ${SHA})"
test -f "${BASE}/incoming/app.jar" || { echo "받은 jar 가 없습니다" >&2; exit 1; }

# 실행 중인 JVM 이 현재 jar 를 열고 있으므로 덮어쓰지 않고 통째로 갈아끼운다.
# mv 는 같은 파일시스템 안에서 rename(2) 이라 반쪽 파일이 남지 않는다.
if [ -f "${BASE}/app.jar" ]; then
  cp -f "${BASE}/app.jar" "${BASE}/app.jar.prev"
  echo "      직전 버전을 app.jar.prev 로 보관"
fi
mv -f "${BASE}/incoming/app.jar" "${BASE}/app.jar"

echo "[2/4] 컨테이너 재시작: ${SERVICE}"
cd "$COMPOSE_DIR"
# restart 가 아니라 up -d --force-recreate 를 쓴다.
# 볼륨으로 마운트한 파일이 바뀌었을 때 restart 만으로는 컨테이너가 예전 inode 를
# 잡고 있을 수 있어서, 컨테이너를 새로 만드는 편이 확실하다.
docker compose up -d --force-recreate "$SERVICE"

echo "[3/4] 헬스체크 (최대 ${TIMEOUT_SEC}초)"
deadline=$(( $(date +%s) + TIMEOUT_SEC ))
ok=0
while [ "$(date +%s)" -lt "$deadline" ]; do
  code=$(curl -s -o /dev/null -w '%{http_code}' --max-time 3 \
         "http://127.0.0.1:${PORT}${HEALTH_PATH}" || echo 000)
  if [ "$code" = "200" ]; then ok=1; break; fi
  echo "      대기 중... (HTTP ${code})"
  sleep 3
done

if [ "$ok" -ne 1 ]; then
  echo "[!] 헬스체크 실패. 배포를 실패로 처리합니다."
  echo "----- ${SERVICE} 최근 로그 80줄 -----"
  docker compose logs --tail 80 --no-log-prefix "$SERVICE" || true
  echo "-------------------------------------"
  echo "되돌리려면 이전 커밋에서 workflow_dispatch 로 다시 배포하거나,"
  echo "서버에서 mv ${BASE}/app.jar.prev ${BASE}/app.jar 후 다시 띄우세요."
  exit 1
fi

echo "[4/4] 배포 완료: ${SERVICE} (${SHA})"
