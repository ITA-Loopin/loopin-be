output "instance_public_ip" {
  description = "OCI 인스턴스의 퍼블릭 IP 주소"
  value       = oci_core_instance.instance_1.public_ip
}
