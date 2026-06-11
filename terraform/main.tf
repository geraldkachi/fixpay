terraform {
  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "~> 5.0"
    }
  }
}

provider "google" {
  project = var.project_id
  region  = var.region
}

# Enable required APIs
resource "google_project_service" "run_api" {
  service            = "run.googleapis.com"
  disable_on_destroy = false
}

resource "google_project_service" "build_api" {
  service            = "cloudbuild.googleapis.com"
  disable_on_destroy = false
}

resource "google_project_service" "artifact_registry_api" {
  service            = "artifactregistry.googleapis.com"
  disable_on_destroy = false
}

# Artifact Registry Repository for Docker images
resource "google_artifact_registry_repository" "laravel_repo" {
  provider      = google
  location      = var.region
  repository_id = "fixpay-laravel-repo"
  description   = "Docker repository for FixPay Laravel backend"
  format        = "DOCKER"
  depends_on    = [google_project_service.artifact_registry_api]
}

# Cloud Run Service (Initial dummy image, will be overwritten by Cloud Build)
resource "google_cloud_run_v2_service" "laravel_backend" {
  name     = "fixpay-laravel-api"
  location = var.region
  ingress  = "INGRESS_TRAFFIC_ALL"

  template {
    containers {
      # Use a placeholder image initially so the service can be created.
      # Cloud Build will update this with the real image.
      image = "us-docker.pkg.dev/cloudrun/container/hello"
      
      env {
        name  = "DB_CONNECTION"
        value = "pgsql"
      }
      env {
        name  = "DB_HOST"
        value = "placeholder.neon.tech" # To be manually updated or injected via Secret Manager
      }
      # Additional env vars like APP_KEY should be handled securely, ideally via Secret Manager
    }
  }

  depends_on = [google_project_service.run_api]
}

# Allow unauthenticated access to the Cloud Run service
resource "google_cloud_run_v2_service_iam_member" "public_access" {
  name     = google_cloud_run_v2_service.laravel_backend.name
  location = google_cloud_run_v2_service.laravel_backend.location
  role     = "roles/run.invoker"
  member   = "allUsers"
}
