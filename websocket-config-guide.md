# WebSocket 설정 뜯어보기

> 딱중간 · 채팅 ① 단계

이번 PR에 들어간 설정 클래스 두 개가 각각 무엇을 하는지, 왜 그 줄이 필요한지 정리했습니다.
메시지 처리 로직은 다음 단계라 여기서는 **연결이 맺어지기까지**만 다룹니다.

---

## 1. 이름이 세 개나 붙는 이유

코드 한 줄에 WebSocket, STOMP, SockJS가 다 나와서 헷갈리기 쉬운데, 셋은 경쟁하는 기술이 아니라 **층층이 쌓인 것**입니다.

| 층 | 하는 일 | 없으면 |
| --- | --- | --- |
| WebSocket | 브라우저와 서버가 연결을 끊지 않고 양방향으로 데이터를 주고받는 통신 규약 | 새로고침하거나 계속 서버에 물어봐야 함 |
| STOMP | 그 연결 위에서 "이건 어디로 가는 메시지인지" 정하는 약속 | 바이트만 오갈 뿐, 어느 방 메시지인지 서버가 모름 |
| SockJS | WebSocket이 막힌 환경에서 다른 방식으로 대신 연결 | 회사 방화벽·구형 브라우저에서 연결 실패 |

WebSocket만 쓰면 전화선은 깔았는데 주소 체계가 없는 상태예요. "누구에게 보내는 메시지인가"를 매번 직접 파싱해야 합니다. STOMP는 그 위에 *목적지 주소*와 *구독*이라는 개념을 얹어줍니다.

> **한 줄 요약** — WebSocket은 전화선, STOMP는 우편 주소 체계, SockJS는 전화가 안 될 때 쓰는 우회로.

---

## 2. 애노테이션 한 줄이 켜는 것

```java
@Configuration
@EnableWebSocketMessageBroker      // ← 이 줄
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
```

이 애노테이션이 붙으면 스프링이 STOMP 메시지를 처리할 부품 한 세트를 자동으로 만들어 줍니다. 메시지를 받아 적절한 곳으로 넘기는 통로, 목적지별로 나눠주는 브로커, `@MessageMapping`이 붙은 메서드를 찾아 연결해주는 장치 같은 것들이요.

`WebSocketMessageBrokerConfigurer`는 그렇게 만들어진 기본값을 **우리 프로젝트에 맞게 손볼 수 있는 자리**를 열어주는 인터페이스입니다. 우리는 그중 두 개를 재정의했어요. 하나는 문을 어디에 낼지, 다른 하나는 안에서 길을 어떻게 낼지.

---

## 3. 문 열기 — `registerStompEndpoints`

```java
@Override
public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry.addEndpoint("/ws")
            .setAllowedOriginPatterns("*")
            .withSockJS();
}
```

### `addEndpoint("/ws")`

브라우저가 연결을 요청할 주소입니다. 프론트에서 `new SockJS('/ws')`라고 쓰면 바로 이 문을 두드리는 거예요.

여기서 한 번 악수(핸드셰이크)를 하고 나면, 그 뒤로는 **같은 연결 하나로 모든 메시지가 오갑니다.** REST처럼 요청마다 새로 연결하지 않아요.

### `setAllowedOriginPatterns("*")`

어느 주소에서 온 접속을 허용할지 정합니다. 지금은 개발 단계라 전부 열어뒀어요.

> ⚠️ **헷갈리기 쉬운 곳**
>
> **WebSocket은 `WebMvcConfigurer`의 CORS 설정을 타지 않습니다.** `addCorsMappings()`로 `/api/**`를 아무리 열어놔도 `/ws` 핸드셰이크에는 적용되지 않아요. 그래서 여기서 따로 지정해야 합니다.
>
> "CORS 다 열었는데 웹소켓만 연결이 안 된다"는 상황이 대부분 이것 때문입니다. 배포할 때는 `"*"` 대신 실제 프론트 주소로 좁혀야 하고요.

### `withSockJS()`

WebSocket 연결이 실패하면 자동으로 다른 방식(HTTP 폴링 등)으로 대신 연결을 시도합니다. 회사 방화벽이나 일부 프록시가 WebSocket을 막는 경우가 있어서 넣어뒀어요.

다만 이건 **프론트와 짝이 맞아야 합니다.**

- 서버에 `withSockJS()`가 있으면 → 프론트도 `new SockJS(...)`
- 프론트가 `ws://`로 직접 붙으면 → 서버에서 이 줄을 빼야 함

한쪽만 있으면 핸드셰이크 단계에서 바로 실패합니다.

---

## 4. 길 내기 — `configureMessageBroker`

```java
@Override
public void configureMessageBroker(MessageBrokerRegistry registry) {
    registry.enableSimpleBroker("/topic");
    registry.setApplicationDestinationPrefixes("/app");
}
```

제가 처음에 제일 헷갈렸던 부분이 여기예요. **왜 접두사가 두 개나 필요한가?** 답은 메시지가 가는 방향이 두 가지이기 때문입니다.

