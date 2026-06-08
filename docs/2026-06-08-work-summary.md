# 2026-06-08 작업 정리

## 개요

오늘 작업의 핵심은 쇼핑몰 프로젝트의 기본 구조를 정리하고, Spring Security 기반 회원가입/로그인/JWT 인증 흐름을 로컬 MySQL 환경에서 테스트할 수 있게 만드는 것이었다.

확인 기준:

- 오늘 커밋: `9fd2a4c 프로젝트 공통 및 구조 세팅 + 로그인`
- 추가 확인한 미커밋 변경: 테스트 DB 설정, 테스트 프로필 적용, 로컬 인증 테스트 문서 수정

## 추가한 것

### 1. 로그인/인증 기능

- `UserController`를 추가해 회원가입(`/register`)과 로그아웃(`/logout`) 엔드포인트를 구성했다.
- `UserService`, `UserServiceImpl`을 추가해 회원가입 시 중복 아이디를 검사하고 비밀번호를 BCrypt로 암호화하도록 했다.
- `LoginFilter`, `JWTFilter`, `JWTUtil`을 통해 로그인 성공 시 JWT를 발급하고, 이후 요청에서 `Authorization: Bearer ...` 토큰을 검증하도록 구성했다.
- `CustomDetailsService`, `CustomUserDetails`, `SecurityConfig`를 정리해 Spring Security 인증 흐름과 사용자 도메인을 연결했다.

추가 이유:

- API 기반 쇼핑몰에서 세션 로그인보다 JWT 기반 인증이 REST 요청 처리에 적합하다.
- 회원가입, 로그인, 로그아웃이 먼저 안정화되어야 이후 고객/관리자 기능에서 인증된 사용자 정보를 활용할 수 있다.

### 2. 공통 응답과 예외 처리 구조

- `ApiResponse`, `ErrorResponse`를 추가해 성공/실패 응답 형식을 분리했다.
- `DefaultExceptionAdvice`를 추가해 도메인 예외를 HTTP 상태 코드와 에러 응답으로 변환하도록 했다.
- `UserException`, `ProductException`, `OrderException`, `CartException`, `ChatException` 등 도메인별 예외 클래스를 추가했다.
- `ErrorCode`에 사용자, 상품, 주문, 장바구니, 채팅 관련 에러 코드를 정리했다.

추가 이유:

- 컨트롤러마다 예외 응답을 직접 만들면 응답 형식이 쉽게 달라진다.
- 기능이 늘어날수록 예외 코드와 메시지를 중앙에서 관리하는 편이 유지보수에 유리하다.

### 3. 상품 도메인 기초 구조

- `Product` 엔티티와 `ProductRepository`를 추가했다.

추가 이유:

- 쇼핑몰의 핵심 도메인인 상품 기능을 이후 구현할 수 있도록 JPA 엔티티와 저장소의 기본 뼈대를 먼저 마련했다.

### 4. 로컬/테스트 MySQL 실행 환경

- `docker-compose.yml`을 추가해 로컬 MySQL 컨테이너를 실행할 수 있게 했다.
- `docker-compose.test.yml`을 추가해 테스트 전용 MySQL 컨테이너를 별도 포트(`3307`)로 실행할 수 있게 했다.
- `.env.example`을 추가해 DB 계정, 비밀번호, JWT Secret 같은 환경 변수를 샘플로 남겼다.

추가 이유:

- 개발 DB와 테스트 DB를 분리해야 테스트가 실제 개발 데이터에 영향을 주지 않는다.
- Docker 기반 DB 실행 방법을 정리해 다른 PC나 EC2에서도 같은 방식으로 환경을 재현할 수 있다.

### 5. 문서와 인프라 초안

- 기존 문서와 ERD를 `docs/` 폴더로 정리했다.
- `docs/mysql-docker.md`에 MySQL Docker 실행 방법을 추가했다.
- `docs/terraform-ec2-mysql.md`와 `infra/terraform/` 하위 파일을 추가해 EC2와 MySQL 배포를 위한 Terraform 초안을 만들었다.
- `docs/local-auth-test.md`에 로컬 인증 테스트 실행 절차를 정리했다.

