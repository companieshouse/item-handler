package uk.gov.companieshouse.itemhandler.kafka;

import static org.junit.jupiter.api.Assertions.assertNull;

import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DatumReader;
import org.apache.kafka.common.errors.SerializationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.orders.OrderReceived;

import java.io.IOException;

@ExtendWith(MockitoExtension.class)
public class OrderReceivedDeserializerTest {
    @InjectMocks
    private OrderReceivedDeserializer deserializer;
    @Mock
    private BinaryDecoder binaryDecoder;
    @Mock
    private DatumReader<OrderReceived> datumReader;

    @Test
    public void deserializeReturnsNullIfMessageCannotBeDeserialised() {
        assertNull(deserializer.deserialize("email-send", "Test data".getBytes()));
    }

}
