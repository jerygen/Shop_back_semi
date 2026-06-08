output "instance_id" {
  description = "EC2 instance ID."
  value       = aws_instance.mysql.id
}

output "public_ip" {
  description = "EC2 public IP address."
  value       = aws_instance.mysql.public_ip
}

output "ssh_command" {
  description = "SSH command for the EC2 instance."
  value       = "ssh -i ${local_sensitive_file.mysql_private_key.filename} ec2-user@${aws_instance.mysql.public_ip}"
}

output "ssh_tunnel_command" {
  description = "SSH tunnel command for local MySQL access."
  value       = "ssh -i ${local_sensitive_file.mysql_private_key.filename} -L 3306:127.0.0.1:3306 ec2-user@${aws_instance.mysql.public_ip}"
}

output "private_key_path" {
  description = "Generated private key path for SSH access."
  value       = local_sensitive_file.mysql_private_key.filename
}

output "spring_db_url_for_local_tunnel" {
  description = "Spring DB URL when connected through the SSH tunnel."
  value       = "jdbc:mysql://localhost:3306/${var.mysql_database}?serverTimezone=Asia/Seoul&characterEncoding=UTF-8"
}
