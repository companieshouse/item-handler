package uk.gov.companieshouse.itemhandler.kafka;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertNull;

import email.email_send;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.kafka.exceptions.SerializationException;
import uk.gov.companieshouse.kafka.serialization.AvroSerializer;
import uk.gov.companieshouse.kafka.serialization.SerializerFactory;

@ExtendWith(MockitoExtension.class)
class MessageDeserialiserTest {
    @Test
    void deserializeReturnsNullIfMessageCannotBeDeserialised() {
        assertNull(new MessageDeserialiser<>(email_send.class).deserialize("email-send",
                "Test data".getBytes()));
    }

    @Test
    void correctlyDeserialisesWellFormedData() throws SerializationException {
        //given
        AvroSerializer<email_send> serializer = new SerializerFactory().getSpecificRecordSerializer(email_send.class);
        email_send email_send = new email_send();
        email_send.setAppId("app_id");
        email_send.setMessageId("message_id");
        email_send.setMessageType("message_type");
        email_send.setData("data");
        email_send.setEmailAddress("email");
        email_send.setCreatedAt("created");
        byte [] bytes = serializer.toBinary(email_send);

        //when
        email_send result = new MessageDeserialiser<>(email_send.class).deserialize("email_send", bytes);

        //then
        assertThat(result, equalTo(email_send));
    }
}