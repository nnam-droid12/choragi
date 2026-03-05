package com.choragi.negotiator.agent;

import com.choragi.negotiator.tools.TwilioDialerTool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class VoiceNegotiatorAgent {

    private final TwilioDialerTool dialerTool;

    public boolean startNegotiation(String venueName, String venuePhoneNumber) {
        log.info("Choragi Voice Agent: Initiating negotiation protocol for venue: {}", venueName);
        log.info("Target Phone Number: {}", venuePhoneNumber);


        boolean callDispatched = dialerTool.initiateNegotiationCall(venuePhoneNumber, venueName);

        if (callDispatched) {
            log.info("Outbound call triggered successfully. The agent is ringing the venue and waiting for an answer...");
        } else {
            log.error("Failed to trigger the outbound negotiation call.");
        }

        return callDispatched;
    }
}