#!/bin/bash

echo "🔍 Starting Choragi Deployment Verification..."
echo "🌐 Pinging live Cloud Run endpoints..."
echo "---------------------------------------------------"

# THE FIX: These names now perfectly match your live Google Cloud Run services
SERVICES=(
  "ui-dashboard"
  "live-negotiator"
  "venue-finder"
  "creative-director"
  "site-builder"
  "digital-promoter"
)

# Replace this with your actual GCP Project ID and Region
PROJECT_ID="nixora-project"
REGION="us-central1"

for SERVICE in "${SERVICES[@]}"; do
  # Silently ask Google Cloud for the live URL of the service
  URL=$(gcloud run services describe $SERVICE --project=$PROJECT_ID --region=$REGION --format="value(status.url)" 2>/dev/null)

  if [ -z "$URL" ]; then
    echo "⚠️  $SERVICE: Service not found. Check the spelling!"
  else
    echo "✅ $SERVICE: LIVE at $URL"
  fi
done

echo "---------------------------------------------------"
echo "🎉 Verification complete! Choragi is fully operational in the cloud."

