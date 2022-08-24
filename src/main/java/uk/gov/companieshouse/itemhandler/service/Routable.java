package uk.gov.companieshouse.itemhandler.service;

import uk.gov.companieshouse.itemhandler.model.OrderData;

public interface Routable {
    void route(OrderData order);
}
