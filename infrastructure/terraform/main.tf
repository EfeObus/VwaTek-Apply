# VwaTek Apply - Terraform Configuration
# Infrastructure as Code for Google Cloud Platform

terraform {
  required_version = ">= 1.0.0"
  
  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "~> 5.0"
    }
    google-beta = {
      source  = "hashicorp/google-beta"
      version = "~> 5.0"
    }
  }
  
  # Store state in Cloud Storage (uncomment after creating bucket)
  # backend "gcs" {
  #   bucket = "vwatek-apply-terraform-state"
  #   prefix = "terraform/state"
  # }
}

# ============================================
# Variables
# ============================================
variable "project_id" {
  description = "GCP Project ID"
  type        = string
  default     = "vwatek-apply"
}

variable "region" {
  description = "GCP Region - Primary (Canadian for data residency)"
  type        = string
  default     = "northamerica-northeast1"  # Montreal - Canadian data residency
}

variable "region_secondary" {
  description = "GCP Region - Secondary (US for fallback)"
  type        = string
  default     = "us-central1"
}

variable "zone" {
  description = "GCP Zone"
  type        = string
  default     = "northamerica-northeast1-a"
}

variable "db_password" {
  description = "Database root password"
  type        = string
  sensitive   = true
}

variable "gemini_api_key" {
  description = "Gemini API Key"
  type        = string
  sensitive   = true
}

# ============================================
# Provider Configuration
# ============================================
provider "google" {
  project = var.project_id
  region  = var.region
}

provider "google-beta" {
  project = var.project_id
  region  = var.region
}

# ============================================
# Enable Required APIs
# ============================================
resource "google_project_service" "apis" {
  for_each = toset([
    "run.googleapis.com",
    "cloudbuild.googleapis.com",
    "secretmanager.googleapis.com",
    "sqladmin.googleapis.com",
    "containerregistry.googleapis.com",
    "compute.googleapis.com",
    "vpcaccess.googleapis.com",
    "servicenetworking.googleapis.com",
    "cloudresourcemanager.googleapis.com",
  ])
  
  service            = each.value
  disable_on_destroy = false
}

# ============================================
# Cloud SQL Instance (Already exists - import or skip)
# ============================================
# Note: Your Cloud SQL instance already exists
# To import: terraform import google_sql_database_instance.main vwatek-apply:us-central1:vwatekapply

# resource "google_sql_database_instance" "main" {
#   name             = "vwatekapply"
#   database_version = "MYSQL_8_0"
#   region           = var.region
#   
#   settings {
#     tier = "db-f1-micro"
#     
#     ip_configuration {
#       ipv4_enabled    = true
#       private_network = google_compute_network.vpc.id
#       
#       authorized_networks {
#         name  = "allow-all"
#         value = "0.0.0.0/0"
#       }
#     }
#     
#     backup_configuration {
#       enabled            = true
#       binary_log_enabled = true
#       start_time         = "03:00"
#     }
#   }
#   
#   deletion_protection = true
# }

# ============================================
# Secret Manager - Database Credentials
# ============================================
resource "google_secret_manager_secret" "db_username" {
  secret_id = "db-username"
  
  replication {
    auto {}
  }
  
  depends_on = [google_project_service.apis]
}

resource "google_secret_manager_secret_version" "db_username" {
  secret      = google_secret_manager_secret.db_username.id
  secret_data = "root"
}

resource "google_secret_manager_secret" "db_password" {
  secret_id = "db-password"
  
  replication {
    auto {}
  }
  
  depends_on = [google_project_service.apis]
}

resource "google_secret_manager_secret_version" "db_password" {
  secret      = google_secret_manager_secret.db_password.id
  secret_data = var.db_password
}

# ============================================
# Secret Manager - API Keys
# ============================================
resource "google_secret_manager_secret" "gemini_api_key" {
  secret_id = "gemini-api-key"
  
  replication {
    auto {}
  }
  
  depends_on = [google_project_service.apis]
}

resource "google_secret_manager_secret_version" "gemini_api_key" {
  secret      = google_secret_manager_secret.gemini_api_key.id
  secret_data = var.gemini_api_key
}

# ============================================
# Cloud Storage - Frontend Hosting
# ============================================
resource "google_storage_bucket" "frontend" {
  name          = "vwatek-apply-web"
  location      = var.region
  force_destroy = false
  
  uniform_bucket_level_access = true
  
  website {
    main_page_suffix = "index.html"
    not_found_page   = "index.html"
  }
  
  cors {
    origin          = ["*"]
    method          = ["GET", "HEAD", "OPTIONS"]
    response_header = ["*"]
    max_age_seconds = 3600
  }
}

