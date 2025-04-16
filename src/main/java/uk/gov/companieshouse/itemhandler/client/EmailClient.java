package uk.gov.companieshouse.itemhandler.client;

import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.chskafka.SendEmail;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.chskafka.PrivateSendEmailHandler;
import uk.gov.companieshouse.api.handler.chskafka.request.PrivateSendEmailPost;
import uk.gov.companieshouse.api.model.ApiResponse;
import uk.gov.companieshouse.email.EmailSend;
import uk.gov.companieshouse.itemhandler.exception.EmailClientException;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

@Component
public class EmailClient {

    private static final Logger LOGGER = LoggerFactory.getLogger("item-handler");

    private final ApiClient apiClient;

    public EmailClient(final ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public ApiResponse<Void> sendEmail(final EmailSend document) throws EmailClientException {
        try {
            SendEmail sendEmail = new SendEmail();
            sendEmail.setAppId(document.getAppId());
            sendEmail.setMessageId(document.getMessageId());
            sendEmail.setMessageType(document.getMessageType());
            sendEmail.setJsonData(document.getData());
            sendEmail.setEmailAddress(document.getEmailAddress());

            PrivateSendEmailHandler emailHandler = apiClient.getInternalApiClient().sendEmailHandler();
            PrivateSendEmailPost emailPost = emailHandler.postSendEmail("/send-email", sendEmail);

            ApiResponse<Void> response = emailPost.execute();

            LOGGER.info(String.format("Posted '%s' email to CHS Kafka API: (Response %d)",
                    sendEmail.getMessageType(), response.getStatusCode()));

            return response;

        } catch (ApiErrorResponseException ex) {
            LOGGER.error("Error sending email", ex);
            throw new EmailClientException("Error sending payload to CHS Kafka API: ", ex);
        }
    }
}
