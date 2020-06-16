package uk.gov.companieshouse.itemhandler.kafka;

import static uk.gov.companieshouse.itemhandler.logging.LoggingUtils.APPLICATION_NAMESPACE;
import java.util.Date;
import java.util.Map;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.email.EmailSend;
import uk.gov.companieshouse.itemhandler.logging.LoggingUtils;
import uk.gov.companieshouse.kafka.exceptions.SerializationException;
import uk.gov.companieshouse.kafka.message.Message;
import uk.gov.companieshouse.kafka.serialization.AvroSerializer;
import uk.gov.companieshouse.kafka.serialization.SerializerFactory;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

@Service
public class EmailSendMessageFactory {
	private static final Logger LOGGER = LoggerFactory.getLogger(APPLICATION_NAMESPACE);
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
	    Map<String, Object> logMap = LoggingUtils.createLogMap();
	    LoggingUtils.logIfNotNull(logMap, LoggingUtils.ORDER_REFERENCE_NUMBER, orderReference);
	    logMap.put(LoggingUtils.TOPIC, EMAIL_SEND_TOPIC);
		LOGGER.info("Create kafka message", logMap);
		final AvroSerializer<EmailSend> serializer =
				serializerFactory.getGenericRecordSerializer(EmailSend.class);
		final Message message = new Message();
		message.setValue(serializer.toBinary(emailSend));
		message.setTopic(EMAIL_SEND_TOPIC);
		LoggingUtils.logIfNotNull(logMap, LoggingUtils.ORDER_REFERENCE_NUMBER, orderReference);
		message.setTimestamp(new Date().getTime());
		LOGGER.info("Kafka message created", logMap);
		return message;
	}
}
