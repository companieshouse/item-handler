package uk.gov.companieshouse.itemhandler.kafka;

import org.springframework.stereotype.Component;
import uk.gov.companieshouse.itemgroupordered.ItemGroupOrdered;
import uk.gov.companieshouse.itemgroupordered.Links;
import uk.gov.companieshouse.itemgroupordered.OrderedBy;
import uk.gov.companieshouse.itemhandler.itemsummary.ItemGroup;
import uk.gov.companieshouse.itemhandler.model.ActionedBy;
import uk.gov.companieshouse.itemhandler.model.CertificateItemOptions;
import uk.gov.companieshouse.itemhandler.model.CertifiedCopyItemOptions;
import uk.gov.companieshouse.itemhandler.model.Item;
import uk.gov.companieshouse.itemhandler.model.ItemCosts;
import uk.gov.companieshouse.itemhandler.model.ItemLinks;
import uk.gov.companieshouse.itemhandler.model.ItemOptions;
import uk.gov.companieshouse.itemhandler.model.OrderData;
import uk.gov.companieshouse.logging.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;

@Component
public class ItemGroupOrderedFactory {

    private final Logger logger;

    public ItemGroupOrderedFactory(Logger logger) {
        this.logger = logger;
    }

    public ItemGroupOrdered buildMessage(final ItemGroup digitalItemGroup) {
        final OrderData order = digitalItemGroup.getOrder();
        final ActionedBy actionedBy = order.getOrderedBy();
        final OrderedBy orderedBy = new OrderedBy(actionedBy.getEmail(), actionedBy.getId());
        final Item item = order.getItems().get(0);
        final uk.gov.companieshouse.itemgroupordered.Item avroItem =
                new uk.gov.companieshouse.itemgroupordered.Item(
                        item.getCompanyName(),
                        item.getCompanyNumber(),
                        item.getCustomerReference(),
                        item.getDescription(),
                        item.getDescriptionIdentifier(),
                        item.getDescriptionValues(),
                        "digital document location"/* TODO DCAC-254 digital document location */,
                        item.getEtag(),
                        item.getId(),
                        createItemCosts(item.getItemCosts()),
                        createItemOptions(item.getItemOptions()),
                        item.getItemUri(),
                        item.getKind(),
                        createLinks(item.getLinks()),
                        item.getPostageCost(),
                        item.isPostalDelivery(),
                        item.getQuantity(),
                        item.getTotalItemCost()
                );
        return ItemGroupOrdered.newBuilder()
                .setOrderId(order.getReference())
                .setOrderedAt(order.getOrderedAt().toString())
                .setOrderedBy(orderedBy)
                .setPaymentReference(order.getPaymentReference())
                .setReference(order.getReference())
                .setTotalOrderCost(order.getTotalOrderCost())
                .setItems(singletonList(avroItem))
                .build();
    }

    private List<uk.gov.companieshouse.itemgroupordered.ItemCosts> createItemCosts(final List<ItemCosts> itemCosts) {
        return itemCosts.stream()
                .map(costs -> new uk.gov.companieshouse.itemgroupordered.ItemCosts(
                        costs.getCalculatedCost(),
                        costs.getDiscountApplied(),
                        costs.getItemCost(),
                        costs.getProductType().toString()))
                .collect(Collectors.toList());
    }

    private Map<String, String> createItemOptions(final ItemOptions options) {

        if (options instanceof CertificateItemOptions) {
            return createCertificateItemOptions((CertificateItemOptions) options);
        } else if (options instanceof CertifiedCopyItemOptions) {
            return createCertifiedCopyItemOptions((CertifiedCopyItemOptions) options);
        }

        // TODO DCAC-254 Structured logging
        logger.error("ItemOptions instance " + options + " of unknown type.");
        return null;
    }

    private Map<String, String> createCertificateItemOptions(final CertificateItemOptions options) {
        // TODO DCAC-254 Implement mapping
        return new HashMap<>();
    }

    private Map<String, String> createCertifiedCopyItemOptions(final CertifiedCopyItemOptions options) {
        // TODO DCAC-254 Implement mapping
        return new HashMap<>();
    }

    private uk.gov.companieshouse.itemgroupordered.Links createLinks(final ItemLinks links) {
        return new Links("fetch_document"/* TODO DCAC-254 links fetch_document */, links.getSelf());
    }


}
