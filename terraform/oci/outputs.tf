output "instance_public_ips" {
  description = "OCI 인스턴스들의 퍼블릭 IP 주소"
  value       = oci_core_instance.instance[*].public_ip
}
