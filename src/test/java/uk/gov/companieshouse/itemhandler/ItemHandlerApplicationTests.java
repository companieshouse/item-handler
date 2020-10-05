package uk.gov.companieshouse.itemhandler;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Rule;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EmbeddedKafka
class ItemHandlerApplicationTests {

    @Rule
    public EnvironmentVariables environmentVariables = new EnvironmentVariables();

    private final String CHS_API_KEY = "CHS_API_KEY";
    private final String IS_ERROR_QUEUE_CONSUMER = "IS_ERROR_QUEUE_CONSUMER";
    private final String CERTIFICATE_ORDER_CONFIRMATION_RECIPIENT =
            "CERTIFICATE_ORDER_CONFIRMATION_RECIPIENT";
    private final String CERTIFIED_COPY_ORDER_CONFIRMATION_RECIPIENT =
            "CERTIFIED_COPY_ORDER_CONFIRMATION_RECIPIENT";
    private final String MISSING_IMAGE_DELIVERY_ORDER_CONFIRMATION_RECIPIENT =
            "MISSING_IMAGE_DELIVERY_ORDER_CONFIRMATION_RECIPIENT";

    @Test
    void checkEnvironmentVariablesAllPresentReturnsTrue() {
        environmentVariables.set(CHS_API_KEY, CHS_API_KEY);
        environmentVariables.set(IS_ERROR_QUEUE_CONSUMER, IS_ERROR_QUEUE_CONSUMER);
        environmentVariables.set(CERTIFICATE_ORDER_CONFIRMATION_RECIPIENT,
                CERTIFICATE_ORDER_CONFIRMATION_RECIPIENT);
        environmentVariables.set(CERTIFIED_COPY_ORDER_CONFIRMATION_RECIPIENT,
                CERTIFIED_COPY_ORDER_CONFIRMATION_RECIPIENT);
        environmentVariables.set(MISSING_IMAGE_DELIVERY_ORDER_CONFIRMATION_RECIPIENT,
                MISSING_IMAGE_DELIVERY_ORDER_CONFIRMATION_RECIPIENT);

        boolean present = ItemHandlerApplication.checkEnvironmentVariables();
        assertTrue(present);
        environmentVariables.clear(CHS_API_KEY, IS_ERROR_QUEUE_CONSUMER,
                CERTIFICATE_ORDER_CONFIRMATION_RECIPIENT,
                CERTIFIED_COPY_ORDER_CONFIRMATION_RECIPIENT,
                MISSING_IMAGE_DELIVERY_ORDER_CONFIRMATION_RECIPIENT);
    }

    @Test
    void checkEnvironmentVariablesChsApiKeyMissingReturnsFalse() {
        environmentVariables.set(IS_ERROR_QUEUE_CONSUMER, IS_ERROR_QUEUE_CONSUMER);
        environmentVariables.set(CERTIFICATE_ORDER_CONFIRMATION_RECIPIENT,
                CERTIFICATE_ORDER_CONFIRMATION_RECIPIENT);
        environmentVariables.set(CERTIFIED_COPY_ORDER_CONFIRMATION_RECIPIENT,
                CERTIFIED_COPY_ORDER_CONFIRMATION_RECIPIENT);
        environmentVariables.set(MISSING_IMAGE_DELIVERY_ORDER_CONFIRMATION_RECIPIENT,
                MISSING_IMAGE_DELIVERY_ORDER_CONFIRMATION_RECIPIENT);

        boolean present = ItemHandlerApplication.checkEnvironmentVariables();
        assertFalse(present);
        environmentVariables.clear(IS_ERROR_QUEUE_CONSUMER,
                CERTIFICATE_ORDER_CONFIRMATION_RECIPIENT,
                CERTIFIED_COPY_ORDER_CONFIRMATION_RECIPIENT,
                MISSING_IMAGE_DELIVERY_ORDER_CONFIRMATION_RECIPIENT);
    }

