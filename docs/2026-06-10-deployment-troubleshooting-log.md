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

The deployment workflow initially assumed that Terraform `user_data` had already created `/opt/shop/.env` and `/opt/shop/docker-compose.yml`.

The workflow was:

1. Run tests using a MySQL service container.
2. Build the Spring Boot Docker image.
3. Push image tags to GHCR:
   - `ghcr.io/<owner>/<repo>:<commit-sha>`
   - `ghcr.io/<owner>/<repo>:latest`
4. SSH into EC2.
5. Update `/opt/shop/.env` with the newly built image tag.
6. Pull the new app image.
7. Restart only the app container.
8. Check whether the application responds inside EC2.

Because the EC2 OS is now Ubuntu, the default SSH user in `deploy.yml` was changed from:

```yaml
ec2-user
```

to:

```yaml
ubuntu
```

The `ubuntu` user is added to the `docker` group during `user_data`, so the deploy script can run Docker without `sudo`.

Later, the deployment target was changed from Terraform-created EC2 to a manually created EC2 instance. Because of that, the workflow was updated again to remove runtime dependency on Terraform `user_data`.

The final GitHub Actions deployment step is responsible for:

1. Checking whether Docker is installed on the EC2 host.
2. Installing Docker if it is missing.
3. Checking whether Docker Compose v2 is available.
4. Installing Docker Compose if it is missing.
5. Creating `/opt/shop`.
6. Creating `/opt/shop/.env`.
7. Creating `/opt/shop/docker-compose.yml`.
8. Logging in to GHCR when `GHCR_USERNAME` and `GHCR_TOKEN` are provided.
9. Pulling the app image from GHCR.
10. Running MySQL and the Spring Boot app with Docker Compose.
11. Checking `http://127.0.0.1:9000/swagger` from inside EC2.
12. Printing `shop-app` logs when the health check fails.

This made the deployment independent of Terraform. A manually created EC2 instance only needs SSH access and a compatible Ubuntu environment.

## Manual EC2 Deployment Model

The final direction was to create the EC2 instance manually instead of continuing to rely on Terraform for provisioning.

Required GitHub Actions secrets for manual EC2 deployment:

```text
EC2_HOST
EC2_USER
EC2_SSH_KEY
```

Recommended secrets for stable application runtime:

```text
MYSQL_PASSWORD
MYSQL_ROOT_PASSWORD
JWT_SECRET
```

Optional admin bootstrap secrets:

```text
ADMIN_USER_ID
ADMIN_PASSWORD
```

Optional variable:

```text
ADMIN_USER_NAME
```

If GHCR package access is private:

```text
GHCR_USERNAME
GHCR_TOKEN
```

The manual EC2 security group should allow:

```text
22    SSH
80    HTTP
443   HTTPS or SSH fallback
9000  Spring Boot application
```

MySQL port `3306` should not be opened publicly. In Docker Compose it is bound only to:

```text
127.0.0.1:3306
```

## Docker Image Pull Appeared to Hang

At one point GitHub Actions appeared to hang at:

```text
Pulling application image
```

This indicated that SSH into EC2 had already succeeded. The deployment was not stuck before connection; it had reached the remote Docker image pull step.

Possible causes considered:

- GHCR image pull was slow.
- GHCR package access required credentials.
- Docker Compose progress output made the command appear idle.

The workflow was briefly changed to use direct `docker pull`, then reverted at request. The key lesson was that logs should identify which remote command is running so that SSH issues and Docker issues are not confused.

## App Container Started but Spring Boot Was Not Ready

Another deployment reached:

```text
Container shop-mysql Healthy
Container shop-app Started
Checking application from EC2
curl: (56) Recv failure: Connection reset by peer
```

Interpretation:

- SSH succeeded.
- Docker succeeded.
- MySQL became healthy.
- The Spring Boot container started.
- The HTTP check failed because Spring Boot was not ready yet or the app exited shortly after startup.

The deployment script was updated to retry the local health check:

```text
http://127.0.0.1:9000/swagger
```

If the app still does not respond, the script prints:

```bash
docker logs shop-app --tail=100
```

This turns a vague "browser does not open" problem into an application log problem.

## EC2 Resource Pressure

While using `t3.micro`, CPU and memory pressure were suspected because the instance was running both:

- Spring Boot application container.
- MySQL 8.4 container.

This can be tight on a free-tier instance, especially during image pull, container startup, and MySQL initialization.

Commands recommended for checking resource pressure:

```bash
free -h
df -h
docker stats --no-stream
sudo dmesg -T | grep -i -E "killed process|out of memory|oom"
```

Recommended mitigation for `t3.micro`:

```bash
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
```

MySQL memory can also be reduced with options such as:

```yaml
command:
  - --character-set-server=utf8mb4
  - --collation-server=utf8mb4_unicode_ci
  - --innodb-buffer-pool-size=128M
  - --max-connections=30
```

## External Browser Access

Even after deployment succeeded, browser access failed until the EC2 external network path and security group were corrected.

Final application URL format:

```text
http://<EC2_PUBLIC_IP>:9000/swagger
```

Important distinction:

- If GitHub Actions reports that `http://127.0.0.1:9000/swagger` works from EC2, the app is alive inside the instance.
- If the browser cannot open `http://<EC2_PUBLIC_IP>:9000/swagger`, the remaining issue is external access: public IP, security group, NACL, route table, or local network path.

The final browser access worked after the relevant EC2 inbound access was opened.

## Admin Login Issue

After the application became accessible, admin login failed.

The admin account is not hard-coded in the application. It is created by:

```text
AdminAccountInitializer
```

The initializer only creates an admin user when both environment variables are provided:

```text
ADMIN_USER_ID
ADMIN_PASSWORD
```

If either value is missing, startup logs:

```text
Admin account initialization skipped. ADMIN_USER_ID and ADMIN_PASSWORD are not configured.
```

At that time, GitHub Secrets only contained EC2 connection values. Therefore, no admin account had been bootstrapped.

Resolution:

- Add `ADMIN_USER_ID` and `ADMIN_PASSWORD` to GitHub Secrets.
- Optionally add `ADMIN_USER_NAME`.
- Redeploy.

If a user with the same `ADMIN_USER_ID` already exists, the initializer skips creation and does not reset the password. In that case, update the existing DB row manually or choose a new admin ID.

## Secrets to Update After New Apply

When using Terraform-created EC2, update GitHub Actions secrets after `terraform apply`:

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

When using manually created EC2, set `EC2_HOST` to the manually created instance's public IPv4 address.

## Recommended Next Steps

For the final manual EC2 flow:

1. Create an Ubuntu EC2 instance manually.
2. Open required inbound ports in the EC2 security group:

```text
22
80
443
9000
```

3. Add GitHub Actions secrets:

```text
EC2_HOST
EC2_USER
EC2_SSH_KEY
MYSQL_PASSWORD
MYSQL_ROOT_PASSWORD
JWT_SECRET
ADMIN_USER_ID
ADMIN_PASSWORD
```

4. Run GitHub Actions deployment.
5. Check Docker on EC2 if troubleshooting is needed:

```bash
groups
docker ps
docker compose version
```

6. Test:

```text
http://<public-ip>:9000/swagger
```

7. Log in with the admin credentials provided through `ADMIN_USER_ID` and `ADMIN_PASSWORD`.

## Lessons Learned

- A missing `terraform.tfvars` causes Terraform to prompt for required variables.
- Deleted AWS access keys must be replaced locally with `aws configure`.
- Windows OpenSSH refuses private keys with overly broad file permissions.
- User data failure can leave EC2 partially configured even if the instance is running.
- GitHub Actions SSH timeout means network reachability failed before key authentication.
- Default VPC and default subnet dependencies make troubleshooting harder.
- For learning and small deployments, explicitly creating VPC, subnet, route table, Internet Gateway, and security group is easier to reason about.
- MySQL should not be exposed through the EC2 security group when it only serves the app on the same host.
- When moving away from Terraform provisioning, the deployment workflow must create the server-side `.env` and `docker-compose.yml` itself.
- `Container Started` does not prove Spring Boot is ready; check the application endpoint from inside EC2.
- `t3.micro` can be too tight for Spring Boot plus MySQL; watch for OOM and consider swap.
- Admin login depends on `ADMIN_USER_ID` and `ADMIN_PASSWORD`; no secret means no bootstrap admin account.
