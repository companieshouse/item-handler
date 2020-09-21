package uk.gov.companieshouse.itemhandler.kafka;

import org.springframework.stereotype.Service;
import uk.gov.companieshouse.itemhandler.logging.LoggingUtils;
import uk.gov.companieshouse.itemhandler.model.Item;
import uk.gov.companieshouse.kafka.exceptions.SerializationException;
import uk.gov.companieshouse.kafka.message.Message;
import uk.gov.companieshouse.kafka.serialization.AvroSerializer;
import uk.gov.companieshouse.kafka.serialization.SerializerFactory;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.orders.OrderReceived;

import java.util.Date;

@Service
public class ItemMessageFactory {
	private static final Logger LOGGER = LoggingUtils.getLogger();
	// TODO GCI-1427 Configurability?
	private static final String CHD_ITEM_ORDERED_TOPIC = "chd-item-ordered";

	private final SerializerFactory serializerFactory;

	public ItemMessageFactory(SerializerFactory serializer) {
		serializerFactory = serializer;
	}

	/**
	 * Creates an item message for onward production to a outbound Kafka topic.
	 * @param item item from order retrieved from the Orders API
	 * @return the avro message representing the item
	 * @throws SerializationException should something unexpected happen
	 */
	public Message createMessage(final Item item) throws SerializationException {
		LOGGER.trace("Creating item message");
		// TODO GCI-1301 Replace hijacked OrderReceived kafka-models class with a class for chd-item-ordered.
		final OrderReceived outgoing = new OrderReceived();
		outgoing.setOrderUri(item.getId());
		final AvroSerializer<OrderReceived> serializer =
				serializerFactory.getGenericRecordSerializer(OrderReceived.class);
		final Message message = new Message();
		message.setValue(serializer.toBinary(outgoing));
		message.setTopic(CHD_ITEM_ORDERED_TOPIC);
		message.setTimestamp(new Date().getTime());
		return message;
	}
}
