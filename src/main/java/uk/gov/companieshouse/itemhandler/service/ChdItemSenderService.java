package uk.gov.companieshouse.itemhandler.service;

import java.util.Map;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.itemhandler.itemsummary.OrderItemPair;
import uk.gov.companieshouse.itemhandler.kafka.ItemMessageProducer;
import uk.gov.companieshouse.itemhandler.model.Item;
import uk.gov.companieshouse.itemhandler.itemsummary.ItemGroup;
import uk.gov.companieshouse.itemhandler.model.OrderData;

import static uk.gov.companieshouse.itemhandler.logging.LoggingUtils.ITEM_ID;
import static uk.gov.companieshouse.itemhandler.logging.LoggingUtils.createLogMap;
import static uk.gov.companieshouse.itemhandler.logging.LoggingUtils.logIfNotNull;
import static uk.gov.companieshouse.itemhandler.logging.LoggingUtils.logWithOrderReference;

/**
 * Service responsible for dispatching a message for each item in an order to CHD downstream.
 */
@Service
public class ChdItemSenderService {

    private final ItemMessageProducer itemMessageProducer;

    public ChdItemSenderService(final ItemMessageProducer itemMessageProducer) {
        this.itemMessageProducer = itemMessageProducer;
    }

    /**
     * Sends each item on the order individually to CHD.
     * This must be revisited post MVP as it currently is subject to the following limitations:
     * <ol>
     *     <li>it only sends the first (assumed only) item</li>
     *     <li>it assumes that item is a missing image delivery item</li>
     * </ol>
     * @param itemGroup a {@link ItemGroup group of missing image delivery items}.
     */
    public void sendItemsToChd(final ItemGroup itemGroup) {
        final String orderReference = itemGroup.getOrder().getReference();
        logWithOrderReference("Sending items for order to CHD", orderReference);
        final Map<String, Object> logMap = createLogMap();
        itemGroup.getItems().forEach(item -> {
            logIfNotNull(logMap, ITEM_ID, item.getId());
            itemMessageProducer.sendMessage(new OrderItemPair(itemGroup.getOrder(), item));
        });
    }
}
