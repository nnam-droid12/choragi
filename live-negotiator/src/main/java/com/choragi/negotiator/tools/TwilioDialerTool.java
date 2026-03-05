package com.choragi.negotiator.tools;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Call;
import com.twilio.type.PhoneNumber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
@Slf4j
public class TwilioDialerTool {

    @Value("${twilio.account.sid}")
    private String accountSid;

    @Value("${twilio.auth.token}")
    private String authToken;

    @Value("${twilio.phone.number}")
    private String twilioNumber;

    @Value("${choragi.websocket.url}")
    private String websocketUrl;

    public boolean initiateNegotiationCall(String targetVenueNumber, String venueName) {
        log.info("Choragi Dialer: Waking up and initializing Twilio SDK...");
        Twilio.init(accountSid, authToken);

        log.info("Choragi Dialer: Placing outbound negotiation call to Venue at {}", targetVenueNumber);

        try {
            // Encode the venue name to safely attach it to the WebSocket URL
            String encodedVenue = URLEncoder.encode(venueName, StandardCharsets.UTF_8);
            String dynamicSocketUrl = websocketUrl + "?venue=" + encodedVenue;

            String twiml = String.format(
                    "<Response>" +
                            "<Say>Connecting to the agent now.</Say>" +
                            "<Pause length=\"1\"/>" +
                            "<Connect><Stream url=\"%s\" /></Connect>" +
                            "</Response>",
                    dynamicSocketUrl
            );

            log.info("Choragi Dialer: Sending TwiML payload with dynamic URL");

            Call call = Call.creator(
                    new PhoneNumber(targetVenueNumber),
                    new PhoneNumber(twilioNumber),
                    new com.twilio.type.Twiml(twiml)
            ).create();

            log.info("Call successfully dispatched to telecom network! Call SID: {}", call.getSid());
            return true;

        } catch (Exception e) {
            log.error("Choragi Dialer: Failed to dispatch outbound call. Error: {}", e.getMessage(), e);
            return false;
        }
    }
}