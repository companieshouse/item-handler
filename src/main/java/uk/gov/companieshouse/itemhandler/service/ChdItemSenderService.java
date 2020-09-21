package uk.gov.companieshouse.itemhandler.service;

import org.springframework.stereotype.Service;
import uk.gov.companieshouse.itemhandler.model.OrderData;

import static uk.gov.companieshouse.itemhandler.logging.LoggingUtils.logWithOrderReference;

/**
 * Service responsible for dispatching a message for each item in an order to CHD downstream.
 */
@Service
public class ChdItemSenderService {

    // TODO GCI-1300 Implement this
    public void sendItemsToChd(final OrderData order) {
        logWithOrderReference("Sending items for order to CHD", order.getReference());
    }
}
