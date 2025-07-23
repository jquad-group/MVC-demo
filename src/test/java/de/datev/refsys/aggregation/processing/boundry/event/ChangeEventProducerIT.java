package de.datev.refsys.aggregation.processing.boundry.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.datev.refsys.aggregation.processing.boundry.event.model.ChangedEventDto;
import de.datev.refsys.aggregation.processing.configuration.TestApplicationInitializer;
import de.datev.refsys.aggregation.processing.configuration.TestMongoClientConfiguration;
import de.datev.refsys.aggregation.processing.configuration.TestcontainersConfiguration;
import de.datev.refsys.aggregation.processing.util.ClearDatabaseAnCreateIndexesBeforeEachTest;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.util.Map;

import static de.datev.refsys.aggregation.processing.util.TestUtil.KAFKA_TEST_PROFILE;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_BASE_VERSION_UPDATE;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_CLIENT;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_CONSULTANT;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_DELTA_VERSION_UPDATE;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_FISCAL_YEAR_2021_START;
import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest
@ActiveProfiles(KAFKA_TEST_PROFILE)
@ImportAutoConfiguration(TestMongoClientConfiguration.class)
@Import(TestcontainersConfiguration.class)
@EmbeddedKafka(partitions = 1, topics = {"local.changedata.events"})
@ContextConfiguration(initializers = TestApplicationInitializer.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ClearDatabaseAnCreateIndexesBeforeEachTest

class ChangeEventProducerIT {

    @Value("${kafka-config.kafka-topic-aggregation-service-new-or-changed-data.topic}")
    private String topic;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private ChangeEventProducer producerService;

    private ChangedEventDto expectedResult;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeAll
    public void setup(){
        expectedResult =
                new ChangedEventDto(TEST_CONSULTANT, TEST_CLIENT, TEST_FISCAL_YEAR_2021_START, TEST_BASE_VERSION_UPDATE, TEST_DELTA_VERSION_UPDATE);
    }

    @Test
    void should_send_the_message_successfully_for_correct_data() throws JsonProcessingException {
        Map<String, Object> consumerProps = KafkaTestUtils
                .consumerProps(groupId, "false",  embeddedKafkaBroker);

        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        DefaultKafkaConsumerFactory<String, String> cf = new DefaultKafkaConsumerFactory<String, String>(
                consumerProps, new StringDeserializer(), new StringDeserializer());

        Consumer<String, String> consumerServiceTest = cf.createConsumer();

        embeddedKafkaBroker.consumeFromAnEmbeddedTopic(consumerServiceTest, topic);

        producerService.sendMessage(TEST_CONSULTANT, TEST_CLIENT, TEST_FISCAL_YEAR_2021_START, TEST_BASE_VERSION_UPDATE, TEST_DELTA_VERSION_UPDATE).block();

        ConsumerRecord<String,String> consumerRecord = KafkaTestUtils.getSingleRecord(consumerServiceTest, topic);

        assertThat(consumerRecord).isNotNull();

        var expectedString = objectMapper.writeValueAsString(expectedResult);
        assertThat(consumerRecord.value()).isEqualTo(expectedString);

        consumerServiceTest.close();
    }
}