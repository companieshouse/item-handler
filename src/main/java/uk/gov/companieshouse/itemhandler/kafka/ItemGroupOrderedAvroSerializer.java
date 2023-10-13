package uk.gov.companieshouse.itemhandler.kafka;

import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;
import uk.gov.companieshouse.itemgroupordered.ItemGroupOrdered;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ItemGroupOrderedAvroSerializer implements Serializer<ItemGroupOrdered> {

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
        } catch (IOException e) {
            throw new SerializationException("Error when serializing ItemGroupOrdered to byte[]");
        }
    }
}
