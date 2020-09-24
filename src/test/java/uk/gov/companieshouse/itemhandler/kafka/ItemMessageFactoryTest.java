package uk.gov.companieshouse.itemhandler.kafka;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.itemhandler.model.Item;
import uk.gov.companieshouse.kafka.message.Message;
import uk.gov.companieshouse.kafka.serialization.AvroSerializer;
import uk.gov.companieshouse.kafka.serialization.SerializerFactory;
import uk.gov.companieshouse.orders.OrderReceived;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.TimeZone;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests the {@link ItemMessageFactory} class.
 */
@ExtendWith(MockitoExtension.class)
class ItemMessageFactoryTest {

    private static final byte[] MESSAGE_CONTENT = new byte[]{};

    @InjectMocks
    private ItemMessageFactory factoryUnderTest;

    @Mock
    private SerializerFactory serializerFactory;

    @Mock
    private Item item;

    @Mock
    private AvroSerializer<OrderReceived> serializer;

    @Test
    void createMessageCreatesMessageFromItem() throws Exception {

        // Given
        when(serializerFactory.getGenericRecordSerializer(OrderReceived.class)).thenReturn(serializer);
        when(serializer.toBinary(any(OrderReceived.class))).thenReturn(MESSAGE_CONTENT);

        final LocalDateTime intervalStart = LocalDateTime.now();

        // When
        final Message message = factoryUnderTest.createMessage(item);

        final LocalDateTime intervalEnd = LocalDateTime.now();

        // Then
        verify(serializerFactory).getGenericRecordSerializer(OrderReceived.class);
        verify(serializer).toBinary(any(OrderReceived.class));

        assertThat(message, is(notNullValue()));
        assertThat(message.getValue(), is(MESSAGE_CONTENT));
        assertThat(message.getTopic(), is("chd-item-ordered"));
        assertThat(isWithinExecutionInterval(message.getTimestamp(), intervalStart, intervalEnd), is(true));

    }

    /**
     * Determines whether the timestamp is within the interval between the start and end times.
     * @param timestamp the timestamp
     * @param intervalStart the interval start - roughly the start of the test
     * @param intervalEnd the interval end - roughly the end of the test
     * @return  whether the timestamp is within the interval (<code>true</code>) or not (<code>false</code>)
     */
    private boolean isWithinExecutionInterval(final long timestamp,
                                              final LocalDateTime intervalStart,
                                              final LocalDateTime intervalEnd) {
        final LocalDateTime time = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp),
                TimeZone.getDefault().toZoneId());
        final boolean isOnOrAfterStart = time.isAfter(intervalStart) || time.isEqual(intervalStart);
        final boolean isBeforeOrOnEnd = time.isBefore(intervalEnd) || time.isEqual(intervalEnd);
        return isOnOrAfterStart && isBeforeOrOnEnd;
    }

}
