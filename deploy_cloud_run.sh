#!/bin/bash
set -e

echo "🚀 Initiating Choragi Automated Deployment Protocol..."

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
)

echo "🔧 Enabling Google Cloud APIs..."
gcloud services enable run.googleapis.com cloudbuild.googleapis.com bigquery.googleapis.com --project=$GCP_PROJECT_ID

for SERVICE_DIR in "${!SERVICES[@]}"; do
  PORT=${SERVICES[$SERVICE_DIR]}
  SERVICE_NAME=$(echo "$SERVICE_DIR" | tr '_' '-')
  DOCKERFILE_NAME="Dockerfile.$SERVICE_DIR"

  echo "---------------------------------------------------"
  echo "⚡ Deploying $SERVICE_NAME using $DOCKERFILE_NAME (Port: $PORT)..."

  cp $DOCKERFILE_NAME Dockerfile

  gcloud builds submit . \
      --tag gcr.io/$GCP_PROJECT_ID/$SERVICE_NAME \
      --project=$GCP_PROJECT_ID

  rm Dockerfile

  MEMORY="2048Mi"
  TIMEOUT="300"
  EXEC_ENV="gen1"

  # THE FIX: Gen 2 Environment explicitly injected for Playwright
  if [ "$SERVICE_DIR" == "digital_promoter" ]; then
    echo "⚙️  Detected Playwright Agent. Injecting Gen 2 Environment, 4GB RAM, and 15-Min Timeout..."
    MEMORY="4096Mi"
    TIMEOUT="900"
    EXEC_ENV="gen2"
  fi

  gcloud run deploy $SERVICE_NAME \
      --image gcr.io/$GCP_PROJECT_ID/$SERVICE_NAME \
      --region $GCP_REGION \
      --platform managed \
      --port $PORT \
      --memory $MEMORY \
      --cpu 2 \
      --timeout $TIMEOUT \
      --execution-environment $EXEC_ENV \
      --allow-unauthenticated \
      --set-env-vars="SERVER_PORT=${PORT},GEMINI_API_KEY=${GEMINI_API_KEY},TWILIO_ACCOUNT_SID=${TWILIO_ACCOUNT_SID},TWILIO_AUTH_TOKEN=${TWILIO_AUTH_TOKEN},TWILIO_PHONE_NUMBER=${TWILIO_PHONE_NUMBER},GOOGLE_MAPS_API_KEY=${GOOGLE_MAPS_API_KEY},FIREBASE_TOKEN=${FIREBASE_TOKEN},CHORAGI_WEBSOCKET_URL=${CHORAGI_WEBSOCKET_URL}" \
      --project=$GCP_PROJECT_ID

  echo "✅ $SERVICE_NAME successfully deployed!"
done

echo "🎉 All Choragi services deployed! Run ./test_deploy.sh to verify."
