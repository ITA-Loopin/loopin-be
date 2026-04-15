output "instance_public_ips" {
  description = "OCI 인스턴스들의 퍼블릭 IP 주소"
  value       = oci_core_instance.instance[*].public_ip
}

output "instance_private_ips" {
  description = "OCI 인스턴스들의 프라이빗 IP 주소 (VCN 내부 통신용)"
  value       = oci_core_instance.instance[*].private_ip
}

output "server1_private_ip" {
  description = "Server 1 프라이빗 IP — docker/compose.server2.yml의 SERVER2_INTERNAL_IP 참고용 아님, Kafka advertise 주소 확인용"
  value       = oci_core_instance.instance[0].private_ip
}

output "server2_private_ip" {
  description = "Server 2 프라이빗 IP — docker/compose.server2.yml의 SERVER2_INTERNAL_IP 환경변수에 사용"
  value       = oci_core_instance.instance[1].private_ip
}
