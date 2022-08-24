package uk.gov.companieshouse.itemhandler.model;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.itemhandler.service.ChdItemSenderService;
import uk.gov.companieshouse.itemhandler.service.EmailService;

public enum ItemType {
    CERTIFICATE("item#certificate"),
    CERTIFIED_COPY("item#certified-copy"),
    MISSING_IMAGE_DELIVERY("item#missing-image-delivery") {
        @Override
        public void sendMessages(OrderData order) {
            getItemSender().sendItemsToChd(null);
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

    ItemType(final String kind) {
        this.kind = kind;
    }

    public static ItemType getItemType(final String kind) {
        return TYPES_BY_KIND.get(kind);
    }

    @Component
    public static class Injector {

        @Autowired
        private EmailService emailer;

        @Autowired
        private ChdItemSenderService itemSender;

        @PostConstruct
        public void postConstruct() {
            for (final ItemType type : EnumSet.allOf(ItemType.class)) {
                type.setEmailer(emailer);
                type.setItemSender(itemSender);
            }
        }
    }

    private String kind;
    private EmailService emailer;
    private ChdItemSenderService itemSender;

    public String getKind(){
        return this.kind;
    }

    /**
     * Sends outbound Kafka message(s) to propagate the order and/or its items onwards for further processing.
     * @param order the order to be propagated
     */
    public void sendMessages(final OrderData order) {
        emailer.sendOrderConfirmation(new DeliverableItemGroup(order, "", DeliveryTimescale.STANDARD));
    }

    protected void setEmailer(EmailService emailer) {
        this.emailer = emailer;
    }

    protected ChdItemSenderService getItemSender() {
        return itemSender;
    }

    protected void setItemSender(ChdItemSenderService itemSender) {
        this.itemSender = itemSender;
    }
}
