output "instance_public_ip" {
  description = "EC2 인스턴스의 퍼블릭 IP 주소"
  value       = aws_instance.ec2_1.public_ip
}
