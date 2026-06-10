# 2026-06-10 Deployment Troubleshooting Log

## Goal

Deploy the Spring Boot shop application to an AWS EC2 instance with MySQL running as a Docker container on the same host.

The intended runtime shape is:

- Spring Boot application: publicly accessible on the application port.
- MySQL: reachable only from the Spring Boot container and, when needed, from the developer through SSH tunneling.
- EC2: Ubuntu, `t3.micro`, new security group.
- CI/CD: GitHub Actions builds the application image, pushes it to GHCR, then deploys it to EC2 over SSH.

## Initial Terraform Variable Prompt

While running Terraform from `infra/terraform`, Terraform prompted for values such as:

- `allowed_ssh_cidr`
- `jwt_secret`

This happened because `terraform.tfvars` was missing on the current machine. The repository only contained `terraform.tfvars.example`, while real `terraform.tfvars` is ignored by Git because it contains local and sensitive values.

Action taken:

- Created `infra/terraform/terraform.tfvars` from `terraform.tfvars.example`.
- Left placeholders for values that must be filled manually.

Reasoning:

- `terraform.tfvars` should not be committed because it can contain secrets such as database passwords and JWT secrets.
- The public SSH CIDR must be a public IP with `/32`, not a private LAN IP such as `192.168.x.x`.

## AWS CLI Credentials

The local AWS access key had previously been deleted from AWS IAM.

Conclusion:

- Even if old credentials remain on the local PC, deleted AWS access keys no longer work.
- A new access key must be created and configured with `aws configure`.

Verification command discussed:

```powershell
aws sts get-caller-identity
```

## First EC2 Creation and SSH Issues

Terraform created an EC2 instance and generated a private key at:

```text
infra/terraform/generated/shop.pem
```

Terraform output produced an SSH command similar to:

```powershell
ssh -i ./generated/shop.pem ec2-user@<public-ip>
```

The first SSH attempt failed with:

```text
WARNING: UNPROTECTED PRIVATE KEY FILE!
Permissions for './generated/shop.pem' are too open.
```

Conclusion:

- This was not an AWS key rejection.
- Windows OpenSSH refused to use the private key because the file permissions were too open.

Action discussed:

- Restrict the `.pem` permissions using `icacls` so only the current user can read it.

## Docker Installation Failure on Amazon Linux 2023

After connecting to the instance, Docker was expected to be installed by `user_data`.

The cloud-init log showed:

```text
No match for argument: docker-compose-plugin
Error: Unable to find a match: docker-compose-plugin
```

Conclusion:

- The existing Amazon Linux 2023 setup tried to install `docker-compose-plugin` with `dnf`.
- That package was not available from the configured repositories, causing `user_data` to fail partway through.

Action taken:

- Updated `user_data.sh.tftpl` so Docker Compose v2 is installed manually from the official Docker Compose GitHub release binary.

## GitHub Actions Deployment Timeout

GitHub Actions failed during `Deploy application container` with:

```text
dial tcp ***:22: i/o timeout
```

Conclusion:

- This was not a Docker or application error.
- GitHub Actions could not establish an SSH connection to EC2 on port 22.
- Since timeout happens before authentication, this pointed to network reachability rather than an invalid SSH key.

Checks performed:

- Confirmed the GitHub Secret `EC2_SSH_KEY` might need to be updated when the generated `.pem` changes.
- Confirmed `EC2_HOST` must match the current EC2 public IP.
- Opened security group SSH access broadly during testing.

## Application URL Issue

After GitHub Actions reported deployment success, the application was tested at:

```text
http://<public-ip>/swagger
```

This did not work because the Spring Boot application is exposed on port `9000`.

Correct URL:

```text
http://<public-ip>:9000/swagger
```

Further connection refusal indicated that either:

- the app container was not running,
- the app failed after startup,
- port `9000` was not reachable,
- or the EC2 instance itself was not reachable.

Suggested checks:

```bash
sudo docker ps -a
sudo docker logs shop-app --tail=200
sudo docker logs shop-mysql --tail=100
curl -v http://localhost:9000/swagger
```

## EC2 Console and PowerShell SSH Timeout

AWS Console connection failed with:

```text
Failed to connect to your instance
Error establishing SSH connection to your instance.
```

PowerShell SSH also timed out:

```text
Connection to <public-ip> port 22 timed out
```

This confirmed the issue was not specific to AWS Console.

AWS-side checks showed:

- EC2 state: running.
- Status checks: passed.
- Public IPv4: present.
- Security group: port 22 and 9000 open.
- NACL: inbound and outbound allowed.
- Main route table: `0.0.0.0/0 -> igw-...` existed and was active.

