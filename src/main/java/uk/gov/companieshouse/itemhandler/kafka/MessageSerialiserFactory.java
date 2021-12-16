package uk.gov.companieshouse.itemhandler.kafka;


import org.apache.avro.generic.GenericRecord;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.itemhandler.exception.ApplicationSerialisationException;
import uk.gov.companieshouse.itemhandler.logging.LoggingUtils;
import uk.gov.companieshouse.kafka.exceptions.SerializationException;
import uk.gov.companieshouse.kafka.message.Message;
import uk.gov.companieshouse.kafka.serialization.AvroSerializer;
import uk.gov.companieshouse.kafka.serialization.SerializerFactory;

import java.util.Date;
import uk.gov.companieshouse.logging.Logger;

@Service
public class MessageSerialiserFactory<T extends GenericRecord> {
	private static final Logger LOGGER = LoggingUtils.getLogger();
	private final SerializerFactory serializerFactory;
	private final Class<T> clazz;

	public MessageSerialiserFactory(SerializerFactory serializer, Class<T> clazz) {
		serializerFactory = serializer;
		this.clazz = clazz;
	}

	/**
	 * Creates an avro message from a payload object.
	 * @param payload object
	 * @return avro message
	 */
	public Message createMessage(final T payload, String topic) {
		try {
			final AvroSerializer<T> serializer = serializerFactory.getGenericRecordSerializer(clazz);
			final Message message = new Message();
			message.setValue(serializer.toBinary(payload));
			message.setTopic(topic);
			message.setTimestamp(new Date().getTime());
			return message;
		} catch (SerializationException e) {
			String msg = String.format("Serialisation error: %s", e.getMessage());
			LOGGER.error(msg, e);
			throw new ApplicationSerialisationException(msg);
		}
	}
}
