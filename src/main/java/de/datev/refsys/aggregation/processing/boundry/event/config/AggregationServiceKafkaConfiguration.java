package de.datev.refsys.aggregation.processing.boundry.event.config;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import reactor.kafka.sender.SenderOptions;

import java.util.Map;

/**
 * A class that extends {@link AggregationServiceKafkaConfiguration} to provide all necessary kafka configurations needed for personIdChangeDto message payload
 */
@EnableKafka
@Configuration
@ConditionalOnProperty(value = "kafka-config.enabled", havingValue = "true")
public class AggregationServiceKafkaConfiguration {

    @Value("${kafka-config.kafka-topic-aggregation-service-new-or-changed-data.security-protocol}")
    private String nowSecurityProtocol;

    @Value("${kafka-config.kafka-topic-aggregation-service-new-or-changed-data.sasl-mechanism:}")
    private String nowSaslMechanism;

    @Value("${kafka-config.kafka-topic-aggregation-service-new-or-changed-data.sasl-jaas-config:}")
    private String nowSaslJaasConfig;

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean(name="cacheInvalidationProducer")
    public ReactiveKafkaProducerTemplate<String, String> reactiveKafkaProducerForCacheInvalidationTemplate(
            KafkaProperties properties) {
        Map<String, Object> props = properties.buildProducerProperties(null);
        props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, this.nowSecurityProtocol);
        props.put(SaslConfigs.SASL_MECHANISM, this.nowSaslMechanism);
        props.put(SaslConfigs.SASL_JAAS_CONFIG, this.nowSaslJaasConfig);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return new ReactiveKafkaProducerTemplate<>(SenderOptions.create(props));
    }
}
