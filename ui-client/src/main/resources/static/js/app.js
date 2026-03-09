// ==========================================
// 1. CONFIGURATION & STATE
// ==========================================
const GEMINI_API_KEY = "YOUR_GEMINI_API_KEY";
const GEMINI_WS_URL = `wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1alpha.GenerativeService.BidiGenerateContent?key=${GEMINI_API_KEY}`;

let geminiWs = null;
let audioContext = null;
let mediaStream = null;
let processor = null;

// UI Elements
const btnStart = document.getElementById('start-voice-btn');
const btnStop = document.getElementById('stop-voice-btn');
const visualizer = document.getElementById('visualizer');
const voiceStatus = document.getElementById('voice-status');
const terminalOutput = document.getElementById('terminal-output');

// ==========================================
// 2. JAVA BACKEND WEBSOCKET (STOMP/SockJS)
// ==========================================
function connectToJavaBackend() {
    // We use SockJS and Stomp to connect to the Spring Boot endpoint
    const socket = new SockJS('/choragi-live');
    const stompClient = Stomp.over(socket);

    stompClient.debug = null;

    stompClient.connect({}, function (frame) {
        logToTerminal('system', 'Connected to Choragi Internal Network.');


        stompClient.subscribe('/topic/agent-status', function (message) {
            const data = JSON.parse(message.body);
            logToTerminal(data.agent, data.message);
            updateAgentCards(data.agent, data.message);
        });
    }, function(error) {
        logToTerminal('error', 'Connection to backend lost. Retrying...');
        setTimeout(connectToJavaBackend, 5000);
    });
}

function logToTerminal(agent, message) {
    const p = document.createElement('p');
    p.className = `log-line ${agent}`;
    p.innerText = `> [${agent.toUpperCase()}] ${message}`;
    terminalOutput.appendChild(p);
    terminalOutput.scrollTop = terminalOutput.scrollHeight;
}

function updateAgentCards(activeAgent, message) {

    const agents = ['venue', 'negotiator', 'creative', 'website', 'promoter'];
    let reachedActive = false;

    agents.forEach(agent => {
        const card = document.getElementById(`card-${agent}`);
        if (!card) return;

        if (agent === activeAgent) {
            card.className = 'agent-card active';
            card.querySelector('.agent-state').innerText = "Processing...";
            reachedActive = true;
        } else if (!reachedActive) {
            card.className = 'agent-card completed';
            card.querySelector('.agent-state').innerText = "Completed";
        } else {
            card.className = 'agent-card';
            card.querySelector('.agent-state').innerText = "Awaiting Orders";
        }
    });

    if (message.includes("SUCCESS")) {
        const activeCard = document.querySelector('.agent-card.active');
        if (activeCard) {
            activeCard.className = 'agent-card completed';
            activeCard.querySelector('.agent-state').innerText = "Completed";
        }
    }
}

// ==========================================
// 3. GEMINI MULTIMODAL LIVE VOICE AGENT
// ==========================================

async function startVoiceSession() {
    voiceStatus.innerText = "Connecting to Gemini...";
    btnStart.disabled = true;

    try {
        // 1. Initialize Audio Context for 16kHz (Required by Gemini)
        audioContext = new (window.AudioContext || window.webkitAudioContext)({ sampleRate: 16000 });

        // 2. Open WebSocket to Gemini
        geminiWs = new WebSocket(GEMINI_WS_URL);

        geminiWs.onopen = async () => {
            voiceStatus.innerText = "Listening...";
            visualizer.classList.add('active');
            btnStop.disabled = false;

            // Send Initial Setup & Tool Instructions
            const setupMessage = {
                setup: {
                    model: "models/gemini-2.0-flash-exp",
                    generationConfig: {
                        responseModalities: ["AUDIO"],
                        speechConfig: { voiceConfig: { prebuiltVoiceConfig: { voiceName: "Aoede" } } }
                    },
                    systemInstruction: {
                        parts: [{
                            text: "You are Choragi, an AI assistant that scouts venues, negotiates deals, generates creative assets, and launches campaigns for live musical concerts. Keep your responses short and natural. Ask the user where they want to host their concert. Once they reply, ask them what date. Once you have BOTH the location and the date, you MUST use the 'trigger_choragi_agents' tool to start the backend process. Tell the user you are starting the work."
                        }]
                    },
                    tools: [{
                        functionDeclarations: [{
                            name: "trigger_choragi_agents",
                            description: "Starts the autonomous backend agents once the user provides a location and a date.",
                            parameters: {
                                type: "OBJECT",
                                properties: {
                                    location: { type: "STRING", description: "The city or venue location" },
                                    date: { type: "STRING", description: "The date of the concert" }
                                },
                                required: ["location", "date"]
                            }
                        }]
                    }]
                }
            };
            geminiWs.send(JSON.stringify(setupMessage));

            // 3. Start capturing Microphone
            await startMicrophone();
        };

        geminiWs.onmessage = async (event) => {
            if (event.data instanceof Blob) {
                // Ignore raw blobs if they happen to come through
                return;
            }

            const data = JSON.parse(event.data);

            // Handle Incoming Audio from Gemini
            if (data.serverContent && data.serverContent.modelTurn) {
                const parts = data.serverContent.modelTurn.parts;
                for (let part of parts) {
                    if (part.inlineData && part.inlineData.data) {
                        playAudioChunk(part.inlineData.data);
                    }
                    // Handle Tool Call (The Magic Moment!)
                    if (part.functionCall && part.functionCall.name === "trigger_choragi_agents") {
                        const args = part.functionCall.args;
                        logToTerminal('system', `Gemini triggered autonomous sequence for ${args.location} on ${args.date}`);


                        fetch('/api/trigger-agents', {
                            method: 'POST',
                            headers: { 'Content-Type': 'application/json' },
                            body: JSON.stringify({ location: args.location, date: args.date })
                        });


                        const toolResponse = {
                            clientContent: {
                                turns: [{
                                    role: "user",
                                    parts: [{
                                        functionResponse: {
                                            name: "trigger_choragi_agents",
                                            response: { result: "Success. Agents are now running." }
                                        }
                                    }]
                                }],
                                turnComplete: true
                            }
                        };
                        geminiWs.send(JSON.stringify(toolResponse));
                    }
                }
            }
        };

        geminiWs.onclose = () => stopVoiceSession();

    } catch (err) {
        console.error("Error starting voice session", err);
        stopVoiceSession();
    }
}

