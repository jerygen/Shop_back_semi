# Local Auth Test

The main `application.properties` uses environment variables for Docker or EC2 MySQL.

The `test` profile loads:

```text
src/test/resources/application-test.properties
```

This profile uses a local MySQL test database on port `3307`.

Start the test database first:

```bash
cd Shop
docker compose -f docker-compose.test.yml up -d mysql-test
```

Then run the test from IntelliJ:

```text
src/test/java/web/mvc/UserAuthIntegrationTest.java
```

Open the file and click the run icon next to `registerLoginLogout()`.

The test database is created from:

```text
docker-compose.test.yml
```

The default test connection is:

```properties
spring.datasource.url=jdbc:mysql://localhost:3307/shop_test?serverTimezone=Asia/Seoul&characterEncoding=UTF-8
spring.datasource.username=shop_test_user
spring.datasource.password=shop_test_password
```

The auth integration test is:

```text
src/test/java/web/mvc/UserAuthIntegrationTest.java
```

It checks:

- `POST /register`
- `POST /login`
- `POST /logout` with the JWT returned from login
