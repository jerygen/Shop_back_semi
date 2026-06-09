data "aws_vpc" "default" {
  default = true
}

data "aws_subnets" "default" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.default.id]
  }
}

data "aws_ami" "amazon_linux_2023" {
  most_recent = true
  owners      = ["amazon"]

  filter {
    name   = "name"
    values = ["al2023-ami-*-kernel-6.1-x86_64"]
  }

  filter {
    name   = "architecture"
    values = ["x86_64"]
  }

  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }
}

resource "tls_private_key" "shop" {
  algorithm = "RSA"
  rsa_bits  = 4096
}

resource "aws_key_pair" "shop" {
  key_name   = var.key_pair_name
  public_key = tls_private_key.shop.public_key_openssh

  tags = {
    Name    = var.key_pair_name
    Project = var.project_name
  }
}

resource "local_sensitive_file" "shop_private_key" {
  content              = tls_private_key.shop.private_key_pem
  filename             = "${path.module}/generated/${var.key_pair_name}.pem"
  directory_permission = "0700"
  file_permission      = "0600"
}

resource "aws_security_group" "shop_ec2" {
  name        = "${var.project_name}-ec2-sg"
  description = "Security group for ${var.project_name} application EC2"
  vpc_id      = data.aws_vpc.default.id

  ingress {
    description = "SSH"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = [var.allowed_ssh_cidr]
  }

  ingress {
    description = "Spring Boot application"
    from_port   = var.app_port
    to_port     = var.app_port
    protocol    = "tcp"
    cidr_blocks = [var.allowed_app_cidr]
  }

  dynamic "ingress" {
    for_each = var.expose_mysql ? [1] : []

    content {
      description = "MySQL"
      from_port   = 3306
      to_port     = 3306
      protocol    = "tcp"
      cidr_blocks = [var.allowed_mysql_cidr]
    }
  }

  egress {
    description = "All outbound"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name    = "${var.project_name}-ec2-sg"
    Project = var.project_name
  }
}

resource "aws_instance" "shop" {
  ami                         = data.aws_ami.amazon_linux_2023.id
  instance_type               = var.instance_type
  subnet_id                   = data.aws_subnets.default.ids[0]
  vpc_security_group_ids      = [aws_security_group.shop_ec2.id]
  associate_public_ip_address = true
  key_name                    = aws_key_pair.shop.key_name

  user_data_replace_on_change = true
  user_data = templatefile("${path.module}/user_data.sh.tftpl", {
    app_image           = var.app_image
    app_port            = var.app_port
    jwt_secret          = var.jwt_secret
    jpa_ddl_auto        = var.jpa_ddl_auto
    jpa_show_sql        = var.jpa_show_sql
    mysql_database      = var.mysql_database
    mysql_user          = var.mysql_user
    mysql_password      = var.mysql_password
    mysql_root_password = var.mysql_root_password
    mysql_bind_host     = var.expose_mysql ? "0.0.0.0" : "127.0.0.1"
  })

  root_block_device {
    volume_size           = var.root_volume_size
    volume_type           = "gp3"
    encrypted             = true
    delete_on_termination = false
  }

  tags = {
    Name    = var.project_name
    Project = var.project_name
  }
}
