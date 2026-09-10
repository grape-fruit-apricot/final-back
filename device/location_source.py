"""좌표를 하나씩 내주는 곳.

이 파일이 이 프로그램에서 유일하게 "좌표가 어디서 오는지"를 아는 곳이다.
tracker.py 는 LocationSource 인터페이스에만 의존하므로, 실제 GPS 모듈을 붙일 때
여기에 구현체 하나를 더 만들고 tracker.py 가 그것을 만들도록 한 줄만 바꾸면 된다.
서버와 프론트 코드는 손대지 않는다.

지금 들어 있는 것은 MockRouteSource 하나뿐이다. 라즈베리파이 본체에 GPS 모듈이 없고,
실내에서는 위성 신호를 못 받아 시연 중에 좌표가 아예 안 잡힐 수 있기 때문이다.
이 한계를 숨기지 않고 교체 지점을 인터페이스로 드러내 둔다.
"""

from abc import ABC, abstractmethod

# DB 의 좌표 컬럼이 NUMBER(8,6) / NUMBER(9,6) 이라 소수점 7자리부터는 저장될 때 잘린다.
# 보내는 값을 미리 같은 자리수로 맞춰 두면 로그에 찍힌 좌표와 DB 에 남은 좌표가 일치한다.
COORDINATE_DIGITS = 6


class LocationSource(ABC):
    """좌표 공급자. 한 참가자의 이동 한 건을 담당한다."""

    @abstractmethod
    def next(self) -> tuple[float, float]:
        """다음 좌표 (위도, 경도) 한 건."""

    @abstractmethod
    def has_next(self) -> bool:
        """더 내줄 좌표가 남아 있는지."""


class MockRouteSource(LocationSource):
    """출발점에서 목적지까지 직선을 steps 등분해 순서대로 내준다.

    마지막 좌표는 목적지와 정확히 같다. 그래야 서버의 도착 판정(기본 50m)에
    반드시 걸린다. 중간에서 끊기면 세션이 계속 MOVING 으로 남는다.

    실제 도로를 따라가지 않는다는 점은 분명한 한계다. 다만 이 프로그램이 증명하려는 것은
    "외부 기기가 좌표를 만들어 서버로 보내고 서버가 그것을 처리한다"이고,
    그 경로는 좌표가 직선이든 곡선이든 똑같이 동작한다.
    """

    def __init__(self, start: tuple[float, float], destination: tuple[float, float], steps: int):
        if steps < 1:
            raise ValueError(f"steps 는 1 이상이어야 합니다: {steps}")

        self._start = start
        self._destination = destination
        self._steps = steps
        self._sent = 0

    def has_next(self) -> bool:
        return self._sent < self._steps

    def next(self) -> tuple[float, float]:
        if not self.has_next():
            raise StopIteration("더 보낼 좌표가 없습니다.")

        self._sent += 1
        ratio = self._sent / self._steps
        latitude = self._start[0] + (self._destination[0] - self._start[0]) * ratio
        longitude = self._start[1] + (self._destination[1] - self._start[1]) * ratio

        return round(latitude, COORDINATE_DIGITS), round(longitude, COORDINATE_DIGITS)

    @property
    def progress(self) -> str:
        return f"{self._sent}/{self._steps}"
