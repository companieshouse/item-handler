package uk.gov.companieshouse.itemhandler.kafka;

import static org.mockito.ArgumentMatchers.any;
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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

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
    private CompletableFuture<SendResult<String, ItemGroupOrdered>> future;

    @Mock
    private Function<?,?> function;

    @Captor
    private ArgumentCaptor<CompletableFuture<SendResult<String, ItemGroupOrdered>>> callbackCaptor;

    @Test
    @DisplayName("sendMessage() logs failure exception without further error")
    void testSendMessageLogsFailureExceptionOK() throws ExecutionException, InterruptedException    {
        testSendMessageLogsFailureOK(new Exception("Test simulated exception"));
    }

    @Test
    @DisplayName("sendMessage() logs failure throwable without further error")
    void testSendMessageLogsFailureThrowableOK() throws ExecutionException, InterruptedException {
        testSendMessageLogsFailureOK(new Throwable("Test simulated throwable"));
    }

    private void testSendMessageLogsFailureOK(final Throwable throwable) throws ExecutionException, InterruptedException {
        // Given
        final ItemGroupOrderedMessageProducer producer =
                new ItemGroupOrderedMessageProducer(kafkaTemplate, factory, "item-group-ordered", LOGGER);
        when(digitalItemGroup.getOrder()).thenReturn(order);
        when(order.getReference()).thenReturn("ORD-123123-123123");
        when(factory.createMessage(digitalItemGroup)).thenReturn(message);
        CompletableFuture<SendResult<String, ItemGroupOrdered>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(throwable);

        when(kafkaTemplate.send(any(), any())).thenReturn(failedFuture);

        // When
        producer.sendMessage(digitalItemGroup);

        // Then and When
        verify(kafkaTemplate).send("item-group-ordered", message);
        verify(factory).createMessage(digitalItemGroup);
    }

}