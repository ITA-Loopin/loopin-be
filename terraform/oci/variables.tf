variable "prefix" {
  description = "Prefix for all resources"
  type        = string
  default     = "loopin"
}

variable "region" {
  description = "OCI region"
  type        = string
}

variable "tenancy_ocid" {
  description = "OCI tenancy OCID"
  type        = string
}

variable "user_ocid" {
  description = "OCI user OCID"
  type        = string
}

variable "fingerprint" {
  description = "Fingerprint for the OCI API signing key"
  type        = string
}

variable "private_key_path" {
  description = "Path to the OCI API private key"
  type        = string
}

variable "private_key_password" {
  description = "Password for the OCI API private key, if any"
  type        = string
  default     = null
  sensitive   = true
}

variable "compartment_ocid" {
  description = "Compartment OCID where network, compute, and bucket resources are created"
  type        = string
}

variable "availability_domain" {
  description = "Availability domain name. Leave empty to use the first AD in the tenancy"
  type        = string
  default     = ""
}

variable "ssh_public_keys" {
  description = "Compute 인스턴스(opc 사용자)에 등록할 SSH 공개키(authorized_keys)"
  type        = list(string)
  default     = []
  sensitive   = true
}

variable "instance_shape" {
  description = "OCI compute instance shape"
  type        = string
  default     = "VM.Standard.A1.Flex"
}

variable "instance_ocpus" {
  description = "Number of OCPUs for flexible shapes"
  type        = number
  default     = 4
}

variable "instance_memory_in_gbs" {
  description = "Memory in GBs for flexible shapes"
  type        = number
  default     = 24
}

variable "boot_volume_size_in_gbs" {
  description = "Boot volume size for the compute instance"
  type        = number
  default     = 200
}

variable "bucket_name" {
  description = "Object Storage bucket name"
  type        = string
}

variable "create_instance_principal_resources" {
  description = "Create dynamic group and policy for instance principal based Object Storage access"
  type        = bool
  default     = true
}
