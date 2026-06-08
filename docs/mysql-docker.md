# MySQL Docker Setup

## Local or EC2 setup

1. Create an environment file from the sample.

```bash
cp .env.example .env
```

2. Change the passwords in `.env`.

```properties
MYSQL_PASSWORD=...
MYSQL_ROOT_PASSWORD=...
DB_PASSWORD=...
JWT_SECRET=...
```

3. Start MySQL.

```bash
docker compose up -d mysql
```

4. Check the container status.

```bash
docker compose ps
docker compose logs -f mysql
```

## Spring Boot connection

The default application settings connect to:

```properties
DB_URL=jdbc:mysql://localhost:3306/shop?serverTimezone=Asia/Seoul&characterEncoding=UTF-8
DB_USERNAME=shop_user
DB_PASSWORD=change_me_shop_password
```

If the app also runs in Docker later, use the compose service name instead:

```properties
DB_URL=jdbc:mysql://mysql:3306/shop?serverTimezone=Asia/Seoul&characterEncoding=UTF-8
```

## EC2 security note

The compose file binds MySQL to `127.0.0.1` by default. This keeps port `3306` closed to the public internet.

For external admin access, prefer an SSH tunnel:

```bash
ssh -L 3306:127.0.0.1:3306 ec2-user@YOUR_EC2_PUBLIC_IP
```

Then connect your database client to `localhost:3306`.

## Backup

Run backups from the EC2 host:

```bash
docker exec shop-mysql mysqldump -u root -p shop > shop-backup.sql
```

For production-like use, automate this with cron and upload backups to S3.
