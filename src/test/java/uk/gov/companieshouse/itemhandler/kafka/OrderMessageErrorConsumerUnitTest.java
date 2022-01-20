package uk.gov.companieshouse.itemhandler.kafka;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.event.ConsumerStoppedEvent;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.messaging.Message;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.orders.OrderReceived;

@ExtendWith(MockitoExtension.class)
class OrderMessageErrorConsumerUnitTest {

    @Mock
    private Message<OrderReceived> message;

    @Mock
    private KafkaConsumer<String, OrderReceived> consumer;

    @Mock
    private OrderMessageHandler orderMessageHandler;

    @Mock
    private ErrorConsumerController errorConsumerController;

    @Spy
    private PartitionOffset partitionOffset = new PartitionOffset();

    @Mock
    private ConsumerStoppedEvent consumerStoppedEvent;

    @Mock
    private KafkaMessageListenerContainer<String, OrderReceived> kafkaMessageListenerContainer;

    @Mock
    private Logger logger;

    @InjectMocks
    private OrderMessageErrorConsumer orderMessageErrorConsumer;

    @BeforeEach
    void beforeEach() {
        orderMessageErrorConsumer.setErrorGroup("item-handler-order-received-error");
        orderMessageErrorConsumer.setErrorTopic("order-received-error");
    }

    @Test
    void shouldNotHandleMessageAndShouldStopConsumerWhenOffsetIsEqualToRecoveryOffset() {
        //given
        when(partitionOffset.getOffset()).thenReturn(1L);

        //when
        orderMessageErrorConsumer.processOrderReceived(message, 1L, consumer);

        //then
        verify(orderMessageHandler, times(0)).handleMessage(message);
        verify(errorConsumerController).pauseConsumerThread();
    }

    @Test
    void shouldNotHandleMessageAndShouldStopConsumerWhenOffsetIsGreaterThanRecoveryOffset() {
        //given
        when(partitionOffset.getOffset()).thenReturn(1L);

        //when
        orderMessageErrorConsumer.processOrderReceived(message, 2L, consumer);

        //then
        verify(orderMessageHandler, times(0)).handleMessage(message);
        verify(errorConsumerController).pauseConsumerThread();
    }

    @Test
    void shouldHandleMessageAndStopConsumerWhenOffsetIsOneLessThanRecoveryOffset() {
        //given
        when(partitionOffset.getOffset()).thenReturn(1L);

        //when
        orderMessageErrorConsumer.processOrderReceived(message, 0L, consumer);

        //then
        verify(orderMessageHandler).handleMessage(message);
        verify(errorConsumerController).pauseConsumerThread();
    }

    @Test
    void shouldHandleMessageAndNotStopConsumerWhenOffsetIsTwoLessThanRecoveryOffset() {
        //given
        when(partitionOffset.getOffset()).thenReturn(2L);

        //when
        orderMessageErrorConsumer.processOrderReceived(message, 0L, consumer);

        //then
        verify(orderMessageHandler).handleMessage(message);
        verify(errorConsumerController, times(0)).pauseConsumerThread();
    }

    @Test
    void shouldStopConsumerThreadForErrorTopic() {
        //given
        when(consumerStoppedEvent.getSource(KafkaMessageListenerContainer.class)).thenReturn(kafkaMessageListenerContainer);
        when(kafkaMessageListenerContainer.getBeanName()).thenReturn("item-handler-order-received-error-0");

        //when
        orderMessageErrorConsumer.consumerStopped(consumerStoppedEvent);

        //then
        verify(partitionOffset).clear();
    }

    @Test
    void shouldNotStopConsumerThreadForRetryTopic() {
        //given
        when(consumerStoppedEvent.getSource(KafkaMessageListenerContainer.class)).thenReturn(kafkaMessageListenerContainer);
        when(kafkaMessageListenerContainer.getBeanName()).thenReturn("item-handler-order-received-retry-0");

        //when
        orderMessageErrorConsumer.consumerStopped(consumerStoppedEvent);

        //then
        verify(partitionOffset, times(0)).clear();
    }

    @Test
    void shouldNotSetErrorRecoveryOffsetWhenOffsetHasAlreadyBeenSet() {
        //given
        when(partitionOffset.getOffset()).thenReturn(null);

        //when
        orderMessageErrorConsumer.configureErrorRecoveryOffset(consumer);

        //then
        verify(partitionOffset, times(0)).setOffset(1L);
    }

    @Test
    void shouldCorrectlySetErrorRecoveryOffsetFromConsumerEndOffsets() {
        //given
        when(consumer.endOffsets(any())).thenReturn(new HashMap<TopicPartition, Long>() {
            {
                put(new TopicPartition("order-received-error", 0), 1L);
            }
        });

        //when
        orderMessageErrorConsumer.configureErrorRecoveryOffset(consumer);

        //then
        verify(partitionOffset).setOffset(1L);
        verify(logger).info("Setting Error Consumer Recovery Offset to '1'");
    }
}