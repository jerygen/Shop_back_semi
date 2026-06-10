variable "aws_region" {
  description = "AWS region where the EC2 instance will be created."
  type        = string
  default     = "ap-northeast-2"
}

variable "project_name" {
  description = "Project name used for AWS resource names and tags."
  type        = string
  default     = "shop"
}

variable "instance_type" {
  description = "EC2 instance type for the application and MySQL host."
  type        = string
  default     = "t3.micro"
}

variable "vpc_cidr" {
  description = "CIDR block for the application VPC."
  type        = string
  default     = "10.20.0.0/16"

  validation {
    condition     = can(cidrhost(var.vpc_cidr, 0))
    error_message = "vpc_cidr must be a valid CIDR block."
  }
}

variable "public_subnet_cidr" {
  description = "CIDR block for the public subnet that hosts the EC2 instance."
  type        = string
  default     = "10.20.1.0/24"

  validation {
    condition     = can(cidrhost(var.public_subnet_cidr, 0))
    error_message = "public_subnet_cidr must be a valid CIDR block."
  }
}

variable "key_pair_name" {
  description = "EC2 key pair name to create for SSH access."
  type        = string
  default     = "shop"
}

variable "allowed_ssh_cidr" {
  description = "CIDR allowed to connect over SSH. Use your public IP with /32, or 0.0.0.0/0 for temporary broad access."
  type        = string

  validation {
    condition     = can(cidrhost(var.allowed_ssh_cidr, 0))
    error_message = "allowed_ssh_cidr must be a valid CIDR block, for example 203.0.113.10/32."
  }
}

variable "allowed_ssh_cidrs" {
  description = "Additional CIDR blocks allowed to connect over SSH. When set, this replaces allowed_ssh_cidr."
  type        = list(string)
  default     = []

  validation {
    condition     = alltrue([for cidr in var.allowed_ssh_cidrs : can(cidrhost(cidr, 0))])
    error_message = "Every allowed_ssh_cidrs value must be a valid CIDR block, for example 203.0.113.10/32."
  }
}

variable "root_volume_size" {
  description = "Root EBS volume size in GiB. Docker images and the MySQL volume are stored on this disk."
  type        = number
  default     = 30
}

variable "app_image" {
  description = "Docker image for the Spring Boot application. GitHub Actions should push this image before deployment."
  type        = string
  default     = "shop-app:latest"
}

variable "app_port" {
  description = "External and container port for the Spring Boot application."
  type        = number
  default     = 9000
}

variable "allowed_app_cidr" {
  description = "CIDR allowed to connect to the Spring Boot application."
  type        = string
  default     = "0.0.0.0/0"

  validation {
    condition     = can(cidrhost(var.allowed_app_cidr, 0))
    error_message = "allowed_app_cidr must be a valid CIDR block, for example 0.0.0.0/0 or 203.0.113.10/32."
  }
}

variable "jpa_ddl_auto" {
  description = "Hibernate ddl-auto value for the deployed application."
  type        = string
  default     = "update"
}

variable "jpa_show_sql" {
  description = "Whether the deployed application should print SQL logs."
  type        = bool
  default     = false
}

variable "jwt_secret" {
  description = "JWT signing secret used by the Spring Boot application."
  type        = string
  sensitive   = true

  validation {
    condition     = length(var.jwt_secret) >= 32
    error_message = "jwt_secret must be at least 32 characters."
  }
}

variable "mysql_database" {
  description = "Initial MySQL database name."
  type        = string
  default     = "shop"
}

variable "mysql_user" {
  description = "Application MySQL user."
  type        = string
  default     = "admin"
}

variable "mysql_password" {
  description = "Application MySQL user password."
  type        = string
  sensitive   = true

  validation {
    condition     = length(var.mysql_password) >= 8
    error_message = "mysql_password must be at least 8 characters."
  }
}

variable "mysql_root_password" {
  description = "MySQL root password."
  type        = string
  sensitive   = true

  validation {
    condition     = length(var.mysql_root_password) >= 8
    error_message = "mysql_root_password must be at least 8 characters."
  }
}

variable "admin_user_id" {
  description = "Initial admin account user id. Leave empty to skip admin bootstrap."
  type        = string
  default     = ""
}

variable "admin_password" {
  description = "Initial admin account password. Leave empty to skip admin bootstrap."
  type        = string
  sensitive   = true
  default     = ""

  validation {
    condition     = var.admin_password == "" || length(var.admin_password) >= 8
    error_message = "admin_password must be empty or at least 8 characters."
  }
}

variable "admin_user_name" {
  description = "Initial admin account display name."
  type        = string
  default     = "Administrator"
}
