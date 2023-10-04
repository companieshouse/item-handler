package uk.gov.companieshouse.itemhandler.service;

import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.itemhandler.itemsummary.DeliverableItemGroup;

import java.util.concurrent.CountDownLatch;

@Aspect
@Component
public class EmailServiceAspect {

    private final CountDownLatch latch = new CountDownLatch(1);
    private DeliverableItemGroup itemGroupSent;

    @After(value = "execution(* uk.gov.companieshouse.itemhandler.service.EmailService.sendOrderConfirmation(..)) && args(itemGroup)")
    public void sendOrderConfirmation(final DeliverableItemGroup itemGroup) {
        latch.countDown();
        itemGroupSent = itemGroup;
    }

    public CountDownLatch getLatch() {
        return latch;
    }

    public DeliverableItemGroup getItemGroupSent() {
        return itemGroupSent;
    }
}
