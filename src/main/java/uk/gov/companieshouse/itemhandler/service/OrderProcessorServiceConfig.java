package uk.gov.companieshouse.itemhandler.service;

import java.util.Map;
import java.util.function.Consumer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import uk.gov.companieshouse.itemhandler.kafka.OrdersKafkaConsumer;
import uk.gov.companieshouse.orders.OrderReceived;

@Configuration
class OrderProcessorServiceConfig {

    @Bean
    public Map<OrderProcessResponse.Status, Consumer<Message<OrderReceived>>> orderProcessorServiceResponseHandler() {
        return
        {
            put(OrderProcessResponse.Status.OK, (message) -> {});
            put(OrderProcessResponse.Status.SERVICE_UNAVAILABLE, OrdersKafkaConsumer.this::publishToRetryTopic);
            put(OrderProcessResponse.Status.SERVICE_ERROR, OrdersKafkaConsumer.this::logServiceError);
        }
    }
}
