package uk.gov.companieshouse.itemhandler.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.itemhandler.service.ChdItemSenderService;
import uk.gov.companieshouse.itemhandler.service.EmailService;
import uk.gov.companieshouse.kafka.exceptions.SerializationException;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

public enum ItemType {
    // TODO GCI-1300 Have we nailed down the SCUD naming conventions? Do they affect certs too?
    // TODO GCI-1300 Do the topic names need to be configurable? Do they even need to be exposed at this level?
    CERTIFICATE("item#certificate", "email-send"),
    CERTIFIED_COPY("item#certified-copy", "email-send"),
    SCAN_ON_DEMAND("item#scan-on-demand", "chd-item-ordered") {
        @Override
        public void sendMessages(OrderData order)
                throws InterruptedException, ExecutionException, SerializationException, JsonProcessingException {
            itemSender.sendItemsToChd(order);
        }
    };

    private static final Map<String, ItemType> TYPES_BY_KIND;

    static {
        final Map<String, ItemType> map = new ConcurrentHashMap<>();
        for (final ItemType type: ItemType.values()) {
            map.put(type.getKind(), type);
        }
        TYPES_BY_KIND = Collections.unmodifiableMap(map);
    }

    ItemType(final String kind, final String topicName) {
        this.kind = kind;
        this.topicName = topicName;
    }

    // TODO GCI-1300 Make robust
    public static ItemType getItemType(final OrderData order) {
        return TYPES_BY_KIND.get(order.getItems().get(0).getKind());
    }

    @Component
    public static class Injector {

        @Autowired
        private EmailService emailer;

        @Autowired
        private ChdItemSenderService itemSender;

        @PostConstruct
        public void postConstruct() {
            ItemType.emailer = emailer;
            ItemType.itemSender = itemSender;
        }
    }

    private String kind;
    private String topicName;
    private static EmailService emailer;
    private static ChdItemSenderService itemSender;

    public String getKind(){
        return this.kind;
    }

    public String getTopicName() {
        return topicName;
    }

    /**
     * Sends outbound Kafka message(s) to propagate the order and/or its items onwards for further processing.
     * @param order the order to be propagated
     * @throws InterruptedException
     * @throws ExecutionException
     * @throws SerializationException
     * @throws JsonProcessingException
     */
    public void sendMessages(final OrderData order)
            throws InterruptedException, ExecutionException, SerializationException, JsonProcessingException {
        emailer.sendOrderConfirmation(order);
    }

}
