# Chat Feature Implementation Guide

이 문서는 현재 프로젝트에 추가한 채팅 기능을 기준으로, 어떤 파일을 왜 만들었고 각 필드와 메서드가 어떤 역할을 하는지 정리한 문서다. 처음 읽는 사람이 채팅 기능을 만들 때 고려해야 할 구조, 인증, 권한, 저장, 조회, WebSocket 흐름을 이해하는 것을 목표로 한다.

## 1. 전체 요구사항

추가한 채팅 기능은 다음 API와 WebSocket 경로를 지원한다.

| Method/Protocol | URL/Destination | 설명 | 권한 |
| --- | --- | --- | --- |
| POST | `/api/chat/rooms` | 고객 문의 채팅방 생성 | ROLE_USER |
| GET | `/api/admin/chat/rooms` | 관리자 채팅방 목록 조회 | ROLE_ADMIN |
| GET | `/api/chat/rooms/{chatRoomNo}/messages` | 이전 메시지 조회 | 로그인 |
| WebSocket | `/ws` | STOMP 연결 엔드포인트 | 로그인 |
| SEND | `/pub/chat/rooms/{chatRoomNo}` | 메시지 발행 | 로그인 |
| SUBSCRIBE | `/sub/chat/rooms/{chatRoomNo}` | 채팅방 메시지 구독 | 로그인 |

핵심 설계는 다음과 같다.

- REST API는 채팅방 생성, 채팅방 목록 조회, 이전 메시지 조회처럼 요청-응답으로 끝나는 작업을 담당한다.
- WebSocket/STOMP는 실시간 메시지 발행과 구독을 담당한다.
- 메시지는 DB에 저장한다. 그래야 사용자가 나중에 접속해도 이전 메시지를 조회할 수 있다.
- 관리자는 모든 채팅방에 접근할 수 있다.
- 일반 사용자는 자신이 만든 채팅방에만 접근할 수 있다.
- HTTP 요청 권한은 `SecurityConfig`에서 1차로 막는다.
- STOMP 연결과 메시지 경로 권한은 `WebSocketConfig`에서 별도로 검사한다.

## 2. 왜 REST와 WebSocket을 나눴는가

채팅 기능에는 두 종류의 작업이 있다.

첫 번째는 일반 HTTP로 충분한 작업이다.

- 채팅방 생성
- 관리자 채팅방 목록 조회
- 이전 메시지 조회

이 작업들은 클라이언트가 요청하면 서버가 한 번 응답하고 끝난다. 그래서 REST API가 적합하다.

두 번째는 실시간성이 필요한 작업이다.

- 메시지 보내기
- 같은 채팅방에 접속한 사용자들에게 메시지 즉시 전달

이 작업은 서버가 클라이언트에게 능동적으로 메시지를 보내야 한다. 일반 HTTP만으로 처리하면 polling이 필요하고 비효율적이다. 그래서 WebSocket/STOMP를 사용했다.

## 3. pom.xml

