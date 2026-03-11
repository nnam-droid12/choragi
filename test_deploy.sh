#!/bin/bash

# Exit immediately if a command exits with a non-zero status
set -e

echo "🔍 Starting Choragi Deployment Verification..."

# Load environment variables to get the Project ID and Region
if [ -f .env ]; then
  export $(cat .env | xargs)
else
  echo "❌ .env file not found! Cannot fetch GCP variables."
  exit 1
fi

# The exact Cloud Run service names we deployed
SERVICES=("ui-client" "live-negotiator" "venue-finder" "creative" "sitebuilder" "digital-promoter")

echo "🌐 Pinging live Cloud Run endpoints..."
echo "---------------------------------------------------"

for SERVICE in "${SERVICES[@]}"; do
  # Fetch the live public URL directly from Google Cloud
  URL=$(gcloud run services describe $SERVICE \
      --platform managed \
      --region $GCP_REGION \
      --project $GCP_PROJECT_ID \
      --format 'value(status.url)' 2>/dev/null || echo "NOT_FOUND")

  if [ "$URL" == "NOT_FOUND" ] || [ -z "$URL" ]; then
    echo "⚠️  $SERVICE: Service not found. Did the deployment fail?"
    continue
  fi

  # Ping the URL and grab the HTTP status code silently
  HTTP_STATUS=$(curl -o /dev/null -s -w "%{http_code}\n" $URL)

  # Note: 200 is a perfect success.
  # 404 or 405 means the server is actively running but we didn't hit a specific GET endpoint (which is normal for REST APIs expecting POSTs).
  if [ "$HTTP_STATUS" == "200" ] || [ "$HTTP_STATUS" == "404" ] || [ "$HTTP_STATUS" == "405" ]; then
    echo "✅ $SERVICE is LIVE!"
    echo "   🔗 $URL (Status: $HTTP_STATUS)"
  else
    echo "❌ $SERVICE might be down or crashing."
    echo "   🔗 $URL (Status: $HTTP_STATUS)"
  fi
  echo "---------------------------------------------------"
done

echo "🎉 Verification complete! If all services are LIVE, Choragi is fully operational in the cloud."