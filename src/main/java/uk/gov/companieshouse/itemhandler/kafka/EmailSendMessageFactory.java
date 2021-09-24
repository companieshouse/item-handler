package uk.gov.companieshouse.itemhandler.kafka;


import org.springframework.stereotype.Service;
import uk.gov.companieshouse.email.EmailSend;
import uk.gov.companieshouse.itemhandler.logging.LoggingUtils;
import uk.gov.companieshouse.kafka.exceptions.SerializationException;
import uk.gov.companieshouse.kafka.message.Message;
import uk.gov.companieshouse.kafka.serialization.AvroSerializer;
import uk.gov.companieshouse.kafka.serialization.SerializerFactory;

import java.util.Date;
import java.util.Map;

@Service
public class EmailSendMessageFactory {

	private final SerializerFactory serializerFactory;
	private static final String EMAIL_SEND_TOPIC = "email-send";

	public EmailSendMessageFactory(SerializerFactory serializer) {
		serializerFactory = serializer;
	}

	/**
	 * Creates an email-send avro message.
	 * @param emailSend email-send object
	 * @return email-send avro message
	 * @throws SerializationException should there be a failure to serialize the EmailSend object
	 */
	public Message createMessage(final EmailSend emailSend, String orderReference) throws SerializationException {
        Map<String, Object> logMap = LoggingUtils.createLogMapWithOrderReference(orderReference);
	    logMap.put(LoggingUtils.TOPIC, EMAIL_SEND_TOPIC);
		LoggingUtils.getLogger().info("Create kafka message", logMap);
		final AvroSerializer<EmailSend> serializer =
				serializerFactory.getGenericRecordSerializer(EmailSend.class);
		final Message message = new Message();
		message.setValue(serializer.toBinary(emailSend));
		message.setTopic(EMAIL_SEND_TOPIC);
		message.setTimestamp(new Date().getTime());
		LoggingUtils.getLogger().info("Kafka message created", logMap);
		return message;
	}
}
