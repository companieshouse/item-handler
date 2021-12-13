package uk.gov.companieshouse.itemhandler.kafka;

import java.util.Arrays;
import java.util.Map;
import org.apache.avro.generic.IndexedRecord;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.reflect.ReflectDatumReader;
import org.apache.kafka.common.serialization.Deserializer;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.itemhandler.logging.LoggingUtils;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.orders.OrderReceived;

/**
 * OrderReceived deserializer based on apache kafka Deserializer interface
 *
 * @param <T>
 */
@Component
public class OrderReceivedDeserializer<T extends IndexedRecord> implements Deserializer<T> {
    private static final Logger LOGGER = LoggingUtils.getLogger();

    @SuppressWarnings("unchecked")
    @Override
    public T deserialize(String topic, byte[] data) {
        T object;
        try {
            Decoder decoder = DecoderFactory.get().binaryDecoder(data, null);
            DatumReader<OrderReceived> reader = new ReflectDatumReader<>(OrderReceived.class);
            object = (T) reader.read(null, decoder);
        } catch (Exception e) {
            String msg = String.format(
                    "Message data [%s] from topic [%s] cannot be deserialized: %s",
                    Arrays.toString(data),
                    topic,
                    e.getMessage());
            LOGGER.error(msg, e);
            object = null;
        }
        return object;
    }

    @Override
    public void close() {
        // No-op
    }

    @Override
    public void configure(Map<String, ?> arg0, boolean arg1) {
        // No-op
    }
}