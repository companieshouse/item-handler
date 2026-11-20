package uk.gov.companieshouse.itemhandler.kafka;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.apache.avro.Schema;
import org.apache.avro.AvroRuntimeException;
import org.apache.avro.generic.IndexedRecord;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.reflect.ReflectData;
import org.apache.avro.reflect.ReflectDatumReader;
import org.apache.avro.util.ClassSecurityValidator;
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
        trustSchemaClasses(requiredType);
    }

    /**
     * Avro 1.12.2+ rejects classes that are not explicitly trusted, so trust the message type and any
     * record/enum types nested within its schema (and nothing else).
     */
    private static void trustSchemaClasses(Class<?> type) {
        ClassSecurityValidator.Builder builder = ClassSecurityValidator.builder().add(type);
        addNamedTypes(ReflectData.get().getSchema(type), builder, new HashSet<>());
        ClassSecurityValidator.setGlobal(
                ClassSecurityValidator.composite(ClassSecurityValidator.getGlobal(), builder.build()));
    }

    private static void addNamedTypes(Schema schema, ClassSecurityValidator.Builder builder, Set<String> visited) {
        switch (schema.getType()) {
            case RECORD -> addRecordTypes(schema, builder, visited);
            case ENUM -> builder.add(schema.getFullName());
            case ARRAY -> addNamedTypes(schema.getElementType(), builder, visited);
            case MAP -> addNamedTypes(schema.getValueType(), builder, visited);
            case UNION -> schema.getTypes().forEach(t -> addNamedTypes(t, builder, visited));
            default -> {
                // primitive types have no classes that require trusting
            }
        }
    }

    private static void addRecordTypes(Schema schema, ClassSecurityValidator.Builder builder, Set<String> visited) {
        if (!visited.add(schema.getFullName())) {
            return;
        }
        builder.add(schema.getFullName());
        schema.getFields().forEach(field -> addNamedTypes(field.schema(), builder, visited));
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