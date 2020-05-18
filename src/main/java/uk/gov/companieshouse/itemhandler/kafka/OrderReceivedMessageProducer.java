package uk.gov.companieshouse.itemhandler.kafka;

import org.springframework.stereotype.Service;
import uk.gov.companieshouse.kafka.exceptions.SerializationException;
import uk.gov.companieshouse.kafka.message.Message;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.orders.OrderReceived;

import java.util.concurrent.ExecutionException;

import static uk.gov.companieshouse.itemhandler.ItemHandlerApplication.APPLICATION_NAMESPACE;

@Service
public class OrderReceivedMessageProducer {
    private static final Logger LOGGER = LoggerFactory.getLogger(APPLICATION_NAMESPACE);
    private final OrdersMessageFactory ordersAvroSerializer;
    private final OrdersKafkaProducer ordersKafkaProducer;

    public OrderReceivedMessageProducer(final OrdersMessageFactory avroSerializer, final OrdersKafkaProducer kafkaMessageProducer) {
        this.ordersAvroSerializer = avroSerializer;
        this.ordersKafkaProducer = kafkaMessageProducer;
    }

    /**
     * Sends order-received message to CHKafkaProducer
     * @param orderReceived order-received object
     * @throws SerializationException should there be a failure to serialize the order received
     * @throws ExecutionException should something unexpected happen
     * @throws InterruptedException should something unexpected happen
     */
    public void sendMessage(final OrderReceived orderReceived)
            throws SerializationException, ExecutionException, InterruptedException {
        LOGGER.trace("Sending message to kafka producer");
        Message message = ordersAvroSerializer.createMessage(orderReceived);
        ordersKafkaProducer.sendMessage(message);
    }
}