Conclusion:

- The usual AWS network pieces appeared valid.
- The instance was still not reachable on 22 or 9000.
- At this point, continuing to patch the existing instance was taking too long and the infrastructure design had become unclear.

## Decision to Rebuild Terraform

Because the previous Terraform depended on the default VPC and default subnets, troubleshooting became noisy. The decision was made to rewrite the Terraform structure explicitly.

New intended Terraform design:

- Create a dedicated VPC.
- Create a public subnet.
- Create and attach an Internet Gateway.
- Create a route table with `0.0.0.0/0 -> Internet Gateway`.
- Associate the public subnet with the route table.
- Create a new security group.
- Use Ubuntu 22.04 instead of Amazon Linux 2023.
- Use `t3.micro`.
- Install Docker and Docker Compose through Ubuntu-compatible commands.
- Run Spring Boot and MySQL with Docker Compose.

## Updated Security Model

Spring Boot:

- Exposed publicly through security group port `9000`.
- Docker Compose maps host `0.0.0.0:9000` to container `9000`.

MySQL:

- Security group does not expose port `3306`.
- Docker Compose binds MySQL to `127.0.0.1:3306` on the EC2 host.
- Spring Boot accesses MySQL through Docker's internal Compose network using:

```text
mysql:3306
```

Optional local access to MySQL can be done through SSH tunneling:

```powershell
ssh -i ./generated/shop.pem -L 3306:127.0.0.1:3306 ubuntu@<public-ip>
```

## Updated Terraform Files

The Terraform files were changed to:

- Stop using the default VPC.
- Create a dedicated VPC, subnet, Internet Gateway, route table, security group, and EC2 instance.
- Use Ubuntu 22.04 AMI.
- Remove external MySQL security group rules.
- Remove Elastic IP after deciding that changing public IP is acceptable.
- Output the EC2 public IP, app URL, SSH command, and tunnel command.

Validation result:

```text
terraform validate
Success! The configuration is valid.
```

## Updated GitHub Actions Flow

The deployment workflow is:

1. Run tests using a MySQL service container.
2. Build the Spring Boot Docker image.
3. Push image tags to GHCR:
   - `ghcr.io/<owner>/<repo>:<commit-sha>`
   - `ghcr.io/<owner>/<repo>:latest`
4. SSH into EC2.
5. Update `/opt/shop/.env` with the newly built image tag.
6. Pull the new app image.
7. Restart only the app container.
8. Print Docker Compose status.
9. Prune unused images.

Because the EC2 OS is now Ubuntu, the default SSH user in `deploy.yml` was changed from:

```yaml
ec2-user
```

to:

```yaml
ubuntu
```

The `ubuntu` user is added to the `docker` group during `user_data`, so the deploy script can run Docker without `sudo`.

## Secrets to Update After New Apply

After `terraform apply`, update GitHub Actions secrets:

```text
EC2_HOST = terraform output public_ip
EC2_USER = ubuntu
EC2_SSH_KEY = full contents of infra/terraform/generated/shop.pem
```

If GHCR package access is private, also configure:

```text
GHCR_USERNAME
GHCR_TOKEN
```

The token should have package read permission.

## Recommended Next Steps

1. Review `infra/terraform/terraform.tfvars`.
2. Run:

```powershell
cd C:\Projects\Shop\infra\terraform
terraform plan
terraform apply
```

3. Check outputs:

```powershell
terraform output public_ip
terraform output ssh_command
terraform output app_url
```

4. Test SSH:

```powershell
ssh -i ./generated/shop.pem ubuntu@<public-ip>
```

5. Check Docker on EC2:

```bash
groups
docker ps
docker compose version
```

6. Update GitHub Actions secrets.
7. Run GitHub Actions deployment.
8. Test:

```text
http://<public-ip>:9000/swagger
```

## Lessons Learned

- A missing `terraform.tfvars` causes Terraform to prompt for required variables.
- Deleted AWS access keys must be replaced locally with `aws configure`.
- Windows OpenSSH refuses private keys with overly broad file permissions.
- User data failure can leave EC2 partially configured even if the instance is running.
- GitHub Actions SSH timeout means network reachability failed before key authentication.
- Default VPC and default subnet dependencies make troubleshooting harder.
- For learning and small deployments, explicitly creating VPC, subnet, route table, Internet Gateway, and security group is easier to reason about.
- MySQL should not be exposed through the EC2 security group when it only serves the app on the same host.
