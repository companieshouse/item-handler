package uk.gov.companieshouse.itemhandler.kafka;

import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.itemhandler.exception.NonRetryableException;
import uk.gov.companieshouse.kafka.producer.CHKafkaProducer;
import uk.gov.companieshouse.kafka.message.Message;
import uk.gov.companieshouse.logging.Logger;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageProducerTest {

    @InjectMocks
    private MessageProducer messageProducer;

    @Mock
    private CHKafkaProducer kafkaProducer;

    @Mock
    private Message message;

    @Mock
    private Future<RecordMetadata> result;

    @Mock
    private RecordMetadata recordMetadata;

    @Mock
    private Logger logger;

    @Test
    void testThrowNonRetryableExceptionIfExecutionException() throws ExecutionException, InterruptedException {
        //given
        ExecutionException expectedException = new ExecutionException("an error occurred", null);
        when(kafkaProducer.sendAndReturnFuture(any())).thenReturn(result);
        when(result.get()).thenThrow(expectedException);

        //when
        Executable executable = () -> messageProducer.sendMessage(message, a -> {});

        //then
        NonRetryableException actual = assertThrows(NonRetryableException.class, executable);
        assertEquals("Unexpected Kafka error: an error occurred", actual.getMessage());
        verify(logger).error("Unexpected Kafka error: an error occurred", expectedException);
    }

    @Test
    void testThrowNonRetryableExceptionIfInterruptedException() throws ExecutionException, InterruptedException {
        //given
        InterruptedException expectedException = new InterruptedException("an error occurred");
        when(kafkaProducer.sendAndReturnFuture(any())).thenReturn(result);
        when(result.get()).thenThrow(expectedException);

        //when
        Executable executable = () -> messageProducer.sendMessage(message, a -> {});

        //then
        NonRetryableException actual = assertThrows(NonRetryableException.class, executable);
        assertEquals("Unexpected Kafka error: an error occurred", actual.getMessage());
        verify(logger).error("Unexpected Kafka error: an error occurred", expectedException);
    }

    @Test
    void testSendMessage() throws ExecutionException, InterruptedException {
        //given
        when(kafkaProducer.sendAndReturnFuture(any())).thenReturn(result);
        when(result.get()).thenReturn(recordMetadata);

        //when
        Executable executable = () -> messageProducer.sendMessage(message, a -> assertSame(recordMetadata, a));

        //then
        assertDoesNotThrow(executable);
    }
}
