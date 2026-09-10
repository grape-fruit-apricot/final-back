#!/usr/bin/env python3
"""확정된 약속 장소로 이동하는 참가자들의 좌표를 서버로 보내는 디바이스 프로그램.

동작 순서
  1. config.json 을 읽는다.
  2. 서버에 "내 키로 배정된 추적 세션이 있나" 를 주기적으로 물어보며 대기한다.
  3. 방장이 앱에서 이동 추적을 시작하면 배정이 생기고, 그것을 받아 전송을 시작한다.
  4. 서버가 알려준 주기마다 참가자 전원의 좌표를 한 번에 보낸다.
  5. 전원이 도착하면 다시 대기 상태로 돌아간다.

서버가 디바이스에게 먼저 말을 걸 수 없어서(디바이스가 연결을 열고 있지 않다)
디바이스 쪽이 물어보는 구조다. 덕분에 발표 중에 이 프로그램을 손댈 일이 없다.

거리 계산과 도착 판정은 여기서 하지 않는다. 전부 서버가 한다.
디바이스가 "도착했다"를 보내는 구조면 그 값을 조작하는 것으로 결과가 바뀌기 때문이다.
"""

import json
import logging
import sys
import time
from pathlib import Path

import requests

from location_source import MockRouteSource

CONFIG_PATH = Path(__file__).with_name("config.json")

log = logging.getLogger("tracker")


def load_config() -> dict:
    if not CONFIG_PATH.exists():
        sys.exit(
            f"{CONFIG_PATH.name} 이 없습니다. config.example.json 을 복사해서 값을 채우세요.\n"
            f"  cp {CONFIG_PATH.parent}/config.example.json {CONFIG_PATH}"
        )

    config = json.loads(CONFIG_PATH.read_text(encoding="utf-8"))
    for key in ("apiBaseUrl", "deviceKey"):
        if not config.get(key):
            sys.exit(f"config.json 에 {key} 가 필요합니다.")

    config.setdefault("steps", 40)
    config.setdefault("pollIntervalSeconds", 3)
    # 서버가 알려준 주기(intervalMs)를 그대로 쓰는 것이 기본이다.
    # 시연 시간을 줄이고 싶을 때만 여기서 덮어쓴다.
    config.setdefault("intervalMsOverride", None)
    config["apiBaseUrl"] = config["apiBaseUrl"].rstrip("/")

    return config


def fetch_assignments(http: requests.Session, config: dict) -> list[dict]:
    """서버에 배정을 물어본다. 없으면 빈 목록이다."""
    url = f"{config['apiBaseUrl']}/api/tracking/devices/{config['deviceKey']}/sessions"
    response = http.get(url, timeout=5)
    response.raise_for_status()

    return response.json().get("data", [])


def send_locations(http: requests.Session, config: dict, points: list[dict]) -> list[dict] | None:
    """좌표를 한 번에 보낸다. 실패하면 None 을 돌려주고 호출한 쪽이 다음 주기에 이어간다."""
    url = f"{config['apiBaseUrl']}/api/tracking/locations"
    body = {"deviceKey": config["deviceKey"], "points": points}

    try:
        response = http.post(url, json=body, timeout=5)
    except requests.RequestException as error:
        log.warning("전송 실패(네트워크) - %s", error)
        return None

    if response.status_code == 401:
        # 서버에 저장된 키와 다르다. 계속 보내봐야 전부 거절당하므로 대기로 돌아간다.
        log.error("디바이스 키가 서버와 다릅니다. config.json 의 deviceKey 를 확인하세요.")
        return None

    if response.status_code == 409:
        # 같은 순번이 이미 저장돼 있다. 순번은 시각 기준이라 다음 주기에는 겹치지 않는다.
        log.warning("이미 저장된 순번입니다. 다음 좌표로 넘어갑니다.")
        return None

    if not response.ok:
        log.warning("전송 실패(%s) - %s", response.status_code, response.text[:200])
        return None

    return response.json().get("data", [])


