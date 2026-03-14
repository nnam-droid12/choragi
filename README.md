# 🚀 Choragi - AI-Powered Concert & Tour Orchestration System

A comprehensive, multi-agent AI system built to autonomously plan, negotiate, and promote live musical concerts. From scouting venues to negotiating via live phone calls, generating cinematic promo videos, and launching Google Ads — Choragi does it all.

## ☁️ Cloud Deployment Proof

*Note: Be sure to unmute the audio.*

**(DRAG AND DROP YOUR .MP4 VIDEO FILE RIGHT HERE IN GITHUB)**


## 🎯 Project Overview

Choragi is an autonomous event management platform that **finds, books, and promotes live events** through a network of specialized AI agents. The system is triggered entirely by a natural voice conversation with the user. Once launched, it automatically discovers potential venues, calls the venue managers to negotiate space, generates stunning promotional assets, deploys a custom website, and launches targeted digital ad campaigns.

## 🏗️ Architecture

<img
src="https://res.cloudinary.com/william-crm4/image/upload/v1773396916/choragi-architectural-diagram_hu7jq5.png"
alt="Choragi System Architecture"
width="1000"
/>

Choragi consists of 6 specialized microservices working together in Google Cloud:

### Core Services

- **🖥️ UI Dashboard (`ui-client`)** - The real-time WebSockets command center. Features a voice-activated Gemini Live interface that parses user intent and orchestrates the backend.
- **🏟️ Venue Scout (`venue-finder`)** - Discovers potential concert spaces and extracts contact information based on the target city and date.
- **📞 Live Negotiator (`live-negotiator`)** - Telephony agent that connects to Twilio via WebSockets, utilizing Native Audio AI to converse with venue owners and negotiate booking terms in real-time.
- **🎨 Creative Director (`creative-director`)** - Generates high-fidelity promotional assets, including 8K tour posters (Gemini Flash Image) and cinematic video trailers (Vertex AI Veo 3.0 Fast).
- **🌐 Site Builder (`site-builder`)** - Autonomously constructs and deploys a live promotional website for the concert featuring the generated assets.
- **📈 Digital Promoter (`digital-promoter`)** - Uses Playwright browser automation and Gemini Vision to autonomously navigate the Google Ads dashboard and launch targeted campaigns.

### Agent Workflow

~~~mermaid
graph TD
    A[UI Dashboard / Voice Command] -->|Orchestrator| B[Choragi Engine]
    B --> C[Venue Scout]
    C --> D[Live Negotiator]
    D -.->|Twilio + Gemini Native Audio| E((Venue Owner))
    B --> F[Creative Director]
    F -.->|Veo 3.0 Fast + Flash Image| G((Promo Assets))
    B --> H[Site Builder]
    H -.->|Asset Integration| I((Live Website))
    B --> J[Digital Promoter]
    J -.->|Playwright + Gemini Vision| K((Google Ads Campaign))
~~~

## ✨ Key Features

### Voice-Activated Orchestration
- **Native Audio Streaming** - Talk directly to the Choragi UI via the browser microphone.
- **Intelligent Intent Parsing** - The AI extracts the Artist, Location, and Date from natural conversation and automatically triggers the deployment sequence.

### Autonomous Telephony Negotiation
- **Real-Time Voice Activity Detection (VAD)** - Custom RMS audio thresholding to handle noisy phone lines.
- **MuLaw / PCM Transcoding** - On-the-fly audio byte conversion between Twilio's telephony standard and Gemini's 16kHz requirement.
- **Goal-Oriented AI** - The agent is prompted to be brief, ignore static, and secure concert space availability.

### Multi-Modal AI Creative
- **Cinematic Video Generation** - Uses Google Cloud Vertex AI (Veo 3.0 Fast) via Long-Running Operations to render stunning concert stage visuals.
- **Photorealistic Posters** - Uses Gemini 2.5 Flash Image to design highly detailed, text-accurate tour posters.

### Visual Web Automation
- **Robotic Campaign Setup** - The Digital Promoter literally "looks" at the screen using Gemini Vision (`gemini-2.5-pro`) to decide which buttons to click in Google Ads.
- **Playwright Integration** - Headless browser manipulation for data entry and campaign launch.

## 🛠️ Technology Stack

- **Framework**: Java 17+, Spring Boot
- **AI Models**:
  - Gemini 2.5 Flash Native Audio (Live API)
  - Gemini 2.5 Pro (Vision / Computer Use)
  - Gemini 2.5 Flash Image
  - Vertex AI Veo 3.0 Fast
- **Telephony**: Twilio WebSockets
- **Browser Automation**: Playwright (Chromium)
- **Frontend**: HTML/CSS/JavaScript with SockJS & STOMP WebSockets
- **Deployment**: Docker, Google Cloud Run, Google Cloud Storage

## 📋 Prerequisites

- Java 17+ and Maven
- Google Cloud Project (with Vertex AI and Cloud Storage enabled)
- API Keys & Accounts:
  - Google Gemini API Key
  - Google Cloud Service Account Credentials (for Vertex AI)
  - Twilio Account (Phone Number & TwiML App)
  - Google Ads Account
  - Ngrok (for local webhook testing)

## 🚀 Quick Start

### 1. Clone and Setup

~~~bash
git clone <repository-url>
cd choragi
~~~

### 2. Environment Configuration

Set up your environment variables (or `.properties` files) for the respective services:

~~~env
# Global
GEMINI_API_KEY=your_gemini_api_key_here
GOOGLE_CLOUD_PROJECT=your_gcp_project_id

# Creative Director
CHORAGI_STORAGE_BUCKET_NAME=your_gcs_bucket_name

# Twilio Configuration
TWILIO_ACCOUNT_SID=your_account_sid
TWILIO_AUTH_TOKEN=your_auth_token
~~~

### 3. Build and Run Locally

~~~bash
# Example: Running the UI Dashboard
cd ui-client
mvn spring-boot:run
~~~
*(Repeat for `venue-finder`, `live-negotiator`, `creative-director`, `site-builder`, and `digital-promoter`)*

### 4. Cloud Deployment

Deploy the microservices directly to Google Cloud Run using the provided script:

~~~bash
chmod +x deploy_cloud_run.sh
./deploy_cloud_run.sh
~~~

### 5. Access the Dashboard

Open your browser to the deployed Cloud Run URL or `http://localhost:8000` to access the command center. Click "Start Voice" and say: *"Book [Artist] in [Location] on [Date]."*

## 🐳 Docker Deployment

Each service contains a standard `Dockerfile` optimized for Cloud Run:

~~~dockerfile
# Build specific service
docker build -t choragi-ui-client ./ui-client
docker build -t choragi-live-negotiator ./live-negotiator
~~~

## 🔐 Security & Privacy

- Telephony audio is processed in real-time in memory and discarded after the session.
- Google Cloud operations utilize scoped Application Default Credentials (ADC).
- WebSockets fallback mechanisms ensure stable connections over unstable networks.

## 🤝 Acknowledgements

Special thanks to the Google Gemini team for project resources provided for this challenge.
