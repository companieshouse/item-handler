package uk.gov.companieshouse.itemhandler.kafka;

import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

import java.util.concurrent.CountDownLatch;

@Aspect
@Service
public class OrdersKafkaConsumerWrapper {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrdersKafkaConsumerWrapper.class.getName());
    private static CountDownLatch latch = new CountDownLatch(1);
    private String orderUri;

    @After(value = "execution(* uk.gov.companieshouse.itemhandler.kafka.OrdersKafkaConsumer.*(..)) && args(message)")
    public void afterOrderProcessed(final String message){
        LOGGER.info("OrdersKafkaConsumer.processOrderReceived() triggered");
        latch.countDown();
        this.orderUri = message;
    }

    void reset() {
        if (latch.getCount() == 0) {
            latch = new CountDownLatch(1);
        }
    }

    CountDownLatch getLatch() { return latch; }
    String getOrderUri() { return orderUri; }
}
