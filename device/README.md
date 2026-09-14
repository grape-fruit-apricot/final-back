# 위치 추적 디바이스 (라즈베리파이)

확정된 약속 장소로 이동하는 참가자들의 좌표를 만들어 서버로 보낸다.
서버는 받은 좌표를 저장하고, 확정 지점까지의 거리를 계산해 50m 안에 들어오면 도착으로 표시한다.

## 지금 어떤 상태인가

라즈베리파이 본체만 있고 **GPS 모듈이 없다.** 그래서 좌표를 실제로 측정하지 않고,
출발점에서 목적지까지 직선을 등분해 만들어 보낸다(`MockRouteSource`).

이 한계를 감추지 않고 **교체 지점을 인터페이스로 드러내 두었다.**
`tracker.py` 는 좌표가 어디서 오는지 모르고 `LocationSource` 만 알고 있으므로,
실제 GPS 모듈을 붙일 때 구현체 하나만 추가하면 **서버와 프론트는 한 줄도 바뀌지 않는다.**

```
LocationSource (인터페이스)
├── MockRouteSource     지금 쓰는 것 — 직선 보간
└── GpsModuleSource     나중에 추가할 것 — NEO-6M 등
```

## 설치

라즈베리파이 OS 에는 파이썬 3 이 이미 들어 있다. 필요한 것은 `requests` 하나다.

```bash
sudo apt install -y python3-requests
# 또는
pip3 install -r requirements.txt
```

## 설정

```bash
cp config.example.json config.json
```

| 키 | 설명 |
| --- | --- |
| `apiBaseUrl` | 서버 주소. 개발 `https://dev-api.midpoint.my` / 운영 `https://api.midpoint.my` |
| `deviceKey` | **서버의 `application-local.yml` 에 넣은 `tracking.device-key` 와 같은 값.** 다르면 좌표 전송이 401 로 거절된다 |
| `steps` | 출발점에서 목적지까지 몇 번에 나눠 갈지. 클수록 움직임이 부드럽다 |
| `pollIntervalSeconds` | 대기 중일 때 배정을 확인하는 간격(초) |
| `intervalMsOverride` | 좌표 전송 간격(ms). 비워 두면 서버가 알려준 값(3000)을 쓴다 |

`config.json` 은 `deviceKey` 가 들어가므로 **커밋하지 않는다**(`.gitignore` 에 등록되어 있다).

> 시연 시간은 `steps × 간격` 이다. 기본값 40스텝 × 1.5초 = **약 60초**.
> 스텝을 늘리면 부드러워지고, 간격을 줄이면 짧아진다.

## 실행

```bash
python3 tracker.py
```

```
16:52:39 INFO 대기 시작 - https://dev-api.midpoint.my / deviceKey=local-rpi-001
16:52:39 INFO 배정 없음. 방장이 이동 추적을 시작할 때까지 기다립니다.
        ← 여기서 앱의 방장이 "이동 추적 시작" 을 누른다
16:53:02 INFO 전송 시작 - 4명 / 40스텝 / 1.5초 간격 (경환, 지호, 민수, 서연)
16:53:02 INFO   경환 2989m
16:53:02 INFO   지호 4988m
...
16:54:09 INFO   경환 도착
16:54:09 INFO 전원 도착. 대기 상태로 돌아갑니다.
```

**미리 켜 두고 방치하면 된다.** 방장이 앱에서 시작을 누를 때까지 기다리다가 알아서 붙는다.
서버가 디바이스에게 먼저 말을 걸 수 없어서(디바이스가 연결을 열고 있지 않다) 디바이스 쪽이 물어보는 구조다.

`Ctrl+C` 로 종료한다.

## 부팅 시 자동 실행 (선택)

```bash
sudo tee /etc/systemd/system/midpoint-tracker.service > /dev/null <<'UNIT'
[Unit]
Description=Midpoint location tracker
After=network-online.target
Wants=network-online.target

[Service]
Type=simple
User=pi
WorkingDirectory=/home/pi/final-back/device
ExecStart=/usr/bin/python3 /home/pi/final-back/device/tracker.py
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
UNIT

sudo systemctl daemon-reload
sudo systemctl enable --now midpoint-tracker
journalctl -u midpoint-tracker -f      # 로그 보기
```

## 알아둘 것

**전송 순번(`seq`)은 1 부터 세지 않고 현재 시각(초)에서 시작한다.**
서버가 같은 순번을 두 번 저장하지 않도록 막고 있어서, 프로그램이 중간에 꺼졌다 켜지면
1 부터 다시 세는 방식은 전부 중복으로 거절된다. 시각 기준이면 재시작해도 겹치지 않는다.
(전송 간격이 1초 이상이어야 성립한다)

**이동 중인 방이 여러 개면 가장 최근 방 하나만 재생한다.**
서버는 이 키로 배정된 "이동 중인" 세션을 전부 돌려주므로, 예전에 시작해 두고 끝내지 않은 방이
남아 있으면 그것까지 함께 움직이게 된다. 기기는 한 대이고 한 번에 한 방을 시연하므로
가장 나중에 만들어진 방만 남긴다. 나머지는 건드리지 않고 그대로 둔다.

**한 방에서 추적을 두 번 재생할 수 없다.**
도착한 세션은 상태가 `ARRIVED` 로 굳고 되돌아가지 않는다(서버가 그렇게 막고 있다).
리허설할 때는 방을 새로 만든다.

**전송이 실패하면 그 좌표는 건너뛴다.**
다음 주기에 이어서 보내므로 지도에서 한 칸 건너뛴 것처럼 보일 뿐이다.
목적지 좌표를 못 보내 도착 처리가 안 되면, 대기로 돌아갔다가 같은 배정을 다시 받아
처음부터 재생한다.

## 실제 GPS 모듈을 붙이려면

1. `location_source.py` 에 구현체를 추가한다.

```python
class GpsModuleSource(LocationSource):
    def __init__(self, serial_port):
        ...  # NMEA 문장을 읽어 위경도를 뽑는다

    def has_next(self) -> bool:
        return True          # 실제 기기는 계속 좌표를 낸다

    def next(self) -> tuple[float, float]:
        ...                  # 최근 측정값
```

2. `tracker.py` 의 `build_routes()` 에서 만드는 클래스를 바꾼다. 그 함수 하나뿐이다.

3. 서버와 프론트는 **바꿀 것이 없다.** 좌표의 출처가 무엇이든 API 는 같다.

실제 좌표를 쓸 때는 서버 쪽에 이상값 필터(직전 좌표에서 비현실적으로 튄 값 버리기)를
추가하는 것을 검토한다. 지금은 모의 좌표라 값이 튀지 않아 넣지 않았다.
