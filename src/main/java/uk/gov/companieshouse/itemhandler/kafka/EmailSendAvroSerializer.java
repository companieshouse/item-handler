package uk.gov.companieshouse.itemhandler.kafka;

import email.email_send;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class EmailSendAvroSerializer implements Serializer<email_send> {

    @Override
    public byte[] serialize(String topic, email_send data) {
        final DatumWriter<email_send> datumWriter = new SpecificDatumWriter<>();

        try (final ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            final BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(out, null);
            datumWriter.setSchema(data.getSchema());
            datumWriter.write(data, encoder);
            encoder.flush();

            final byte[] serializedData = out.toByteArray();
            encoder.flush();

            return serializedData;
        } catch (IOException e) {
            throw new SerializationException("Error when serializing email_send to byte[]");
        }
    }
}
