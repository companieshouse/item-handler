package uk.gov.companieshouse.itemhandler.itemsummary;

import org.springframework.stereotype.Component;
import uk.gov.companieshouse.itemhandler.model.OrderData;
import uk.gov.companieshouse.itemhandler.service.Routable;
import uk.gov.companieshouse.logging.Logger;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Routes any digital items within the order on to digital processing
 * (eventually through the `item-group-ordered` topic).
 */
@Component
public class DigitalOrderItemRouter implements Routable {

    private final Logger logger;

    public DigitalOrderItemRouter(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void route(final OrderData order) {
        // TODO DCAC-253 Structured logging?
        logger.info("Routing digital items from order " + order.getReference());
        final List<ItemGroup> digitalItemGroups = order.getItems().stream()
                .filter(item -> !item.isPostalDelivery())
                .map(item -> new ItemGroup(order, item.getKind(), Collections.singletonList(item)))
                .collect(Collectors.toList());
        logItemGroupsCreated(order, digitalItemGroups);
    }

    private void logItemGroupsCreated(final OrderData order, final List<ItemGroup> digitalItemGroups) {
        // TODO DCAC-253 Structured logging?
        if (digitalItemGroups.isEmpty()) {
            logger.info("No digital items were found, no digital item groups were created.");
            return;
        }
        final StringBuilder sb = new StringBuilder();
        sb.append("For order " + order.getReference() + " created " + digitalItemGroups.size() +
                " digital item groups:\n");
        for (int i = 0; i < digitalItemGroups.size(); i++) {
            final ItemGroup ig = digitalItemGroups.get(i);
            sb.append("\n + IG " + (i + 1) + " with kind " + ig.getKind() + " and " + ig.getItems().size() + " items:\n"
                    + describeItemGroup(digitalItemGroups.get(i)));
        }
        logger.info(sb.toString());
    }

    private String describeItemGroup(final ItemGroup itemGroup) {
        return itemGroup.getItems().stream()
                .map(item -> " - + " + item.getId() + " [" + item.getKind() + "]")
                .collect(Collectors.joining("\n"));
    }
}
