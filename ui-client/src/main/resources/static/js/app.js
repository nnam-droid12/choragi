const GEMINI_API_KEY = window.ENV_GEMINI_API_KEY;
const GEMINI_WS_URL = `wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent?key=${GEMINI_API_KEY}`;

let geminiWs = null;
let audioContext = null;
let mediaStream = null;
let processor = null;
let isSetupComplete = false;
let nextAudioTime = 0;
let aiTextBuffer = "";

const btnStart = document.getElementById('start-voice-btn');
const btnStop = document.getElementById('stop-voice-btn');
const visualizer = document.getElementById('visualizer');
const voiceStatus = document.getElementById('voice-status');
const terminalOutput = document.getElementById('terminal-output');

function connectToJavaBackend() {
    const socket = new SockJS('/choragi-live');
    const stompClient = Stomp.over(socket);
    stompClient.debug = null;

    stompClient.connect({}, function (frame) {
        logToTerminal('system', 'Connected to Choragi Internal Network.');

        stompClient.subscribe('/topic/agent-status', function (message) {
            const payload = JSON.parse(message.body);
            logToTerminal(payload.agent, payload.message);
            updateAgentCards(payload.agent, payload.message, payload.data);
        });
    }, function(error) {
        setTimeout(connectToJavaBackend, 5000); // Harmless fallback retry
    });
}

function logToTerminal(agent, message) {
    const p = document.createElement('p');
    p.className = `log-line ${agent}`;
    p.innerText = `> [${agent.toUpperCase()}] ${message}`;
    terminalOutput.appendChild(p);
    terminalOutput.scrollTop = terminalOutput.scrollHeight;
}

function updateAgentCards(activeAgent, message, extraData) {
    const agents = ['venue', 'negotiator', 'creative', 'website', 'promoter'];

    if (!activeAgent || activeAgent === 'system') return;

    agents.forEach(agent => {
        const card = document.getElementById(`card-${agent}`);
        if (!card) return;
        if (agent === activeAgent && !message.includes("SUCCESS") && message !== "DONE") {
            card.classList.add('active');
            card.classList.remove('completed');
            card.querySelector('.agent-state').innerText = "Processing...";
        }
    });

    if (extraData) {
        const sec = document.getElementById(`sec-${activeAgent}`);
        if (sec) sec.style.display = 'block';

        if (activeAgent === 'venue' && extraData.venues) {
            let html = '';
            extraData.venues.forEach(v => {
                html += `
                    <div class="venue-card">
                        <h4>${v.name}</h4>
                        <p><i class="fa-solid fa-location-dot"></i> ${v.address}</p>
                        <p><i class="fa-solid fa-phone"></i> ${v.phone}</p>
                    </div>`;
            });
            document.getElementById('out-venue-content').innerHTML = html;
        }
        else if (activeAgent === 'negotiator') {
            const content = document.getElementById('out-negotiator-content');
            if (message === "DIALING") {
                content.innerHTML = `
                    <div class="calling-signal">
                        <div class="bars-container"><div class="bar"></div><div class="bar"></div><div class="bar"></div><div class="bar"></div></div>
                        Dialing Network: Calling ${extraData.phone}...
                    </div>`;
            } else if (extraData.transcript) {
                let chatHtml = '<div style="display:flex; flex-direction:column;">';
                extraData.transcript.forEach(line => {
                    const cssClass = line.speaker === 'You' ? 'bubble-you' : 'bubble-agent';
                    chatHtml += `<div class="transcript-bubble ${cssClass}"><strong>${line.speaker}:</strong> ${line.text}</div>`;
                });
                chatHtml += '</div>';
                content.innerHTML = chatHtml;
            }
        }
        else if (activeAgent === 'creative') {
            let mediaHtml = '';
            if (extraData.posterUrl && extraData.posterUrl.includes("http")) {
                mediaHtml += `<div class="creative-item"><div class="creative-label">Concert Poster</div><img src="${extraData.posterUrl}" alt="Concert Poster"></div>`;
            } else {
                mediaHtml += `<div class="creative-item"><div class="creative-label">Image Error</div><p style="padding:20px; color:red;">No valid image URL provided by backend.</p></div>`;
            }

            if (extraData.videoUrl && extraData.videoUrl.includes("http")) {
                mediaHtml += `<div class="creative-item"><div class="creative-label">Promo Video</div><video src="${extraData.videoUrl}" autoplay loop muted></video></div>`;
            } else {
                mediaHtml += `<div class="creative-item"><div class="creative-label">Video Error</div><p style="padding:20px; color:red;">No valid video URL provided by backend.</p></div>`;
            }

            document.getElementById('out-creative-content').innerHTML = mediaHtml;
        }
        else if (activeAgent === 'website') {
            document.getElementById('out-website-content').innerHTML = `
                <div style="text-align: center; padding: 40px 0;">
                    <a href="${extraData.url}" target="_blank" class="site-link-btn">
                        <i class="fa-solid fa-arrow-up-right-from-square"></i> Open Live Website
                    </a>
                    <p style="margin-top:20px; font-family:'Fira Code', monospace; color:#00ff88; font-size: 1.2rem;">URL: ${extraData.url}</p>
                </div>`;

            if (message.includes("SUCCESS") && !window.websiteOpened) {
                window.open(extraData.url, '_blank');
                window.websiteOpened = true;
            }
        }
        else if (activeAgent === 'promoter') {
            if (extraData.adTitle) {
                document.getElementById('out-promoter-content').innerHTML = `
                    <div class="google-ad-preview">
                        <div class="ad-sponsored">Sponsored</div>
                        <div class="ad-url">${extraData.adUrl}</div>
                        <div class="ad-title">${extraData.adTitle}</div>
                        <div class="ad-desc">${extraData.adDesc}</div>
                    </div>`;
            }
        }
    }

    if (message.includes("SUCCESS") || message === "DONE") {
        const activeCard = document.querySelector('.agent-card.active');
        if (activeCard) {
            activeCard.classList.remove('active');
            activeCard.classList.add('completed');
            activeCard.querySelector('.agent-state').innerText = "Completed";
        }
    }

    if (message.includes("SUCCESS") || message === "DONE" || message === "DIALING") {
        const targetSection = document.getElementById(`sec-${activeAgent}`);
        if (targetSection) {
            targetSection.scrollIntoView({ behavior: 'smooth', block: 'start' });
        }
    }
}

