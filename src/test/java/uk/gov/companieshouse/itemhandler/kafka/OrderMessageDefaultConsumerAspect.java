package uk.gov.companieshouse.itemhandler.kafka;

import static java.util.Objects.isNull;

import java.util.concurrent.CountDownLatch;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

@Aspect
@Component
class OrderMessageDefaultConsumerAspect {
    private CountDownLatch afterProcessOrderReceivedEventLatch;

    @Pointcut("execution(public void uk.gov.companieshouse.itemhandler.kafka.OrderMessageDefaultConsumer.processOrderReceived(..))")
    void processOrderReceived() {
    }

    @After("processOrderReceived()")
    void afterProcessOrderReceived() {
        if (!isNull(afterProcessOrderReceivedEventLatch)) {
            afterProcessOrderReceivedEventLatch.countDown();
        }
    }

    CountDownLatch getAfterProcessOrderReceivedEventLatch() {
        return afterProcessOrderReceivedEventLatch;
    }

    void setAfterProcessOrderReceivedEventLatch(CountDownLatch afterProcessOrderReceivedEventLatch) {
        this.afterProcessOrderReceivedEventLatch = afterProcessOrderReceivedEventLatch;
    }
}