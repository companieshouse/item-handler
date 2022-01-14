package uk.gov.companieshouse.itemhandler.kafka;

import static java.util.Objects.isNull;

import java.util.Objects;
import org.apache.kafka.common.cache.LRUCache;
import org.springframework.messaging.Message;
import uk.gov.companieshouse.orders.OrderReceived;

/**
 * Records OrderReceived messages and filters messages supplied with the same URI and attempt properties.
 *
 * <p>Note: Supplied messages are aged out of the internal cache on a least recently used basis.</p>
 */
public class DuplicateMessageFilter implements MessageFilter<OrderReceived>{
    final LRUCache<OrderReceivedKey, OrderReceivedKey> cache;

    public DuplicateMessageFilter(int cacheSize) {
        cache = new LRUCache<>(cacheSize);
    }

    @Override
    public synchronized boolean include(Message<OrderReceived> message) {
        OrderReceivedKey key = new OrderReceivedKey(message.getPayload());
        if (isNull(cache.get(key))) {
            cache.put(key, key);
            return true;
        } else {
            return false;
        }
    }

    private static class OrderReceivedKey {
        private final String uri;
        private final int attempt;

        OrderReceivedKey(OrderReceived orderReceived) {
            this.uri = orderReceived.getOrderUri();
            this.attempt = orderReceived.getAttempt();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            OrderReceivedKey that = (OrderReceivedKey) o;
            return attempt == that.attempt && Objects.equals(uri, that.uri);
        }

        @Override
        public int hashCode() {
            return Objects.hash(uri, attempt);
        }
    }
}