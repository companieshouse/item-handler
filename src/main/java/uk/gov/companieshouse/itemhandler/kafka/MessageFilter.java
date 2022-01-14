package uk.gov.companieshouse.itemhandler.kafka;

import org.springframework.messaging.Message;

public interface MessageFilter<T> {
    boolean include(Message<T> message);
}