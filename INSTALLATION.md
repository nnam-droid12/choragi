# 🚀 Choragi Installation Guide

This comprehensive guide will walk you through setting up the Choragi AI-Powered Concert & Tour Orchestration System, from initial setup to full Cloud Run deployment.

## 📋 Table of Contents

- [System Requirements](#system-requirements)
- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Detailed Setup](#detailed-setup)
- [Service Configuration](#service-configuration)
- [Deployment Options](#deployment-options)
- [Testing](#testing)
- [Troubleshooting](#troubleshooting)

## 💻 System Requirements

### Minimum Requirements
- **OS**: Linux, macOS, or Windows 10+
- **Java**: JDK 17 or higher
- **Build Tool**: Maven 3.8+
- **Memory**: 8GB RAM (Playwright and multiple Spring Boot apps require decent memory)
- **Storage**: 5GB available space
- **Network**: High-speed internet connection for WebSockets and API access

### Recommended Requirements
- **OS**: Linux (Ubuntu 20.04+) or macOS
- **Java**: JDK 21
- **Memory**: 16GB RAM
- **Storage**: 10GB available space
- **CPU**: 4+ cores for optimal local multi-service orchestration

## 🔑 Prerequisites

### 1. Google Cloud Account Setup

#### Create Google Cloud Project
```bash
# Install gcloud CLI if not already installed
curl https://sdk.cloud.google.com | bash
exec -l $SHELL

# Create new project
gcloud projects create choragi-$(date +%s) --name="Choragi-Orchestrator"
export PROJECT_ID=$(gcloud projects list --format="value(projectId)" --filter="name:Choragi-Orchestrator" --limit=1)
gcloud config set project $PROJECT_ID
```

#### Enable Required APIs
```bash
# Enable all required Google Cloud APIs for AI, Storage, and Cloud Run
gcloud services enable \
  generativelanguage.googleapis.com \
  aiplatform.googleapis.com \
  storage.googleapis.com \
  run.googleapis.com \
  cloudbuild.googleapis.com
```

#### Create Service Account (For Vertex AI & Cloud Storage)
```bash
# Create service account for Choragi
gcloud iam service-accounts create choragi-sa \
  --description="Choragi Service Account" \
  --display-name="Choragi"

# Grant necessary roles
gcloud projects add-iam-policy-binding $PROJECT_ID \
  --member="serviceAccount:choragi-sa@$PROJECT_ID.iam.gserviceaccount.com" \
  --role="roles/aiplatform.user"

gcloud projects add-iam-policy-binding $PROJECT_ID \
  --member="serviceAccount:choragi-sa@$PROJECT_ID.iam.gserviceaccount.com" \
  --role="roles/storage.admin"

# Create and download application default credentials (ADC)
gcloud auth application-default login
```

### 2. API Keys Setup

#### Google Gemini API Key
1. Go to Google AI Studio (aistudio.google.com)
2. Generate an API Key
3. Save this for your `GEMINI_API_KEY` environment variable.

#### Twilio Setup
1. Sign up at [Twilio](https://www.twilio.com)
2. Purchase a phone number or use a verified trial number.
3. Note your **Account SID** and **Auth Token** from the console dashboard.
4. Set up a TwiML App pointing to your `live-negotiator` WebSocket endpoint (e.g., `wss://your-ngrok-url/voice`).

## ⚡ Quick Start

### 1. Clone and Setup
```bash
# Clone repository
git clone <repository-url>
cd choragi

# Build all microservices
mvn clean install -DskipTests
```

### 2. Environment Configuration
```bash
# Set global environment variables in your terminal or IDE
export GEMINI_API_KEY="your_gemini_api_key_here"
export GOOGLE_CLOUD_PROJECT="your_gcp_project_id"
export CHORAGI_STORAGE_BUCKET_NAME="your_gcs_bucket_name"
export TWILIO_ACCOUNT_SID="your_twilio_sid"
export TWILIO_AUTH_TOKEN="your_twilio_auth_token"
```

### 3. Start Core Services
```bash
# Start the UI Dashboard in a new terminal
cd ui-client
mvn spring-boot:run

# (Optional for local testing) Start other agents in separate terminals
cd ../venue-finder && mvn spring-boot:run
cd ../live-negotiator && mvn spring-boot:run
cd ../creative-director && mvn spring-boot:run
cd ../digital-promoter && mvn spring-boot:run
```

### 4. Access Dashboard
Open your browser to `http://localhost:8000`

## 🔧 Detailed Setup

### 1. Project Setup

#### Clone Repository
```bash
git clone <repository-url>
cd choragi
```

#### Playwright Setup (For Digital Promoter)
The `digital-promoter` requires Playwright browsers to be installed to interact with Google Ads.
```bash
cd digital-promoter
mvn exec:java -e -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="install chromium"
```

### 2. Environment Configuration

You can configure application properties inside each service's `src/main/resources/application.properties` or set them via environment variables.

Required environment variables:
```env
# Core AI (REQUIRED)
GEMINI_API_KEY=your_gemini_api_key_here
GOOGLE_CLOUD_PROJECT=your_gcp_project_id

# Cloud Storage Configuration (For Creative Assets)
CHORAGI_STORAGE_BUCKET_NAME=your_gcs_bucket_name

# Twilio (For Live Negotiator)
TWILIO_ACCOUNT_SID=your_twilio_sid
TWILIO_AUTH_TOKEN=your_twilio_auth_token
TWILIO_PHONE_NUMBER=+1234567890
```

### 3. Google Cloud Storage Setup

#### Create Asset Bucket
```bash
# Create a multi-region bucket for generated posters and videos
gcloud storage buckets create gs://choragi-assets-bucket --location=US

# Make objects publicly readable (required for the website to display them)
gcloud storage buckets add-iam-policy-binding gs://choragi-assets-bucket \
    --member="allUsers" \
    --role="roles/storage.objectViewer"
```

## 🎛️ Service Configuration

### UI Client (Port 8000)
```bash
cd ui-client
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8000"
```

### Venue Finder (Port 8081)
```bash
cd venue-finder
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8081"
```

### Live Negotiator (Port 8080)
```bash
cd live-negotiator
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8080"
```

### Creative Director (Port 8082)
```bash
cd creative-director
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8082"
```

### Digital Promoter (Port 8084)
```bash
cd digital-promoter
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8084"
```

## 🐳 Deployment Options

### Option 1: Local Development with Ngrok
If you want to test Twilio callbacks locally, you must expose your local `live-negotiator` port.
```bash
# Expose port 8080 to the internet
ngrok http 8080

# Update your Twilio Webhook URL to point to the provided ngrok WSS URL
```

### Option 2: Docker Deployment
```bash
# Build all Docker images
docker build -t choragi-ui-client ./ui-client
docker build -t choragi-venue-finder ./venue-finder
docker build -t choragi-negotiator ./live-negotiator
docker build -t choragi-creative ./creative-director
docker build -t choragi-promoter ./digital-promoter

# Run a specific container
docker run -d -p 8000:8000 -e GEMINI_API_KEY=$GEMINI_API_KEY choragi-ui-client
```

### Option 3: Google Cloud Run Deployment (Recommended)
```bash
# Make the deployment script executable
chmod +x deploy_cloud_run.sh

# Deploy all services to Cloud Run
./deploy_cloud_run.sh
```
*Note: Ensure your `ChoragiOrchestrator.java` points to the deployed `.a.run.app` URLs when deploying to the cloud.*

## 🧪 Testing

### 1. Service Health Checks
```bash
# Check all services are running locally
curl -f http://localhost:8000/actuator/health
curl -f http://localhost:8081/actuator/health
```

### 2. Integration Testing
```bash
# Test triggering the sequence directly without voice
curl -X POST http://localhost:8000/api/trigger-agents \
  -H 'Content-Type: application/json' \
  -d '{
    "artistName": "Burna Boy",
    "location": "Victoria Island Lagos",
    "date": "15th April 2026"
  }'
```

### 3. End-to-End Testing
1. Access dashboard at `http://localhost:8000` or your Cloud Run URL.
2. Click **Start Voice**.
3. Say: "Book Adele in Austin Texas on April 15th."
4. Wait for the AI to reply "Deploying agents for..."
5. Monitor real-time terminal output in the browser dashboard.

## 🔧 Troubleshooting

### Common Issues

#### Playwright Fails to Launch (Digital Promoter)
```text
Error: Browser closed unexpectedly
```
* **Fix**: Ensure `setChannel("chrome")` is only used if Google Chrome is actually installed on your machine. On Cloud Run, use the provided `--single-process` and `--disable-gpu` flags in `PlaywrightConfig.java`.

#### Twilio Call Drops Instantly
```text
WebSocket is closed before the connection is established.
```
* **Fix**: Ensure your `LiveConnectConfig` uses exactly `["AUDIO"]` for `responseModalities`. Passing `"TEXT"` over Native Audio WebSockets will cause Google to drop the connection.

#### Creative Director Throws 500 Error
```text
VERTEX AI REJECTED THE REQUEST
```
* **Fix**: Veo 3.0 Fast requires specific payload parameters. Ensure `durationSeconds`, `resolution`, and `aspectRatio` are explicitly defined in the `RestTemplate` request. Verify your `GOOGLE_CLOUD_PROJECT` has billing enabled for Vertex AI.

#### Microphone Not Detected in UI
* **Fix**: Browsers block microphone access on `http://` unless it's `localhost`. If deploying to Cloud Run, ensure you are accessing the UI via `https://`.

## 📚 Next Steps

After successful installation:

1. **Configure Twilio Webhooks**:
    - Point your Twilio phone number to your deployed Cloud Run URL for the `live-negotiator`.
2. **Setup Google Ads Billing**:
    - Ensure the Google account logged into your Playwright session has active Google Ads billing for the `digital-promoter` to successfully complete campaign creation.
3. **Customize Prompts**:
    - Adjust the System Instructions in `VoiceStreamHandler.java` to change how the Live Agent negotiates.
    - Tweak the Visual Prompts in `CreativeDirectorAgent.java` to fit different musical genres.

## 🆘 Getting Help

If you encounter issues during installation:

1. Check the Google Cloud Logs Explorer for specific container crashes.
2. Ensure all API keys and Project IDs are correctly mapped in your environment variables.
3. Verify that your Google Cloud Service Account has `roles/aiplatform.user` and `roles/storage.admin`.

---

**Ready to orchestrate live concerts! 🚀**