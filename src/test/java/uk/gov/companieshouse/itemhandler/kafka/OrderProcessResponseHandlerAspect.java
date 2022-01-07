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
    private CountDownLatch postServiceUnavailableLatch;

    @Pointcut("execution(public void uk.gov.companieshouse.itemhandler.kafka.OrderProcessResponseHandler.serviceUnavailable(..))")
    void serviceUnavailable() {
    }

    @After("serviceUnavailable()")
    void afterServiceUnavailable() {
        if (!isNull(postServiceUnavailableLatch)) {
            postServiceUnavailableLatch.countDown();
        }
    }

    CountDownLatch getPostServiceUnavailableLatch() {
        return postServiceUnavailableLatch;
    }

    void setPostServiceUnavailableLatch(CountDownLatch postServiceUnavailableLatch) {
        this.postServiceUnavailableLatch = postServiceUnavailableLatch;
    }
}
