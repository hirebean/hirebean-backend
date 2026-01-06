# HireBean DevOps & Cloud Infrastructure

This document outlines the cloud architecture, CI/CD processes, and Kubernetes configuration for the HireBean Backend. 

---

## 1. AWS Cloud Architecture

We utilize **Amazon Web Services (AWS)** for file storage, strictly decoupling public assets from private sensitive data to ensure maximum security and performance.

### S3 Storage (Secure Data Layer)
* **Bucket Type:** Private (Block all public access: ON).
* **Region:** `eu-central-1` (Frankfurt).
* **Purpose:** Storage for company logos, candidate resumes (CVs), and documents.
* **Security:** Direct public access via the internet is completely restricted.

### CloudFront CDN (Content Delivery Network)
Used **exclusively for public assets** (e.g., Company Logos).
* **Mechanism:** Origin Access Control (OAC).
* **Workflow:** CloudFront holds a specific cryptographic permission to read from the locked S3 bucket.
* **Benefits:**
    * **Edge Caching:** Delivers images from the server closest to the user (Low Latency).
    * **Security:** Hides the actual S3 bucket origin from the public internet.
    * **Encryption:** Enforces HTTPS/SSL by default.

### Presigned URLs (Sensitive Data Protection)
Used **exclusively for Resumes (CVs) and Personal Documents**.
* **Process:** The Backend generates a temporary, cryptographically signed URL (valid for 10 minutes).
* **Flow:**
    1.  A user (e.g., HR) requests access to a candidate's CV.
    2.  Spring Boot validates permissions (Authentication/Authorization).
    3.  Spring Boot generates a Signed URL via the AWS SDK.
    4.  The browser accesses the file directly from S3 using the temporary token.
* **Compliance:** Ensures GDPR compliance by preventing permanent public access to personal data.

---

## 2. CI/CD Pipelines (GitHub Actions)

The project features fully automated pipelines for integration and deployment.

### Continuous Integration (CI)
Triggered on every `push` or `pull_request` to the `main` branch.
1.  **Code Checkout & Java Setup:** Installs JDK 23.
2.  **Code Formatting Check:** Enforces code style using **Spotless** (Google Java Format).
3.  **Unit Testing:** Executes the full JUnit test suite.
4.  **Security Scan:** Scans the codebase for vulnerabilities using **Trivy**.

### Continuous Deployment (CD)
Triggered manually (`workflow_dispatch`) or upon release.
1.  **Docker Build:** Creates an optimized Docker Image using a Multi-stage build.
2.  **Docker Push:** Pushes the image to Docker Hub (`uchihadari/hirebean-backend`).
3.  **Deploy to Kubernetes:**
    * Creates/Updates the `hirebean` Namespace.
    * Updates Kubernetes Secrets.
    * Applies Database and Backend manifests.
    * Executes a custom **Health Check** script to verify deployment success.

---

## 3. Kubernetes (K8s) Configuration

The application is architected to run in a Kubernetes cluster (Kind, Minikube, or Cloud Provider).

### Resource Structure
| Component | Type | Description |
| :--- | :--- | :--- |
| **Backend** | Deployment | The Spring Boot application (ReplicaSet: 1). |
| **Backend Service** | Service | ClusterIP for internal communication (Port 80 -> 8080). |
| **Database** | Deployment | PostgreSQL 17 database instance. |
| **DB Storage** | PVC | PersistentVolumeClaim (1Gi) for data persistence across restarts. |
| **Secrets** | Secret | Stores encrypted DB credentials and AWS keys. |
| **Probes** | Liveness/Readiness | *Added:* Spring Boot Actuator integration to ensure zero-downtime deployments. Checks `/actuator/health`. |
| **Ingress** | Ingress | *Optional:* Configured to expose the application externally via an Ingress Controller (e.g., NGINX). |

---

## 4. Local & Production Configuration

### Environment Variables (Required)
To run the application (locally or in the cluster), the following variables must be provided:

```yaml
# Database Configuration
SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/hirebean_db
HIREBEAN_DB_USERNAME: <db-user>
HIREBEAN_DB_PASS: <db-pass>

# AWS Cloud Configuration
AWS_ACCESS_KEY_ID: <IAM-User-Access-Key>
AWS_SECRET_ACCESS_KEY: <IAM-User-Secret-Key>
AWS_BUCKET_NAME: <AWS-Bucket-Name>
AWS_REGION: <AWS-Region>

# CDN (For Public Logos)
CDN_URL: [https://dxxxxxxxx.cloudfront.net](https://dxxxxxxxx.cloudfront.net)
```