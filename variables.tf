variable "prefix" {
  description = "Prefix for all resources"
  default     = "loopin"
}

variable "region" {
  description = "region"
  default     = "ap-northeast-2"
}

variable "nickname" {
  description = "nickname"
  default     = "v"
}

variable "admin_allowed_cidrs" {
  type        = list(string)
  description = "CIDRs allowed to access NPM admin (port 81)"
  default     = []
}