async function startMicrophone() {
    mediaStream = await navigator.mediaDevices.getUserMedia({ audio: true });
    const source = audioContext.createMediaStreamSource(mediaStream);

    // ScriptProcessor is used here for simplicity in a single file without needing Web Workers
    processor = audioContext.createScriptProcessor(4096, 1, 1);

    processor.onaudioprocess = (e) => {
        if (!geminiWs || geminiWs.readyState !== WebSocket.OPEN) return;

        const inputData = e.inputBuffer.getChannelData(0);

        // Convert Float32 to Int16 (PCM)
        const pcmData = new Int16Array(inputData.length);
        for (let i = 0; i < inputData.length; i++) {
            let s = Math.max(-1, Math.min(1, inputData[i]));
            pcmData[i] = s < 0 ? s * 0x8000 : s * 0x7FFF;
        }

        // Convert Int16 to Base64
        const uint8Array = new Uint8Array(pcmData.buffer);
        let binary = '';
        for (let i = 0; i < uint8Array.byteLength; i++) {
            binary += String.fromCharCode(uint8Array[i]);
        }
        const base64Data = btoa(binary);

        // Send to Gemini
        geminiWs.send(JSON.stringify({
            realtimeInput: {
                mediaChunks: [{
                    mimeType: "audio/pcm;rate=16000",
                    data: base64Data
                }]
            }
        }));
    };

    source.connect(processor);
    processor.connect(audioContext.destination);
}

function playAudioChunk(base64Data) {
    const binaryStr = atob(base64Data);
    const len = binaryStr.length;
    const bytes = new Uint8Array(len);
    for (let i = 0; i < len; i++) {
        bytes[i] = binaryStr.charCodeAt(i);
    }

    // Convert PCM Int16 back to Float32 for playback
    const int16Array = new Int16Array(bytes.buffer);
    const audioBuffer = audioContext.createBuffer(1, int16Array.length, 16000);
    const channelData = audioBuffer.getChannelData(0);

    for (let i = 0; i < int16Array.length; i++) {
        channelData[i] = int16Array[i] / 32768.0;
    }

    const source = audioContext.createBufferSource();
    source.buffer = audioBuffer;
    source.connect(audioContext.destination);
    source.start();
}

function stopVoiceSession() {
    if (geminiWs) {
        geminiWs.close();
        geminiWs = null;
    }
    if (mediaStream) {
        mediaStream.getTracks().forEach(track => track.stop());
        mediaStream = null;
    }
    if (processor) {
        processor.disconnect();
        processor = null;
    }

    voiceStatus.innerText = "Disconnected";
    visualizer.classList.remove('active');
    btnStart.disabled = false;
    btnStop.disabled = true;
}

// ==========================================
// 4. EVENT LISTENERS
// ==========================================
btnStart.addEventListener('click', startVoiceSession);
btnStop.addEventListener('click', stopVoiceSession);

// Initialize Java backend connection on load
window.addEventListener('DOMContentLoaded', connectToJavaBackend);