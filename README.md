<div align="center">

# 딱 중간

**출발지가 다른 사람들의 만남 장소와 식당을 하나의 흐름으로 정해주는 서비스**

거리가 아닌 이동시간으로 중간 지점을 찾고,
참여 · 선택 · 게임 · 결과 확정 · 경로 안내 · 이동 추적까지 한 방 안에서 이어집니다.

<br>

![Java](https://img.shields.io/badge/Java-21-007396?style=flat-square&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.8-6DB33F?style=flat-square&logo=springboot&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring%20Security-6DB33F?style=flat-square&logo=springsecurity&logoColor=white)
![MyBatis](https://img.shields.io/badge/MyBatis-4.0.1-000000?style=flat-square&logo=mybatis&logoColor=white)
![Oracle](https://img.shields.io/badge/Oracle-21c-F80000?style=flat-square&logo=oracle&logoColor=white)
![Gradle](https://img.shields.io/badge/Gradle-9.5.1-02303A?style=flat-square&logo=gradle&logoColor=white)
![STOMP](https://img.shields.io/badge/STOMP%20over%20WebSocket-010101?style=flat-square&logo=socketdotio&logoColor=white)
![Caffeine](https://img.shields.io/badge/Caffeine%20Cache-2E7D32?style=flat-square)
![Swagger](https://img.shields.io/badge/springdoc%20OpenAPI-85EA2D?style=flat-square&logo=swagger&logoColor=black)
![Kakao](https://img.shields.io/badge/Kakao%20Local%20%C2%B7%20Transit-FFCD00?style=flat-square&logo=kakao&logoColor=black)
![Tmap](https://img.shields.io/badge/Tmap%20Pedestrian-EE2B2B?style=flat-square)
![Raspberry Pi](https://img.shields.io/badge/Raspberry%20Pi%204B-C51A4A?style=flat-square&logo=raspberrypi&logoColor=white)
![AWS EC2](https://img.shields.io/badge/Amazon%20EC2-FF9900?style=flat-square&logo=amazonec2&logoColor=white)
![AWS ALB](https://img.shields.io/badge/Application%20Load%20Balancer-8C4FFF?style=flat-square&logo=awselasticloadbalancing&logoColor=white)
![ACM](https://img.shields.io/badge/AWS%20Certificate%20Manager-DD344C?style=flat-square&logo=amazonwebservices&logoColor=white)
![GitHub Actions](https://img.shields.io/badge/GitHub%20Actions-2088FF?style=flat-square&logo=githubactions&logoColor=white)

<br>

**2026.08 - 2026.09 · 4인 팀 프로젝트**

[프로젝트 개요](#1-프로젝트-개요) · [핵심 흐름](#2-핵심-사용자-흐름) · [구현 포인트](#구현-포인트) · [배포 환경](#6-운영--배포-환경)

</div>

---

## 1. 프로젝트 개요

### 1.1 기획 배경

여러 명이 모일 때 장소를 정하는 과정은 대화로 조율되고, 그만큼 시간이 걸립니다.
출발지가 다르면 누군가는 항상 더 멀리 이동하게 되는데도, 그 사실이 드러나지 않은 채 결정되는 경우가 많습니다.

**딱 중간**은 이 결정을 계산으로 대신하고, 남은 선택은 게임으로 끝내는 것을 목표로 개발했습니다.

<table>
  <thead>
    <tr><th>기존 불편</th><th>딱 중간의 접근</th><th>기대 효과</th></tr>
  </thead>
  <tbody>
    <tr><td>출발지가 달라 중간을 정하기 어려움</td><td>참가자 전원의 이동시간을 비교해 지점 선정</td><td>특정 참가자에게 불리한 장소를 방지</td></tr>
    <tr><td>후보가 갈리면 결정이 계속 늦어짐</td><td>진행 방식 투표 후 미니게임으로 확정</td><td>조율 없이 한 번에 결론</td></tr>
    <tr><td>정하고 나서도 각자 길을 찾아야 함</td><td>참가자별 경로와 실제 소요시간 안내</td><td>약속 직전까지 필요한 정보 제공</td></tr>
    <tr><td>가입해야 쓸 수 있는 서비스</td><td>링크 하나로 즉시 참여</td><td>진입 장벽 없음</td></tr>
  </tbody>
</table>

### 1.2 주요 사용자

<table>
  <thead>
    <tr><th>사용자</th><th>주요 목적</th></tr>
  </thead>
  <tbody>
    <tr><td><b>방장</b></td><td>방 생성과 링크 공유, 중간 지점 계산 실행, 진행 방식 투표 시작, 게임 시작, 경로 추적</td></tr>
    <tr><td><b>참가자</b></td><td>링크로 입장, 출발지 선택, 식당 검색 · 선택, 투표, 게임 참여, 경로 확인</td></tr>
  </tbody>
</table>

### 1.3 서비스 소개

다음 단계가 앞 단계의 결과 위에서만 열리도록 설계했습니다.

> **방 생성 · 링크 입장 → 중간 지점 → 식당 선택 → 진행 방식 투표 → 게임 → 결과 확정 → 경로 안내 → 이동 추적**

회원 테이블 없이 `roomUuid` + `participantId` 조합으로 참가자를 식별하고,
React SPA와 Spring Boot REST API를 분리해 프론트엔드와 백엔드를 독립적으로 개발했습니다.

---

## 2. 핵심 사용자 흐름

<p align="center">
  <img width="2000" height="1050" alt="image" src="https://github.com/user-attachments/assets/5d0cbeb3-2df0-4b60-8490-45222bcdb713" />
</p>

방의 진행 단계는 `WAITING`, `MIDPOINT_FOUND`, `MODE_SELECTED`, `RESOLVING`, `GAME_PLAYING`, `RESOLVED` 로 관리하며,
**입장이 허용되는 구간은 앞의 두 단계까지**입니다. 단계 전이와 결과 확정은 모두 서버가 판단합니다.

---

## 3. 주요 기능

<table>
  <thead>
    <tr><th>기능 영역</th><th>주요 기능</th></tr>
  </thead>
  <tbody>
    <tr><td>방 · 참가자</td><td>방 생성(정원 2~10명), 링크 입장, 지도에서 출발지 선택, 준비 상태, 방장 자동 승계</td></tr>
    <tr><td>실시간 통신</td><td>실시간 채팅, 참가자 현황 동기화, 진행 상태 전파, 재연결 시 놓친 메시지 보충</td></tr>
    <tr><td>중간 지점 · 식당</td><td>이동시간 기반 중간 지점 계산, 중간 위치 재설정, 주변 식당 자동 수집, 검색 · 추가, 선택</td></tr>
    <tr><td>진행 방식 · 게임</td><td>게임 · 무작위 투표, 보물 주머니 게임, 차례 · 타임아웃 처리, 이탈 처리, 무작위 확정</td></tr>
    <tr><td>결과 · 경로</td><td>최종 식당 확정, 참가자별 경로, 도보 · 대중교통 전환, 구간별 안내, 결과 공유</td></tr>
    <tr><td>이동 추적</td><td>라즈베리파이 좌표 수신, 남은 거리 계산, 도착 판정(50m), 카카오맵 표시</td></tr>
  </tbody>
</table>

---

## 4. 핵심 구현 상세

### 4.1 이동시간 기반 중간 지점 선정

<p align="center">
  <img width="3040" height="2080" alt="image" src="https://github.com/user-attachments/assets/26860102-2737-402e-904c-2994bb1c42a7" />
</p>

참가자 좌표의 중심점을 구한 뒤 **주변 지하철역 3곳 + 중심점** 을 후보로 두고,
후보마다 참가자 전원의 도보 시간을 조회해 **최댓값이 가장 작은 후보**를 선택합니다.

```
후보      A     B     C    최대
중심점    8분   22분  6분   22분
○○역    12분   11분  9분   12분  ← 선정
△△역     5분   26분 14분   26분
```

- 후보마다 **가장 오래 걷는 사람이 몇 분 걸리는지**를 보고, 그 시간이 가장 짧은 곳을 고릅니다
- 한 명이라도 도보 경로를 찾지 못한 후보는 탈락시키고, 모든 후보가 실패하면 404로 알립니다
- 참가자가 1명이면 외부 API를 호출하지 않고 그 좌표를 그대로 씁니다
- 중간 지점이 정해지면 곧바로 주변 식당을 모아옵니다. 식당을 못 가져와도 **이미 정해진 중간 지점은 그대로 남습니다**

### 4.2 로그인 없는 참가자 확인

회원 테이블이 없으므로 `roomUuid` + `participantId` 조합으로 참가자를 식별하고, 요청마다 재확인합니다.

```java
// A방 uuid + B방 participantId 로 남의 방 참가자를 사칭하는 것을 막는다
private ParticipantResponseDto findParticipantInRoom(String roomUuid, Long participantId) {
    RoomResponseDto room = roomService.findRoom(roomUuid);
    ParticipantResponseDto participant = participantMapper.findParticipant(participantId);
    if (participant == null || !participant.getRoomId().equals(room.getRoomId())) {
        throw new NotFoundException("존재하지 않는 참가자입니다: " + participantId);
    }
    return participant;
}
```

소켓은 **연결되는 순간 딱 한 번** 누구인지 확인하고, 그 결과를 서버가 들고 있습니다.
이후에는 연결할 때 확인해 둔 값으로만 판단합니다.
그래서 남의 이름으로 메시지를 보내거나 남의 차례를 대신 쓸 수 없습니다.

### 4.3 실시간 채팅

<p align="center">
  <img width="3040" height="2080" alt="image" src="https://github.com/user-attachments/assets/d06a3ea9-3704-4d3e-98f9-f81d507f89f6" />
</p>

상태가 바뀌는 전 구간을 방 단위 토픽으로 전파합니다. 바뀐 부분만 보내지 않고 **갱신된 현황 전체**를 내려주기 때문에,
중간에 메시지를 놓친 참가자도 다음 한 번의 수신으로 화면이 다시 맞춰집니다.

연결 끊김과 실제 이탈은 구분합니다. 끊김은 게임 차례만 넘기고 참가자는 게임에 남겨둡니다.
새로고침이나 탭 이동만으로도 연결은 끊기므로, 이를 이탈로 처리하면 정상 참여자가 탈락하기 때문입니다.

### 4.4 미니게임

<p align="center">
  <img width="3040" height="2080" alt="image" src="https://github.com/user-attachments/assets/6e95cc39-2853-4934-a191-ba89bb23fae7" />
</p>

당첨 위치는 게임을 만들 때 **DB에만 넣어두고, 서버 코드는 그 번호를 꺼내 보지 않습니다.**
대신 "내가 고른 번호가 당첨 번호와 같으면 게임을 끝내라" 는 명령을 DB에 보내고,
실제로 바뀐 줄이 1줄이면 당첨으로 봅니다. 서버가 값을 들고 있지 않으니 실수로 화면에 흘러나갈 일이 없습니다.

```java
// 당첨 위치를 자바로 읽지 않는다. 바뀐 행 수가 1이면 당첨이다.
if (gameMapper.updateGameFinished(roomId, requestDto.getBagIndex()) == 1) {
    gameMapper.updateGameWinner(participantId);
    roomService.updateStage(roomId, stageResolving);
    return findGameStatus(roomUuid);
}
```

조회 쿼리도 게임이 끝난 뒤에만 당첨 위치를 내보냅니다.

```sql
CASE WHEN G.STATUS = 'FINISHED' THEN G.WINNING_INDEX END AS WINNING_INDEX
```

남은 시간도 브라우저 시계가 아니라 **DB 시계로 계산**해 내려줍니다. 기기별 시차가 게임에 반영되지 않도록 했습니다.

### 4.5 최종 결과 안내

<p align="center">
  <img width="3040" height="2080" alt="image" src="https://github.com/user-attachments/assets/cafe8b06-87bd-4271-9052-2933b62660c8" />
</p>

대중교통 길찾기는 "어느 정류장에서 타서 어디서 내려라" 까지만 알려줍니다.
선택한 위치에서 정류장까지 걷는 시간, 내려서 식당까지 걷는 시간은 빠져 있습니다.
그래서 **걷기 + 타고 가기 + 걷기** 세 토막을 이어 붙여 선택한 위치에서 식당까지 걸리는 진짜 시간을 만듭니다.

토막마다 이동수단 · 걸리는 시간 · 안내 문구 · 좌표를 따로 저장해, 
지도에서 걷는 구간과 타고 가는 구간을 다른 색으로 표시했습니다.

---

## 5. 시스템 아키텍처

<p align="center">
  <img width="1000" alt="시스템 아키텍처" src="https://github.com/user-attachments/assets/15ee02ec-c7c0-4d71-ab0f-3833599bc87c" />
</p>

---

## 기술 스택

| 구분 | 기술 스택 | 활용 내용 |
| :--- | :--- | :--- |
| **Backend** | Java **21**, Spring Boot **4.0.8**, Spring Web, Spring Validation | REST API 개발, 계층형 아키텍처(Controller-Service-Mapper), 비즈니스 규칙 처리 및 입력값 검증 |
| **Security** | Spring Security | CORS · CSRF 정책 관리, 소켓 연결 시 참가자 검증 |
| **Persistence** | MyBatis Spring Boot Starter **4.0.1**, JDBC | SQL Mapper 기반 데이터 접근, 동적 SQL, 트랜잭션 및 행 잠금 처리 |
| **Database** | Oracle Database **21c** | 방 · 참가자 · 식당 · 선택 · 투표 · 게임 · 경로 · 채팅 데이터 관리, 시퀀스 및 제약조건 |
| **실시간 통신** | STOMP over WebSocket, SockJS | 방 단위 토픽 브로드캐스트, CONNECT 헤더 검증, 세션 기반 행위자 식별 |
| **Cache** | Spring Cache, Caffeine | 좌표 기준 외부 API 응답 캐싱으로 중복 호출 감소 |
| **외부 API** | 카카오 로컬, 카카오 대중교통, Tmap 보행자 경로 | 후보 지하철역 검색, 주변 식당 수집, 도보 · 대중교통 경로 산출 |
| **Build** | Gradle **9.5.1** | 백엔드 빌드 및 의존성 관리, bootJar 산출물 이름 고정 |
| **IoT** | Raspberry Pi 4 Model B Rev 1.5 | 이동 중 좌표 수집 및 서버 전송 |
| **Infra** | AWS EC2, ALB, ACM, Route 53 | 서버 배포, TLS 종료 및 Host 기반 분기, 인증서 1장에 도메인 5개 등록 |
| **CI/CD** | GitHub Actions | PR 빌드 검증, 시크릿 유출 검사, 원자적 배포 및 롤백 |
| **Collaboration** | Git, GitHub, Postman, Figma, ERDCloud, Notion, slack | 브랜치 기반 형상 관리, PR 협업, API 테스트, 설계 산출물 관리 |

### 설계 원칙

- **Controller - Service - Mapper** 계층을 분리하고, 같은 작업은 세 계층이 같은 메서드명(`findXxx` · `insertXxx` · `updateXxx`)을 씁니다.
- **외부 API 호출은 트랜잭션 밖에서** 수행합니다. 응답을 기다리는 동안 DB 커넥션을 붙잡으면 무관한 요청까지 커넥션 대기로 죽습니다.
- 내부 식별자 `roomId` 는 어떤 REST 응답에도, 어떤 소켓 페이로드에도 노출하지 않습니다.
- 동시성이 필요한 지점은 `SELECT ... FOR UPDATE` 로 방 행을 잠가 직렬화하고, 잠금 순서를 `ROOM → GAME` 으로 고정해 교착을 피합니다.
- 상수는 자바 쪽에 기본값을 두지 않고 `application-constant.yml` 한 곳에서만 관리합니다.

---

## 6. 운영 · 배포 환경

| 구분 | 적용 기술 | 적용 내용 |
| :--- | :--- | :--- |
| 서버 환경 | AWS EC2 | EC2 1대에 운영 · 개발 환경을 함께 구성 |
| 트래픽 처리 | AWS Application Load Balancer | TLS 종료 후 **Host 헤더 기준 4갈래 분기** |
| HTTPS | AWS Certificate Manager | 인증서 1장에 도메인 5개를 등록(SAN) 해 ALB 리스너에 연결 |
| 도메인 | Route 53 | 도메인을 ALB에 연결 |
| 배포 | GitHub Actions | 원자적 교체 방식 배포, 직전 버전 보관으로 롤백 가능 |
| 품질 게이트 | GitHub Actions | PR 단계에서 **빌드 검증 + 시크릿 유출 검사** 동시 수행 |
| IoT | Raspberry Pi 4 Model B | 이동 중 좌표를 주기적으로 서버에 전송 |

### 배포 설계에서 신경 쓴 것

- 운영과 개발을 **같은 인스턴스에 두되 도메인으로 갈랐습니다.** ALB가 Host 헤더를 보고 분기하므로 인스턴스를 늘리지 않고도 환경이 섞이지 않습니다.
- **인증서 한 장에 도메인 5개를 추가 이름(SAN)으로 등록**해 관리 지점을 하나로 만들었습니다.
- 배포는 파일을 덮어쓰지 않고 **교체**합니다. 중간 상태가 노출되지 않고, 직전 버전이 남아 있어 되돌리는 데 재빌드가 필요 없습니다.
- 시크릿 검사를 PR 단계에 둔 이유는, 병합된 뒤에 발견하면 이미 이력에 남기 때문입니다.

---

## 구현 포인트

<table>
  <tbody>
    <tr>
      <td width="180"><b>하나의 흐름</b></td>
      <td>방 생성 · UUID 링크 입장 → 중간 지점 → 식당 선택 → 게임 → 결과 확정 → 경로 탐색 → 이동 추적을 <b>한 방 안에서 이어지도록</b> 구성했습니다. 단계 전이와 결과 확정은 서버가 관리합니다.</td>
    </tr>
    <tr>
      <td><b>채팅</b></td>
      <td>흐름이 진행되는 동안 참가자끼리 대화합니다. 방 탭과 채팅 탭이 <b>같은 연결을 공유</b>해, 탭을 옮겨도 이탈로 처리되지 않습니다.</td>
    </tr>
    <tr>
      <td><b>STOMP 연동</b></td>
      <td>지도 · 채팅 · 게임 · 식당 선택을 <b>연결 하나 위에</b> 올리고, 목적지 · 구독 · 예외 경로를 같은 규칙으로 통일했습니다.</td>
    </tr>
    <tr>
      <td><b>이동 추적</b></td>
      <td>라즈베리파이는 <b>좌표만 보내고</b>, 남은 거리와 도착 판정(50m)은 서버가 계산합니다. 프론트는 결과를 카카오맵에 표시합니다.</td>
    </tr>
    <tr>
      <td><b>CI/CD</b></td>
      <td>PR 단계에서 <b>빌드와 시크릿 유출을 함께 검사</b>하고, 배포는 원자적 교체와 직전 버전 보관으로 되돌릴 수 있게 구성했습니다.</td>
    </tr>
    <tr>
      <td><b>AWS 배포 환경</b></td>
      <td>EC2 1대에 운영 · 개발을 함께 올리고, ALB가 TLS를 끝낸 뒤 <b>Host 헤더로 4갈래 분기</b>합니다. ACM 인증서 1장에 도메인 5개를 등록해 HTTPS를 처리합니다</td>
    </tr>
  </tbody>
</table>

---

## 7. 개발 기간 및 팀 구성

- **기간:** 2026.08 - 2026.09 (6주)
- **인원:** 4명
- **방식:** 프론트엔드 · 백엔드 저장소 분리, 기능별 브랜치와 Pull Request 기반 협업

| 팀원 | 주요 담당 | GitHub |
| :---: | :--- | :---: |
| **신순주** | 프로젝트 총괄 · 일정 관리, 지도 연동, 중간 지점 계산, 산출물 문서 총괄 | [![GitHub](https://img.shields.io/badge/GitHub-grape--fruit--apricot-F59E0B?style=flat-square&logo=github&logoColor=white)](https://github.com/grape-fruit-apricot) |
| **박경환** | 방 생성 · 입장, ERD 설계, 서버 구축, 코드 리팩토링, 미니게임 구현 | [![GitHub](https://img.shields.io/badge/GitHub-ghksl0204--shapa-22C55E?style=flat-square&logo=github&logoColor=white)](https://github.com/ghksl0204-shapa) |
| **남지호** | 실시간 채팅, WebSocket 세션 관리, UI 스타일 개선, 화면 설계, 산출물 문서 작성 | [![GitHub](https://img.shields.io/badge/GitHub-jiho0828-FF6B6B?style=flat-square&logo=github&logoColor=white)](https://github.com/jiho0828) |
| **지세웅** | 미니게임 설계, 테스트 검증, 오류 케이스 작성  | [![GitHub](https://img.shields.io/badge/GitHub-CU0--0-3B82F6?style=flat-square&logo=github&logoColor=white)](https://github.com/CU0-0) |

### 협업 방식

- 기능 하나당 `feature` 브랜치 하나를 만들고, Pull Request와 코드 리뷰를 거쳐 `develop` 에 병합했습니다.
- 처음에는 완성된 기능을 한 번에 병합하려 했으나, 리뷰 부담이 크다는 의견을 반영해 **기능 단위 PR로 전환**했습니다.
- 주간 회의와 KPT 회고로 진행 상황과 충돌 가능성을 공유했습니다.
- API 요청 · 응답 규격과 DTO 구조를 먼저 합의한 뒤 화면을 연결했습니다.
- 공통 응답(`ApiResponse`), 예외 처리, 소켓 연결 등 반복 요소를 공용 모듈로 관리했습니다.

---

## 8. 개발 산출물

| 산출물 | 결과 |
| :--- | ---: |
| 백엔드 도메인 | **10개** |
| REST API | **15개** |
| WebSocket 발행 경로 | **12개** |
| 데이터베이스 테이블 | **15개** |
| 프론트엔드 화면 | **7종** |
| QA 테스트 케이스 | **188건** |
| Pull Request | **65개** |

### 설계 산출물


### 설계 산출물

| 산출물 | 도구 | 내용 |
| :--- | :--- | :--- |
| 유스케이스 다이어그램 | draw.io | 방장 · 참가자 역할별로 가능한 동작 범위 정의 |
| 화면 설계 | Figma | 화면 7종의 레이아웃과 화면 간 이동 흐름 설계 |
| ERD | ERDCloud | 테이블 15개의 관계와 제약조건 설계 |
| API 명세서 | Notion | REST 15개 · WebSocket 발행 경로 12개 규격 합의 |

### 설계 산출물

**유스케이스 다이어그램** — 방장 · 참가자 역할별로 가능한 동작 범위 정의

<p align="center">
  <img width="330" alt="유스케이스 다이어그램" src="https://github.com/user-attachments/assets/7a18b45c-082d-4807-a3e5-f9648925d601" />
</p>

**화면 설계 (Figma)** — 화면의 레이아웃과 화면 간 이동 흐름 설계

<p align="center">
  <img width="900" alt="화면 설계" src="https://github.com/user-attachments/assets/38c6861c-4bd5-41c4-a07b-3fcea0d34158" />
</p>

**ERD (ERDCloud)** — 테이블 15개의 관계와 제약조건 설계

<p align="center">
  <img width="900" alt="ERD" src="https://github.com/user-attachments/assets/98099d31-8a09-48db-8ebf-699ec47d61d9" />
</p>

**API 명세서 (Notion)** — REST 15개 · WebSocket 발행 경로 12개 규격 합의

https://app.notion.com/p/3c110df0798f80aa8f9af2cea02df10f?v=3c110df0798f801fa6ee000c572f9392&source=copy_link

## 9. 데이터베이스 설계

방, 참가자, 식당, 선택, 투표, 게임, 결과, 경로, 채팅, 이동 추적 도메인별로 테이블을 분리하고 외래키로 관계를 구성했습니다.

- **테이블:** 15개
- 참가자 삭제 시 하위 데이터가 함께 사라지지 않도록, **게임 중 이탈은 행을 지우지 않고 이탈 시각만 기록**합니다.
- 경로는 `경로 → 구간 → 좌표` 3단계로 나눠 저장해 구간별 이동수단과 안내 문구를 함께 관리합니다.
- 1인 1표가 필요한 선택과 투표는 `MERGE` 로 처리해 재선택이 기존 행을 덮어쓰도록 했습니다.
- 채팅 메시지 조회는 `FETCH FIRST 100 ROWS ONLY` 로 최근 100건을 가져오고, 재연결 시에는 마지막 메시지 이후 구간만 조회합니다.

---

## 10. 주요 API

### REST

| 영역 | Method | Endpoint | 설명 |
| :--- | :---: | :--- | :--- |
| 방 | `POST` | `/api/rooms` | 방 생성 |
| 방 | `GET` | `/api/rooms/{roomUuid}` | 방 조회 |
| 참가자 | `POST` | `/api/rooms/{roomUuid}/participants` | 방 참가 |
| 참가자 | `GET` | `/api/rooms/{roomUuid}/participants` | 참가자 목록 |
| 참가자 | `PATCH` | `/api/rooms/{roomUuid}/participants/{id}/ready` | 준비 완료 |
| 참가자 | `DELETE` | `/api/rooms/{roomUuid}/participants/{id}` | 방 나가기 |
| 식당 | `POST` | `/api/rooms/{roomUuid}/restaurants` | 식당 등록 |
| 식당 | `GET` | `/api/rooms/{roomUuid}/restaurants` | 식당 목록 |
| 식당 | `GET` | `/api/rooms/{roomUuid}/restaurants/nearby` | 주변 식당 검색 |
| 선택 | `POST` | `/api/rooms/{roomUuid}/participants/{id}/selection` | 식당 선택 |
| 선택 | `GET` | `/api/rooms/{roomUuid}/selections` | 선택 현황 |
| 투표 | `GET` | `/api/rooms/{roomUuid}/votes` | 진행 방식 투표 현황 |
| 게임 | `GET` | `/api/rooms/{roomUuid}/games` | 게임 현황 |
| 결과 | `GET` | `/api/rooms/{roomUuid}/routes?travelMode=` | 확정 결과 · 경로 |
| 채팅 | `GET` | `/api/rooms/{roomUuid}/messages` | 채팅 내역 |

조회 전용 경로를 따로 둔 이유는, **행위 자체는 소켓으로만 받되 새로고침 · 재접속 시 화면을 복원**해야 하기 때문입니다.

### WebSocket

엔드포인트 `/ws` (SockJS) · 발행 `/app` · 구독 `/topic`

| 발행 | 설명 | 권한 |
| :--- | :--- | :--- |
| `/app/chat/enter` · `/send` · `/leave` | 채팅 입장 · 메시지 · 퇴장 | 참가자 |
| `/app/midpoint/find` · `/reset` | 중간 지점 찾기 · 재설정 | 방장 |
| `/app/mode/start` · `/vote` | 투표 시작 · 투표 | 방장 · 참가자 |
| `/app/game/start` · `/pick` · `/expire` · `/leave` | 게임 시작 · 주머니 열기 · 차례 만료 · 이탈 | 방장 · 차례인 참가자 |
| `/app/result/find` | 결과 확정 | 방장 |

| 구독 `/topic/room/{roomUuid}` | 전송 시점 |
| :--- | :--- |
| *(루트)* | 채팅 메시지 |
| `/participants` · `/participants/ready` | 새 참가자 입장 · 준비 상태 변경 · 퇴장 |
| `/restaurants` · `/selections` | 식당 등록 · 선택 현황 |
| `/midpoint` · `/midpoint/reset` | 중간 지점 확정 · 재설정 |
| `/mode` · `/game` · `/result` | 투표 현황 · 게임 현황 · 최종 결과 |
| `/*/error` | 각 도메인의 실패 사유 |

STOMP에는 HTTP 상태코드가 없어, 실패는 **도메인별 에러 토픽**으로 사유를 전달합니다.

---

## 11. 방 진행 단계와 예외 처리

```
WAITING  →  MIDPOINT_FOUND  →  MODE_SELECTED  →  RESOLVING  →  GAME_PLAYING  →  RESOLVED
   └───── 입장 가능 ─────┘                          ↑______________|
                                                  게임 종료 · 중단 시 복귀
```

모든 응답은 `ApiResponse<T>` 로 감싸고, 예외는 `GlobalExceptionHandler` 한 곳에서 상태코드로 변환합니다.

| 예외 | 상태 | 쓰이는 상황 |
| :--- | :---: | :--- |
| `NotFoundException` | 404 | 없는 방 · 참가자 · 식당 |
| `DuplicateException` | 409 | 이미 등록된 식당, 이미 열린 주머니 |
| `ForbiddenException` | 403 | 방장 전용 동작을 참가자가 시도 |
| `UnauthorizedException` | 401 | 인증 실패 |
| `InvalidStateException` | 400 | 단계에 맞지 않는 요청, 정원 초과, 차례 아님 |
| `ExternalApiException` | 502 | 카카오 · Tmap 호출 실패 |

예상하지 못한 예외는 원인을 응답에 싣지 않고 로그로만 남깁니다. SQL이나 내부 경로가 새지 않도록 했습니다.

---

## 12. 테스트 및 품질 검증

코드를 전수 확인해 **QA 테스트 케이스 188건**을 작성했습니다.

| 영역 | 테스트 케이스 | 설명 |
| :--- | ---: | :--- |
| 방 생성 · 입장 | **30건** | 방 생성, 정원 검증, 링크 입장, 입장 차단 조건 |
| 참가자 · 멤버 | **16건** | 준비 상태, 나가기, 방장 승계 |
| 중간 지점 | **15건** | 후보 선정, 재설정, 외부 API 실패 처리 |
| 식당 등록 · 선택 | **21건** | 검색 · 추가, 중복 방지, 선택 변경 |
| 진행 방식 투표 | **15건** | 투표 시작, 집계, 동점 처리 |
| 보물 주머니 게임 | **30건** | 시작 조건, 차례 · 타임아웃, 당첨 판정, 이탈 |
| 결과 · 경로 | **15건** | 결과 확정, 이동수단 전환, 복원 |
| 채팅 | **14건** | 메시지 송수신, 재연결 보충, 사칭 차단 |
| 소켓 · 에러 화면 | **16건** | 커넥션 공유, 재연결, 에러 처리 |
| 보안 · 비기능 | **16건** | 식별자 노출, 권한 우회, 반응형 |
| **합계** | **188건** | |

### 우선순위 분포

| P1 (핵심 경로) | P2 (주요 기능) | P3 (부가 · 예외) |
| :---: | :---: | :---: |
| **64건** | **83건** | **41건** |

### 코드 기준 사전 판정

실행 전에 코드만 읽고 결과를 미리 판정해 검증 순서를 정했습니다.

| 통과 예상 | 실패 예상 | 확인 필요 |
| :---: | :---: | :---: |
| **171건** | **7건** | **10건** |

- **실패 예상** 7건은 코드상 기대 결과대로 동작하지 않을 것으로 보이는 케이스로, 원인과 수정 방향을 함께 정리했습니다.
- **확인 필요** 10건은 DDL 제약이나 외부 API 키처럼 코드만으로는 판정할 수 없는 케이스입니다.

---

## 13. 프로젝트 결과

- 방 생성부터 이동 추적까지 이어지는 **단일 흐름**을 구현하고, 단계 전이와 결과 확정을 서버가 관리하도록 구성했습니다.
- 로그인 없이도 사칭이 불가능한 **세션 기반 신원 확인** 구조를 만들었습니다.
- 당첨 위치를 애플리케이션이 읽지 않는 설계로 **정보 유출 경로 자체를 제거**했습니다.
- 게임 상태를 DB에 저장해 **새로고침 · 재접속에도 진행 상황이 복원**되도록 했습니다.
- 외부 API 호출을 트랜잭션 밖으로 빼고 **일부 실패가 전체를 막지 않도록** 실패 범위를 격리했습니다.
- ALB Host 분기와 도메인 5개를 담은 인증서 1장으로 EC2 1대 분리 운용

<div align="center">

<br>

**딱 중간 — 모두의 중간에서 만나고, 게임으로 식당을 정한다**

<sub> Team.Legend 딱 중간 프로젝트 · 6주</sub>

</div>
