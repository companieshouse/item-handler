package uk.gov.companieshouse.itemhandler.kafka;

import org.springframework.stereotype.Service;
import uk.gov.companieshouse.kafka.exceptions.SerializationException;
import uk.gov.companieshouse.kafka.message.Message;
import uk.gov.companieshouse.kafka.serialization.AvroSerializer;
import uk.gov.companieshouse.kafka.serialization.SerializerFactory;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.orders.OrderReceived;

import java.util.Date;

import static uk.gov.companieshouse.itemhandler.ItemHandlerApplication.APPLICATION_NAMESPACE;

@Service
public class OrdersMessageFactory {
	private static final Logger LOGGER = LoggerFactory.getLogger(APPLICATION_NAMESPACE);
	private final SerializerFactory serializerFactory;
	private static final String ORDER_RECEIVED_TOPIC = "order-received";

	public OrdersMessageFactory(SerializerFactory serializer) {
		serializerFactory = serializer;
	}

	/**
	 * Creates order-received avro message
	 * @param orderReceived order-received object
	 * @return order-received avro message
	 * @throws SerializationException should there be a failure to serialize the order received
	 */
	public Message createMessage(final OrderReceived orderReceived) throws SerializationException {
		LOGGER.trace("Configuring CH Kafka producer");
		final AvroSerializer<OrderReceived> serializer =
				serializerFactory.getGenericRecordSerializer(OrderReceived.class);
		final Message message = new Message();
		message.setValue(serializer.toBinary(orderReceived));
		message.setTopic(ORDER_RECEIVED_TOPIC);
		message.setTimestamp(new Date().getTime());
		return message;
	}
}
