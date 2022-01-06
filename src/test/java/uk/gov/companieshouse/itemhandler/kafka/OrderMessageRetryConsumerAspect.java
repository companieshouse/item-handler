package uk.gov.companieshouse.itemhandler.kafka;

import static java.util.Objects.isNull;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.logging.Logger;

@Aspect
@Component
class OrderMessageRetryConsumerAspect {
    private CountDownLatch beforeProcessOrderReceivedEventLatch;
    private CountDownLatch afterProcessOrderReceivedEventLatch;
    private final Logger logger;

    OrderMessageRetryConsumerAspect(Logger logger) {
        this.logger = logger;
    }

    @Pointcut("execution(public void uk.gov.companieshouse.itemhandler.kafka.OrderMessageRetryConsumer.processOrderReceived(..))")
    void processOrderReceived() {
    }

    @Before("processOrderReceived()")
    void beforeProcessOrderReceived() throws InterruptedException {
        if (!isNull(beforeProcessOrderReceivedEventLatch) && !beforeProcessOrderReceivedEventLatch.await(30, TimeUnit.SECONDS)) {
            logger.debug("pre order consumed latch timed out");
        }
    }

    @After("processOrderReceived()")
    void afterProcessOrderReceived() {
        if (!isNull(afterProcessOrderReceivedEventLatch)) {
            afterProcessOrderReceivedEventLatch.countDown();
        }
    }

    CountDownLatch getBeforeProcessOrderReceivedEventLatch() {
        return beforeProcessOrderReceivedEventLatch;
    }

    void setBeforeProcessOrderReceivedEventLatch(CountDownLatch countDownLatch) {
        this.beforeProcessOrderReceivedEventLatch = countDownLatch;
    }

    CountDownLatch getAfterProcessOrderReceivedEventLatch() {
        return afterProcessOrderReceivedEventLatch;
    }

    void setAfterProcessOrderReceivedEventLatch(CountDownLatch afterProcessOrderReceivedEventLatch) {
        this.afterProcessOrderReceivedEventLatch = afterProcessOrderReceivedEventLatch;
    }
}