# Make bucket public
resource "google_storage_bucket_iam_member" "public_access" {
  bucket = google_storage_bucket.frontend.name
  role   = "roles/storage.objectViewer"
  member = "allUsers"
}

# ============================================
# Cloud Storage - Terraform State
# ============================================
resource "google_storage_bucket" "terraform_state" {
  name          = "vwatek-apply-terraform-state"
  location      = var.region
  force_destroy = false
  
  versioning {
    enabled = true
  }
  
  lifecycle_rule {
    condition {
      num_newer_versions = 5
    }
    action {
      type = "Delete"
    }
  }
}

# ============================================
# VPC Network for Cloud Run -> Cloud SQL
# ============================================
resource "google_compute_network" "vpc" {
  name                    = "vwatek-vpc"
  auto_create_subnetworks = false
  
  depends_on = [google_project_service.apis]
}

resource "google_compute_subnetwork" "subnet" {
  name          = "vwatek-subnet"
  ip_cidr_range = "10.0.0.0/24"
  region        = var.region
  network       = google_compute_network.vpc.id
}

# ============================================
# VPC Access Connector for Serverless
# ============================================
resource "google_vpc_access_connector" "connector" {
  name          = "vwatek-connector"
  region        = var.region
  ip_cidr_range = "10.8.0.0/28"
  network       = google_compute_network.vpc.name
  
  depends_on = [google_project_service.apis]
}

# ============================================
# Cloud Run Service
# ============================================
resource "google_cloud_run_v2_service" "backend" {
  name     = "vwatek-backend"
  location = var.region
  
  template {
    scaling {
      min_instance_count = 0
      max_instance_count = 10
    }
    
    vpc_access {
      connector = google_vpc_access_connector.connector.id
      egress    = "PRIVATE_RANGES_ONLY"
    }
    
    containers {
      image = "gcr.io/${var.project_id}/vwatek-backend:latest"
      
      ports {
        container_port = 8080
      }
      
      resources {
        limits = {
          cpu    = "2"
          memory = "1Gi"
        }
      }
      
      env {
        name  = "CLOUD_SQL_DATABASE"
        value = "Vwatek_Apply"
      }
      
      env {
        name  = "ENVIRONMENT"
        value = "production"
      }
      
      env {
        name = "CLOUD_SQL_USER"
        value_source {
          secret_key_ref {
            secret  = google_secret_manager_secret.db_username.secret_id
            version = "latest"
          }
        }
      }
      
      env {
        name = "CLOUD_SQL_PASSWORD"
        value_source {
          secret_key_ref {
            secret  = google_secret_manager_secret.db_password.secret_id
            version = "latest"
          }
        }
      }
      
      env {
        name = "GEMINI_API_KEY"
        value_source {
          secret_key_ref {
            secret  = google_secret_manager_secret.gemini_api_key.secret_id
            version = "latest"
          }
        }
      }
      
      startup_probe {
        http_get {
          path = "/health"
          port = 8080
        }
        initial_delay_seconds = 10
        period_seconds        = 5
        failure_threshold     = 10
      }
      
      liveness_probe {
        http_get {
          path = "/health"
          port = 8080
        }
        period_seconds = 30
      }
    }
    
    # Cloud SQL connection
    volumes {
      name = "cloudsql"
      cloud_sql_instance {
        instances = ["${var.project_id}:${var.region}:vwatekapply"]
      }
    }
  }
  
  traffic {
    type    = "TRAFFIC_TARGET_ALLOCATION_TYPE_LATEST"
    percent = 100
  }
  
  depends_on = [
    google_project_service.apis,
    google_secret_manager_secret_version.db_username,
    google_secret_manager_secret_version.db_password,
    google_secret_manager_secret_version.gemini_api_key,
  ]
}

# Allow unauthenticated access to Cloud Run
resource "google_cloud_run_v2_service_iam_member" "public" {
  name     = google_cloud_run_v2_service.backend.name
  location = var.region
  role     = "roles/run.invoker"
  member   = "allUsers"
}

# ============================================
# Cloud Build Trigger
# ============================================
resource "google_cloudbuild_trigger" "main" {
  name        = "vwatek-deploy"
  description = "Deploy on push to main"
  
  github {
    owner = "EfeObus"
    name  = "VwaTek-Apply"
    push {
      branch = "^main$"
    }
  }
  
  filename = "cloudbuild.yaml"
  
  depends_on = [google_project_service.apis]
}