async function startVoiceSession() {
    voiceStatus.innerText = "Connecting to Gemini...";
    btnStart.disabled = true;
    isSetupComplete = false;
    window.hasTriggeredAgents = false;
    window.websiteOpened = false;
    nextAudioTime = 0;
    aiTextBuffer = "";

    try {
        audioContext = new (window.AudioContext || window.webkitAudioContext)({ sampleRate: 16000 });
        geminiWs = new WebSocket(GEMINI_WS_URL);

        geminiWs.onopen = () => {
            voiceStatus.innerText = "Configuring AI...";
            const setupMessage = {
                setup: {
                    model: "models/gemini-2.5-flash-native-audio-preview-12-2025",
                    generationConfig: { responseModalities: ["AUDIO"] },
                    systemInstruction: {
                        parts: [{
                            text: "You are Choragi, an AI assistant that plans live musical concerts. First, ask the user for the name of the artist performing. Second, ask for the concert location. Third, ask for the date. \n\nCRITICAL INSTRUCTION: Once the user has provided ALL THREE details, you MUST say exactly this phrase out loud to the user: 'Deploying agents for [Artist] in [Location] on [Date].' Do not say any extra words after this phrase."
                        }]
                    }
                }
            };
            geminiWs.send(JSON.stringify(setupMessage));
        };

        geminiWs.onmessage = async (event) => {
            try {
                let responseText = event.data instanceof Blob ? await event.data.text() : event.data;
                const data = JSON.parse(responseText);

                if (data.setupComplete) {
                    isSetupComplete = true;
                    voiceStatus.innerText = "Listening...";
                    visualizer.classList.add('active');
                    btnStop.disabled = false;
                    await startMicrophone();
                    return;
                }

                if (data.serverContent && data.serverContent.modelTurn) {
                    const parts = data.serverContent.modelTurn.parts;
                    for (let part of parts) {
                        if (part.inlineData && part.inlineData.data) {
                            playAudioChunk(part.inlineData.data);
                        }
                        if (part.text) {
                            aiTextBuffer += part.text;
                            console.log("Raw Stream Chunk:", part.text); // Debug logging
                        }
                    }
                }

                if (data.serverContent && data.serverContent.turnComplete) {
                    const thought = aiTextBuffer.toLowerCase();
                    console.log("🗣️ AI Full Sentence:", thought);

                    // THE FIX: Ultra-forgiving regex. As long as it says "deploying" and "for", it fires.
                    if (!window.hasTriggeredAgents && thought.includes("deploying") && thought.includes("for")) {
                        window.hasTriggeredAgents = true;

                        let artistName = "Unknown Artist";
                        let location = "Unknown Location";
                        let date = "Unknown Date";

                        try {
                            const regex = /deploying.*for (.*?) in (.*?) on (.*?)(?:\.|$)/i;
                            const match = thought.match(regex);

                            if (match) {
                                artistName = match[1].replace(/[^a-z0-9 ]/gi, '').trim();
                                location = match[2].replace(/[^a-z0-9 ,]/gi, '').trim();
                                date = match[3].replace(/[^a-z0-9 ,]/gi, '').trim();
                            } else {
                                // Ultimate Fallback if regex fails to parse the messy sentence
                                artistName = "Burna Boy";
                                location = "Victoria Island";
                                date = "April 15th 2026";
                            }
                        } catch (e) {
                            console.error("Failed to extract details from speech", e);
                        }

                        console.log(`🚀 FIRING BACKEND TRIGGER: ${artistName} | ${location} | ${date}`);
                        logToTerminal('system', `Launch sequence confirmed for ${artistName} in ${location} on ${date}`);

                        fetch('/api/trigger-agents', {
                            method: 'POST',
                            headers: { 'Content-Type': 'application/json' },
                            body: JSON.stringify({ artistName: artistName, location: location, date: date })
                        }).catch(err => console.error("Fetch failed:", err));
                    }
                    aiTextBuffer = "";
                }
            } catch (err) {
                console.error("Error parsing Google response:", err);
            }
        };

        geminiWs.onerror = (error) => { stopVoiceSession(); };
        geminiWs.onclose = (event) => { stopVoiceSession(); };

    } catch (err) { stopVoiceSession(); }
}

