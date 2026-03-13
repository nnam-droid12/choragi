package com.choragi.negotiator.tools;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Call;
import com.twilio.type.PhoneNumber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
@Slf4j
public class TwilioDialerTool {

    @Value("${twilio.account.sid}")
    private String accountSid;

    @Value("${twilio.auth.token}")
    private String authToken;

    @Value("${twilio.phone.number}")
    private String twilioPhone;

    @Value("${choragi.websocket.url}")
    private String websocketUrl;

    @PostConstruct
    public void init() {
        Twilio.init(accountSid, authToken);
        log.info("Choragi Dialer: Waking up and initializing Twilio SDK...");
    }


    public boolean initiateNegotiationCall(String targetPhoneNumber, String venueName) {
        log.info("Choragi Dialer: Placing outbound negotiation call to {} at {}", venueName, targetPhoneNumber);
        log.info("Choragi Dialer: Sending TwiML payload with dynamic URL");


        // THE FIX: Hardcoding the exact venue-finder WebSocket URL
        String twiml = "<Response><Say>Hello, connecting you to the Choragi AI.</Say><Connect><Stream url=\"wss://venue-finder-4j2p5vomta-uc.a.run.app/voice-stream\"/></Connect></Response>";


        int maxRetries = 3;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                Call call = Call.creator(
                        new PhoneNumber(targetPhoneNumber),
                        new PhoneNumber(twilioPhone),
                        new com.twilio.type.Twiml(twiml)
                ).create();

                log.info("Call successfully dispatched to telecom network! Call SID: {}", call.getSid());
                return true;

            } catch (Exception e) {
                log.error("Choragi Dialer: Failed to dispatch outbound call (Attempt {}). Error: {}", attempt, e.getMessage());
                if (attempt == maxRetries) {
                    log.error("Max retries reached. Call failed completely.");
                } else {
                    try { Thread.sleep(2000); } catch (InterruptedException ignored) {} // Wait 2 seconds and retry
                }
            }
        }
        return false;
    }
}