    @Test
    void checkEnvironmentVariablesIsErrorQueueConsumerMissingReturnsFalse() {
        environmentVariables.set(CHS_API_KEY, CHS_API_KEY);
        environmentVariables.set(CERTIFICATE_ORDER_CONFIRMATION_RECIPIENT,
                CERTIFICATE_ORDER_CONFIRMATION_RECIPIENT);
        environmentVariables.set(CERTIFIED_COPY_ORDER_CONFIRMATION_RECIPIENT,
                CERTIFIED_COPY_ORDER_CONFIRMATION_RECIPIENT);
        environmentVariables.set(MISSING_IMAGE_DELIVERY_ORDER_CONFIRMATION_RECIPIENT,
                MISSING_IMAGE_DELIVERY_ORDER_CONFIRMATION_RECIPIENT);

        boolean present = ItemHandlerApplication.checkEnvironmentVariables();
        assertFalse(present);
        environmentVariables.clear(CHS_API_KEY, CERTIFICATE_ORDER_CONFIRMATION_RECIPIENT,
                CERTIFIED_COPY_ORDER_CONFIRMATION_RECIPIENT,
                MISSING_IMAGE_DELIVERY_ORDER_CONFIRMATION_RECIPIENT);
    }

    @Test
    void checkEnvironmentVariablesCertificateOrderConfirmationRecipientMissingReturnFalse() {
        environmentVariables.set(CHS_API_KEY, CHS_API_KEY);
        environmentVariables.set(IS_ERROR_QUEUE_CONSUMER, IS_ERROR_QUEUE_CONSUMER);
        environmentVariables.set(CERTIFIED_COPY_ORDER_CONFIRMATION_RECIPIENT,
                CERTIFIED_COPY_ORDER_CONFIRMATION_RECIPIENT);
        environmentVariables.set(MISSING_IMAGE_DELIVERY_ORDER_CONFIRMATION_RECIPIENT,
                MISSING_IMAGE_DELIVERY_ORDER_CONFIRMATION_RECIPIENT);
        boolean present = ItemHandlerApplication.checkEnvironmentVariables();
        assertFalse(present);
        environmentVariables.clear(CHS_API_KEY, IS_ERROR_QUEUE_CONSUMER,
                CERTIFIED_COPY_ORDER_CONFIRMATION_RECIPIENT,
                MISSING_IMAGE_DELIVERY_ORDER_CONFIRMATION_RECIPIENT);
    }

    @Test
    void checkEnvironmentVariablesCertifiedCopyOrderConfirmationRecipientMissingReturnsFalse() {
        environmentVariables.set(CHS_API_KEY, CHS_API_KEY);
        environmentVariables.set(IS_ERROR_QUEUE_CONSUMER, IS_ERROR_QUEUE_CONSUMER);
        environmentVariables.set(CERTIFICATE_ORDER_CONFIRMATION_RECIPIENT,
                CERTIFICATE_ORDER_CONFIRMATION_RECIPIENT);
        environmentVariables.set(MISSING_IMAGE_DELIVERY_ORDER_CONFIRMATION_RECIPIENT,
                MISSING_IMAGE_DELIVERY_ORDER_CONFIRMATION_RECIPIENT);
        boolean present = ItemHandlerApplication.checkEnvironmentVariables();
        assertFalse(present);
        environmentVariables.clear(CHS_API_KEY, IS_ERROR_QUEUE_CONSUMER,
                CERTIFICATE_ORDER_CONFIRMATION_RECIPIENT,
                MISSING_IMAGE_DELIVERY_ORDER_CONFIRMATION_RECIPIENT);
    }

    @Test
    void checkEnvironmentVariablesMissingImageDeliveryOrderConfirmationRecipientMissingReturnsFalse() {
        environmentVariables.set(CHS_API_KEY, CHS_API_KEY);
        environmentVariables.set(IS_ERROR_QUEUE_CONSUMER, IS_ERROR_QUEUE_CONSUMER);
        environmentVariables.set(CERTIFICATE_ORDER_CONFIRMATION_RECIPIENT,
                CERTIFICATE_ORDER_CONFIRMATION_RECIPIENT);
        environmentVariables.set(CERTIFIED_COPY_ORDER_CONFIRMATION_RECIPIENT,
                CERTIFIED_COPY_ORDER_CONFIRMATION_RECIPIENT);
        boolean present = ItemHandlerApplication.checkEnvironmentVariables();
        assertFalse(present);
        environmentVariables.clear(CHS_API_KEY, IS_ERROR_QUEUE_CONSUMER,
                CERTIFICATE_ORDER_CONFIRMATION_RECIPIENT,
                CERTIFIED_COPY_ORDER_CONFIRMATION_RECIPIENT);
    }
}
