package uk.gov.companieshouse.itemhandler.service;

import static org.apache.commons.collections.CollectionUtils.isEmpty;

import java.util.List;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.itemhandler.exception.NonRetryableException;
import uk.gov.companieshouse.itemhandler.model.Item;
import uk.gov.companieshouse.itemhandler.model.ItemType;
import uk.gov.companieshouse.itemhandler.model.OrderData;

/**
 * Service responsible for routing orders on from this application for further processing downstream.
 */
@Service
public class OrderRouterService {

    /**
     * Routes the order onwards for further processing.
     * @param order the incoming order
     */
    public void routeOrder(final OrderData order) {
        getItemType(order).sendMessages(order);
    }

    /**
     * Determines the item type of the order by examining the kind of the first item in the order.
     * <b>This logic will cease to be correct should there ever be items of more than one type in the order.</b>
     * @param order the incoming order
     * @return the item type inferred for the order
     * @throws NonRetryableException should it be impossible to determine the item type from the order
     */
    ItemType getItemType(final OrderData order) {
        final String kind = getKind(order);
        final ItemType type = ItemType.getItemType(kind);
        if (type == null) {
            throw new NonRetryableException("Kind " + kind + " on item " + order.getItems().get(0).getId() +
                    " on order " + order.getReference() + " is unknown.");
        }
        return type;
    }

    private String getKind(final OrderData order) {
        final List<Item> items = order.getItems();
        if (isEmpty(items)) {
            throw new NonRetryableException("Order " + order.getReference() + " contains no items.");
        }
        final Item firstItem = items.get(0);
        if (Strings.isEmpty(firstItem.getKind())) {
            throw new NonRetryableException("Order " + order.getReference() + " item " + firstItem.getId() + " has no kind.");
        }
        return firstItem.getKind();
    }

}
