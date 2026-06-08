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

Then run the tests:

```bash
./mvnw test
```

On Windows:

```powershell
cd Shop
.\mvnw.cmd test
```

The test database is created from:

```text
docker-compose.test.yml
```

The default test connection is:

```properties
TEST_DB_URL=jdbc:mysql://localhost:3307/shop_test?serverTimezone=Asia/Seoul&characterEncoding=UTF-8
TEST_DB_USERNAME=shop_test_user
TEST_DB_PASSWORD=shop_test_password
```

The auth integration test is:

```text
src/test/java/web/mvc/UserAuthIntegrationTest.java
```

It checks:

- `POST /register`
- `POST /login`
- `POST /logout` with the JWT returned from login