def pick_latest_room(assignments: list[dict]) -> list[dict]:
    """배정이 여러 방에 걸쳐 있으면 가장 최근 방 하나만 고른다.

    서버는 이 키로 배정된 "이동 중인" 세션을 전부 돌려준다. 그래서 예전에 시작해 두고
    끝내지 않은 방이 남아 있으면 그것까지 함께 재생하게 된다. 기기는 한 대이고
    한 번에 한 방을 시연하므로, 여기서 가장 나중에 만들어진 방(세션 번호가 가장 큰 방)만 남긴다.

    남은 방들은 서버에 그대로 있다가, 방이 만료되어 정리될 때 함께 사라진다.
    """
    rooms: dict[str, list[dict]] = {}
    for assignment in assignments:
        rooms.setdefault(assignment["roomUuid"], []).append(assignment)

    latest = max(rooms, key=lambda room: max(a["sessionId"] for a in rooms[room]))
    if len(rooms) > 1:
        log.warning(
            "이동 중인 방이 %d개라 가장 최근 방(%s)만 재생합니다. 나머지는 건드리지 않습니다.",
            len(rooms), latest,
        )

    return rooms[latest]


def build_routes(assignments: list[dict], steps: int) -> dict[int, MockRouteSource]:
    """배정마다 좌표 공급자를 하나씩 만든다.

    실제 GPS 모듈을 붙일 때 바꾸는 곳은 이 한 줄이다.
    나머지 코드는 LocationSource 인터페이스만 알고 있다.
    """
    return {
        assignment["sessionId"]: MockRouteSource(
            start=(assignment["startLat"], assignment["startLng"]),
            destination=(assignment["destLat"], assignment["destLng"]),
            steps=steps,
        )
        for assignment in assignments
    }


def run_assignments(http: requests.Session, config: dict, assignments: list[dict]) -> None:
    routes = build_routes(assignments, config["steps"])
    names = {a["sessionId"]: a["nickname"] for a in assignments}

    interval_ms = config["intervalMsOverride"] or assignments[0]["intervalMs"]
    interval_seconds = interval_ms / 1000

    # 전송 순번의 시작값을 현재 시각(초)으로 잡는다. 프로그램이 중간에 꺼졌다 켜져도
    # 이전에 쓴 순번과 겹치지 않아, 중복 저장 방지 제약(UK_LOCATION_LOG_SEQ)에 걸리지 않는다.
    # 전송 간격이 1초 이상이면 재시작 전후로 값이 겹칠 수 없다.
    sequences = {session_id: int(time.time()) for session_id in routes}

    log.info(
        "전송 시작 - %d명 / %d스텝 / %.1f초 간격 (%s)",
        len(routes), config["steps"], interval_seconds, ", ".join(names.values()),
    )

    while routes:
        points = []
        for session_id, source in list(routes.items()):
            if not source.has_next():
                routes.pop(session_id)
                continue

            latitude, longitude = source.next()
            sequences[session_id] += 1
            points.append({
                "sessionId": session_id,
                "seq": sequences[session_id],
                "lat": latitude,
                "lng": longitude,
            })

        if not points:
            break

        results = send_locations(http, config, points)
        if results is not None:
            for result in results:
                remaining = "도착" if result["status"] == "ARRIVED" else f"{round(result['distanceM'])}m"
                log.info("  %s %s", names.get(result["sessionId"], result["sessionId"]), remaining)
                if result["status"] == "ARRIVED":
                    routes.pop(result["sessionId"], None)

        if routes:
            time.sleep(interval_seconds)

    if routes:
        # 마지막 좌표는 목적지와 같아서 정상 흐름에서는 여기 오지 않는다.
        # 전송이 실패해 도착 좌표를 못 보낸 경우인데, 서버가 아직 MOVING 으로 들고 있으므로
        # 대기로 돌아가면 같은 배정을 다시 받아 처음부터 재생한다.
        log.warning("도착하지 못한 세션 %d건 - 대기 상태로 돌아가 다시 시도합니다.", len(routes))
    else:
        log.info("전원 도착. 대기 상태로 돌아갑니다.")


def main() -> None:
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s %(levelname)s %(message)s",
        datefmt="%H:%M:%S",
    )

    config = load_config()
    http = requests.Session()

    log.info("대기 시작 - %s / deviceKey=%s", config["apiBaseUrl"], config["deviceKey"])

    waiting_logged = False
    while True:
        try:
            assignments = fetch_assignments(http, config)
        except requests.RequestException as error:
            log.warning("배정 조회 실패 - %s", error)
            time.sleep(config["pollIntervalSeconds"])
            continue

        if not assignments:
            if not waiting_logged:
                log.info("배정 없음. 방장이 이동 추적을 시작할 때까지 기다립니다.")
                waiting_logged = True
            time.sleep(config["pollIntervalSeconds"])
            continue

        waiting_logged = False
        run_assignments(http, config, pick_latest_room(assignments))


if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print()
        log.info("종료합니다.")