추가한 의존성은 다음과 같다.

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>
```

이 의존성을 추가한 이유는 Spring에서 WebSocket과 STOMP 메시지 브로커 기능을 사용하기 위해서다.

없으면 다음 클래스와 어노테이션을 사용할 수 없다.

- `@EnableWebSocketMessageBroker`
- `WebSocketMessageBrokerConfigurer`
- `@MessageMapping`
- `SimpMessagingTemplate`
- STOMP endpoint 설정

## 4. ChatRoom 엔티티

파일 위치:

`src/main/java/web/mvc/domain/ChatRoom.java`

역할:

채팅방 자체를 나타내는 JPA 엔티티다. 고객 문의 채팅은 먼저 방이 있어야 하고, 그 방 안에 여러 메시지가 쌓인다.

```java
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoom {
```

`@Entity`는 DB 테이블과 매핑되는 클래스라는 뜻이다.

`@Getter`, `@Setter`는 Lombok이 getter/setter를 생성한다.

`@Builder`는 객체 생성 시 빌더 패턴을 사용할 수 있게 한다.

`@NoArgsConstructor`는 JPA가 엔티티를 만들 때 필요하다.

`@AllArgsConstructor`는 모든 필드를 받는 생성자를 만든다.

### chatRoomNo

```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long chatRoomNo;
```

채팅방의 PK다.

`Long`을 사용한 이유는 DB auto increment PK와 잘 맞고, 주문 번호나 상품 번호처럼 프로젝트의 다른 엔티티도 `Long` PK를 사용하기 때문이다.

`@GeneratedValue(strategy = GenerationType.IDENTITY)`는 DB가 자동으로 번호를 생성하게 한다.

이 값은 URL에서도 사용된다.

```text
/api/chat/rooms/{chatRoomNo}/messages
/pub/chat/rooms/{chatRoomNo}
/sub/chat/rooms/{chatRoomNo}
```

### user

```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "user_no")
private User user;
```

채팅방을 만든 고객이다.

`ManyToOne`인 이유는 한 명의 사용자가 여러 채팅방을 만들 수 있기 때문이다.

```text
User 1명 : ChatRoom 여러 개
```

`fetch = FetchType.LAZY`를 사용한 이유는 채팅방을 조회할 때 항상 사용자 정보가 필요한 것은 아니기 때문이다. 필요한 경우 Repository에서 `join fetch`로 명시적으로 가져온다.

`@JoinColumn(name = "user_no")`는 `chat_room` 테이블에 `user_no` 외래키 컬럼을 만든다.

이 필드는 권한 검사에도 중요하다. 일반 사용자가 특정 방에 접근할 때 `chatRoom.getUser().getUserId()`와 현재 로그인한 사용자의 userId를 비교한다.

### createdAt

```java
@CreationTimestamp
private LocalDateTime createdAt;
```

채팅방 생성 시간이다.

관리자 채팅방 목록에서 최신 문의방을 먼저 보여주기 위해 필요하다.

Repository에서는 이 값을 기준으로 정렬한다.

```java
order by cr.createdAt desc
```

서비스에서 `LocalDateTime.now()`도 같이 넣은 이유는 생성 직후 응답 DTO를 만들 때 `@CreationTimestamp` 값이 아직 null일 수 있기 때문이다. Hibernate가 flush 시점에 값을 채우는 경우가 있어 즉시 응답 안정성을 위해 명시적으로 세팅했다.

### messages

```java
@Builder.Default
@OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL, orphanRemoval = true)
private List<ChatMessage> messages = new ArrayList<>();
```

채팅방에 속한 메시지 목록이다.

`OneToMany`인 이유는 하나의 채팅방에 여러 메시지가 들어가기 때문이다.

```text
ChatRoom 1개 : ChatMessage 여러 개
```

`mappedBy = "chatRoom"`은 연관관계의 주인이 `ChatMessage.chatRoom`이라는 뜻이다. 실제 외래키는 `ChatMessage` 테이블에 있다.

`cascade = CascadeType.ALL`은 채팅방 저장/삭제 같은 작업이 메시지에도 전파되게 한다.

`orphanRemoval = true`는 채팅방에서 메시지가 제거되면 DB에서도 고아 객체를 제거한다.

`@Builder.Default`는 Lombok builder를 사용할 때도 `new ArrayList<>()` 기본값을 유지하게 한다. 없으면 builder로 만든 객체의 `messages`가 null이 될 수 있다.

## 5. ChatMessage 엔티티

파일 위치:

`src/main/java/web/mvc/domain/ChatMessage.java`

역할:

채팅방 안에서 오가는 개별 메시지를 저장하는 엔티티다.

### chatMessageNo

```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long chatMessageNo;
```

메시지의 PK다.

메시지 하나하나를 구분하기 위해 필요하다. 응답 DTO에서도 메시지 식별자로 내려준다.

### chatRoom

```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "chat_room_no")
private ChatRoom chatRoom;
```

이 메시지가 어느 채팅방에 속하는지 나타낸다.

`ManyToOne`인 이유는 여러 메시지가 하나의 채팅방에 속하기 때문이다.

`chat_room_no`는 메시지 테이블의 외래키다.

메시지를 저장할 때 반드시 필요하다. 그래서 `sendMessage` 메서드에서는 `chatRoomNo`를 인수로 받아 기존 채팅방을 조회한 뒤 `ChatMessage.chatRoom`에 넣는다.

### sender

```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "sender_no")
private User sender;
```

메시지를 보낸 사용자다.

관리자와 고객 모두 메시지를 보낼 수 있기 때문에 `User`와 연결했다.

응답에서 다음 정보를 만들 때 사용한다.

- senderNo
- senderId
- senderName
- senderRole

이 필드가 없으면 메시지 내용은 알 수 있지만 누가 보냈는지 알 수 없다.

### content

```java
@Column(nullable = false, length = 1000)
private String content;
```

메시지 본문이다.

`nullable = false`는 빈 메시지를 DB에 저장하지 않기 위한 최소 제약이다.

`length = 1000`은 메시지 길이를 제한하기 위한 값이다. 무제한 문자열을 허용하면 DB 저장, 화면 렌더링, 악성 요청 측면에서 부담이 커질 수 있다.

서비스에서도 다음 검증을 한다.

```java
if (chatMessageReq.getContent() == null || chatMessageReq.getContent().isBlank()) {
    throw new ChatException(ErrorCode.INVALID_INPUT);
}
```

DB 제약 전에 비즈니스 계층에서 먼저 막는 것이 응답 에러를 더 명확하게 만들 수 있다.

### sentAt

```java
@CreationTimestamp
private LocalDateTime sentAt;
```

메시지를 보낸 시간이다.

이전 메시지를 조회할 때 오래된 메시지부터 보여주기 위해 필요하다.

Repository에서 다음 정렬에 사용한다.

```java
order by cm.sentAt asc
```

서비스에서 `LocalDateTime.now()`도 함께 넣은 이유는 `ChatRoom.createdAt`과 같다. 저장 직후 WebSocket으로 바로 브로드캐스트할 때 시간이 null이면 클라이언트 화면에서 처리하기 불편하기 때문이다.

## 6. ChatMessageReq DTO

파일 위치:

`src/main/java/web/mvc/dto/request/ChatMessageReq.java`

역할:

클라이언트가 STOMP SEND로 메시지를 보낼 때 사용하는 요청 DTO다.

```java
public class ChatMessageReq {
    private String content;
}
```

필드는 `content` 하나만 둔다.

`chatRoomNo`를 body에 넣지 않은 이유는 destination에 이미 들어있기 때문이다.

```text
/pub/chat/rooms/{chatRoomNo}
```

즉 채팅방 식별자는 경로에서 받고, body에는 실제 메시지 내용만 둔다. 이렇게 하면 API 구조가 명확해진다.

예시 payload:

```json
{
  "content": "문의드립니다."
}
```

## 7. ChatRoomRes DTO

파일 위치:

`src/main/java/web/mvc/dto/response/ChatRoomRes.java`

역할:

채팅방 정보를 클라이언트에 응답할 때 사용하는 DTO다.

엔티티 `ChatRoom`을 그대로 반환하지 않는 이유는 다음과 같다.

- 엔티티를 직접 노출하면 DB 구조가 API에 그대로 드러난다.
- LAZY 관계로 인해 JSON 직렬화 문제가 생길 수 있다.
- 응답에 필요한 값만 선별하기 어렵다.
- User 엔티티를 직접 반환하면 password 같은 민감 정보가 노출될 위험이 있다.

### chatRoomNo

```java
private Long chatRoomNo;
```

채팅방 식별자다.

클라이언트가 이후 메시지 조회, 구독, 발행에 사용한다.

### userNo

```java
private Long userNo;
```

채팅방을 만든 고객의 내부 번호다.

관리자 화면에서 고객을 식별하거나 상세 조회로 연결할 때 사용할 수 있다.

### userId

```java
private String userId;
```

고객 로그인 ID다.

관리자 채팅방 목록에서 누가 문의했는지 보여주기 위해 필요하다.

### userName

```java
private String userName;
```

고객 이름이다.

관리자 화면에서 사람이 읽기 쉬운 사용자 정보를 보여주기 위해 포함했다.

### createdAt

```java
private LocalDateTime createdAt;
```

채팅방 생성 시간이다.

관리자 목록에서 최신 문의를 판단할 수 있게 한다.

### 생성자

```java
public ChatRoomRes(ChatRoom chatRoom) {
    this.chatRoomNo = chatRoom.getChatRoomNo();
    this.userNo = chatRoom.getUser().getUserNo();
    this.userId = chatRoom.getUser().getUserId();
    this.userName = chatRoom.getUser().getUserName();
    this.createdAt = chatRoom.getCreatedAt();
}
```

엔티티를 DTO로 변환하는 생성자다.

이 생성자를 둔 이유는 서비스 코드에서 변환을 간단하게 하기 위해서다.

```java
.map(ChatRoomRes::new)
```

단, 이 생성자는 `chatRoom.getUser()`를 사용한다. 그래서 Repository에서 `join fetch cr.user`로 user를 미리 가져오게 했다. 그렇지 않으면 트랜잭션 밖에서 LAZY 로딩 문제가 생길 수 있다.

## 8. ChatMessageRes DTO

파일 위치:

`src/main/java/web/mvc/dto/response/ChatMessageRes.java`

역할:

저장된 메시지 또는 방금 발송된 메시지를 클라이언트에 내려주는 응답 DTO다.

### chatMessageNo

메시지 PK다. 화면에서 메시지 key로 쓰거나 중복 메시지 처리를 할 때 사용할 수 있다.

### chatRoomNo

메시지가 속한 방 번호다. WebSocket 브로드캐스트를 받은 클라이언트가 어느 방의 메시지인지 확인할 수 있다.

### senderNo

발신자의 내부 번호다.

### senderId

발신자의 로그인 ID다.

### senderName

발신자 이름이다. 채팅 UI에 표시하기 좋다.

### senderRole

발신자의 권한이다.

관리자가 보낸 메시지인지, 고객이 보낸 메시지인지 구분할 수 있다.

예를 들어 프론트에서 `ROLE_ADMIN`이면 오른쪽 정렬 또는 관리자 뱃지를 붙일 수 있다.

### content

메시지 내용이다.

### sentAt

메시지 발송 시간이다.

### 생성자

```java
public ChatMessageRes(ChatMessage chatMessage) {
    this.chatMessageNo = chatMessage.getChatMessageNo();
    this.chatRoomNo = chatMessage.getChatRoom().getChatRoomNo();
    this.senderNo = chatMessage.getSender().getUserNo();
    this.senderId = chatMessage.getSender().getUserId();
    this.senderName = chatMessage.getSender().getUserName();
    this.senderRole = chatMessage.getSender().getRole();
    this.content = chatMessage.getContent();
    this.sentAt = chatMessage.getSentAt();
}
```

엔티티를 API 응답용 구조로 바꾸는 역할이다.

이 생성자는 `sender`와 `chatRoom` 정보를 사용한다. 따라서 메시지 조회 Repository에서는 `join fetch cm.sender`를 사용했고, 저장 직후 응답에서는 서비스 안의 트랜잭션 범위에서 DTO를 만든다.

## 9. ChatRoomRepository

파일 위치:

`src/main/java/web/mvc/repository/ChatRoomRepository.java`

역할:

채팅방 조회를 담당하는 Repository다.

`JpaRepository<ChatRoom, Long>`을 상속해서 기본 CRUD를 사용할 수 있다.

### findAllWithUser

```java
@Query("""
        select cr
        from ChatRoom cr
        join fetch cr.user
        order by cr.createdAt desc
        """)
