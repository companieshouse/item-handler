package uk.gov.companieshouse.itemhandler.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import uk.gov.companieshouse.email.EmailSend;
import uk.gov.companieshouse.itemhandler.email.CertificateOrderConfirmation;
import uk.gov.companieshouse.kafka.message.Message;
import uk.gov.companieshouse.kafka.serialization.AvroSerializer;
import uk.gov.companieshouse.kafka.serialization.SerializerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;

@SpringBootTest
@DirtiesContext
@EmbeddedKafka
public class EmailSendMessageProducerIntegrationTest {

    @Autowired
    EmailSendMessageProducer emailSendMessageProducerUnderTest;

    @Autowired
    TestEmailSendMessageConsumer testEmailSendMessageConsumer;

    @Autowired
    SerializerFactory serializerFactory;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void testSendOrderReceivedMessageToKafkaTopic() throws Exception {

        // Given an EmailSend object is created
        final CertificateOrderConfirmation confirmation = new CertificateOrderConfirmation();
        confirmation.setTo("lmccarthy@companieshouse.gov.uk");

        confirmation.setTitle("Miss");
        confirmation.setForename("Jenny");
        confirmation.setSurname("Wilson");

        confirmation.setAddressLine1("Kemp House Capital Office");
        confirmation.setAddressLine2("LTD");
        confirmation.setHouseName("Kemp House");
        confirmation.setHouseNumberStreetName("152-160 City Road");
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
        // TODO GCI-931 Is this app id as registered?
        email.setAppId("item-handler.certificate-order-confirmation");
        email.setEmailAddress("test@test.com");
        email.setMessageId(UUID.randomUUID().toString());
        email.setMessageType("certificate_order_confirmation_email");
        email.setData(objectMapper.writeValueAsString(confirmation));
        email.setCreatedAt(LocalDateTime.now().toString());

        // When email-send message is sent to kafka topic
        final List<Message> messages = sendAndConsumeMessage(email);

        // Then we can successfully consume the message.
        assertThat(messages.isEmpty(), is(false));
        byte[] consumedMessageSerialized = messages.get(0).getValue();
        final String deserializedConsumedMessage = new String(consumedMessageSerialized);

        // and it matches the serialized email-send object sent
        final AvroSerializer<EmailSend> serializer = serializerFactory.getGenericRecordSerializer(EmailSend.class);
        final byte[] serializedEmail = serializer.toBinary(email);
        final String deserializedEmail = new String(serializedEmail);

        assertEquals(deserializedConsumedMessage, deserializedEmail);
    }

    private List<Message> sendAndConsumeMessage(final EmailSend email) throws Exception {
        List<Message> messages;
        testEmailSendMessageConsumer.connect();
        int count = 0;
        do {
            messages = testEmailSendMessageConsumer.pollConsumerGroup();
            emailSendMessageProducerUnderTest.sendMessage(email);
            count++;
        } while (messages.isEmpty() && count < 15);

        return messages;
    }
}
