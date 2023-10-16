package uk.gov.companieshouse.itemhandler.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.itemgroupordered.ItemGroupOrdered;
import uk.gov.companieshouse.itemgroupordered.Links;
import uk.gov.companieshouse.itemgroupordered.OrderedBy;
import uk.gov.companieshouse.itemhandler.exception.KafkaMessagingException;
import uk.gov.companieshouse.itemhandler.itemsummary.ItemGroup;
import uk.gov.companieshouse.itemhandler.model.ActionedBy;
import uk.gov.companieshouse.itemhandler.model.CertifiedCopyItemOptions;
import uk.gov.companieshouse.itemhandler.model.FilingHistoryDocument;
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

import static java.lang.String.format;
import static java.util.Collections.singletonList;

@Component
public class ItemGroupOrderedFactory {

    private static final String FILING_HISTORY_ID = "filingHistoryId";
    private static final String FILING_HISTORY_DESCRIPTION = "filingHistoryDescription";
    private static final String FILING_HISTORY_DESCRIPTION_VALUES = "filingHistoryDescriptionValues";
    private static final String FILING_HISTORY_TYPE = "filingHistoryType";

    private final Logger logger;
    private final ObjectMapper objectMapper;

    public ItemGroupOrderedFactory(Logger logger, ObjectMapper objectMapper) {
        this.logger = logger;
        this.objectMapper = objectMapper;
    }

    public ItemGroupOrdered createMessage(final ItemGroup digitalItemGroup) {
        final OrderData order = digitalItemGroup.getOrder();
        final Item item = digitalItemGroup.getItems().get(0);
        // TODO DCAC-254 Structured logging
        logger.info("Creating ItemGroupOrdered message for order " + order.getReference() + ".");
        try {
            return ItemGroupOrdered.newBuilder()
                    .setOrderId(order.getReference())
                    .setOrderedAt(order.getOrderedAt().toString())
                    .setOrderedBy(createOrderedBy(order))
                    .setPaymentReference(order.getPaymentReference())
                    .setReference(order.getReference())
                    .setTotalOrderCost(order.getTotalOrderCost())
                    .setItems(singletonList(createItem(item)))
                    .build();
        } catch (Exception ex) {
            // TODO DCAC-254 Structured logging
            final String errorMessage =
                    format("Unable to create ItemGroupOrdered message for order %s item ID %s!",
                            order.getReference(),
                            item.getId());
            logger.error(errorMessage, ex);
            throw new KafkaMessagingException(errorMessage, ex);
        }
    }

    private OrderedBy createOrderedBy(final OrderData order) {
        final ActionedBy actionedBy = order.getOrderedBy();
        return new OrderedBy(actionedBy.getEmail(), actionedBy.getId());
    }

    private uk.gov.companieshouse.itemgroupordered.Item createItem(final Item item) throws JsonProcessingException {
        return new uk.gov.companieshouse.itemgroupordered.Item(
                item.getCompanyName(),
                item.getCompanyNumber(),
                item.getCustomerReference(),
                item.getDescription(),
                item.getDescriptionIdentifier(),
                item.getDescriptionValues(),
                item.getEtag(),
                item.getId(),
                createItemCosts(item.getItemCosts()),
                createFilingHistoryItemOptions(item.getItemOptions()),
                item.getItemUri(),
                item.getKind(),
                createLinks(item.getLinks()),
                item.getPostageCost(),
                item.isPostalDelivery(),
                item.getQuantity(),
                item.getTotalItemCost()
        );
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

    /**
     * Creates filing history options from the item options provided, for certified copies only.
     * @param options the item options from which filing history options may be extracted
     * @return map of filing history item options, or <code>null</code> if the options provided are not those for
     * a certified copy
     * @throws JsonProcessingException should there be an error serialising filing history description values
     */
    private Map<String, String> createFilingHistoryItemOptions(final ItemOptions options)
            throws JsonProcessingException {
       if (options instanceof CertifiedCopyItemOptions) {
            return createCertifiedCopyFirstFilingHistoryDocOptions((CertifiedCopyItemOptions) options);
       }
       return null;
    }

    /**
     * Creates a suitable map of values representing copy filing history item options ready for use as part of a
     * Kafka message.
     *
     * @param options {@link uk.gov.companieshouse.itemhandler.model.ItemOptions} the current copy item being processed
     *                                                                           in the order
     * @return map of values representing copy item filing history options
     * @throws JsonProcessingException should there be an error serialising filing history description values
     */
    private Map<String, String> createCertifiedCopyFirstFilingHistoryDocOptions(final CertifiedCopyItemOptions options)
            throws JsonProcessingException {
        final Map<String, String> filingHistoryOptions = new HashMap<>();
        final FilingHistoryDocument firstDocument = options.getFilingHistoryDocuments().get(0);
        filingHistoryOptions.put(FILING_HISTORY_TYPE, firstDocument.getFilingHistoryType());
        filingHistoryOptions.put(FILING_HISTORY_ID, firstDocument.getFilingHistoryId());
        filingHistoryOptions.put(FILING_HISTORY_DESCRIPTION, firstDocument.getFilingHistoryDescription());
        // Note this implicit contract - consumer needs to deserialise to Map<String, Object>.
        filingHistoryOptions.put(FILING_HISTORY_DESCRIPTION_VALUES,
                objectMapper.writeValueAsString(firstDocument.getFilingHistoryDescriptionValues()));
        return filingHistoryOptions;
    }

    private uk.gov.companieshouse.itemgroupordered.Links createLinks(final ItemLinks links) {
        return new Links(links.getSelf());
    }

}