List<ChatRoom> findAllWithUser();
```

관리자 채팅방 목록 조회에 사용한다.

`join fetch cr.user`를 사용한 이유는 응답 DTO에서 userNo, userId, userName이 필요하기 때문이다.

일반 `findAll()`로 조회하면 `ChatRoom.user`가 LAZY 상태일 수 있다. DTO 변환 시 추가 쿼리가 많이 나가거나, 트랜잭션 밖이면 LazyInitializationException이 발생할 수 있다.

`order by cr.createdAt desc`는 최신 채팅방을 먼저 보여주기 위해서다.

### findByIdWithUser

```java
@Query("""
        select cr
        from ChatRoom cr
        join fetch cr.user
        where cr.chatRoomNo = :chatRoomNo
        """)
Optional<ChatRoom> findByIdWithUser(Long chatRoomNo);
```

특정 채팅방을 조회할 때 사용한다.

인수로 `Long chatRoomNo`를 받는 이유는 채팅방의 PK 타입이 `Long`이기 때문이다.

반환 타입을 `Optional<ChatRoom>`으로 둔 이유는 채팅방이 없을 수 있기 때문이다. 서비스에서 다음처럼 명확하게 예외로 바꾼다.

```java
.orElseThrow(() -> new ChatException(ErrorCode.CHAT_NOT_FOUND))
```

`join fetch cr.user`가 필요한 이유는 권한 검사에서 채팅방 소유자 userId를 확인하기 때문이다.

## 10. ChatMessageRepository

파일 위치:

`src/main/java/web/mvc/repository/ChatMessageRepository.java`

역할:

채팅 메시지 저장과 조회를 담당한다.

### findAllByChatRoomNoWithSender

```java
@Query("""
        select cm
        from ChatMessage cm
        join fetch cm.sender
        where cm.chatRoom.chatRoomNo = :chatRoomNo
        order by cm.sentAt asc
        """)
