package uk.gov.companieshouse.itemhandler.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.companieshouse.itemhandler.kafka.OrdersKafkaProducer;
import uk.gov.companieshouse.kafka.exceptions.SerializationException;
import uk.gov.companieshouse.kafka.message.Message;
import uk.gov.companieshouse.kafka.serialization.AvroSerializer;
import uk.gov.companieshouse.kafka.serialization.SerializerFactory;
import uk.gov.companieshouse.orders.OrderReceived;

import java.util.Date;
import java.util.concurrent.ExecutionException;

/**
 * Returns HTTP OK response to indicate a healthy service is running
 */
@RestController
public class HealthcheckController {
    @GetMapping("${uk.gov.companieshouse.item-handler.health}")
    public ResponseEntity<Void> getHealthCheck (){
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @Autowired
    private SerializerFactory serializerFactory;
    @Autowired
    private OrdersKafkaProducer producer;

    @GetMapping("/test/{uri}")
    public ResponseEntity<Void> testKafka(@PathVariable("uri") String uri)
            throws SerializationException, ExecutionException, InterruptedException {
        Message message = createRetryMessage("/orders/" + uri, "order-received");
        producer.sendMessage(message);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    protected Message createRetryMessage(String orderUri, String topic) throws SerializationException {
        final Message message = new Message();
        AvroSerializer serializer = serializerFactory.getGenericRecordSerializer(OrderReceived.class);
        OrderReceived orderReceived = new OrderReceived();
        orderReceived.setOrderUri(orderUri.trim());

        message.setKey("order-received");
        message.setValue(serializer.toBinary(orderReceived));
        message.setTopic(topic);
        message.setTimestamp(new Date().getTime());

        return message;
    }
}
