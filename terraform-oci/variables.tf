variable "tenancy_ocid" {
  description = "OCID of the OCI Tenancy"
  type        = string
}

variable "user_ocid" {
  description = "OCID of the OCI User"
  type        = string
}

variable "fingerprint" {
  description = "Fingerprint for the API Key"
  type        = string
}

variable "private_key_path" {
  description = "Path to the private key used for API authentication"
  type        = string
}

variable "region" {
  description = "OCI Region (e.g., us-ashburn-1)"
  type        = string
}

variable "compartment_ocid" {
  description = "OCID of the Compartment where resources will be created"
  type        = string
}

variable "ssh_public_key" {
  description = "Public SSH key to access the compute instance"
  type        = string
}