# ============================================
# Service Account for Cloud Build
# ============================================
resource "google_service_account" "cloudbuild" {
  account_id   = "cloudbuild-sa"
  display_name = "Cloud Build Service Account"
}

resource "google_project_iam_member" "cloudbuild_roles" {
  for_each = toset([
    "roles/run.admin",
    "roles/storage.admin",
    "roles/secretmanager.secretAccessor",
    "roles/cloudsql.client",
    "roles/iam.serviceAccountUser",
  ])
  
  project = var.project_id
  role    = each.value
  member  = "serviceAccount:${google_service_account.cloudbuild.email}"
}

# ============================================
# Outputs
# ============================================
output "backend_url" {
  description = "Cloud Run Backend URL"
  value       = google_cloud_run_v2_service.backend.uri
}

output "frontend_url" {
  description = "Frontend URL"
  value       = "https://storage.googleapis.com/${google_storage_bucket.frontend.name}/index.html"
}

output "cloud_sql_connection" {
  description = "Cloud SQL Connection Name"
  value       = "${var.project_id}:${var.region}:vwatekapply"
}

# ============================================
# Canadian Data Residency Configuration
# ============================================

# Cloud SQL Instance for Canadian Region (Montreal)
# For PIPEDA compliance - Canadian user data stays in Canada
resource "google_sql_database_instance" "canada" {
  name             = "vwatekapply-canada"
  database_version = "MYSQL_8_0"
  region           = var.region  # northamerica-northeast1 (Montreal)
  
  settings {
    tier              = "db-f1-micro"
    availability_type = "ZONAL"  # Can upgrade to REGIONAL for HA
    
    ip_configuration {
      ipv4_enabled    = true
      private_network = google_compute_network.vpc_canada.id
      
      require_ssl = true
      
      # Restrict to Cloud Run only
      authorized_networks {
        name  = "cloud-run"
        value = "0.0.0.0/0"  # Will be restricted via IAM
      }
    }
    
    backup_configuration {
      enabled                        = true
      binary_log_enabled             = true
      start_time                     = "03:00"
      point_in_time_recovery_enabled = true
      transaction_log_retention_days = 7
      
      backup_retention_settings {
        retained_backups = 30
        retention_unit   = "COUNT"
      }
    }
    
    maintenance_window {
      day          = 7  # Sunday
      hour         = 4  # 4 AM
      update_track = "stable"
    }
    
    insights_config {
      query_insights_enabled  = true
      query_string_length     = 1024
      record_client_address   = false  # Privacy
    }
  }
  
  deletion_protection = true
  
  depends_on = [google_project_service.apis]
}

resource "google_sql_database" "canada_db" {
  name     = "Vwatek_Apply"
  instance = google_sql_database_instance.canada.name
  charset  = "utf8mb4"
  collation = "utf8mb4_unicode_ci"
}

# Canadian VPC Network
resource "google_compute_network" "vpc_canada" {
  name                    = "vwatek-vpc-canada"
  auto_create_subnetworks = false
  
  depends_on = [google_project_service.apis]
}

resource "google_compute_subnetwork" "subnet_canada" {
  name          = "vwatek-subnet-canada"
  ip_cidr_range = "10.1.0.0/24"
  region        = var.region
  network       = google_compute_network.vpc_canada.id
  
  private_ip_google_access = true
}

# Canadian VPC Access Connector
resource "google_vpc_access_connector" "connector_canada" {
  name          = "vwatek-connector-canada"
  region        = var.region
  ip_cidr_range = "10.9.0.0/28"
  network       = google_compute_network.vpc_canada.name
  
  depends_on = [google_project_service.apis]
}