추가 이유:

- 프로젝트 구조, 로컬 실행, 배포 준비 자료가 흩어져 있으면 작업을 이어가기 어렵다.
- 인프라 초안을 먼저 만들어두면 이후 EC2 배포 작업에서 필요한 값과 흐름을 빠르게 확인할 수 있다.

## 테스트한 것

### 1. 인증 통합 테스트 추가

`UserAuthIntegrationTest.registerLoginLogout()` 테스트를 추가했다.

검증 내용:

- `POST /register`로 회원가입 요청이 성공하는지 확인
- `POST /login`으로 로그인 요청이 성공하는지 확인
- 로그인 응답 헤더에 `Authorization` 값이 존재하는지 확인
- 발급된 JWT가 `Bearer `로 시작하는지 확인
- 발급받은 JWT를 사용해 `POST /logout` 요청이 성공하는지 확인

테스트 이유:

- 회원가입, 로그인, JWT 발급, 인증 헤더 사용 흐름은 서로 연결되어 있어서 단위 테스트보다 통합 테스트로 확인하는 것이 적합하다.
- 로그인 성공 후 실제로 토큰이 내려오는지 확인해야 이후 인증이 필요한 API 테스트를 확장할 수 있다.

### 2. 테스트 프로필 적용

- `UserAuthIntegrationTest`에 `@ActiveProfiles("test")`를 적용했다.
- `ShopApplicationTests`에도 `@ActiveProfiles("test")`를 추가했다.

수정 이유:

- 테스트 실행 시 기본 `application.properties`를 사용하면 개발 DB(`3306`)에 연결될 수 있다.
- 테스트는 `application-test.properties`와 테스트 DB(`3307`)를 사용해야 데이터 오염을 막을 수 있다.

### 3. 테스트 실행 시도 결과

현재 작업 환경에서 `mvnw.cmd test`와 `cmd /c mvnw.cmd test`를 실행해 확인했다.

결과:

- 애플리케이션 테스트까지 진입하지 못하고 Maven wrapper 단계에서 실패했다.
- 오류 메시지: `Cannot start maven from wrapper`

현재 판단:

- 테스트 코드 실패라기보다 Maven wrapper 실행 또는 Maven 배포본 준비 문제로 보인다.
- IntelliJ에서 `UserAuthIntegrationTest.registerLoginLogout()`을 직접 실행하는 방식으로 문서를 조정했다.

## 수정이 필요했던 것

### 1. 테스트 DB 계정 불일치

수정한 파일:

- `Shop/docker-compose.test.yml`
- `Shop/src/test/resources/application-test.properties`

변경 내용:

- 테스트 DB 기본 계정을 로컬 실행에 맞춰 `admin / admin1234` 기준으로 변경했다.
- 테스트 DB 접속 URL을 `localhost:3307/shop_test`로 고정했다.

수정 이유:

- Docker 테스트 DB 계정과 Spring 테스트 프로필의 접속 계정이 다르면 통합 테스트가 DB 연결 단계에서 실패한다.
- 로컬에서 바로 실행 가능한 값을 맞춰 테스트 진입 장벽을 낮췄다.

### 2. 기본 애플리케이션 DB 설정 조정

수정한 파일:

- `Shop/src/main/resources/application.properties`

변경 내용:

- 로컬 MySQL 접속 정보를 `localhost:3306/shop`, `admin / admin1234`로 맞췄다.
- `spring.jpa.hibernate.ddl-auto`를 `create`로 변경했다.

수정 이유:

- 로컬 개발 중에는 스키마를 빠르게 재생성하며 회원가입/로그인 흐름을 확인해야 했다.
- 실제 DB 계정과 애플리케이션 설정이 맞지 않으면 앱 실행 자체가 실패한다.

주의할 점:

- `ddl-auto=create`는 실행 시 테이블을 다시 만들 수 있으므로 운영 또는 보존해야 하는 개발 DB에는 위험하다.
- 배포나 공동 개발 환경에서는 다시 환경 변수 기반 설정과 `update` 또는 더 보수적인 설정으로 돌리는 것이 좋다.

