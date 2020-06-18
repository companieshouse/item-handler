package uk.gov.companieshouse.itemhandler.kafka;

import org.apache.kafka.common.KafkaException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.itemhandler.exception.RetryableEmailSendException;
import uk.gov.companieshouse.kafka.message.Message;
import uk.gov.companieshouse.kafka.producer.CHKafkaProducer;

import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.doThrow;

/** Partially unit tests the {@link EmailSendKafkaProducer} class. */
@ExtendWith(MockitoExtension.class)
public class EmailSendKafkaProducerTest {

    private static final String ORDER_REFERENCE_NUMBER = "ORD-469415-911973";
    private static final String TEST_EXCEPTION_MESSAGE = "Test message!";

    @InjectMocks
    private EmailSendKafkaProducer producerUnderTest;

    @Mock
    private Message message;

    @Mock
    private CHKafkaProducer chKafkaProducer;

    @Test
    void wrapsIllegalStateExceptionAsRetryable() throws ExecutionException, InterruptedException {

        // Given
        final IllegalStateException illegalStateException = new IllegalStateException(TEST_EXCEPTION_MESSAGE);
        doThrow(illegalStateException).when(chKafkaProducer).send(message);

        // When and then
        assertThatExceptionOfType(RetryableEmailSendException.class).isThrownBy(() ->
                producerUnderTest.sendMessage(message, ORDER_REFERENCE_NUMBER))
                .withCause(illegalStateException)
                .withMessage("Exception caught sending message to Kafka.");
    }

    @Test
    void wrapsKafkaExceptionAsRetryable() throws ExecutionException, InterruptedException {

        // Given
        final KafkaException kafkaException = new KafkaException(TEST_EXCEPTION_MESSAGE);
        doThrow(kafkaException).when(chKafkaProducer).send(message);

        // When and then
        assertThatExceptionOfType(RetryableEmailSendException.class).isThrownBy(() ->
                producerUnderTest.sendMessage(message, ORDER_REFERENCE_NUMBER))
                .withCause(kafkaException)
                .withMessage("Exception caught sending message to Kafka.");
    }

    @Test
    void propagatesExceptionAsIs() throws ExecutionException, InterruptedException {

        // Given
        final IllegalArgumentException illegalArgumentException =
                new IllegalArgumentException(TEST_EXCEPTION_MESSAGE);
        doThrow(illegalArgumentException).when(chKafkaProducer).send(message);

        // When and then
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() ->
                producerUnderTest.sendMessage(message, ORDER_REFERENCE_NUMBER))
                .withNoCause()
                .withMessage(TEST_EXCEPTION_MESSAGE);
    }

}
