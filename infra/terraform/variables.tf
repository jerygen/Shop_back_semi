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
  description = "EC2 instance type for the MySQL host."
  type        = string
  default     = "t3.micro"
}

variable "key_pair_name" {
  description = "EC2 key pair name to create for SSH access."
  type        = string
  default     = "shop-mysql"
}

variable "allowed_ssh_cidr" {
  description = "CIDR allowed to connect over SSH. Use your public IP with /32."
  type        = string

  validation {
    condition     = can(cidrhost(var.allowed_ssh_cidr, 0))
    error_message = "allowed_ssh_cidr must be a valid CIDR block, for example 203.0.113.10/32."
  }
}

variable "expose_mysql" {
  description = "Expose MySQL port 3306 through the EC2 security group. Keep false unless you know you need it."
  type        = bool
  default     = false
}

variable "allowed_mysql_cidr" {
  description = "CIDR allowed to connect to MySQL when expose_mysql is true."
  type        = string
  default     = "127.0.0.1/32"

  validation {
    condition     = can(cidrhost(var.allowed_mysql_cidr, 0))
    error_message = "allowed_mysql_cidr must be a valid CIDR block, for example 203.0.113.10/32."
  }
}

variable "root_volume_size" {
  description = "Root EBS volume size in GiB. The Docker MySQL volume is stored on this disk."
  type        = number
  default     = 30
}

variable "mysql_database" {
  description = "Initial MySQL database name."
  type        = string
  default     = "shop"
}

variable "mysql_user" {
  description = "Application MySQL user."
  type        = string
  default     = "shop_user"
}

variable "mysql_password" {
  description = "Application MySQL user password."
  type        = string
  sensitive   = true

  validation {
    condition     = length(var.mysql_password) >= 12
    error_message = "mysql_password must be at least 12 characters."
  }
}

variable "mysql_root_password" {
  description = "MySQL root password."
  type        = string
  sensitive   = true

  validation {
    condition     = length(var.mysql_root_password) >= 12
    error_message = "mysql_root_password must be at least 12 characters."
  }
}