```
                     /ws 로 맺어진 하나의 연결
                              ┊
  ┌──────────────┐            ┊   ┌─────────────────────────────┐
  │  브라우저 A   │ ──────────────▶│  @MessageMapping 핸들러       │
  │   보내는 쪽   │  ① publish  ┊   │  (아직 없음 · ③단계에서 추가)  │
  └──────────────┘  /app/…    ┊   └──────────────┬──────────────┘
                              ┊                  │
                              ┊    ② convertAndSend
                              ┊     저장·가공한 뒤 브로커로
                              ┊                  │
                              ┊                  ▼
  ┌──────────────┐            ┊   ┌─────────────────────────────┐
  │ 브라우저 A·B·C │ ◀──────────────│        SimpleBroker          │
  │ 같은 방 구독자 │  ③ /topic/… ┊   │      스프링 내장 · 메모리       │
  └──────────────┘            ┊   └─────────────────────────────┘

     /app   →  내가 짠 코드로 들어옴
     /topic →  브로커가 그대로 뿌림
```

### `setApplicationDestinationPrefixes("/app")`

"이 접두사로 오는 메시지는 **내가 짠 코드로 넘겨라**"는 뜻입니다.

프론트가 `/app/chat.send`로 보내면 스프링이 `/app`을 떼고 `@MessageMapping("/chat.send")`가 붙은 메서드를 찾아 호출해요. 메시지를 DB에 저장하거나, 보낸 사람을 검증하거나, 내용을 가공하는 건 전부 이 경로로 들어옵니다.

### `enableSimpleBroker("/topic")`

"이 접두사로 가는 메시지는 **구독자들에게 그대로 뿌려라**"는 뜻입니다.

중간에 내 코드를 거치지 않아요. `/topic/room/abc`를 구독한 사람이 다섯이면, 그 다섯에게 똑같이 전달됩니다.

> **그래서 규칙은**
>
> 클라이언트가 **보낼 때**는 `/app`, **받을 때(구독)**는 `/topic`. 방향이 다르니 접두사도 다릅니다.

> ⚠️ **알아둘 한계**
>
> `SimpleBroker`는 스프링에 내장된 간단한 브로커라 **구독 정보를 서버 메모리에 들고 있습니다.** 그래서 서버를 재시작하면 초기화되고, 서버를 여러 대로 늘리면 A 서버에 붙은 사람과 B 서버에 붙은 사람끼리 메시지가 안 갑니다.
>
> 규모가 커지면 RabbitMQ 같은 외부 브로커로 바꾸는데, 우리 프로젝트 규모에서는 `SimpleBroker`로 충분해요. 다만 *왜* 충분한지는 알고 쓰는 게 좋겠죠.

---

## 5. SecurityConfig가 같이 들어간 이유

이건 채팅 기능이라서 넣은 게 아니라, **넣지 않으면 아무것도 동작하지 않아서** 넣었습니다.

`build.gradle`에 `spring-boot-starter-security`가 들어 있으면, 스프링 시큐리티는 설정 클래스가 하나도 없을 때 *안전한 기본값*으로 동작합니다. 그 기본값이 "모든 요청에 인증을 요구한다"예요. 로그인 페이지로 돌리거나 401을 반환합니다. `/ws` 핸드셰이크도 예외가 아니고요.

```java
http
    .csrf(csrf -> csrf.disable())
    .cors(Customizer.withDefaults())
    .formLogin(form -> form.disable())
    .httpBasic(basic -> basic.disable())
    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
```

| 줄 | 설정 | 이유 |
| --- | --- | --- |
| `csrf` | **끔** | CSRF 토큰은 브라우저가 폼을 자동 제출하는 상황을 막는 장치인데, 우리는 세션 쿠키로 인증하지 않는 API 서버라 해당이 없습니다. 켜두면 POST 요청이 403으로 막혀요. |
| `cors` | **켬** | 나중에 `WebConfig`에서 정의할 CORS 설정을 시큐리티 필터도 따르게 합니다. 지금은 정의된 게 없어 사실상 아무 일도 안 해요. |
| `formLogin` | **끔** | 기본 로그인 페이지가 안 뜨게 합니다. 로그인이 없는 서비스니까요. |
| `httpBasic` | **끔** | 브라우저 기본 인증 팝업을 막습니다. |
| `permitAll` | **전부 허용** | 현재는 인증 개념 자체가 없습니다. |

> ⚠️ **임시 설정**
>
> `anyRequest().permitAll()`은 **지금 단계에서만** 맞는 값입니다. 방 참가자 검증 정책이 정해지면 경로별로 다시 잡아야 해요. 코드에도 `TODO`로 표시해뒀습니다.

---

## 6. 지금 되는 것과 안 되는 것

| 동작 | 지금 | 필요한 것 |
| --- | --- | --- |
| 서버 연결 (CONNECT) | ✅ 됨 | — |
| 연결 끊기 (DISCONNECT) | ✅ 됨 | — |
| 구독 (SUBSCRIBE) | ✅ 됨 | — (받을 메시지가 없을 뿐) |
| 누가 접속했는지 알기 | ❌ | ② 인터셉터로 방·참가자 확인 |
| 메시지 보내기 | ❌ | ③ `@MessageMapping` 핸들러 |
| 퇴장 자동 처리 | ❌ | ④ 연결 종료 이벤트 처리 |

그래서 이번 PR의 확인 기준은 하나입니다 — **테스트 페이지에서 `CONNECTED`가 뜨는가.** 메시지가 안 오가는 건 정상이에요.

---

다음 단계에서는 `configureClientInboundChannel`을 재정의해서, 연결이 맺어지는 그 순간에 "이 사람이 이 방의 참가자가 맞는지"를 확인하는 장치를 답니다. 한 번 확인해둔 정보를 세션에 보관해두면, 이후 메시지를 처리할 때 클라이언트가 보낸 값을 믿지 않아도 되거든요.
