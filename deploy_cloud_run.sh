#!/bin/bash
set -e

echo "🚀 Initiating Choragi Automated Deployment Protocol..."

# Load environment variables from the root .env file
if [ -f .env ]; then
  export $(cat .env | xargs)
else
  echo "❌ .env file not found! Please ensure it exists in the root."
  exit 1
fi

# Map of your service folders to the ports they run on internally
declare -A SERVICES=(
  ["ui-client"]="8000"
  ["live-negotiator"]="8080"
  ["venue-finder"]="8081"
  ["creative_director"]="8082"
  ["site-builder"]="8083"
  ["digital-promoter"]="8084"
)

echo "🔧 Enabling Google Cloud APIs..."
gcloud services enable run.googleapis.com cloudbuild.googleapis.com bigquery.googleapis.com --project=$GCP_PROJECT_ID

for SERVICE_DIR in "${!SERVICES[@]}"; do
  PORT=${SERVICES[$SERVICE_DIR]}

  # The Cloud Run service name (lowercase, hyphens only)
  SERVICE_NAME=$(echo "$SERVICE_DIR" | tr '_' '-')

  echo "---------------------------------------------------"
  echo "⚡ Deploying $SERVICE_NAME from folder ./$SERVICE_DIR (Port: $PORT)..."

  # 1. Build the Docker image using the specific Dockerfile in the root folder
    gcloud builds submit . -f Dockerfile.${SERVICE_DIR} \
        --tag gcr.io/$GCP_PROJECT_ID/$SERVICE_NAME \
        --project=$GCP_PROJECT_ID

  # 2. Deploy to Cloud Run and inject variables
  gcloud run deploy $SERVICE_NAME \
    --image gcr.io/$GCP_PROJECT_ID/$SERVICE_NAME \
    --region $GCP_REGION \
    --platform managed \
    --port $PORT \
    --allow-unauthenticated \
    --set-env-vars="GEMINI_API_KEY=${GEMINI_API_KEY},TWILIO_ACCOUNT_SID=${TWILIO_ACCOUNT_SID},TWILIO_AUTH_TOKEN=${TWILIO_AUTH_TOKEN},TWILIO_PHONE_NUMBER=${TWILIO_PHONE_NUMBER},GOOGLE_MAPS_API_KEY=${GOOGLE_MAPS_API_KEY},FIREBASE_TOKEN=${FIREBASE_TOKEN},CHORAGI_WEBSOCKET_URL=${CHORAGI_WEBSOCKET_URL}" \
    --project=$GCP_PROJECT_ID

  echo "$SERVICE_NAME successfully deployed!"
done

echo " All Choragi services deployed! Run ./test_deploy.sh to verify."