package de.datev.refsys.aggregation.processing.boundry.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.datev.refsys.aggregation.processing.boundry.event.model.ChangedEventDto;
import de.datev.refsys.aggregation.processing.constant.ProcessingErrorMessageConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@ConditionalOnProperty(value = "kafka-config.enabled", havingValue = "true")
public class ChangeEventProducerImpl implements ChangeEventProducer {
    private final String topicName;
    private final ReactiveKafkaProducerTemplate<String, String> producerTemplate;
    private final ObjectMapper jsonMapper;

    public ChangeEventProducerImpl(
            @Qualifier("cacheInvalidationProducer") final ReactiveKafkaProducerTemplate<String, String> reactiveKafkaProducerTemplate,
            final ObjectMapper jsonMapper,
            @Value("${kafka-config.kafka-topic-aggregation-service-new-or-changed-data.topic}") final String topicName) {
        this.producerTemplate = reactiveKafkaProducerTemplate;
        this.jsonMapper = jsonMapper;
        this.topicName = topicName;
    }

    @Override
    public Mono<Void> sendMessage(Integer consultant, Integer client, Integer fiscalYear, Long baseVersion, Long deltaVersion) {
        String key = consultant + "-" + client;
        ChangedEventDto dto = ChangedEventDto.builder()
                                             .consultant(consultant)
                                             .client(client)
                                             .fiscalYear(fiscalYear)
                                             .baseVersion(baseVersion)
                                             .deltaVersion(deltaVersion)
                                             .build();

        try {
            String jsonDto = jsonMapper.writeValueAsString(dto);
            return producerTemplate.send(topicName, key, jsonDto)
                                   .doOnSuccess(result -> log.info(CHANGED_EVENT_SUCCESS_LOG, result.recordMetadata().offset()))
                                   .onErrorResume(throwable ->
                                                  {
                                                      log.warn(ProcessingErrorMessageConstants.KAFKA_CHANGE_EVENT_PRODUCER_ERROR, throwable);
                                                      return Mono.empty();
                                                  })
                                   .then();
        } catch (JsonProcessingException e) {
            log.warn(ProcessingErrorMessageConstants.KAFKA_SERIALIZATION_ERROR, e);
            return Mono.empty();
        }
    }
}
