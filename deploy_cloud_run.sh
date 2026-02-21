#!/bin/bash
# deploy_cloud_run.sh - Deploys all Choragi microservices to Google Cloud Run

PROJECT_ID="your-google-cloud-project-id"
REGION="us-central1"

echo " Starting Choragi Multi-Agent Deployment to Google Cloud Run..."

# Array of all your services
services=("venue_finder" "live_negotiator" "creative_director" "site_builder" "digital_promoter" "event_manager" "ui_dashboard")

for service in "${services[@]}"
do
    echo " Building and Deploying: $service"

    # 1. Submit the build to Google Cloud Build using the specific Dockerfile
    gcloud builds submit --tag gcr.io/$PROJECT_ID/choragi-$service --file Dockerfile.$service .

    # 2. Deploy the built image to Google Cloud Run
    gcloud run deploy choragi-$service \
        --image gcr.io/$PROJECT_ID/choragi-$service \
        --region $REGION \
        --platform managed \
        --allow-unauthenticated \
        --port 8080

    echo " Successfully deployed $service!"
done

echo " All Choragi services are live!"