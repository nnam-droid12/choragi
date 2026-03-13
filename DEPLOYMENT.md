# Choragi Cloud & Service Account Deployment Guide

## Overview

The Choragi multi-agent system relies on Google Cloud Service Accounts and Application Default Credentials (ADC) to securely interact with Vertex AI (for Veo 3.0 Fast video generation) and Google Cloud Storage (for asset hosting). This eliminates the need for hardcoded JSON keys in production and makes the microservice architecture highly secure.

## Local Development Setup

### 1. Application Default Credentials (ADC)
For local development, you do not need to download a JSON key file. Instead, authenticate your local machine using the Google Cloud CLI. This allows the `CreativeDirectorAgent` to seamlessly access Vertex AI.

```bash
# Authenticate your local machine with Google Cloud
gcloud auth application-default login

# Optional: If you need to specify the quota project explicitly
gcloud auth application-default set-quota-project your_gcp_project_id
```

### 2. Environment Variables
Create a local `.env` file or export these directly in your terminal before running the Spring Boot applications:

```bash
# Required: Core AI and Cloud Configuration
export GOOGLE_CLOUD_PROJECT=nixora-project
export GEMINI_API_KEY=your_gemini_api_key_here
export CHORAGI_STORAGE_BUCKET_NAME=choragi-assets-bucket

# Required: Telephony Configuration
export TWILIO_ACCOUNT_SID=your_twilio_sid
export TWILIO_AUTH_TOKEN=your_twilio_auth_token
```

## Cloud Deployment Options

### Option 1: Cloud Run with Attached Service Account (Highly Recommended)

When deploying to Google Cloud Run, your services should run under a dedicated Service Account rather than the default compute account.

1. **Create a service account in Google Cloud Console:**
   ```bash
   gcloud iam service-accounts create choragi-sa \
     --display-name="Choragi Service Account"
   ```

2. **Grant the service account Vertex AI and Storage permissions:**
   ```bash
   # Allow video generation via Vertex AI
   gcloud projects add-iam-policy-binding $GOOGLE_CLOUD_PROJECT \
     --member="serviceAccount:choragi-sa@$GOOGLE_CLOUD_PROJECT.iam.gserviceaccount.com" \
     --role="roles/aiplatform.user"

   # Allow saving posters and videos to Cloud Storage
   gcloud projects add-iam-policy-binding $GOOGLE_CLOUD_PROJECT \
     --member="serviceAccount:choragi-sa@$GOOGLE_CLOUD_PROJECT.iam.gserviceaccount.com" \
     --role="roles/storage.admin"
   ```

3. **Deploy using the attached service account:**
   Modify your `./deploy_cloud_run.sh` to include the `--service-account` flag:
   ```bash
   gcloud run deploy creative-director \
     --source . \
     --service-account choragi-sa@$GOOGLE_CLOUD_PROJECT.iam.gserviceaccount.com \
     --allow-unauthenticated
   ```

### Option 2: Using Google Secret Manager

Instead of passing API keys as plain text environment variables, use Google Secret Manager to inject the Gemini API Key and Twilio tokens directly into the Cloud Run containers at runtime.

1. **Create the secret:**
   ```bash
   gcloud secrets create GEMINI_API_KEY --replication-policy="automatic"
   echo -n "your_api_key" | gcloud secrets versions add GEMINI_API_KEY --data-file=-
   ```
2. **Grant the `choragi-sa` access to the secret.**
3. **Reference the secret during Cloud Run deployment.**

## Security Best Practices

### For Cloud Deployment:
- ✅ Use attached service accounts (Option 1) instead of JSON files.
- ✅ Store `GEMINI_API_KEY` and `TWILIO_AUTH_TOKEN` in Google Secret Manager.
- ✅ Limit the service account permissions strictly to `aiplatform.user` and `storage.admin`.
- ❌ Never commit `.env` files or hardcode API keys into `application.properties`.

### For Local Development:
- ✅ Use `gcloud auth application-default login` for seamless Google Cloud SDK authentication.
- ✅ Use an `.env` file that is explicitly added to `.gitignore`.
- ✅ Rotate your Twilio Auth Token and Gemini API keys post-hackathon.

## Troubleshooting

### Common Issues:

1. **"VERTEX AI REJECTED THE REQUEST (403 Forbidden)"**
    - **Cause:** The Cloud Run container does not have permission to use Vertex AI.
    - **Fix:** Ensure the `choragi-sa` service account is attached to the Cloud Run deployment and has the `roles/aiplatform.user` IAM role.

2. **"Video Upload Failed / Bucket Access Denied"**
    - **Cause:** The service account cannot write to `choragi-assets-bucket`.
    - **Fix:** Ensure the service account has `roles/storage.admin` or `roles/storage.objectCreator`.

3. **"Playwright Chromium Crash in Cloud Run"**
    - **Cause:** Playwright is running out of memory or conflicting with the container filesystem.
    - **Fix:** Ensure your Java Playwright launch arguments include `--no-sandbox`, `--disable-dev-shm-usage`, and `--single-process` when deployed to the cloud.

4. **"Twilio WebSocket Disconnects Immediately"**
    - **Cause:** Invalid Gemini Native Audio configuration.
    - **Fix:** Verify that the `responseModalities` in `VoiceStreamHandler.java` is strictly set to `["AUDIO"]`.

## Testing

### Local Integration Testing:

Test the Orchestrator trigger manually via cURL without using the UI voice interface:
```bash
# Test the backend orchestration pipeline
curl -X POST http://localhost:8000/api/trigger-agents \
  -H 'Content-Type: application/json' \
  -d '{
    "artistName": "Burna Boy",
    "location": "Victoria Island Lagos",
    "date": "15th April 2026"
  }'
```

Test the Creative Director's Veo 3.0 REST implementation locally:
```bash
# Verify the Cloud Storage Bucket is publicly accessible
curl -I https://storage.googleapis.com/choragi-assets-bucket/
```

### Cloud Testing:
Once deployed, check your Cloud Run endpoints to ensure they are serving traffic:
```bash
curl -f https://ui-dashboard-xxxxxx-uc.a.run.app/actuator/health
curl -f https://venue-finder-xxxxxx-uc.a.run.app/actuator/health
```

## Migration Notes

### Moving from Local to Cloud Native:
- ✅ **WebSockets:** Upgraded from `localhost` WebSockets to secure `wss://` on Cloud Run.
- ✅ **Video Generation:** Migrated from the unstable Java SDK to the official Vertex AI REST API for Veo 3.0 Fast, ensuring 100% parameter compliance.
- ✅ **Telephony:** Migrated from local Ngrok tunneling to direct Twilio-to-Cloud Run WebSocket streaming.
- ✅ **Authentication:** Shifted from manual API key passing for Google Cloud to enterprise-grade Application Default Credentials (ADC).