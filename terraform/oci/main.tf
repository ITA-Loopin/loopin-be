terraform {
  cloud {
    organization = "loopone"

    workspaces {
      name = "loopin-oci"
    }
  }

  required_providers {
    oci = {
      source  = "oracle/oci"
      version = "~> 7.0"
    }
  }
}

# OCI 설정 시작
provider "oci" {
  tenancy_ocid         = var.tenancy_ocid
  user_ocid            = var.user_ocid
  fingerprint          = var.fingerprint
  private_key          = var.private_key
  private_key_password = var.private_key_password
  region               = var.region
}
# OCI 설정 끝

data "oci_identity_availability_domains" "ads" {
  compartment_id = var.tenancy_ocid
}

data "oci_objectstorage_namespace" "ns" {
  compartment_id = var.tenancy_ocid
}

data "oci_core_images" "oracle_linux" {
  compartment_id           = var.compartment_ocid
  operating_system         = "Oracle Linux"
  operating_system_version = "8"
  shape                    = var.instance_shape
  sort_by                  = "TIMECREATED"
  sort_order               = "DESC"
}

locals {
  availability_domain = var.availability_domain != "" ? var.availability_domain : data.oci_identity_availability_domains.ads.availability_domains[var.availability_domain_index].name

  instance_user_data = <<-END_OF_FILE
#!/bin/bash
dnf update -y

# Docker CE (upstream) 설치
dnf install -y yum-utils git curl
yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
dnf install -y docker-ce docker-ce-cli containerd.io

systemctl enable docker
systemctl start docker

usermod -aG docker opc

curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
chmod +x /usr/local/bin/docker-compose
ln -sf /usr/local/bin/docker-compose /usr/bin/docker-compose

dd if=/dev/zero of=/swapfile bs=128M count=64
chmod 600 /swapfile
mkswap /swapfile
swapon /swapfile
grep -q '^/swapfile ' /etc/fstab || echo "/swapfile swap swap defaults 0 0" >> /etc/fstab

# OS 방화벽 설정
firewall-cmd --permanent --add-port=22/tcp
firewall-cmd --permanent --add-service=http
firewall-cmd --permanent --add-service=https
firewall-cmd --reload
END_OF_FILE
}

# VCN 설정 시작
resource "oci_core_vcn" "vcn_1" {
  compartment_id = var.compartment_ocid
  cidr_blocks    = ["10.0.0.0/16"]
  display_name   = "${var.prefix}-vcn-1"
  dns_label      = substr(lower(replace(var.prefix, "-", "")), 0, 15)
}

resource "oci_core_internet_gateway" "igw_1" {
  compartment_id = var.compartment_ocid
  vcn_id         = oci_core_vcn.vcn_1.id
  display_name   = "${var.prefix}-igw-1"
  enabled        = true
}

resource "oci_core_route_table" "rt_1" {
  compartment_id = var.compartment_ocid
  vcn_id         = oci_core_vcn.vcn_1.id
  display_name   = "${var.prefix}-rt-1"

  route_rules {
    network_entity_id = oci_core_internet_gateway.igw_1.id
    destination       = "0.0.0.0/0"
    destination_type  = "CIDR_BLOCK"
  }
}

resource "oci_core_security_list" "sl_1" {
  compartment_id = var.compartment_ocid
  vcn_id         = oci_core_vcn.vcn_1.id
  display_name   = "${var.prefix}-sl-1"

  ingress_security_rules {
    protocol = "6"
    source   = "0.0.0.0/0"

    tcp_options {
      min = 22
      max = 22
    }
  }

  ingress_security_rules {
    protocol = "6"
    source   = "0.0.0.0/0"

    tcp_options {
      min = 80
      max = 80
    }
  }

  ingress_security_rules {
    protocol = "6"
    source   = "0.0.0.0/0"

    tcp_options {
      min = 443
      max = 443
    }
  }

  ingress_security_rules {
    protocol = "1" # ICMP
    source   = "10.0.0.0/16"

    icmp_options {
      type = 3
      code = 4
    }
  }

  ingress_security_rules {
    protocol = "1" # ICMP
    source   = "0.0.0.0/0"

    icmp_options {
      type = 3
      code = 4
    }
  }

  egress_security_rules {
    protocol    = "all"
    destination = "0.0.0.0/0"
  }
}

resource "oci_core_subnet" "subnet_1" {
  compartment_id             = var.compartment_ocid
  vcn_id                     = oci_core_vcn.vcn_1.id
  cidr_block                 = "10.0.1.0/24"
  display_name               = "${var.prefix}-subnet-1"
  route_table_id             = oci_core_route_table.rt_1.id
  security_list_ids          = [oci_core_security_list.sl_1.id]
  prohibit_public_ip_on_vnic = false
  dns_label                  = substr(lower(replace("${var.prefix}sub", "-", "")), 0, 15)
}

# Compute 설정 시작
resource "oci_core_instance" "instance" {
  count               = var.instance_count
  compartment_id      = var.compartment_ocid
  availability_domain = local.availability_domain
  display_name        = "${var.prefix}-instance-${count.index + 1}"
  shape               = var.instance_shape

  create_vnic_details {
    subnet_id        = oci_core_subnet.subnet_1.id
    assign_public_ip = true
    display_name     = "${var.prefix}-vnic-${count.index + 1}"
    hostname_label   = "${var.prefix}vm${count.index + 1}"
  }

  metadata = {
    ssh_authorized_keys = join("\n", var.ssh_public_keys)
    user_data           = base64encode(local.instance_user_data)
  }

  dynamic "shape_config" {
    for_each = var.instance_ocpus != null ? [1] : []
    content {
      ocpus         = var.instance_ocpus
      memory_in_gbs = var.instance_memory_in_gbs
    }
  }

  source_details {
    source_type             = "image"
    source_id               = data.oci_core_images.oracle_linux.images[0].id
    boot_volume_size_in_gbs = var.boot_volume_size_in_gbs
  }
}

# Object Storage 설정 시작
resource "oci_objectstorage_bucket" "loopin_bucket" {
  compartment_id = var.compartment_ocid
  namespace      = data.oci_objectstorage_namespace.ns.namespace
  name           = var.bucket_name
  access_type    = "ObjectRead"
  storage_tier   = "Standard"
}

# 선택 사항: 인스턴스 프린시펄 기반 Object Storage 접근 권한
resource "oci_identity_dynamic_group" "instance_dg" {
  count          = var.create_instance_principal_resources ? 1 : 0
  compartment_id = var.tenancy_ocid
  name           = "${var.prefix}-instance-dg"
  description    = "Dynamic group for ${var.prefix} compute instances"
  matching_rule  = "ALL {instance.compartment.id = '${var.compartment_ocid}'}"
}

resource "oci_identity_policy" "instance_object_policy" {
  count          = var.create_instance_principal_resources ? 1 : 0
  compartment_id = var.tenancy_ocid
  name           = "${var.prefix}-instance-object-policy"
  description    = "Allow ${var.prefix} instances to manage objects in the target compartment"

  statements = [
    "Allow dynamic-group ${oci_identity_dynamic_group.instance_dg[0].name} to manage objects in compartment id ${var.compartment_ocid}",
    "Allow dynamic-group ${oci_identity_dynamic_group.instance_dg[0].name} to read buckets in compartment id ${var.compartment_ocid}",
  ]
}
