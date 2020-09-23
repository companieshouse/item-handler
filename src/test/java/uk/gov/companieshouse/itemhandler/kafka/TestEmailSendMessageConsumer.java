package uk.gov.companieshouse.itemhandler.kafka;

import org.springframework.stereotype.Service;

@Service
public class TestEmailSendMessageConsumer extends TestMessageConsumer {

    private static final String EMAIL_SEND_TOPIC = "email-send";
    private static final String GROUP_NAME = "message-send-consumer-group";

    public TestEmailSendMessageConsumer() {
        super(EMAIL_SEND_TOPIC, GROUP_NAME);
    }

}
