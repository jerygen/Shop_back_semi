data "aws_availability_zones" "available" {
  state = "available"
}

data "aws_ami" "ubuntu" {
  most_recent = true
  owners      = ["099720109477"]

  filter {
    name   = "name"
    values = ["ubuntu/images/hvm-ssd/ubuntu-jammy-22.04-amd64-server-*"]
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

locals {
  allowed_ssh_cidrs = length(var.allowed_ssh_cidrs) > 0 ? var.allowed_ssh_cidrs : [var.allowed_ssh_cidr]
}

resource "aws_vpc" "shop" {
  cidr_block           = var.vpc_cidr
  enable_dns_hostnames = true
  enable_dns_support   = true

  tags = {
    Name    = "${var.project_name}-vpc"
    Project = var.project_name
  }
}

resource "aws_internet_gateway" "shop" {
  vpc_id = aws_vpc.shop.id

  tags = {
    Name    = "${var.project_name}-igw"
    Project = var.project_name
  }
}

resource "aws_subnet" "public" {
  vpc_id                  = aws_vpc.shop.id
  cidr_block              = var.public_subnet_cidr
  availability_zone       = data.aws_availability_zones.available.names[0]
  map_public_ip_on_launch = true

  tags = {
    Name    = "${var.project_name}-public-subnet"
    Project = var.project_name
  }
}

resource "aws_route_table" "public" {
  vpc_id = aws_vpc.shop.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.shop.id
  }

  tags = {
    Name    = "${var.project_name}-public-rt"
    Project = var.project_name
  }
}

resource "aws_route_table_association" "public" {
  subnet_id      = aws_subnet.public.id
  route_table_id = aws_route_table.public.id
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
  description = "SSH and Spring Boot access for ${var.project_name}"
  vpc_id      = aws_vpc.shop.id

  ingress {
    description = "SSH"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = local.allowed_ssh_cidrs
  }

  ingress {
    description = "Spring Boot application"
    from_port   = var.app_port
    to_port     = var.app_port
    protocol    = "tcp"
    cidr_blocks = [var.allowed_app_cidr]
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
  ami                         = data.aws_ami.ubuntu.id
  instance_type               = var.instance_type
  subnet_id                   = aws_subnet.public.id
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
    admin_user_id       = var.admin_user_id
    admin_password      = var.admin_password
    admin_user_name     = var.admin_user_name
  })

  root_block_device {
    volume_size           = var.root_volume_size
    volume_type           = "gp3"
    encrypted             = true
    delete_on_termination = true
  }

  tags = {
    Name    = var.project_name
    Project = var.project_name
  }
}