# Canadian Cloud Run Service
resource "google_cloud_run_v2_service" "backend_canada" {
  name     = "vwatek-backend-canada"
  location = var.region  # Montreal
  
  template {
    scaling {
      min_instance_count = 0
      max_instance_count = 10
    }
    
    vpc_access {
      connector = google_vpc_access_connector.connector_canada.id
      egress    = "PRIVATE_RANGES_ONLY"
    }
    
    containers {
      image = "gcr.io/${var.project_id}/vwatek-backend:latest"
      
      ports {
        container_port = 8080
      }
      
      resources {
        limits = {
          cpu    = "2"
          memory = "1Gi"
        }
      }
      
      env {
        name  = "CLOUD_SQL_INSTANCE"
        value = "${var.project_id}:${var.region}:vwatekapply-canada"
      }
      
      env {
        name  = "CLOUD_SQL_DATABASE"
        value = "Vwatek_Apply"
      }
      
      env {
        name  = "ENVIRONMENT"
        value = "production"
      }
      
      env {
        name  = "DATA_REGION"
        value = "canada"
      }
      
      env {
        name = "CLOUD_SQL_USER"
        value_source {
          secret_key_ref {
            secret  = google_secret_manager_secret.db_username.secret_id
            version = "latest"
          }
        }
      }
      
      env {
        name = "CLOUD_SQL_PASSWORD"
        value_source {
          secret_key_ref {
            secret  = google_secret_manager_secret.db_password.secret_id
            version = "latest"
          }
        }
      }
      
      env {
        name = "GEMINI_API_KEY"
        value_source {
          secret_key_ref {
            secret  = google_secret_manager_secret.gemini_api_key.secret_id
            version = "latest"
          }
        }
      }
      
      startup_probe {
        http_get {
          path = "/health"
          port = 8080
        }
        initial_delay_seconds = 10
        period_seconds        = 5
        failure_threshold     = 10
      }
      
      liveness_probe {
        http_get {
          path = "/health"
          port = 8080
        }
        period_seconds = 30
      }
    }
    
    # Cloud SQL connection
    volumes {
      name = "cloudsql"
      cloud_sql_instance {
        instances = ["${var.project_id}:${var.region}:vwatekapply-canada"]
      }
    }
  }
  
  traffic {
    type    = "TRAFFIC_TARGET_ALLOCATION_TYPE_LATEST"
    percent = 100
  }
  
  depends_on = [
    google_project_service.apis,
    google_sql_database_instance.canada,
  ]
}

# Allow unauthenticated access to Canadian Cloud Run
resource "google_cloud_run_v2_service_iam_member" "public_canada" {
  name     = google_cloud_run_v2_service.backend_canada.name
  location = var.region
  role     = "roles/run.invoker"
  member   = "allUsers"
}

# Canadian Cloud Storage for Frontend
resource "google_storage_bucket" "frontend_canada" {
  name          = "vwatek-apply-web-canada"
  location      = var.region  # Montreal
  force_destroy = false
  
  uniform_bucket_level_access = true
  
  website {
    main_page_suffix = "index.html"
    not_found_page   = "index.html"
  }
  
  cors {
    origin          = ["*"]
    method          = ["GET", "HEAD", "OPTIONS"]
    response_header = ["*"]
    max_age_seconds = 3600
  }
}

resource "google_storage_bucket_iam_member" "public_access_canada" {
  bucket = google_storage_bucket.frontend_canada.name
  role   = "roles/storage.objectViewer"
  member = "allUsers"
}

# ============================================
# Global Load Balancer for Geo-Routing
# ============================================
# Routes Canadian users to Montreal region, others to US

resource "google_compute_global_address" "default" {
  name = "vwatek-global-ip"
}

resource "google_compute_region_network_endpoint_group" "canada_neg" {
  name                  = "vwatek-neg-canada"
  network_endpoint_type = "SERVERLESS"
  region                = var.region
  cloud_run {
    service = google_cloud_run_v2_service.backend_canada.name
  }
}

resource "google_compute_region_network_endpoint_group" "us_neg" {
  name                  = "vwatek-neg-us"
  network_endpoint_type = "SERVERLESS"
  region                = var.region_secondary
  cloud_run {
    service = google_cloud_run_v2_service.backend.name
  }
}

resource "google_compute_backend_service" "default" {
  name       = "vwatek-backend-service"
  protocol   = "HTTPS"
  port_name  = "http"
  enable_cdn = false

  backend {
    group = google_compute_region_network_endpoint_group.canada_neg.id
  }
  
  backend {
    group = google_compute_region_network_endpoint_group.us_neg.id
  }
}

# ============================================
# Canadian Outputs
# ============================================
output "backend_canada_url" {
  description = "Cloud Run Backend URL (Canada)"
  value       = google_cloud_run_v2_service.backend_canada.uri
}

output "frontend_canada_url" {
  description = "Frontend URL (Canada)"
  value       = "https://storage.googleapis.com/${google_storage_bucket.frontend_canada.name}/index.html"
}

output "cloud_sql_canada_connection" {
  description = "Cloud SQL Connection Name (Canada)"
  value       = "${var.project_id}:${var.region}:vwatekapply-canada"
}

output "global_ip" {
  description = "Global IP Address for Load Balancer"
  value       = google_compute_global_address.default.address
}
