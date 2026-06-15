output "public_ip" {
  description = "Public IP address of the FixPay server"
  value       = oci_core_instance.fixpay_server.public_ip
}
