package uk.gov.companieshouse.itemhandler.service;

import org.springframework.stereotype.Service;
import uk.gov.companieshouse.itemhandler.kafka.ItemMessageProducer;
import uk.gov.companieshouse.itemhandler.model.Item;
import uk.gov.companieshouse.itemhandler.model.OrderData;

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
     * @param order the {@link OrderData} instance retrieved from the Orders API
     */
    public void sendItemsToChd(final OrderData order) {
        final String orderReference = order.getReference();
        logWithOrderReference("Sending items for order to CHD", orderReference);
        final Item firstItem = order.getItems().get(0);
        itemMessageProducer.sendMessage(order, orderReference, firstItem.getId());
    }
}
