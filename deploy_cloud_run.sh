#!/bin/bash
set -e

echo "🚀 Initiating Choragi Automated Deployment Protocol..."

# Load environment variables
if [ -f .env ]; then
  while IFS='=' read -r key value || [ -n "$key" ]; do
    key=$(echo "$key" | tr -d '\r' | xargs)
    value=$(echo "$value" | tr -d '\r' | xargs)
    if [[ -n "$key" && ! "$key" =~ ^# ]]; then
      export "$key=$value"
    fi
  done < .env
else
  echo "❌ .env file not found! Please ensure it exists in the root."
  exit 1
fi

declare -A SERVICES=(
  ["ui_dashboard"]="8000"
  ["live_negotiator"]="8080"
  ["venue_finder"]="8081"
  ["creative_director"]="8082"
  ["site_builder"]="8083"
  ["digital_promoter"]="8084"
)

echo "🔧 Enabling Google Cloud APIs..."
gcloud services enable run.googleapis.com cloudbuild.googleapis.com bigquery.googleapis.com --project=$GCP_PROJECT_ID

for SERVICE_DIR in "${!SERVICES[@]}"; do
  PORT=${SERVICES[$SERVICE_DIR]}
  SERVICE_NAME=$(echo "$SERVICE_DIR" | tr '_' '-')
  DOCKERFILE_NAME="Dockerfile.$SERVICE_DIR"

  echo "---------------------------------------------------"
  echo "⚡ Deploying $SERVICE_NAME using $DOCKERFILE_NAME (Port: $PORT)..."

  # THE FIX: Temporarily copy the specific Dockerfile to exactly 'Dockerfile' for gcloud
  cp $DOCKERFILE_NAME Dockerfile

  # 1. Build the Docker image
  gcloud builds submit . \
      --tag gcr.io/$GCP_PROJECT_ID/$SERVICE_NAME \
      --project=$GCP_PROJECT_ID

  # Clean up the temporary file so it doesn't pollute your workspace
  rm Dockerfile

  # 2. Deploy to Cloud Run and inject variables
  gcloud run deploy $SERVICE_NAME \
    --image gcr.io/$GCP_PROJECT_ID/$SERVICE_NAME \
    --region $GCP_REGION \
    --platform managed \
    --port $PORT \
    --allow-unauthenticated \
    --set-env-vars="SERVER_PORT=${PORT},GEMINI_API_KEY=${GEMINI_API_KEY},TWILIO_ACCOUNT_SID=${TWILIO_ACCOUNT_SID},TWILIO_AUTH_TOKEN=${TWILIO_AUTH_TOKEN},TWILIO_PHONE_NUMBER=${TWILIO_PHONE_NUMBER},GOOGLE_MAPS_API_KEY=${GOOGLE_MAPS_API_KEY},FIREBASE_TOKEN=${FIREBASE_TOKEN},CHORAGI_WEBSOCKET_URL=${CHORAGI_WEBSOCKET_URL}" \
    --project=$GCP_PROJECT_ID

  echo "✅ $SERVICE_NAME successfully deployed!"
done

echo "🎉 All Choragi services deployed! Run ./test_deploy.sh to verify."