### 3. 로컬 인증 테스트 문서 수정

수정한 파일:

- `docs/local-auth-test.md`

변경 내용:

- 전체 Maven 테스트 실행 안내 대신 IntelliJ에서 `UserAuthIntegrationTest.registerLoginLogout()`을 직접 실행하는 흐름으로 수정했다.

수정 이유:

- 현재 환경에서는 Maven wrapper가 정상 실행되지 않아 `mvnw.cmd test`로 테스트를 끝까지 확인할 수 없었다.
- 당장 확인해야 하는 범위는 인증 통합 테스트였기 때문에, IntelliJ 단일 테스트 실행 절차를 문서화했다.

### 4. 회원가입 응답 형식 조정

수정한 파일:

- `Shop/src/main/java/web/mvc/controller/UserController.java`

변경 내용:

- 회원가입 성공 응답을 메시지 포함 성공 응답에서 `ApiResponse.created(user)` 형식으로 바꿨다.

수정 이유:

- 회원 생성이라는 동작 의미에 맞춰 응답 형식을 더 명확하게 분리하려는 의도로 보인다.
- 다만 `created` 응답이 실제 HTTP 상태 코드 `201 Created`까지 반영하는지는 추가 확인이 필요하다.

## 추가로 확인하거나 정리하면 좋은 것

### 1. Maven wrapper 실행 문제

현재 `mvnw.cmd test`가 wrapper 단계에서 실패한다.

확인 필요:

- `.mvn/wrapper/maven-wrapper.properties`의 Maven 배포본 다운로드/해석 문제
- 로컬 Maven 설치 여부
- IntelliJ 내장 Maven으로는 실행되는지 여부

### 2. 한글 문자열 인코딩 깨짐

일부 Java 파일의 한글 메시지와 주석이 깨진 상태로 보인다.

예시:

- `UserController`
- `SecurityConfig`
- `JWTFilter`
- `JWTUtil`
- `ErrorCode`

수정 필요 이유:

- API 응답 메시지가 깨진 문자열로 내려갈 수 있다.
- 주석과 로그 의미를 파악하기 어려워 유지보수성이 떨어진다.

### 3. 운영 설정과 로컬 설정 분리

현재 일부 설정이 로컬 테스트 편의를 위해 하드코딩되어 있다.

정리 필요 이유:

- DB 비밀번호와 JWT Secret은 코드에 고정하지 않는 편이 안전하다.
- 로컬, 테스트, 운영 설정을 프로필 또는 환경 변수로 분리해야 배포 시 사고를 줄일 수 있다.

### 4. 고객 상품 목록 API 미완성 코드 정리

현재 `CustomerController`에 `/api/products` 상품 목록 조회 API를 추가하던 흔적이 있다.

확인된 상태:

- `CustomerService` 주입과 `GET /api/products` 엔드포인트 선언이 추가됐다.
- `return ResponseEntity.ok()` 뒤가 완성되지 않아 현재 상태로는 컴파일이 실패할 수 있다.

수정 필요 이유:

- 미완성 메서드는 Maven/IDE 빌드 단계에서 바로 오류를 만들 수 있다.
- 상품 목록 조회를 구현하려면 `customerService`에 상품 목록 조회 메서드를 정의하고, 컨트롤러에서 그 결과를 반환하도록 마무리해야 한다.

## 현재 남아 있는 변경 파일

아래 파일들은 오늘 커밋 이후에도 추가 수정된 상태다.

- `Shop/docker-compose.test.yml`
- `Shop/src/main/java/web/mvc/controller/CustomerController.java`
- `Shop/src/main/java/web/mvc/controller/UserController.java`
- `Shop/src/main/resources/application.properties`
- `Shop/src/test/java/web/mvc/ShopApplicationTests.java`
- `Shop/src/test/resources/application-test.properties`
- `docs/local-auth-test.md`
- `docs/2026-06-08-work-summary.md`
