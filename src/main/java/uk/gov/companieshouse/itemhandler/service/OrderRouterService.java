package uk.gov.companieshouse.itemhandler.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.itemhandler.model.ItemType;
import uk.gov.companieshouse.itemhandler.model.OrderData;
import uk.gov.companieshouse.kafka.exceptions.SerializationException;

import java.util.concurrent.ExecutionException;

/**
 * Service responsible for routing orders on from this application for further processing downstream.
 */
@Service
public class OrderRouterService {

    public void routeOrder(final OrderData order)
            throws JsonProcessingException, InterruptedException, ExecutionException, SerializationException
    {
        ItemType.getItemType(order).sendMessages(order);
    }

}
