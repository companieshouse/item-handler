package uk.gov.companieshouse.itemhandler.service;

import uk.gov.companieshouse.itemhandler.itemsummary.ItemGroup;

import java.util.concurrent.CountDownLatch;

public interface SenderServiceAspect {
    CountDownLatch getLatch();
    ItemGroup getItemGroupSent();
}
