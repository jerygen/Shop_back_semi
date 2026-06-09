# Shop 프로젝트 배포 및 CI/CD 작업 정리

## 1. 배포 목표

이 프로젝트는 개인 프로젝트 공유를 목적으로 AWS EC2 한 대 위에 Docker 기반으로 배포한다.

배포 구조는 다음과 같다.

```text
AWS EC2
└── Docker
    ├── shop-app    # Spring Boot 애플리케이션 컨테이너
    └── shop-mysql  # MySQL 컨테이너
```

Spring Boot 앱은 같은 Docker Compose 네트워크 안에서 MySQL에 접속한다.

```text
jdbc:mysql://mysql:3306/shop
```

외부 사용자에게 공개되는 포트는 Spring Boot 앱 포트 `9000`이다. MySQL 포트 `3306`은 외부 공개하지 않는 것을 기본 정책으로 한다.

## 2. 수정 및 추가된 주요 파일

### 2.1 Docker

| 파일 | 역할 |
|---|---|
| `Shop/Dockerfile` | Spring Boot 앱 Docker 이미지 빌드 |
| `Shop/.dockerignore` | Docker 이미지 빌드 시 불필요한 파일 제외 |
| `Shop/docker-compose.yml` | 앱 컨테이너와 MySQL 컨테이너를 함께 실행 |
| `Shop/.env.example` | 배포 환경 변수 예시 |

`docker-compose.yml`은 `app`, `mysql` 두 서비스를 가진다.

- `app`: Spring Boot 컨테이너
- `mysql`: MySQL 8.4 컨테이너
- `app`은 `mysql` healthcheck가 통과한 뒤 실행된다.
- `app`은 `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET` 등 환경 변수를 사용한다.

### 2.2 Spring Boot 설정

| 파일 | 역할 |
|---|---|
| `Shop/src/main/resources/application.properties` | 배포용 기본 Spring 설정 |

현재 주요 설정은 다음 기준이다.

```properties
server.port=${SERVER_PORT:9000}
spring.datasource.url=${DB_URL:jdbc:mysql://mysql:3306/shop?...}
spring.datasource.username=${DB_USERNAME:admin}
spring.datasource.password=${DB_PASSWORD:admin1234}
spring.jpa.hibernate.ddl-auto=${JPA_DDL_AUTO:update}
spring.jpa.show-sql=${JPA_SHOW_SQL:false}
spring.jwt.secret=${JWT_SECRET:change_me_to_a_long_random_secret}
```

운영 배포 시에는 `.env` 또는 Terraform 변수에서 실제 값으로 주입한다.

### 2.3 Terraform

| 파일 | 역할 |
|---|---|
| `infra/terraform/main.tf` | EC2, 보안 그룹, SSH key pair 생성 |
| `infra/terraform/variables.tf` | Terraform 입력 변수 정의 |
| `infra/terraform/user_data.sh.tftpl` | EC2 부팅 시 Docker/Compose 설치 및 컨테이너 실행 |
| `infra/terraform/outputs.tf` | EC2 public IP, 앱 URL, SSH 명령어 출력 |
| `infra/terraform/terraform.tfvars.example` | Terraform 변수 예시 |
| `infra/terraform/terraform.tfvars` | 실제 로컬 Terraform 변수 파일, Git 추적 제외 |

현재 Terraform은 EC2 한 대를 생성하고, 해당 EC2 안에 Docker와 Docker Compose plugin을 설치한다. 이후 `/opt/shop/.env`, `/opt/shop/docker-compose.yml`을 생성하고 MySQL 컨테이너를 실행한다.

앱 이미지를 pull할 수 있으면 `shop-app` 컨테이너도 함께 실행한다. 앱 이미지가 아직 없으면 MySQL만 먼저 실행하고, 이후 GitHub Actions 배포 단계에서 앱 컨테이너를 올린다.

### 2.4 GitHub Actions

| 파일 | 역할 |
|---|---|
| `.github/workflows/deploy.yml` | 테스트, Docker 이미지 빌드/푸시, EC2 배포 자동화 |

workflow는 `main` 브랜치 push 또는 수동 실행 시 동작한다.

```text
test -> build -> deploy
```

## 3. Terraform 구성 요약

Terraform이 생성하는 리소스는 다음과 같다.

- 기본 VPC 조회
- 기본 서브넷 조회
- Amazon Linux 2023 AMI 조회
- SSH key pair 생성
- EC2 보안 그룹 생성
- EC2 인스턴스 생성
- 로컬 private key 파일 생성

보안 그룹 정책은 다음과 같다.

| 포트 | 용도 | 공개 범위 |
|---|---|---|
| 22 | SSH 관리 접속 | `allowed_ssh_cidr` |
| 9000 | Spring Boot 앱 접속 | `allowed_app_cidr` |
| 3306 | MySQL 접속 | 기본 비공개, `expose_mysql=true`일 때만 허용 |

현재 권장값:

```hcl
allowed_ssh_cidr = "125.178.1.177/32"
allowed_app_cidr = "0.0.0.0/0"
expose_mysql = false
```

의미:

- SSH는 내 공인 IP에서만 접속 가능
- 앱은 외부 사용자에게 공개
- MySQL은 외부 직접 접속 차단

## 4. terraform.tfvars 작성 내용

현재 로컬에 생성한 `infra/terraform/terraform.tfvars`는 다음 기준으로 작성했다.

