package uk.gov.companieshouse.itemhandler.service;

import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.itemhandler.itemsummary.ItemGroup;

import java.util.concurrent.CountDownLatch;

@Aspect
@Component
public class EmailServiceAspect implements SenderServiceAspect {

    private CountDownLatch latch = new CountDownLatch(1);
    private ItemGroup itemGroupSent;

    @After(value = "execution(* uk.gov.companieshouse.itemhandler.service.EmailService.sendOrderConfirmation(..)) && args(itemGroup)")
    public void sendOrderConfirmation(final ItemGroup itemGroup) {
        latch.countDown();
        itemGroupSent = itemGroup;
    }

    public CountDownLatch getLatch() {
        return latch;
    }

    @Override
    public void resetLatch() {
        latch = new CountDownLatch(1);
    }

    public ItemGroup getItemGroupSent() {
        return itemGroupSent;
    }
}
