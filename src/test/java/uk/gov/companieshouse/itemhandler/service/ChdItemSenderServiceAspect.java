package uk.gov.companieshouse.itemhandler.service;

import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.itemhandler.itemsummary.ItemGroup;

import java.util.concurrent.CountDownLatch;

@Aspect
@Component
public class ChdItemSenderServiceAspect implements SenderServiceAspect {

    private final CountDownLatch latch = new CountDownLatch(1);
    private ItemGroup itemGroupSent;

    @After(value = "execution(* uk.gov.companieshouse.itemhandler.service.ChdItemSenderService.sendItemsToChd(..)) && args(itemGroup)")
    public void sendItemsToChd(final ItemGroup itemGroup) {
        latch.countDown();
        itemGroupSent = itemGroup;
    }

    public CountDownLatch getLatch() {
        return latch;
    }

    public ItemGroup getItemGroupSent() {
        return itemGroupSent;
    }
}