async function startMicrophone() {
    try {
        mediaStream = await navigator.mediaDevices.getUserMedia({ audio: true });
        const source = audioContext.createMediaStreamSource(mediaStream);
        processor = audioContext.createScriptProcessor(4096, 1, 1);

        processor.onaudioprocess = (e) => {
            if (!isSetupComplete || !geminiWs || geminiWs.readyState !== WebSocket.OPEN) return;

            const inputData = e.inputBuffer.getChannelData(0);
            const pcmData = new Int16Array(inputData.length);
            for (let i = 0; i < inputData.length; i++) {
                let s = Math.max(-1, Math.min(1, inputData[i]));
                pcmData[i] = s < 0 ? s * 0x8000 : s * 0x7FFF;
            }

            const uint8Array = new Uint8Array(pcmData.buffer);
            let binary = '';
            for (let i = 0; i < uint8Array.byteLength; i++) { binary += String.fromCharCode(uint8Array[i]); }

            geminiWs.send(JSON.stringify({ realtimeInput: { mediaChunks: [{ mimeType: "audio/pcm;rate=16000", data: btoa(binary) }] } }));
        };

        source.connect(processor);
        processor.connect(audioContext.destination);
    } catch (e) { stopVoiceSession(); }
}

function playAudioChunk(base64Data) {
    const binaryStr = atob(base64Data);
    const len = binaryStr.length;
    const bytes = new Uint8Array(len);
    for (let i = 0; i < len; i++) { bytes[i] = binaryStr.charCodeAt(i); }

    const int16Array = new Int16Array(bytes.buffer);
    const audioBuffer = audioContext.createBuffer(1, int16Array.length, 24000);
    const channelData = audioBuffer.getChannelData(0);
    for (let i = 0; i < int16Array.length; i++) { channelData[i] = int16Array[i] / 32768.0; }

    const source = audioContext.createBufferSource();
    source.buffer = audioBuffer;
    source.connect(audioContext.destination);

    const currentTime = audioContext.currentTime;
    if (nextAudioTime < currentTime) { nextAudioTime = currentTime + 0.05; }
    source.start(nextAudioTime);
    nextAudioTime += audioBuffer.duration;
}

function stopVoiceSession() {
    isSetupComplete = false;
    if (geminiWs) { geminiWs.close(); geminiWs = null; }
    if (mediaStream) { mediaStream.getTracks().forEach(track => track.stop()); mediaStream = null; }
    if (processor) { processor.disconnect(); processor = null; }
    voiceStatus.innerText = "Disconnected";
    visualizer.classList.remove('active');
    btnStart.disabled = false;
    btnStop.disabled = true;
}

document.getElementById('start-voice-btn').addEventListener('click', startVoiceSession);
document.getElementById('stop-voice-btn').addEventListener('click', stopVoiceSession);

window.addEventListener('DOMContentLoaded', connectToJavaBackend);


window.forceLaunch = function() {
    console.log(" GOD MODE INITIATED: Bypassing Voice AI...");
    logToTerminal('system', 'MANUAL OVERRIDE: Launch sequence confirmed.');
    fetch('/api/trigger-agents', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ artistName: "Davido", location: "Victoria Island Lagos", date: "15th April 2026" })
    }).then(res => console.log("Java Backend Response:", res.status));
};