```hcl
aws_region    = "ap-northeast-2"
project_name  = "shop"
instance_type = "t3.micro"

key_pair_name = "shop"

allowed_ssh_cidr = "125.178.1.177/32"

app_image        = "ghcr.io/jerygen/shop_back_semi:latest"
app_port         = 9000
allowed_app_cidr = "0.0.0.0/0"

expose_mysql       = false
allowed_mysql_cidr = "125.178.1.177/32"

root_volume_size = 30

mysql_database      = "shop"
mysql_user          = "admin"
mysql_password      = "admin1234"
mysql_root_password = "admin1234"

jpa_ddl_auto = "update"
jpa_show_sql = false
jwt_secret   = "shop-jwt-secret-2026-change-this-before-public-release"
```

주의:

- `terraform.tfvars`는 비밀번호와 JWT secret이 들어가므로 Git에 올리지 않는다.
- 현재 `.gitignore`에서 `**/terraform.tfvars`가 제외되어 있다.
- `jwt_secret`은 공개 배포 전 더 긴 랜덤 문자열로 교체하는 것이 좋다.

## 5. GitHub Actions 동작 흐름

### 5.1 Test

GitHub Actions에서 MySQL 8.4 서비스 컨테이너를 실행한다.

기준 설정:

```text
MYSQL_DATABASE=shop
MYSQL_USER=admin
MYSQL_PASSWORD=admin1234
MYSQL_ROOT_PASSWORD=admin1234
port=3306
```

이후 `Shop` 디렉터리에서 Maven 테스트를 실행한다.

```bash
./mvnw -q test
```

### 5.2 Build and Push Image

테스트가 통과하면 Docker 이미지를 빌드한다.

이미지 이름은 GitHub repository 이름을 기준으로 만든다.

```text
ghcr.io/jerygen/shop_back_semi:<commit-sha>
ghcr.io/jerygen/shop_back_semi:latest
```

이미지는 GHCR에 push된다.

### 5.3 Deploy to EC2

배포 job은 EC2에 SSH로 접속한다.

EC2 내부 작업:

```bash
cd /opt/shop
APP_IMAGE 갱신
docker compose pull app
docker compose up -d app
docker image prune -f
```

즉, Terraform이 만든 EC2 배포 환경 위에 GitHub Actions가 새 앱 이미지를 반영한다.

## 6. GitHub Secrets

GitHub repository의 `Settings -> Secrets and variables -> Actions`에 다음 값을 등록해야 한다.

| Secret | 설명 |
|---|---|
| `EC2_HOST` | Terraform output의 `public_ip` |
| `EC2_SSH_KEY` | Terraform이 생성한 `generated/shop.pem` 파일 내용 전체 |
| `EC2_USER` | 선택값, 기본 `ec2-user` |
| `GHCR_USERNAME` | GHCR private image pull용 GitHub 사용자명 |
| `GHCR_TOKEN` | GHCR private image pull용 token |

참고:

- GHCR package를 public으로 공개하면 `GHCR_USERNAME`, `GHCR_TOKEN` 없이도 pull 가능하다.
- private image를 유지하려면 `GHCR_TOKEN`에 `read:packages` 권한이 필요하다.

## 7. 배포 실행 순서

### 7.1 로컬 준비

Terraform CLI 설치:

```powershell
terraform -version
```

AWS CLI 설치 및 인증:

```powershell
aws configure
aws sts get-caller-identity
```

### 7.2 Terraform 실행

```powershell
cd C:\KOSTA_Projects\Shop\infra\terraform
terraform init
terraform plan
terraform apply
```

성공하면 output에서 다음 값을 확인한다.

- `public_ip`
- `app_url`
- `ssh_command`
- `private_key_path`

### 7.3 GitHub Secrets 등록

Terraform output의 `public_ip`를 `EC2_HOST`에 등록한다.

`generated/shop.pem` 파일 전체 내용을 `EC2_SSH_KEY`에 등록한다.

### 7.4 Git push

`main` 브랜치에 push하면 GitHub Actions가 자동 실행된다.

```bash
git push origin main
```

배포가 성공하면 다음 주소로 접속한다.

```text
http://<EC2_PUBLIC_IP>:9000
```

## 8. 운영 시 주의사항

### 8.1 비밀번호와 JWT secret

현재 일부 예시 값은 학습/개인 프로젝트용이다.

공개 배포 전 다음 값은 강한 값으로 교체한다.

- `mysql_password`
- `mysql_root_password`
- `jwt_secret`

### 8.2 MySQL 볼륨

MySQL 데이터는 Docker volume `mysql_data`에 저장된다.

EC2 root block device는 다음 설정이다.

```hcl
delete_on_termination = false
```

EC2 삭제 시 EBS 볼륨이 남을 수 있으므로, 과금 관리를 위해 삭제 여부를 따로 확인해야 한다.

### 8.3 앱 공개 범위

현재 앱 포트는 전체 공개다.

```hcl
allowed_app_cidr = "0.0.0.0/0"
```

포트폴리오 공유 목적에는 적합하지만, 운영 환경에서는 Nginx와 HTTPS를 추가하는 것이 좋다.

### 8.4 SSH 공개 범위

SSH는 내 공인 IP로 제한한다.

```hcl
allowed_ssh_cidr = "125.178.1.177/32"
```

네트워크가 바뀌어 공인 IP가 변경되면 Terraform 변수도 수정해야 한다.

## 9. 현재 남은 작업

- Terraform CLI 설치 및 `terraform init/plan/apply` 실행
- AWS CLI 자격 증명 설정
- GitHub Secrets 등록
- GHCR package 공개 여부 또는 pull token 준비
- 첫 GitHub Actions 배포 실행 확인
- 배포 후 Swagger 또는 API 엔드포인트 수동 확인