List<ChatMessage> findAllByChatRoomNoWithSender(Long chatRoomNo);
```

특정 채팅방의 이전 메시지 목록을 조회한다.

인수로 `chatRoomNo`를 받는 이유는 URL이 다음 구조이기 때문이다.

```text
/api/chat/rooms/{chatRoomNo}/messages
```

응답에서 발신자 정보가 필요하므로 `join fetch cm.sender`를 사용했다.

오래된 메시지부터 보여주는 것이 일반적인 채팅 UI 흐름이므로 `sentAt asc`로 정렬한다.

## 11. ChatService 인터페이스

파일 위치:

`src/main/java/web/mvc/service/ChatService.java`

역할:

컨트롤러가 직접 Repository를 알지 않도록 채팅 비즈니스 기능을 추상화한다.

```java
ChatRoomRes createRoom(String userId);
```

채팅방을 생성한다.

인수로 `userId`를 받는 이유는 채팅방을 만든 고객을 `ChatRoom.user`에 연결해야 하기 때문이다. Security에서 로그인은 확인하지만, DB에 저장할 실제 `User` 엔티티가 필요하므로 서비스에서 userId로 조회한다.

```java
List<ChatRoomRes> findAllRooms();
```

관리자가 전체 채팅방을 조회한다.

인수가 없는 이유는 관리자는 모든 방을 볼 수 있고, 특정 사용자 기준 필터가 요구사항에 없기 때문이다.

```java
List<ChatMessageRes> findMessages(String userId, Long chatRoomNo);
```

이전 메시지를 조회한다.

`userId`가 필요한 이유는 일반 사용자가 남의 채팅방 메시지를 볼 수 없게 검사해야 하기 때문이다.

`chatRoomNo`가 필요한 이유는 어떤 채팅방의 메시지를 조회할지 식별해야 하기 때문이다.

```java
ChatMessageRes sendMessage(String userId, Long chatRoomNo, ChatMessageReq chatMessageReq);
```

메시지를 저장하고 응답 DTO를 반환한다.

`userId`는 발신자를 찾고 권한을 검사하기 위해 필요하다.

`chatRoomNo`는 메시지가 들어갈 채팅방을 찾기 위해 필요하다.

`ChatMessageReq`는 메시지 본문을 받기 위해 필요하다.

## 12. ChatServiceImpl

파일 위치:

`src/main/java/web/mvc/service/ChatServiceImpl.java`

역할:

채팅 관련 핵심 비즈니스 로직을 구현한다.

### 의존성 필드

```java
private final ChatRoomRepository chatRoomRepository;
private final ChatMessageRepository chatMessageRepository;
private final UserRepository userRepository;
```

`ChatRoomRepository`는 채팅방 생성/조회에 필요하다.

`ChatMessageRepository`는 메시지 저장/조회에 필요하다.

`UserRepository`는 userId로 실제 User 엔티티를 조회하기 위해 필요하다.

### createRoom

```java
@Transactional
public ChatRoomRes createRoom(String userId)
```

고객 문의 채팅방을 생성한다.

`@Transactional`이 필요한 이유는 DB에 채팅방을 저장하는 쓰기 작업이기 때문이다.

처리 순서:

1. `getUser(userId)`로 로그인 사용자를 DB에서 조회한다.
2. `ChatRoom.builder()`로 채팅방 엔티티를 만든다.
3. `user(user)`로 채팅방 소유자를 연결한다.
4. `createdAt(LocalDateTime.now())`로 생성 시간을 즉시 세팅한다.
5. `chatRoomRepository.save(chatRoom)`로 저장한다.
6. `ChatRoomRes`로 변환해 반환한다.

인수로 `userId`를 받는 이유는 컨트롤러가 `@AuthenticationPrincipal`에서 로그인 사용자를 꺼내 서비스에 전달하기 때문이다. 서비스는 이 값을 이용해 실제 `User` 엔티티를 찾는다.

### findAllRooms

```java
@Transactional
public List<ChatRoomRes> findAllRooms()
```

관리자 채팅방 목록을 조회한다.

`@Transactional`이 붙은 이유는 DTO 변환 중 LAZY 관계를 안전하게 읽기 위해서다. 현재 쿼리에서 `join fetch`를 쓰지만, 조회와 변환을 같은 트랜잭션 안에서 처리하는 것이 안전하다.

빈 목록이면 예외를 던지지 않고 빈 배열을 반환한다. 관리자 목록 조회는 데이터가 없을 수 있는 정상 상황이기 때문이다.

### findMessages

```java
@Transactional
public List<ChatMessageRes> findMessages(String userId, Long chatRoomNo)
```

특정 채팅방의 이전 메시지를 조회한다.

처리 순서:

1. `getChatRoom(chatRoomNo)`로 채팅방 존재 여부를 확인한다.
2. `validateParticipant(userId, chatRoom)`로 접근 권한을 검사한다.
3. `chatMessageRepository.findAllByChatRoomNoWithSender(chatRoomNo)`로 메시지를 조회한다.
4. `ChatMessageRes`로 변환해 반환한다.

`userId`를 받는 이유는 Security의 authenticated만으로는 “이 사람이 이 방의 참여자인지”를 알 수 없기 때문이다.

`chatRoomNo`를 받는 이유는 어떤 방의 메시지인지 알아야 하기 때문이다.

### sendMessage

```java
@Transactional
public ChatMessageRes sendMessage(String userId, Long chatRoomNo, ChatMessageReq chatMessageReq)
```

메시지를 저장하고 저장된 메시지를 응답 DTO로 반환한다.

처리 순서:

1. 메시지 내용이 null 또는 blank인지 검사한다.
2. `getUser(userId)`로 발신자를 조회한다.
3. `getChatRoom(chatRoomNo)`로 채팅방을 조회한다.
4. `validateParticipant(userId, chatRoom)`로 권한을 검사한다.
5. `ChatMessage` 엔티티를 만든다.
6. DB에 저장한다.
7. `ChatMessageRes`로 반환한다.

`chatMessageReq` 전체를 인수로 받는 이유는 지금은 `content` 하나뿐이지만, 나중에 이미지 URL, 메시지 타입, 첨부파일 ID 같은 필드가 추가될 수 있기 때문이다.

### getUser

```java
private User getUser(String userId)
```

userId로 User 엔티티를 조회하는 공통 메서드다.

사용자가 없으면 `USER_NOT_FOUND`를 던진다.

이 메서드를 둔 이유는 여러 메서드에서 같은 조회 로직을 반복하지 않기 위해서다.

### getChatRoom

```java
private ChatRoom getChatRoom(Long chatRoomNo)
```

chatRoomNo로 채팅방을 조회하는 공통 메서드다.

채팅방이 없으면 `CHAT_NOT_FOUND`를 던진다.

`findByIdWithUser`를 사용하는 이유는 권한 검사에 채팅방 소유자 정보가 필요하기 때문이다.

### validateParticipant

```java
private void validateParticipant(String userId, ChatRoom chatRoom)
```

채팅방 접근 권한을 검사한다.

관리자는 모든 방 접근 가능하다.

```java
if ("ROLE_ADMIN".equals(user.getRole())) {
    return;
}
```

일반 사용자는 자기 방만 접근 가능하다.

```java
if (!chatRoom.getUser().getUserId().equals(userId)) {
    throw new ChatException(ErrorCode.NOT_CHAT_ROOM_PARTICIPANT);
}
```

이 메서드가 필요한 이유는 HTTP 메시지 조회와 WebSocket 메시지 저장 모두 같은 권한 규칙을 가져야 하기 때문이다.

## 13. ChatController

파일 위치:

`src/main/java/web/mvc/controller/ChatController.java`

역할:

채팅 REST API를 담당한다.

### chatService

```java
private final ChatService chatService;
```

컨트롤러는 HTTP 요청을 받고 응답을 만드는 역할만 한다. 실제 비즈니스 로직은 서비스에 위임한다.

### createRoom

```java
@PostMapping("/api/chat/rooms")
public ResponseEntity<ApiResponse<ChatRoomRes>> createRoom(@AuthenticationPrincipal CustomUserDetails userDetails)
```

고객 문의 채팅방을 생성한다.

`@AuthenticationPrincipal CustomUserDetails userDetails`를 받는 이유는 현재 로그인한 사용자의 userId가 필요하기 때문이다.

body가 없는 이유는 현재 요구사항에서 채팅방 생성에 추가 입력값이 필요하지 않기 때문이다. 채팅방은 “현재 로그인한 고객” 기준으로 만들어진다.

반환 타입이 `ApiResponse<ChatRoomRes>`인 이유는 프로젝트의 공통 응답 형식을 유지하면서 생성된 채팅방 정보를 내려주기 위해서다.

### findAllRooms

```java
@GetMapping("/api/admin/chat/rooms")
public ResponseEntity<ApiResponse<List<ChatRoomRes>>> findAllRooms()
```

관리자가 전체 채팅방 목록을 조회한다.

인수가 없는 이유는 모든 채팅방을 조회하는 API이기 때문이다.

권한은 컨트롤러가 아니라 `SecurityConfig`에서 `ROLE_ADMIN`으로 제한한다.

### findMessages

```java
@GetMapping("/api/chat/rooms/{chatRoomNo}/messages")
public ResponseEntity<ApiResponse<List<ChatMessageRes>>> findMessages(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @PathVariable Long chatRoomNo
)
```

이전 메시지를 조회한다.

`@PathVariable Long chatRoomNo`를 받는 이유는 URL 자체가 특정 채팅방의 메시지를 의미하기 때문이다.

`@AuthenticationPrincipal`을 받는 이유는 로그인 사용자가 해당 방에 접근 가능한지 서비스에서 검사해야 하기 때문이다.

## 14. ChatMessageController

파일 위치:

`src/main/java/web/mvc/controller/ChatMessageController.java`

역할:

STOMP 메시지 발행을 처리한다.

REST 컨트롤러가 아니라 WebSocket 메시지를 받는 컨트롤러다.

### chatService

메시지를 DB에 저장하고 권한을 검사하기 위해 필요하다.

### messagingTemplate

```java
private final SimpMessagingTemplate messagingTemplate;
```

서버가 특정 구독 경로로 메시지를 보내기 위해 사용한다.

### sendMessage

```java
@MessageMapping("/chat/rooms/{chatRoomNo}")
public void sendMessage(
        Principal principal,
        @DestinationVariable Long chatRoomNo,
        @Payload ChatMessageReq chatMessageReq
)
```

클라이언트가 다음 destination으로 SEND하면 이 메서드가 실행된다.

```text
/pub/chat/rooms/{chatRoomNo}
```

`@MessageMapping`에는 `/pub`가 빠진다. `/pub`는 `WebSocketConfig`에서 application destination prefix로 설정했기 때문이다.

`Principal principal`을 받는 이유는 STOMP CONNECT 단계에서 인증된 사용자의 userId를 꺼내기 위해서다.

현재 `WebSocketConfig`에서 `UsernamePasswordAuthenticationToken`을 만들고, `CustomUserDetails.getUsername()`이 userId를 반환하므로 `principal.getName()`은 userId가 된다.

`@DestinationVariable Long chatRoomNo`는 destination 경로의 방 번호를 받는다.

`@Payload ChatMessageReq chatMessageReq`는 메시지 본문을 받는다.

처리 순서:

1. `chatService.sendMessage(...)`로 메시지를 저장한다.
2. 저장된 메시지 DTO를 받는다.
3. `messagingTemplate.convertAndSend(...)`로 구독자들에게 브로드캐스트한다.

```java
messagingTemplate.convertAndSend("/sub/chat/rooms/" + chatRoomNo, message);
```

이 경로를 구독 중인 클라이언트들이 실시간으로 메시지를 받는다.

## 15. WebSocketConfig

파일 위치:

`src/main/java/web/mvc/config/WebSocketConfig.java`

역할:

STOMP WebSocket 연결, 메시지 브로커, WebSocket 인증/권한 검사를 설정한다.

### 클래스 어노테이션

```java
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer
```

`@Configuration`은 Spring 설정 클래스라는 뜻이다.

`@EnableWebSocketMessageBroker`는 STOMP 메시지 브로커를 활성화한다.

`WebSocketMessageBrokerConfigurer`는 WebSocket endpoint와 broker 설정을 커스터마이징하기 위해 구현한다.

### jwtUtil

```java
private final JWTUtil jwtUtil;
```

STOMP CONNECT 요청의 Authorization 헤더에 담긴 JWT를 검증하기 위해 필요하다.

HTTP 요청은 `JWTFilter`가 검증하지만, WebSocket STOMP 메시지는 일반 HTTP 필터 흐름과 다르다. 그래서 STOMP inbound channel에서 별도로 검증한다.

### chatRoomRepository

```java
private final ChatRoomRepository chatRoomRepository;
```

SEND/SUBSCRIBE 대상 채팅방이 존재하는지, 현재 사용자가 해당 방 소유자인지 검사하기 위해 필요하다.

### registerStompEndpoints

```java
public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry.addEndpoint("/ws")
            .setAllowedOriginPatterns("*");
}
```

WebSocket 연결 endpoint를 등록한다.

클라이언트는 `/ws`로 WebSocket 연결을 시작한다.

`setAllowedOriginPatterns("*")`는 모든 origin을 허용한다. 개발 중에는 편하지만 운영 환경에서는 프론트엔드 도메인만 허용하는 것이 안전하다.

### configureMessageBroker

```java
public void configureMessageBroker(MessageBrokerRegistry registry) {
    registry.setApplicationDestinationPrefixes("/pub");
    registry.enableSimpleBroker("/sub");
}
```

`/pub`는 클라이언트가 서버로 메시지를 보낼 때 사용하는 prefix다.

```text
SEND /pub/chat/rooms/1
```

서버에서는 `/pub`를 제외한 `/chat/rooms/1`이 `@MessageMapping`과 매칭된다.

`/sub`는 클라이언트가 서버 메시지를 구독할 때 사용하는 prefix다.

```text
SUBSCRIBE /sub/chat/rooms/1
```

`enableSimpleBroker`는 메모리 기반 간단한 메시지 브로커를 사용한다는 뜻이다.

운영에서 서버가 여러 대가 되면 Redis, RabbitMQ 같은 외부 broker relay를 고려해야 한다.

### configureClientInboundChannel

```java
public void configureClientInboundChannel(ChannelRegistration registration)
```

클라이언트에서 서버로 들어오는 STOMP 프레임을 가로채기 위한 설정이다.

여기서 CONNECT, SEND, SUBSCRIBE를 검사한다.

### preSend

```java
public Message<?> preSend(Message<?> message, MessageChannel channel)
```

STOMP 메시지가 실제 처리되기 전에 실행된다.

`StompHeaderAccessor.wrap(message)`를 사용하는 이유는 STOMP command, destination, native header, user 정보를 읽고 쓰기 위해서다.

CONNECT일 때:

```java
accessor.setUser(createAuthentication(accessor.getFirstNativeHeader("Authorization")));
```

JWT를 검증하고 인증 객체를 STOMP 세션에 저장한다.

SEND 또는 SUBSCRIBE일 때:

```java
validateChatRoomAccess(accessor);
```

채팅방 접근 권한을 검사한다.

### createAuthentication

```java
private Authentication createAuthentication(String authorization)
```

STOMP CONNECT 헤더의 Authorization 값을 받아 Spring Security의 Authentication 객체로 바꾼다.

인수로 String authorization을 받는 이유는 STOMP native header에서 `Authorization` 헤더 하나만 꺼내면 JWT 검증이 가능하기 때문이다.

검사 내용:

- Authorization 헤더가 있는가
- `Bearer `로 시작하는가
- JWT가 만료되지 않았는가
- JWT에서 userNo, userId, userName, role을 읽을 수 있는가

JWT에서 읽은 값으로 임시 `User` 객체를 만든다.

```java
User user = new User();
user.setUserNo(jwtUtil.getUserNo(token));
user.setUserId(jwtUtil.getUserId(token));
user.setUserName(jwtUtil.getUserName(token));
user.setRole(jwtUtil.getRole(token));
```

DB 조회를 하지 않고 JWT claim 기반으로 인증 객체를 만드는 이유는 HTTP의 `JWTFilter`와 같은 방식으로 동작시키기 위해서다. 실제 DB 존재 여부나 권한은 서비스에서 필요한 경우 다시 확인한다.

반환 타입이 `Authentication`인 이유는 STOMP 세션의 principal로 저장하기 위해서다.

### validateChatRoomAccess

```java
private void validateChatRoomAccess(StompHeaderAccessor accessor)
```

SEND/SUBSCRIBE 요청이 해당 채팅방에 접근 가능한지 검사한다.

`StompHeaderAccessor`를 인수로 받는 이유는 destination과 user 정보를 모두 여기서 얻을 수 있기 때문이다.

검사 순서:

1. 인증 정보가 있는지 확인한다.
2. destination에서 chatRoomNo를 추출한다.
3. 관리자면 통과한다.
4. 일반 사용자면 채팅방을 조회한다.
5. 채팅방 소유자 userId와 현재 사용자 userId를 비교한다.

관리자 확인:

```java
boolean admin = authentication.getAuthorities().stream()
        .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
