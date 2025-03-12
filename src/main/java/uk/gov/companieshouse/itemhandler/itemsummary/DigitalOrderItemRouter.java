package uk.gov.companieshouse.itemhandler.itemsummary;

import org.springframework.stereotype.Component;
import uk.gov.companieshouse.itemhandler.model.OrderData;
import uk.gov.companieshouse.itemhandler.service.DigitalItemGroupSenderService;
import uk.gov.companieshouse.itemhandler.service.Routable;
import uk.gov.companieshouse.logging.Logger;

import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;
import static uk.gov.companieshouse.itemhandler.logging.LoggingUtils.getLogMap;

/**
 * Routes any digital items within the order on to digital processing
 * (eventually through the `item-group-ordered` topic).
 */
@Component
public class DigitalOrderItemRouter implements Routable {

    private static final String KIND_MISSING_IMAGE_DELIVERY = "item#missing-image-delivery";

    private final DigitalItemGroupSenderService digitalItemGroupSenderService;
    private final Logger logger;

    public DigitalOrderItemRouter(DigitalItemGroupSenderService digitalItemGroupSenderService, Logger logger) {
        this.digitalItemGroupSenderService = digitalItemGroupSenderService;
        this.logger = logger;
    }

    @Override
    public void route(final OrderData order) {
        final String orderNumber = order.getReference();
        logger.info("Routing digital items from order " + orderNumber + ".", getLogMap(orderNumber));
        final List<ItemGroup> groups = createItemGroups(order);
        groups.forEach(digitalItemGroupSenderService::sendItemGroupForDigitalProcessing);
    }

    List<ItemGroup> createItemGroups(final OrderData order) {
        final List<ItemGroup> digitalItemGroups = order.getItems().stream()
                .filter(item -> !item.getKind().equals(KIND_MISSING_IMAGE_DELIVERY) && !item.isPostalDelivery())
                .map(item -> new ItemGroup(order, item.getKind(), singletonList(item)))
                .collect(Collectors.toList());
        logItemGroupsCreated(order, digitalItemGroups);
        return digitalItemGroups;
    }

    private void logItemGroupsCreated(final OrderData order, final List<ItemGroup> digitalItemGroups) {
        final String orderNumber = order.getReference();
        if (digitalItemGroups.isEmpty()) {
            logger.info("No digital items were found, no digital item groups were created for order "
                    + orderNumber + ".\n", getLogMap(orderNumber));
            return;
        }
        final StringBuilder sb = new StringBuilder();
        sb.append("For order ").append(orderNumber).append(" created ")
                .append(digitalItemGroups.size()).append(" digital item groups:\n \n");
        for (int i = 0; i < digitalItemGroups.size(); i++) {
            final ItemGroup ig = digitalItemGroups.get(i);
            sb.append("\n + IG ").append(i + 1).append(" | ").append(ig).append("\n");
        }
        logger.info(sb.toString(), getLogMap(orderNumber));
    }

}
