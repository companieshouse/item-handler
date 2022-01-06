package uk.gov.companieshouse.itemhandler.kafka;

import java.io.IOException;
import java.util.Arrays;
import org.apache.avro.AvroRuntimeException;
import org.apache.avro.generic.IndexedRecord;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.reflect.ReflectDatumReader;
import org.apache.kafka.common.serialization.Deserializer;
import uk.gov.companieshouse.itemhandler.logging.LoggingUtils;
import uk.gov.companieshouse.logging.Logger;

/**
 * OrderReceived deserializer based on apache kafka Deserializer interface
 *
 * @param <T>
 */
public class MessageDeserialiser<T extends IndexedRecord> implements Deserializer<T> {
    private static final Logger LOGGER = LoggingUtils.getLogger();

    private final Class<T> requiredType;

    public MessageDeserialiser(Class<T> requiredType) {
        this.requiredType = requiredType;
    }

    @Override
    public T deserialize(String topic, byte[] data) {
        try {
            Decoder decoder = DecoderFactory.get().binaryDecoder(data, null);
            DatumReader<T> reader = new ReflectDatumReader<>(requiredType);
            return reader.read(null, decoder);
        } catch (IOException | AvroRuntimeException e) {
            String msg = String.format(
                    "Message data [%s] from topic [%s] cannot be deserialized: %s",
                    Arrays.toString(data),
                    topic,
                    e.getMessage());
            LOGGER.error(msg, e);
            return null;
        }
    }
}