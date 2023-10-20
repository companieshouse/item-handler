package uk.gov.companieshouse.itemhandler.kafka;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;
import uk.gov.companieshouse.itemgroupordered.ItemGroupOrdered;
import uk.gov.companieshouse.itemhandler.itemsummary.ItemGroup;
import uk.gov.companieshouse.itemhandler.model.OrderData;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

/**
 * Unit tests {@link ItemGroupOrderedMessageProducer}.
 */
@ExtendWith(MockitoExtension.class)
class ItemGroupOrderedMessageProducerTest {

    private static final Logger LOGGER = LoggerFactory.getLogger("ItemGroupOrderedMessageProducerTest");

    @Mock
    private KafkaTemplate<String, ItemGroupOrdered> kafkaTemplate;

    @Mock
    private ItemGroupOrderedFactory factory;

    @Mock
    private ItemGroup digitalItemGroup;

    @Mock
    private ItemGroupOrdered message;

    @Mock
    private OrderData order;

    @Mock
    private ListenableFuture<SendResult<String, ItemGroupOrdered>> future;

    @Captor
    private ArgumentCaptor<ListenableFutureCallback<SendResult<String, ItemGroupOrdered>>> callbackCaptor;

    @Test
    @DisplayName("sendMessage() logs failure exception without further error")
    void testSendMessageLogsFailureExceptionOK() {
        testSendMessageLogsFailureOK(new Exception("Test simulated exception"));
    }

    @Test
    @DisplayName("sendMessage() logs failure throwable without further error")
    void testSendMessageLogsFailureThrowableOK() {
        testSendMessageLogsFailureOK(new Throwable("Test simulated throwable"));
    }

    private void testSendMessageLogsFailureOK(final Throwable throwable) {
        // Given
        final ItemGroupOrderedMessageProducer producer =
                new ItemGroupOrderedMessageProducer(kafkaTemplate, factory, "item-group-ordered", LOGGER);
        when(digitalItemGroup.getOrder()).thenReturn(order);
        when(order.getReference()).thenReturn("ORD-123123-123123");
        when(factory.createMessage(digitalItemGroup)).thenReturn(message);
        when(kafkaTemplate.send("item-group-ordered", message)).thenReturn(future);

        // When
        producer.sendMessage(digitalItemGroup);

        // Then and When :)
        verify(future).addCallback(callbackCaptor.capture());
        callbackCaptor.getValue().onFailure(throwable);
    }

}