package uk.gov.companieshouse.itemhandler.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.chskafka.SendEmail;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.chskafka.PrivateSendEmailHandler;
import uk.gov.companieshouse.api.handler.chskafka.request.PrivateSendEmailPost;
import uk.gov.companieshouse.api.model.ApiResponse;
import uk.gov.companieshouse.email.EmailSend;
import uk.gov.companieshouse.itemhandler.exception.EmailClientException;
import uk.gov.companieshouse.itemhandler.itemsummary.DeliverableItemGroup;
import uk.gov.companieshouse.itemhandler.model.DeliveryTimescale;
import uk.gov.companieshouse.itemhandler.model.Item;
import uk.gov.companieshouse.itemhandler.model.OrderData;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EmailClientTest {

    @Mock
    private ApiClient apiClient;

    @InjectMocks
    private EmailClient emailClient;

    @BeforeEach
    void setUp() {

    }

    @AfterEach
    void tearDown() {}

    @Test
    void givenValidPayload_whenEmailRequested_thenReturnSuccess() throws JsonProcessingException, ApiErrorResponseException, EmailClientException {
        // Arrange:
        ApiResponse<Void> apiResponse = new ApiResponse<>(200, Map.of());

        PrivateSendEmailPost privateSendEmailPost = mock(PrivateSendEmailPost.class);
        when(privateSendEmailPost.execute()).thenReturn(apiResponse);

        PrivateSendEmailHandler privateSendEmailHandler = mock(PrivateSendEmailHandler.class);
        when(privateSendEmailHandler.postSendEmail(eq("/send-email"), any(SendEmail.class))).thenReturn(privateSendEmailPost);

        InternalApiClient internalApiClient = mock(InternalApiClient.class);
        when(apiClient.getInternalApiClient()).thenReturn(internalApiClient);

        when(internalApiClient.sendEmailHandler()).thenReturn(privateSendEmailHandler);

        EmailSend emailData = createDeliverableItemGroupWithItems();

        // Act:
        ApiResponse<Void> response = emailClient.sendEmail(emailData);

        // Assert:
        verify(internalApiClient, times(1)).sendEmailHandler();
        verify(privateSendEmailHandler, times(1)).postSendEmail(eq("/send-email"), any(SendEmail.class));
        verify(privateSendEmailPost, times(1)).execute();

        assertThat(response.getStatusCode(), is(200));
    }

    @Test
    void givenInvalidPayload_whenEmailRequested_thenReturnBadRequest() throws JsonProcessingException, ApiErrorResponseException, EmailClientException {
        // Arrange:
        ApiResponse<Void> apiResponse = new ApiResponse<>(400, Map.of());

        PrivateSendEmailPost privateSendEmailPost = mock(PrivateSendEmailPost.class);
        when(privateSendEmailPost.execute()).thenReturn(apiResponse);

        PrivateSendEmailHandler privateSendEmailHandler = mock(PrivateSendEmailHandler.class);
        when(privateSendEmailHandler.postSendEmail(eq("/send-email"), any(SendEmail.class))).thenReturn(privateSendEmailPost);

        InternalApiClient internalApiClient = mock(InternalApiClient.class);
        when(apiClient.getInternalApiClient()).thenReturn(internalApiClient);

        when(internalApiClient.sendEmailHandler()).thenReturn(privateSendEmailHandler);

        EmailSend emailData = createDeliverableItemGroupWithItems();

        // Act:
        ApiResponse<Void> response = emailClient.sendEmail(emailData);

        // Assert:
        verify(internalApiClient, times(1)).sendEmailHandler();
        verify(privateSendEmailHandler, times(1)).postSendEmail(eq("/send-email"), any(SendEmail.class));
        verify(privateSendEmailPost, times(1)).execute();

        assertThat(response.getStatusCode(), is(400));
    }

    @Test
    void givenValidPayload_whenEmailClientThrowsApiException_thenReturnError() throws JsonProcessingException, ApiErrorResponseException, EmailClientException {
        // Arrange:
        PrivateSendEmailPost privateSendEmailPost = mock(PrivateSendEmailPost.class);
        when(privateSendEmailPost.execute()).thenThrow(ApiErrorResponseException.class);

        PrivateSendEmailHandler privateSendEmailHandler = mock(PrivateSendEmailHandler.class);
        when(privateSendEmailHandler.postSendEmail(eq("/send-email"), any(SendEmail.class))).thenReturn(privateSendEmailPost);

        InternalApiClient internalApiClient = mock(InternalApiClient.class);
        when(apiClient.getInternalApiClient()).thenReturn(internalApiClient);

        when(internalApiClient.sendEmailHandler()).thenReturn(privateSendEmailHandler);

        EmailSend emailData = createDeliverableItemGroupWithItems();

        // Act:
        EmailClientException expectedException = assertThrows(EmailClientException.class, () ->
                emailClient.sendEmail(emailData)
        );

        // Assert:
        verify(internalApiClient, times(1)).sendEmailHandler();
        verify(privateSendEmailHandler, times(1)).postSendEmail(eq("/send-email"), any(SendEmail.class));
        verify(privateSendEmailPost, times(1)).execute();

        assertThat(expectedException.getMessage(), is("Error sending payload to CHS Kafka API: "));
    }

    @Test
    void givenValidPayload_whenPaymentReportEmail_thenReturnSuccess() throws JsonProcessingException, ApiErrorResponseException, EmailClientException {
        // Arrange:
        ApiResponse<Void> apiResponse = new ApiResponse<>(200, Map.of());

        PrivateSendEmailPost privateSendEmailPost = mock(PrivateSendEmailPost.class);
        when(privateSendEmailPost.execute()).thenReturn(apiResponse);

        PrivateSendEmailHandler privateSendEmailHandler = mock(PrivateSendEmailHandler.class);
        when(privateSendEmailHandler.postSendEmail(eq("/send-email"), any(SendEmail.class))).thenReturn(privateSendEmailPost);

        InternalApiClient internalApiClient = mock(InternalApiClient.class);
        when(apiClient.getInternalApiClient()).thenReturn(internalApiClient);

        when(internalApiClient.sendEmailHandler()).thenReturn(privateSendEmailHandler);

        EmailSend emailData = createDeliverableItemGroupWithItems();

        // Act:
        ApiResponse<Void> response = emailClient.sendEmail(emailData);

        // Assert:
        verify(internalApiClient, times(1)).sendEmailHandler();
        verify(privateSendEmailHandler, times(1)).postSendEmail(eq("/send-email"), any(SendEmail.class));
        verify(privateSendEmailPost, times(1)).execute();

        assertThat(response.getStatusCode(), is(200));
    }

    @Test
    void givenInvalidPayload_whenPaymentEmailRequested_thenReturnBadRequest() throws JsonProcessingException, ApiErrorResponseException, EmailClientException {
        // Arrange:
        ApiResponse<Void> apiResponse = new ApiResponse<>(400, Map.of());

        PrivateSendEmailPost privateSendEmailPost = mock(PrivateSendEmailPost.class);
        when(privateSendEmailPost.execute()).thenReturn(apiResponse);

        PrivateSendEmailHandler privateSendEmailHandler = mock(PrivateSendEmailHandler.class);
        when(privateSendEmailHandler.postSendEmail(eq("/send-email"), any(SendEmail.class))).thenReturn(privateSendEmailPost);

        InternalApiClient internalApiClient = mock(InternalApiClient.class);
        when(apiClient.getInternalApiClient()).thenReturn(internalApiClient);

        when(internalApiClient.sendEmailHandler()).thenReturn(privateSendEmailHandler);

        EmailSend emailData = createDeliverableItemGroupWithItems();

        // Act:
        ApiResponse<Void> response = emailClient.sendEmail(emailData);

        // Assert:
        verify(internalApiClient, times(1)).sendEmailHandler();
        verify(privateSendEmailHandler, times(1)).postSendEmail(eq("/send-email"), any(SendEmail.class));
        verify(privateSendEmailPost, times(1)).execute();

        assertThat(response.getStatusCode(), is(400));
    }

    private EmailSend createDeliverableItemGroupWithItems() throws JsonProcessingException {
        OrderData orderData = new OrderData();
        String kind = "item#certificate";
        DeliveryTimescale timescale = DeliveryTimescale.STANDARD;
        List<Item> items = new ArrayList<>();

        return mapEmailData(new DeliverableItemGroup(orderData, kind, timescale, items));
    }

    private EmailSend mapEmailData(final DeliverableItemGroup itemGroup) throws JsonProcessingException {
        EmailSend emailSend = new EmailSend();
        emailSend.setAppId("test-app-id");
        emailSend.setMessageId(UUID.randomUUID().toString());
        emailSend.setMessageType("test-message-type");
        emailSend.setData(new ObjectMapper().writeValueAsString(itemGroup));
        emailSend.setEmailAddress("unit@test.com");
        emailSend.setCreatedAt(LocalDateTime.now().toString());
        return emailSend;
    }

}