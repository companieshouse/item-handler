package uk.gov.companieshouse.itemhandler.kafka;

import org.springframework.stereotype.Service;

@Service
public class TestItemMessageConsumer extends TestMessageConsumer {

    private static final String CHD_ITEM_ORDERED_TOPIC = "chd-item-ordered";
    private static final String GROUP_NAME = "chd-item-ordered-consumers";

    public TestItemMessageConsumer() {
        super(CHD_ITEM_ORDERED_TOPIC, GROUP_NAME);
    }

}
