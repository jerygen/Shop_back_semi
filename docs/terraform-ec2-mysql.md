# Terraform EC2 MySQL Setup

This Terraform setup creates one EC2 instance and starts MySQL with Docker Compose through EC2 user data.

## Files

- `infra/terraform/main.tf`: EC2, security group, default VPC lookup, latest Amazon Linux 2023 AMI lookup
- `infra/terraform/user_data.sh.tftpl`: Docker install and MySQL container startup
- `infra/terraform/terraform.tfvars.example`: local variable sample

## Prerequisites

- Terraform installed locally
- AWS credentials configured locally

## Deploy

From the repository root:

```bash
cd infra/terraform
cp terraform.tfvars.example terraform.tfvars
```

Edit `terraform.tfvars`.

Terraform creates an EC2 key pair and writes the private key locally:

```properties
key_pair_name = "shop-mysql"
```

After apply, the key is created at:

```text
infra/terraform/generated/shop-mysql.pem
```

Use your public IP for SSH:

```properties
allowed_ssh_cidr = "YOUR_PUBLIC_IP/32"
```

Keep MySQL private by default:

```properties
expose_mysql = false
```

Then apply:

```bash
terraform init
terraform plan -out shop.tfplan
terraform apply shop.tfplan
```

## Connect to MySQL

Use the Terraform output named `ssh_tunnel_command`.

```bash
ssh -i ./generated/shop-mysql.pem -L 3306:127.0.0.1:3306 ec2-user@EC2_PUBLIC_IP
```

Then connect Spring Boot with:

```properties
DB_URL=jdbc:mysql://localhost:3306/shop?serverTimezone=Asia/Seoul&characterEncoding=UTF-8
DB_USERNAME=shop_user
DB_PASSWORD=your_mysql_password
```

## Check EC2 setup

After SSH login:

```bash
sudo docker compose --env-file /opt/shop/.env -f /opt/shop/docker-compose.yml ps
sudo docker compose --env-file /opt/shop/.env -f /opt/shop/docker-compose.yml logs -f mysql
```

## Destroy

```bash
terraform destroy
```

The EC2 root volume is configured with `delete_on_termination = false`, so database data is not deleted automatically when the instance is terminated. Delete the EBS volume manually when you no longer need the data.
