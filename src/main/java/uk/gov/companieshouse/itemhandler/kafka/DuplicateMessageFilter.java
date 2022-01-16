package uk.gov.companieshouse.itemhandler.kafka;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.messaging.Message;
import uk.gov.companieshouse.orders.OrderReceived;

/**
 * Records OrderReceived messages and filters messages supplied with the same URI and attempt properties.
 *
 * <p>Note: Supplied messages are aged out of the internal cache on a least recently used basis.</p>
 */
class DuplicateMessageFilter implements MessageFilter<OrderReceived> {
    final Set<CacheEntry> cache;

    DuplicateMessageFilter(int cacheSize) {
        cache = Collections.newSetFromMap(new LinkedHashMap<CacheEntry, Boolean>() {
            @Override
            protected boolean removeEldestEntry(Map.Entry<CacheEntry, Boolean> eldest) {
                return size() > cacheSize;
            }
        });
    }

    @Override
    public synchronized boolean include(Message<OrderReceived> message) {
        CacheEntry cacheEntry = new CacheEntry(message.getPayload());

        boolean include = !cache.contains(cacheEntry);
        if (include) {
            cache.add(cacheEntry);
        }

        return include;
    }

    private static class CacheEntry {
        private final String uri;
        private final int attempt;

        CacheEntry(OrderReceived orderReceived) {
            this.uri = orderReceived.getOrderUri();
            this.attempt = orderReceived.getAttempt();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CacheEntry that = (CacheEntry) o;
            return attempt == that.attempt && Objects.equals(uri, that.uri);
        }

        @Override
        public int hashCode() {
            return Objects.hash(uri, attempt);
        }
    }
}