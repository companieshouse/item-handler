package uk.gov.companieshouse.itemhandler.kafka;

import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.itemhandler.exception.NonRetryableException;
import uk.gov.companieshouse.kafka.producer.CHKafkaProducer;
import uk.gov.companieshouse.kafka.message.Message;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailSendKafkaProducerTest {

    @Spy
    private EmailSendKafkaProducer emailSendKafkaProducer;

    @Mock
    private CHKafkaProducer kafkaProducer;

    @Mock
    private Message message;

    @Mock
    private Future<RecordMetadata> result;

    @Mock
    private RecordMetadata recordMetadata;

    @Test
    void testThrowNonRetryableExceptionIfExecutionException() throws ExecutionException, InterruptedException {
        //given
        doReturn(kafkaProducer).when(emailSendKafkaProducer).getChKafkaProducer();
        when(kafkaProducer.sendAndReturnFuture(any())).thenReturn(result);
        when(result.get()).thenThrow(new ExecutionException("an error occurred", null));

        //when
        Executable executable = () -> emailSendKafkaProducer.sendMessage(message, "ORD-123456-123456", a -> {});

        //then
        NonRetryableException actual = assertThrows(NonRetryableException.class, executable);
        assertEquals("Unexpected Kafka error: an error occurred", actual.getMessage());
    }

    @Test
    void testThrowNonRetryableExceptionIfInterruptedException() throws ExecutionException, InterruptedException {
        //given
        doReturn(kafkaProducer).when(emailSendKafkaProducer).getChKafkaProducer();
        when(kafkaProducer.sendAndReturnFuture(any())).thenReturn(result);
        when(result.get()).thenThrow(new InterruptedException("an error occurred"));

        //when
        Executable executable = () -> emailSendKafkaProducer.sendMessage(message, "ORD-123456-123456", a -> {});

        //then
        NonRetryableException actual = assertThrows(NonRetryableException.class, executable);
        assertEquals("Unexpected Kafka error: an error occurred", actual.getMessage());
    }

    @Test
    void testProduceEmailSendMessage() throws ExecutionException, InterruptedException {
        //given
        doReturn(kafkaProducer).when(emailSendKafkaProducer).getChKafkaProducer();
        when(kafkaProducer.sendAndReturnFuture(any())).thenReturn(result);
        when(result.get()).thenReturn(recordMetadata);

        //when
        Executable executable = () -> emailSendKafkaProducer.sendMessage(message, "ORD-123456-123456", a -> assertSame(recordMetadata, a));

        //then
        assertDoesNotThrow(executable);
    }
}
