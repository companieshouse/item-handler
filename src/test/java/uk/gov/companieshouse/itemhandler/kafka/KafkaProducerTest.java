package uk.gov.companieshouse.itemhandler.kafka;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.kafka.exceptions.ProducerConfigException;
import uk.gov.companieshouse.kafka.producer.CHKafkaProducer;
import uk.gov.companieshouse.kafka.producer.ProducerConfig;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Unit tests the {@link KafkaProducer} class.
 */
@ExtendWith(MockitoExtension.class)
class KafkaProducerTest {

    private static final String EXPECTED_CONFIG_ERROR_MESSAGE =
        "Broker addresses for kafka broker missing, check if environment variable KAFKA_BROKER_ADDR is configured. " +
                "[Hint: The property 'kafka.broker.addresses' uses the value of this environment variable in live " +
                "environments and that of 'spring.embedded.kafka.brokers' property in test.]";

    @InjectMocks
    private TestKafkaProducer kafkaProducerUnderTest;

    @Mock
    private CHKafkaProducer chKafkaProducer;

    private static class TestKafkaProducer extends KafkaProducer {}

    private class TestKafkaProducer2 extends KafkaProducer {
        private boolean modifyProducerConfigCalled;

        public boolean isModifyProducerConfigCalled() {
            return modifyProducerConfigCalled;
        }

        @Override
        protected void modifyProducerConfig(ProducerConfig producerConfig) {
            modifyProducerConfigCalled = true;
        }

        @Override
        protected ProducerConfig createProducerConfig() {
            final ProducerConfig config = new ProducerConfig();
            config.setBrokerAddresses(new String[]{"http://non-null-url.com"});
            return config;
        }

        @Override
        protected CHKafkaProducer createChKafkaProducer(final ProducerConfig config) {
            return chKafkaProducer;
        }
    }

    @Test
    @DisplayName("afterPropertiesSet() throws a ProducerConfigException if no spring.kafka.bootstrap-servers value configured")
    void afterPropertiesSetThrowsExceptionIfNoBrokersConfigured() {

        // When
        ProducerConfigException exception = Assertions.assertThrows(ProducerConfigException.class, () ->
                kafkaProducerUnderTest.afterPropertiesSet());
        // Then
        final String actualMessage = exception.getMessage();
        Assert.assertThat(actualMessage, Matchers.is(EXPECTED_CONFIG_ERROR_MESSAGE));

    }

    @Test
    @DisplayName("afterPropertiesSet() calls modifyProducerConfig")
    void afterPropertiesSetCallsModifyProducerConfig() {

        final TestKafkaProducer2 kafkaProducerUnderTest = new TestKafkaProducer2();

        // When
        kafkaProducerUnderTest.afterPropertiesSet();

        // Then
        assertThat(kafkaProducerUnderTest.isModifyProducerConfigCalled(), is(true));

    }

}
