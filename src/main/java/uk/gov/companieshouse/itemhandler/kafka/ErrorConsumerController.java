package uk.gov.companieshouse.itemhandler.kafka;

import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.itemhandler.logging.LoggingUtils;
import uk.gov.companieshouse.logging.Logger;

@Component
public class ErrorConsumerController {
    private final Logger logger;
    private final String errorGroup;
    private final String errorTopic;
    private final PartitionOffset partitionOffset;
    private final KafkaListenerEndpointRegistry registry;

    public ErrorConsumerController(Logger logger,
                                   @Value("${kafka.topics.order-received-error-group}") String errorGroup,
                                   @Value("${kafka.topics.order-received-error}") String errorTopic,
                                   PartitionOffset partitionOffset,
                                   KafkaListenerEndpointRegistry registry) {
        this.logger = logger;
        this.errorGroup = errorGroup;
        this.errorTopic = errorTopic;
        this.partitionOffset = partitionOffset;
        this.registry = registry;
    }

    public void pauseConsumerThread() {
        Map<String, Object> logMap = LoggingUtils.createLogMap();
        logMap.put(errorGroup, partitionOffset.getOffset());
        logMap.put(LoggingUtils.TOPIC, errorTopic);
        logger.info("Pausing error consumer as error recovery offset reached.", logMap);
        Optional.ofNullable(registry.getListenerContainer(errorGroup)).ifPresent(
                MessageListenerContainer::pause);
    }

    void resumeConsumerThread() {
        Map<String, Object> logMap = LoggingUtils.createLogMap();
        logMap.put(errorGroup, partitionOffset.getOffset());
        logMap.put(LoggingUtils.TOPIC, errorTopic);
        logger.info("Resuming error consumer thread.", logMap);
        Optional.ofNullable(registry.getListenerContainer(errorGroup)).ifPresent(
                MessageListenerContainer::resume);
    }
}
