package uk.gov.companieshouse.itemhandler.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.itemhandler.logging.LoggingUtils;
import uk.gov.companieshouse.itemhandler.model.ActionedBy;
import uk.gov.companieshouse.itemhandler.model.MissingImageDeliveryItemOptions;
import uk.gov.companieshouse.itemhandler.model.OrderData;
import uk.gov.companieshouse.kafka.exceptions.SerializationException;
import uk.gov.companieshouse.kafka.message.Message;
import uk.gov.companieshouse.kafka.serialization.AvroSerializer;
import uk.gov.companieshouse.kafka.serialization.SerializerFactory;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.orders.items.ChdItemOrdered;
import uk.gov.companieshouse.orders.items.Item;
import uk.gov.companieshouse.orders.items.ItemCosts;
import uk.gov.companieshouse.orders.items.Links;
import uk.gov.companieshouse.orders.items.OrderedBy;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

@Service
public class ItemMessageFactory {

	private static final Logger LOGGER = LoggingUtils.getLogger();

	private static final String CHD_ITEM_ORDERED_TOPIC = "chd-item-ordered";

	private final SerializerFactory serializerFactory;
	private final ObjectMapper objectMapper;

	public ItemMessageFactory(final SerializerFactory serializer, final ObjectMapper mapper) {
		serializerFactory = serializer;
		objectMapper = mapper;
	}

	/**
	 * Creates an item message for onward production to a outbound Kafka topic.
	 * @param order the {@link OrderData} instance retrieved from the Orders API
	 * @return the avro message representing the item (plus some order related information)
	 * @throws SerializationException should something unexpected happen
	 * @throws JsonProcessingException should something unexpected happen TODO GCI-1301 Better Javadoc?
	 */
	public Message createMessage(final OrderData order) throws SerializationException, JsonProcessingException {
		LOGGER.trace("Creating item message"); // TODO GCI-1301 Consider logging
		final ChdItemOrdered outgoing = createChdItemOrdered(order);
		final AvroSerializer<ChdItemOrdered> serializer =
				serializerFactory.getGenericRecordSerializer(ChdItemOrdered.class);
		final Message message = new Message();
		message.setValue(serializer.toBinary(outgoing));
		message.setTopic(CHD_ITEM_ORDERED_TOPIC);
		message.setTimestamp(new Date().getTime());
		return message;
	}

	// TODO GCI-1301 Could this be better named?
	// TODO GCI-1301 Consider MapStruct or similar?
	ChdItemOrdered createChdItemOrdered(final OrderData order) throws JsonProcessingException {
		final uk.gov.companieshouse.itemhandler.model.Item firstItem = order.getItems().get(0);
		final ChdItemOrdered outgoing = new ChdItemOrdered();
		outgoing.setOrderedAt(order.getOrderedAt().toString());
		outgoing.setOrderedBy(createOrderedBy(order.getOrderedBy()));
		outgoing.setPaymentReference(order.getPaymentReference());
		outgoing.setReference(order.getReference());
		outgoing.setTotalOrderCost(order.getTotalOrderCost());

		final Item item = new Item();
		item.setId(firstItem.getId());
		item.setCompanyName(firstItem.getCompanyName());
		item.setCompanyNumber(firstItem.getCompanyNumber());
		item.setCustomerReference(firstItem.getCustomerReference());
		item.setDescription(firstItem.getDescription());
		item.setDescriptionIdentifier(firstItem.getDescriptionIdentifier());
		item.setDescriptionValues(firstItem.getDescriptionValues());
		item.setItemCosts(createFirstItemCosts(firstItem));
		item.setItemOptions(createFirstItemOptionsForMid(firstItem));
		item.setItemUri(firstItem.getItemUri());
		item.setKind(firstItem.getKind());

		// TODO GCI-1301 Constants
		item.setPostageCost("0");
		item.setIsPostalDelivery(false);

		item.setLinks(new Links(firstItem.getLinks().getSelf()));
		item.setQuantity(firstItem.getQuantity());
		item.setTotalItemCost(firstItem.getTotalItemCost());

		outgoing.setItem(item);
		return outgoing;
	}

	private List<ItemCosts> createFirstItemCosts(final uk.gov.companieshouse.itemhandler.model.Item firstItem) {
		return firstItem.getItemCosts().stream().map(costs ->
				new ItemCosts(costs.getCalculatedCost(),
						      costs.getDiscountApplied(),
						      costs.getItemCost(),
						      costs.getProductType().getJsonName()))
				.collect(toList());

	}

	private Map<String, String> createFirstItemOptionsForMid(final uk.gov.companieshouse.itemhandler.model.Item firstItem)
			throws JsonProcessingException {
		// For now we know we are dealing with MID only.
		final MissingImageDeliveryItemOptions options = (MissingImageDeliveryItemOptions) firstItem.getItemOptions();
		final Map<String, String> optionsForMid = new HashMap<>();
		// TODO GCI-1301 Constant for each key
		optionsForMid.put("filingHistoryId", options.getFilingHistoryId());
		optionsForMid.put("filingHistoryDate", options.getFilingHistoryDate());
		optionsForMid.put("filingHistoryDescription", options.getFilingHistoryDescription());
		// TODO GCI-1301 This becomes an implicit contract?
		optionsForMid.put("filingHistoryDescriptionValues",
				objectMapper.writeValueAsString(options.getFilingHistoryDescriptionValues()));
		optionsForMid.put("filingHistoryType", options.getFilingHistoryType());
		optionsForMid.put("filingHistoryCategory", options.getFilingHistoryCategory());
		return optionsForMid;
	}

	private OrderedBy createOrderedBy(final ActionedBy actionedBy) {
		return new OrderedBy(actionedBy.getEmail(), actionedBy.getId());
	}
}