```

일반 사용자 확인:

```java
if (!chatRoom.getUser().getUserId().equals(authentication.getName())) {
    throw new AccessDeniedException("Not chat room participant");
}
```

### extractChatRoomNo

```java
private Long extractChatRoomNo(String destination)
```

STOMP destination 문자열에서 채팅방 번호를 추출한다.

지원하는 destination은 다음 두 가지다.

```text
/pub/chat/rooms/{chatRoomNo}
/sub/chat/rooms/{chatRoomNo}
```

반환 타입이 `Long`인 이유는 `ChatRoom.chatRoomNo` 타입이 Long이기 때문이다.

`destination`이 null이거나 채팅 경로가 아니면 null을 반환한다. 이렇게 한 이유는 같은 WebSocket 연결에서 향후 다른 STOMP destination이 추가될 수 있기 때문이다.

주의할 점은 현재 구현은 `{chatRoomNo}`가 숫자가 아니면 `Long.parseLong(value)`에서 예외가 발생한다. 운영 수준으로 다듬는다면 여기서 `INVALID_INPUT`에 해당하는 예외로 감싸는 것이 좋다.

## 16. SecurityConfig

파일 위치:

`src/main/java/web/mvc/config/SecurityConfig.java`

추가한 경로 설정:

```java
.requestMatchers("/ws", "/ws/**").permitAll()
.requestMatchers(HttpMethod.POST, "/api/chat/rooms").hasRole("USER")
.requestMatchers(HttpMethod.GET, "/api/admin/chat/rooms").hasRole("ADMIN")
.requestMatchers(HttpMethod.GET, "/api/chat/rooms/*/messages").authenticated()
```

### /ws를 permitAll로 둔 이유

WebSocket handshake는 일반 HTTP 요청으로 시작되지만, STOMP CONNECT 인증은 handshake 이후에 들어온다.

`/ws`를 SecurityFilterChain에서 막아버리면 STOMP CONNECT까지 도달하지 못할 수 있다.

그래서 HTTP handshake는 열고, 실제 로그인 검사는 `WebSocketConfig`의 CONNECT 처리에서 한다.

### POST /api/chat/rooms는 ROLE_USER

고객 문의 채팅방 생성은 일반 고객이 하는 기능이다. 그래서 관리자나 비로그인 사용자가 아니라 `ROLE_USER`만 허용했다.

### GET /api/admin/chat/rooms는 ROLE_ADMIN

전체 채팅방 목록은 관리자 화면 기능이다. 모든 고객의 채팅방 정보가 보이므로 관리자만 허용해야 한다.

### GET /api/chat/rooms/*/messages는 authenticated

관리자도, 일반 사용자도 이전 메시지를 볼 수 있다.

다만 일반 사용자가 남의 방을 보면 안 되므로 URL 레벨에서는 로그인만 검사하고, 실제 방 참여자 검사는 서비스에서 한다.

## 17. 권한 검사 위치를 나눈 이유

권한 검사는 두 단계로 나뉜다.

### 1차: SecurityConfig

URL 자체에 접근 가능한 역할을 막는다.

예:

- 관리자 채팅방 목록은 ROLE_ADMIN만
- 고객 채팅방 생성은 ROLE_USER만
- 이전 메시지 조회는 로그인만

이 단계는 coarse-grained authorization이다. 즉 큰 단위의 접근 제어다.

### 2차: ChatService 또는 WebSocketConfig

특정 채팅방에 접근 가능한지 검사한다.

예:

- 일반 유저 A가 유저 B의 채팅방 메시지를 조회하면 안 된다.
- 일반 유저 A가 유저 B의 채팅방을 구독하면 안 된다.
- 일반 유저 A가 유저 B의 채팅방에 메시지를 보내면 안 된다.

이 단계는 resource ownership authorization이다. 즉 리소스 소유권 검사다.

URL 권한만으로는 이 검사를 할 수 없다. 그래서 서비스와 WebSocket inbound interceptor에서 별도 검사한다.

## 18. 클라이언트 사용 흐름

### 1. 고객이 채팅방 생성

```http
POST /api/chat/rooms
Authorization: Bearer JWT_TOKEN
```

응답:

```json
{
  "status": 200,
  "message": "요청에 성공했습니다.",
  "data": {
    "chatRoomNo": 1,
    "userNo": 10,
    "userId": "user01",
    "userName": "홍길동",
    "createdAt": "2026-06-09T22:10:00"
  }
}
```

### 2. WebSocket 연결

```text
CONNECT /ws
Authorization: Bearer JWT_TOKEN
```

STOMP CONNECT native header에 Authorization을 넣어야 한다.

### 3. 채팅방 구독

```text
SUBSCRIBE /sub/chat/rooms/1
```

서버는 구독 요청에서 채팅방 접근 권한을 검사한다.

### 4. 메시지 발행

```text
SEND /pub/chat/rooms/1
```

payload:

```json
{
  "content": "상품 배송 문의드립니다."
}
```

서버는 메시지를 DB에 저장한 뒤 다음 경로로 브로드캐스트한다.

```text
/sub/chat/rooms/1
```

### 5. 이전 메시지 조회

```http
GET /api/chat/rooms/1/messages
Authorization: Bearer JWT_TOKEN
```

## 19. 채팅 기능 구현 시 고려해야 할 점

### 인증

HTTP와 WebSocket은 인증 흐름이 다르다.

HTTP는 `JWTFilter`에서 Authorization 헤더를 검사한다.

WebSocket/STOMP는 `CONNECT` 프레임에서 Authorization 헤더를 검사해야 한다.

### 권한

관리자는 모든 방에 접근 가능하다.

일반 사용자는 자기 방에만 접근 가능하다.

이 규칙은 메시지 조회, 메시지 발행, 메시지 구독 모두에서 동일해야 한다.

### 저장

실시간으로 보낸 메시지도 DB에 저장해야 한다.

저장하지 않으면 이전 메시지 조회 기능을 만들 수 없다.

### 정렬

이전 메시지는 `sentAt asc`가 자연스럽다.

관리자 채팅방 목록은 `createdAt desc`가 자연스럽다.

### DTO

엔티티를 그대로 반환하면 안 된다.

특히 User 엔티티에는 password가 있다. 채팅 응답에는 필요한 사용자 정보만 DTO에 담아야 한다.

### 트랜잭션

조회 후 DTO 변환에서 LAZY 연관관계를 읽을 수 있다.

그래서 서비스 메서드에 `@Transactional`을 붙이고, 필요한 관계는 fetch join으로 가져오는 것이 안전하다.

### 운영 확장

현재는 `enableSimpleBroker("/sub")`를 사용한다.

이 방식은 단일 서버에서는 간단하고 충분하다.

하지만 서버가 여러 대로 늘어나면 각 서버의 메모리 브로커가 분리된다. 그 경우 Redis, RabbitMQ 같은 외부 메시지 브로커 연동을 고려해야 한다.

### 보안 강화

현재 `setAllowedOriginPatterns("*")`로 모든 Origin을 허용한다.

개발 중에는 편하지만 운영에서는 프론트엔드 도메인만 허용하는 것이 맞다.

예:

```java
.setAllowedOriginPatterns("https://shop.example.com")
```

## 20. 현재 구현의 개선 후보

현재 구현은 기능 동작을 위한 기본 구조다. 다음 개선을 고려할 수 있다.

1. 채팅방 중복 생성 방지

현재는 사용자가 여러 채팅방을 만들 수 있다. 고객당 열린 문의방을 하나만 허용하려면 `ChatRoomRepository`에 사용자별 조회 메서드를 추가해야 한다.

2. 메시지 타입 추가

현재 메시지는 텍스트만 지원한다. 이미지, 파일, 시스템 메시지를 지원하려면 `messageType` 필드를 추가할 수 있다.

3. 읽음 처리

관리자 또는 고객이 메시지를 읽었는지 표시하려면 `readAt`, `readByAdmin`, `readByUser` 같은 필드가 필요하다.

4. 채팅방 상태

문의 처리 상태를 관리하려면 `OPEN`, `CLOSED` 같은 상태 필드를 `ChatRoom`에 추가할 수 있다.

5. WebSocket 에러 응답 정리

현재 WebSocket 권한 오류는 `AccessDeniedException` 중심이다. 클라이언트가 에러를 명확히 처리하게 하려면 STOMP ERROR frame 응답 정책을 별도로 잡는 것이 좋다.

6. Origin 제한

운영 환경에서는 `setAllowedOriginPatterns("*")`를 제거하고 허용 도메인을 명시해야 한다.

7. 메시지 길이 정책

DB 컬럼은 1000자로 제한했지만, 서비스에서 길이 초과 검증도 추가하면 더 명확한 에러를 줄 수 있다.

## 21. 파일별 요약

| 파일 | 추가 이유 |
| --- | --- |
| `ChatRoom.java` | 채팅방을 DB에 저장하기 위해 추가 |
| `ChatMessage.java` | 메시지를 DB에 저장하고 이전 메시지 조회를 지원하기 위해 추가 |
| `ChatMessageReq.java` | STOMP 메시지 발행 payload를 받기 위해 추가 |
| `ChatRoomRes.java` | 채팅방 정보를 안전하게 응답하기 위해 추가 |
| `ChatMessageRes.java` | 메시지와 발신자 정보를 안전하게 응답하기 위해 추가 |
| `ChatRoomRepository.java` | 채팅방 조회와 권한 검사에 필요한 fetch join 쿼리를 위해 추가 |
| `ChatMessageRepository.java` | 특정 방의 이전 메시지를 정렬 조회하기 위해 추가 |
| `ChatService.java` | 채팅 비즈니스 기능을 컨트롤러와 분리하기 위해 추가 |
| `ChatServiceImpl.java` | 채팅방 생성, 메시지 조회, 메시지 저장, 참여자 검사를 구현하기 위해 추가 |
| `ChatController.java` | 채팅 REST API를 제공하기 위해 추가 |
| `ChatMessageController.java` | STOMP 메시지 발행을 처리하기 위해 추가 |
| `WebSocketConfig.java` | `/ws`, `/pub`, `/sub`, STOMP JWT 인증과 구독/발행 권한 검사를 위해 추가 |
| `SecurityConfig.java` | 채팅 REST API와 WebSocket handshake 경로 권한을 설정하기 위해 수정 |
| `pom.xml` | WebSocket/STOMP 기능 사용을 위해 의존성 추가 |

## 22. 핵심 흐름 정리

고객 채팅방 생성:

```text
POST /api/chat/rooms
-> SecurityConfig에서 ROLE_USER 확인
-> ChatController.createRoom
-> ChatService.createRoom
-> User 조회
-> ChatRoom 저장
-> ChatRoomRes 반환
```

이전 메시지 조회:

```text
GET /api/chat/rooms/{chatRoomNo}/messages
-> SecurityConfig에서 로그인 확인
-> ChatController.findMessages
-> ChatService.findMessages
-> ChatRoom 조회
-> 관리자 또는 방 소유자 확인
-> 메시지 목록 조회
-> ChatMessageRes 목록 반환
```

WebSocket 메시지 발행:

```text
CONNECT /ws with Authorization header
-> WebSocketConfig.createAuthentication
-> STOMP 세션에 Authentication 저장

SEND /pub/chat/rooms/{chatRoomNo}
-> WebSocketConfig.validateChatRoomAccess
-> ChatMessageController.sendMessage
-> ChatService.sendMessage
-> 메시지 저장
-> /sub/chat/rooms/{chatRoomNo}로 브로드캐스트
```

WebSocket 구독:

```text
SUBSCRIBE /sub/chat/rooms/{chatRoomNo}
-> WebSocketConfig.validateChatRoomAccess
-> 관리자면 통과
-> 일반 유저면 채팅방 소유자 확인
-> 통과 시 메시지 수신 가능
```
