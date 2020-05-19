package uk.gov.companieshouse.itemhandler.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.companieshouse.email.EmailSend;
import uk.gov.companieshouse.itemhandler.email.CertificateOrderConfirmation;
import uk.gov.companieshouse.itemhandler.kafka.EmailSendMessageProducer;
import uk.gov.companieshouse.kafka.exceptions.SerializationException;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static uk.gov.companieshouse.itemhandler.ItemHandlerApplication.APPLICATION_NAMESPACE;

/**
 * TODO GCI-931 Remove this and its entry from routes.yaml
 */
@RestController
public class TestController {

    private static final Logger LOGGER = LoggerFactory.getLogger(APPLICATION_NAMESPACE);

    @Autowired
    EmailSendMessageProducer emailSendMessageProducer;

    @Autowired
    ObjectMapper objectMapper;

    @GetMapping("/test")
    public ResponseEntity<Void> test() throws InterruptedException, ExecutionException, SerializationException, JsonProcessingException {
        LOGGER.info("Test invoked.");

        final CertificateOrderConfirmation confirmation = new CertificateOrderConfirmation();
        confirmation.setTo("lmccarthy@companieshouse.gov.uk");

        confirmation.setTitle("Miss");
        confirmation.setForename("Jenny");
        confirmation.setSurname("Wilson");

        confirmation.setAddressLine1("Kemp House Capital Office");
        confirmation.setAddressLine2("LTD");
        confirmation.setHouseName("Kemp House");
        // TODO GCI-931 Do we need an extra line in the address? Where do we put 152-160 City Road?
        confirmation.setCity("London");
        confirmation.setPostCode("EC1V 2NX");
        // TODO GCI-931 What is the format of this order reference number?
        confirmation.setOrderReferenceNumber("123");
        confirmation.setEmailAddress("mail@globaloffshore.com");
        confirmation.setDeliveryMethod("Standard delivery");
        confirmation.setFeeAmount("15");
        // TODO GCI-931 check date time format
        confirmation.setTimeOfPayment(LocalDateTime.now().toString());
        confirmation.setPaymentReference("RS5VSNDRE");
        confirmation.setCompanyName("GLOBAL OFFSHORE HOST LIMITED");
        confirmation.setCompanyNumber("11260147");
        confirmation.setCertificateType("Incorporation with all company name changes");
        confirmation.setCertificateIncludes(new String[]{
                "Statement of good standing",
                "Registered office address",
                "Directors",
                "Secretaries",
                "Company objects"
        });

        final EmailSend email = new EmailSend();
        email.setAppId("item-handler.certificate-order-confirmation");
        email.setEmailAddress("test@test.com"); // TODO GCI-931 Make some of these configurable
        email.setMessageId(UUID.randomUUID().toString());
        email.setMessageType("certificate_order_confirmation_email");
        email.setData(objectMapper.writeValueAsString(confirmation));
        email.setCreatedAt(LocalDateTime.now().toString());

        emailSendMessageProducer.sendMessage(email);

        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
