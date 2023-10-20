package uk.gov.companieshouse.itemhandler.kafka;

import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;
import uk.gov.companieshouse.itemgroupordered.ItemGroupOrdered;
import uk.gov.companieshouse.itemhandler.logging.LoggingUtils;
import uk.gov.companieshouse.logging.Logger;

import java.io.ByteArrayOutputStream;

public class ItemGroupOrderedAvroSerializer implements Serializer<ItemGroupOrdered> {

    // Logger cannot be injected because Kafka code uses a public 0-args
    // constructor to instantiate this serializer.
    private static final Logger LOGGER = LoggingUtils.getLogger();

    @Override
    public byte[] serialize(String topic, ItemGroupOrdered data) {
        final DatumWriter<ItemGroupOrdered> datumWriter = new SpecificDatumWriter<>();

        try (final ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            final BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(out, null);
            datumWriter.setSchema(data.getSchema());
            datumWriter.write(data, encoder);
            encoder.flush();

            final byte[] serializedData = out.toByteArray();
            encoder.flush();

            return serializedData;
        } catch (Exception e) {
            final String error = "Error when serializing ItemGroupOrdered to byte[], error: " + e.getMessage();
            LOGGER.error(error);
            throw new SerializationException(error);
        }
    }
}
