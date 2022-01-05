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
    private CountDownLatch postOrderConsumedEventLatch;

    CountDownLatch getPostOrderConsumedEventLatch() {
        return postOrderConsumedEventLatch;
    }

    void setPostOrderConsumedEventLatch(CountDownLatch postOrderConsumedEventLatch) {
        this.postOrderConsumedEventLatch = postOrderConsumedEventLatch;
    }

    @Pointcut("execution(public void uk.gov.companieshouse.itemhandler.kafka.OrderMessageDefaultConsumer.processOrderReceived(..))")
    void orderProcessReceived() {
    }

    @After("orderProcessReceived()")
    void triggerPostOrderConsumed() {
        if (!isNull(postOrderConsumedEventLatch)) {
            postOrderConsumedEventLatch.countDown();
        }
    }
}