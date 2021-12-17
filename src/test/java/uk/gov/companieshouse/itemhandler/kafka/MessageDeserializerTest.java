package uk.gov.companieshouse.itemhandler.kafka;

import static org.junit.jupiter.api.Assertions.assertNull;

import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DatumReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.orders.OrderReceived;

@ExtendWith(MockitoExtension.class)
public class MessageDeserializerTest {
    @InjectMocks
    private MessageDeserialiser deserializer;
    @Mock
    private BinaryDecoder binaryDecoder;
    @Mock
    private DatumReader<OrderReceived> datumReader;

    @Test
    public void deserializeReturnsNullIfMessageCannotBeDeserialised() {
        assertNull(deserializer.deserialize("email-send", "Test data".getBytes()));
    }

}
