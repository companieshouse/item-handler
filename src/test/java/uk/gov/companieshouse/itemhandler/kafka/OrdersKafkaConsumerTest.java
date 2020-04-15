package uk.gov.companieshouse.itemhandler.kafka;

import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.kafka.consumer.resilience.CHKafkaResilientConsumerGroup;
import uk.gov.companieshouse.kafka.exceptions.SerializationException;

import java.util.concurrent.ExecutionException;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class OrdersKafkaConsumerTest {
    @InjectMocks
    private OrdersKafkaConsumer ordersKafkaConsumer;
    @Mock
    private CHKafkaResilientConsumerGroup chKafkaConsumerGroupMain;
    @Mock
    private CHKafkaResilientConsumerGroup chKafkaConsumerGroupRetry;

    @Test(expected = Exception.class)
    public void mainListenerExceptionIsCorrectlyHandled() throws InterruptedException, ExecutionException, SerializationException {
        doThrow(Exception.class).when(ordersKafkaConsumer).processOrderReceived(anyString());
        verify(chKafkaConsumerGroupMain, times(3)).retry(anyInt(), any());
    }

    @Test(expected = Exception.class)
    public void retryListenerExceptionIsCorrectlyHandled() throws InterruptedException, ExecutionException, SerializationException {
        doThrow(Exception.class).when(ordersKafkaConsumer).processOrderReceivedRetry(anyString());
        verify(chKafkaConsumerGroupRetry, times(3)).retry(anyInt(), any());
    }
}
