package uk.gov.companieshouse.itemhandler.kafka;

import static java.util.Objects.isNull;

import java.util.concurrent.CountDownLatch;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class OrderProcessResponseHandlerAspect {
    private CountDownLatch postPublishToErrorTopicLatch;

    @Pointcut("execution(private void uk.gov.companieshouse.itemhandler.kafka.OrderProcessResponseHandler.publishToErrorTopic(..))")
    private void publishToErrorTopic() {
    }

    @After("publishToErrorTopic()")
    private void afterPublishToErrorTopic() {
        if (!isNull(postPublishToErrorTopicLatch)) {
            postPublishToErrorTopicLatch.countDown();
        }
    }

    CountDownLatch getPostPublishToErrorTopicLatch() {
        return postPublishToErrorTopicLatch;
    }

    void setPostPublishToErrorTopicLatch(CountDownLatch postPublishToErrorTopicLatch) {
        this.postPublishToErrorTopicLatch = postPublishToErrorTopicLatch;
    }
}
