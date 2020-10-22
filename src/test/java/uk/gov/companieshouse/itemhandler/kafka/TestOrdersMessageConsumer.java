package uk.gov.companieshouse.itemhandler.kafka;

import org.springframework.stereotype.Service;

@Service
public class TestOrdersMessageConsumer extends TestMessageConsumer {

    private static final String ORDER_RECEIVED_TOPIC = "order-received";
    private static final String GROUP_NAME = "order-received-consumers";

    public TestOrdersMessageConsumer() {
        super(ORDER_RECEIVED_TOPIC, GROUP_NAME);
    }

}
