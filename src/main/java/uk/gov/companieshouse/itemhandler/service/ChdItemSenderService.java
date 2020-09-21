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
     * TODO GCI-1428 Javadoc this
     * @param order
     */
    public void sendItemsToChd(final OrderData order) {
        logWithOrderReference("Sending items for order to CHD", order.getReference());
        for (final Item item: order.getItems()) {
            itemMessageProducer.sendMessage(order.getReference(), item.getId(), item);
        }
    }
}
