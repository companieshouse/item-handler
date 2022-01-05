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
class OrderMessageErrorConsumerAspect {
    private CountDownLatch preOrderConsumedEventLatch;
    private CountDownLatch postOrderConsumedEventLatch;
    private final Logger logger;

    OrderMessageErrorConsumerAspect(Logger logger) {
        this.logger = logger;
    }

    CountDownLatch getPreOrderConsumedEventLatch() {
        return preOrderConsumedEventLatch;
    }

    void setPreOrderConsumedEventLatch(CountDownLatch countDownLatch) {
        this.preOrderConsumedEventLatch = countDownLatch;
    }

    CountDownLatch getPostOrderConsumedEventLatch() {
        return postOrderConsumedEventLatch;
    }

    void setPostOrderConsumedEventLatch(CountDownLatch postOrderConsumedEventLatch) {
        this.postOrderConsumedEventLatch = postOrderConsumedEventLatch;
    }

    @Pointcut("execution(public void uk.gov.companieshouse.itemhandler.kafka.OrderMessageErrorConsumer.processOrderReceived(..))")
    void orderProcessReceived() {
    }

    @Before("orderProcessReceived()")
    void triggerPreOrderConsumed() throws InterruptedException {
        if (!isNull(preOrderConsumedEventLatch) && !preOrderConsumedEventLatch.await(30, TimeUnit.SECONDS)) {
            logger.debug("pre order consumed latch timed out");
        }
    }

    @After("orderProcessReceived()")
    void triggerPostOrderConsumed() {
        if (!isNull(postOrderConsumedEventLatch)) {
            postOrderConsumedEventLatch.countDown();
        }
    }
}