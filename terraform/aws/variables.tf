variable "prefix" {
  description = "Prefix for all resources"
  default     = "loopin"
}

variable "region" {
  description = "region"
  default     = "ap-northeast-2"
}

variable "ssh_public_keys" {
  description = "EC2(ec2-user)에 등록할 SSH 공개키(authorized_keys)"
  type        = list(string)
  default     = []
  sensitive   = true
}

variable "s3_bucket_name" {
  description = "S3 버킷 이름"
  type        = string
}
