<div align="center">

# 딱 중간

**모두의 중간에서 만나고, 게임으로 식당을 정한다**

출발지가 제각각인 사람들의 약속 장소를 정해주는 서비스입니다.<br>
거리가 아닌 **이동시간**으로 중간 지점을 찾고, 의견이 갈리면 **미니게임**으로 식당을 확정합니다.<br>
회원가입 없이 **링크 하나**로 참여합니다.

<br>

![Java](https://img.shields.io/badge/Java-21-007396?style=flat-square&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.8-6DB33F?style=flat-square&logo=springboot&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring%20Security-6DB33F?style=flat-square&logo=springsecurity&logoColor=white)
![MyBatis](https://img.shields.io/badge/MyBatis-000000?style=flat-square&logo=mybatis&logoColor=white)
![Oracle](https://img.shields.io/badge/Oracle-21c-F80000?style=flat-square&logo=oracle&logoColor=white)
![Gradle](https://img.shields.io/badge/Gradle-02303A?style=flat-square&logo=gradle&logoColor=white)

![React](https://img.shields.io/badge/React-19-61DAFB?style=flat-square&logo=react&logoColor=black)
![Vite](https://img.shields.io/badge/Vite-646CFF?style=flat-square&logo=vite&logoColor=white)
![Tailwind CSS](https://img.shields.io/badge/Tailwind%20CSS-06B6D4?style=flat-square&logo=tailwindcss&logoColor=white)
![STOMP](https://img.shields.io/badge/STOMP%20over%20WebSocket-010101?style=flat-square&logo=socketdotio&logoColor=white)
![Kakao](https://img.shields.io/badge/Kakao%20Map%20API-FFCD00?style=flat-square&logo=kakao&logoColor=black)
![Tmap](https://img.shields.io/badge/Tmap%20API-EE2B2B?style=flat-square)

</div>

---

## 목차

- [개요](#개요)
- [주요 기능](#주요-기능)
- [핵심 구현](#핵심-구현)
- [기술 스택](#기술-스택)
- [API](#api)
- [방 진행 단계](#방-진행-단계)
- [프로젝트 구조](#프로젝트-구조)
- [시작하기](#시작하기)
- [팀](#팀)

---

## 개요

여러 명이 모일 때 장소를 정하는 과정에는 두 가지 문제가 있습니다.

| 문제 | 딱 중간의 답 |
| --- | --- |
| 출발지가 달라 중간을 정하기 어렵다 | 참가자 전원의 **도보 이동시간**을 비교해 가장 오래 걷는 사람의 시간이 최소가 되는 지점을 선정 |
| 후보가 갈리면 결정이 늦어진다 | 진행 방식을 투표로 정하고, **미니게임 당첨자**가 고른 식당으로 확정 |

좌표상의 중간은 공정해 보이지만 실제로는 그렇지 않습니다. 강 건너편이면 한 사람만 30분을 더 걷게 되니까요.
그래서 **거리가 아니라 시간**으로 계산합니다.

![중간 지점 계산 흐름](./docs/midpoint-flow.png)

---

## 주요 기능

![주요 기능](./docs/main-features.png)

| 영역 | 기능 |
| --- | --- |
| **방 · 참가자** | 방 생성(정원 2~10명) · 링크 입장 · 지도에서 출발지 선택 · 준비 상태 · 방장 자동 승계 |
| **실시간 통신** | 실시간 채팅 · 참가자 현황 동기화 · 진행 상태 전파 · 재연결 시 놓친 메시지 보충 |
| **중간 지점 · 식당** | 이동시간 기반 중간 지점 계산 · 중간 위치 재설정 · 주변 식당 자동 수집 · 식당 검색/추가 · 식당 선택 |
| **진행 방식 · 게임** | 게임/무작위 투표 · 보물 주머니 게임 · 차례·타임아웃 처리 · 이탈 처리 · 무작위 확정 |
| **결과 · 경로 안내** | 최종 식당 확정 · 참가자별 경로 · 도보/대중교통 전환 · 구간별 안내 · 결과 공유 |

---

## 핵심 구현

### 1. 이동시간 기반 중간 지점 선정

참가자 좌표의 중심점을 구한 뒤, **주변 지하철역 3곳 + 중심점** 을 후보로 두고
후보마다 참가자 전원의 도보 시간을 조회해 **최댓값이 가장 작은 후보**를 선택합니다.

```
후보      A     B     C    최대
중심점    8분   22분  6분   22분
○○역    12분   11분  9분   12분  ← 선정
△△역     5분   26분 14분   26분
```

평균이 아니라 최악의 경우를 기준으로 보기 때문에, 누구도 지나치게 손해 보지 않습니다.
참가자가 1명이면 외부 API 를 호출하지 않고 그 위치를 그대로 씁니다.

### 2. 로그인 없는 신원 확인

회원 테이블이 없는 구조라 `roomUuid` + `participantId` 조합으로 참가자를 식별합니다.

- 입장 시 발급한 `participantId` 를 브라우저에 보관하고, 요청마다 서버가 그 방의 참가자인지 재확인
- **WebSocket 은 연결 시점에 한 번 검증해 세션에 고정** — 이후 모든 행위의 주체는 클라이언트가 보낸 값이 아니라 세션 값
- 다른 방의 ID 를 섞어 보내면 통과하지 못하고, 내부 식별자(`roomId`)는 어떤 응답에도 나가지 않습니다

![방 생성 · 입장 흐름](./docs/room-join-flow.png)

### 3. 실시간 동기화

상태가 바뀌는 전 구간을 방 단위 토픽으로 전파합니다. 커넥션은 `roomUuid + participantId` 조합마다
하나만 열어 화면들이 나눠 쓰고, 탭 전환 시에는 짧은 유예를 둬 연결이 끊기지 않게 했습니다.
연결 끊김을 이탈로 오인해 게임 차례가 넘어가는 문제를 막기 위해서입니다.

![실시간 채팅 흐름](./docs/chat-flow.png)

### 4. 당첨 위치가 새지 않는 미니게임

보물 주머니의 당첨 위치는 게임 생성 시 DB 에만 저장하고, **애플리케이션 코드는 그 값을 한 번도 읽지 않습니다.**
당첨 판정은 조건절에서 변경된 행의 수로만 합니다.

```sql
UPDATE GAME SET STATUS = 'FINISHED'
 WHERE ROOM_ID = #{roomId} AND WINNING_INDEX = #{bagIndex}
-- 변경된 행이 1이면 당첨
```

값이 응답에 섞여 나갈 경로 자체가 없고, 게임이 끝나기 전에는 `winningIndex` 가 언제나 `null` 입니다.

![게임을 통한 식당 선정](./docs/game-flow.png)

### 5. 문 앞부터의 실제 소요시간

대중교통 API 는 정류장까지만 알려줍니다. 그대로 쓰면 집에서 정류장까지 걷는 시간이 빠지므로,
**승차 전 도보 + 대중교통 + 하차 후 도보** 세 구간을 이어 붙여 총 시간을 만듭니다.
구간마다 이동수단·소요시간·안내 문구·좌표를 나눠 저장해 지도에 색을 달리 그립니다.

![최종 결과 안내](./docs/result-flow.png)

---

## 기술 스택

| 구분 | 사용 기술 |
| --- | --- |
| **Backend** | Java 21 · Spring Boot 4.0.8 · Spring Security · MyBatis · Gradle |
| **Database** | Oracle 21c |
| **Frontend** | React 19 · Vite · Tailwind CSS v4 · Axios |
| **실시간 통신** | STOMP over WebSocket (SockJS) |
| **외부 API** | 카카오 지도/로컬 · 카카오 대중교통 길찾기 · Tmap 보행자 경로 |
| **협업** | GitHub (기능 단위 PR + 코드 리뷰) · Figma · ERDCloud · Notion |

### 설계 원칙

- 계층 구조는 `Controller → Service → Mapper Interface → Mapper XML` 로 고정
- 조회는 `@Transactional(readOnly = true)`, 변경은 `@Transactional`
- **외부 API 호출은 트랜잭션 밖에서** — 응답을 기다리는 동안 DB 커넥션을 점유하지 않도록
- 동시성이 필요한 지점은 `SELECT ... FOR UPDATE` 로 방 행을 잠가 직렬화
- 응답은 `ApiResponse<T>` 로 통일, 예외는 `GlobalExceptionHandler` 가 상태코드로 변환

---

## API

### REST

| Method | Endpoint | 설명 |
| --- | --- | --- |
| `POST` | `/api/rooms` | 방 생성 |
| `GET` | `/api/rooms/{roomUuid}` | 방 조회 |
| `POST` | `/api/rooms/{roomUuid}/participants` | 방 참가 |
| `GET` | `/api/rooms/{roomUuid}/participants` | 참가자 목록 |
| `PATCH` | `/api/rooms/{roomUuid}/participants/{id}/ready` | 준비 완료 |
| `DELETE` | `/api/rooms/{roomUuid}/participants/{id}` | 방 나가기 |
| `POST` | `/api/rooms/{roomUuid}/restaurants` | 식당 등록 |
| `GET` | `/api/rooms/{roomUuid}/restaurants` | 식당 목록 |
| `GET` | `/api/rooms/{roomUuid}/restaurants/nearby` | 주변 식당 검색 |
| `POST` | `/api/rooms/{roomUuid}/participants/{id}/selection` | 식당 선택 |
| `GET` | `/api/rooms/{roomUuid}/selections` | 선택 현황 |
| `GET` | `/api/rooms/{roomUuid}/votes` | 진행 방식 투표 현황 |
| `GET` | `/api/rooms/{roomUuid}/games` | 게임 현황 |
| `GET` | `/api/rooms/{roomUuid}/routes?travelMode=` | 확정 결과 · 경로 |
| `GET` | `/api/rooms/{roomUuid}/messages` | 채팅 내역 |

### WebSocket

행위자를 세션에서 확인해야 하는 요청은 REST 가 아닌 소켓으로만 받습니다.

| 발행 (`/app`) | 설명 |
| --- | --- |
| `/chat/enter` · `/chat/send` · `/chat/leave` | 채팅 입장 · 메시지 · 퇴장 |
| `/midpoint/find` · `/midpoint/reset` | 중간 지점 찾기 · 재설정 (방장) |
| `/mode/start` · `/mode/vote` | 진행 방식 투표 시작 (방장) · 투표 |
| `/game/start` · `/game/pick` · `/game/expire` · `/game/leave` | 게임 시작 · 주머니 열기 · 차례 만료 · 이탈 |
| `/result/find` | 결과 확정 (방장) |

| 구독 (`/topic/room/{roomUuid}`) | 설명 |
| --- | --- |
| *(루트)* | 채팅 메시지 |
| `/participants` · `/participants/ready` | 새 참가자 · 참가자 목록 갱신 |
| `/restaurants` · `/selections` | 식당 목록 · 선택 현황 |
| `/midpoint` · `/midpoint/reset` | 중간 지점 확정 · 재설정 |
| `/mode` · `/game` · `/result` | 투표 현황 · 게임 현황 · 최종 결과 |
| `/*/error` | 각 도메인의 실패 사유 |

---

## 방 진행 단계

```
WAITING  →  MIDPOINT_FOUND  →  MODE_SELECTED  →  RESOLVING  →  GAME_PLAYING  →  RESOLVED
   └───── 입장 가능 ─────┘
```

| 단계 | 이 단계에서 가능한 것 |
| --- | --- |
| `WAITING` | 입장 · 중간 지점 찾기(방장) |
| `MIDPOINT_FOUND` | 입장 · 식당 등록/선택 · 준비 · 중간 위치 재설정(방장) · 투표 시작(방장) |
| `MODE_SELECTED` | 진행 방식 투표 |
| `RESOLVING` | 게임 시작(방장) · 무작위로 진행(방장) |
| `GAME_PLAYING` | 주머니 열기 · 게임 나가기 |
| `RESOLVED` | 결과 · 경로 조회 |

---

## 프로젝트 구조

```
com.kh.midpoint
├── room            방 생성 · 조회 · 단계 관리
├── participant     입장 · 준비 · 퇴장 · 방장 승계
├── point           중간 지점 계산 (후보 선정 · 도보시간 비교)
├── restaurant      식당 수집 · 등록 · 조회
├── selection       참가자별 식당 선택
├── vote            진행 방식 투표
├── game            보물 주머니 게임
├── roomresult      최종 확정 결과
├── route           경로 생성 · 조회
├── chat            실시간 채팅
├── tracking        실시간 위치 공유 (개발 중)
├── common
│   ├── config      WebSocket · Security · Cache
│   ├── exception   도메인 예외 + GlobalExceptionHandler
│   └── response    ApiResponse · SocketErrorResponseDto
└── external
    ├── kakao       로컬 검색 · 대중교통 길찾기
    └── tmap        보행자 경로
```

---

## 시작하기

### 요구사항

- JDK 21
- Oracle 21c
- 카카오 REST API 키 / JavaScript 키
- Tmap 앱 키 *(IP 제한이 걸려 있으면 사용할 IP 를 등록해야 합니다)*

### 설정 파일

두 파일은 `.gitignore` 대상이라 저장소에 올라가지 않습니다. 팀 내부에서 공유받아 `src/main/resources/` 에 두세요.

| 파일 | 내용 |
| --- | --- |
| `application-constant.yml` | 비밀이 아닌 상수 (후보 개수, 게임 규칙, 타임아웃, CORS 허용 주소 등) |
| `application-local.yml` | DB 접속 정보, 카카오·Tmap API 키 |

> 자바 쪽에 기본값을 두지 않는 규칙이라 **키가 하나라도 빠지면 기동이 실패합니다.**
> `Could not resolve placeholder ...` 가 보이면 해당 키가 없는 것이니 최신 파일을 받아 덮어쓰세요.

### 실행

```bash
# 백엔드
./gradlew bootRun

# 프론트엔드 (별도 저장소)
npm install
npm run dev
```

프론트엔드는 `.env` 에 `VITE_API_BASE_URL`, `VITE_KAKAO_JS_KEY` 가 필요합니다.
백엔드의 `cors.allowed-origin` 과 프론트 주소가 **정확히 일치**해야 합니다. 다르면 모든 요청이 CORS 로 막힙니다.

---

## 팀

| 이름 | 역할 | 담당 |
| --- | --- | --- |
| 신순주 | 팀장 | 프로젝트 총괄 · 지도 연동 · 산출물 문서 |
| 박경환 | 팀원 | 중간 지점 계산 · 방 생성/입장 · ERD 설계 · 서버 구축 · 코드 리팩토링 |
| 남지호 | 팀원 | 실시간 채팅 · UI 스타일 개선 · 화면 설계 · 산출물 문서 |
| 지세웅 | 팀원 | 미니게임 구현 · 게임 상태 관리 · 이탈 처리 |

### 협업 방식

- **기능 단위 PR + 코드 리뷰** — 기능 하나당 PR 하나를 원칙으로, 리뷰를 거쳐 `develop` 에 병합
- 주간 회의와 KPT 회고로 진행 상황 공유
- QA 테스트 케이스 188건을 작성해 우선순위별로 검증

---

<div align="center">
<sub>KDT 팀 프로젝트 · 6주</sub>
</div>
