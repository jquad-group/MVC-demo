package de.datev.refsys.aggregation.processing.boundry.event;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.datev.refsys.aggregation.processing.util.MemoryAppender;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.SenderResult;
import reactor.test.StepVerifier;

import java.util.List;

import static de.datev.refsys.aggregation.processing.constant.ProcessingErrorMessageConstants.KAFKA_CHANGE_EVENT_PRODUCER_ERROR;
import static de.datev.refsys.aggregation.processing.constant.ProcessingErrorMessageConstants.KAFKA_SERIALIZATION_ERROR;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_BASE_VERSION;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_CLIENT;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_CONSULTANT;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_DELTA_VERSION;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_FISCAL_YEAR_2021_START;
import static de.datev.refsys.aggregation.processing.util.TestUtil.setupMemoryAppender;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Slf4j
class ChangeEventProducerTest {

    @Mock
    private ReactiveKafkaProducerTemplate<String, String> producerTemplate;

    @Mock
    private ObjectMapper objMapper;

    private MemoryAppender memoryAppender;

    @BeforeEach
    public void reset(){
        memoryAppender = setupMemoryAppender(memoryAppender, ChangeEventProducerImpl.class, Level.DEBUG);
        producerTemplate = mock(ReactiveKafkaProducerTemplate.class);
        objMapper = mock(ObjectMapper.class);
    }

    @Test
    void kafka_event_producer_should_throw_json_parser_exception() throws JsonProcessingException {
        when(objMapper.writeValueAsString(any(Object.class))).thenThrow(new JsonProcessingException(""){});

        ChangeEventProducerImpl producer = new ChangeEventProducerImpl(producerTemplate, objMapper, "topic");

        producer.sendMessage(TEST_CONSULTANT, TEST_CLIENT, TEST_FISCAL_YEAR_2021_START, TEST_BASE_VERSION, TEST_DELTA_VERSION).block();

        List<ILoggingEvent> changeEventLog = memoryAppender.search(KAFKA_SERIALIZATION_ERROR, Level.WARN);
        assertThat(changeEventLog).hasSize(1);
    }

    @Test
    void kafka_event_producer_send_should_result_in_exception(){
        SenderResult<Void> senderResult = mock(SenderResult.class);

        String errorMessage = "producer error";

        when(senderResult.exception()).thenReturn(new RuntimeException(errorMessage));
        when(producerTemplate.send(any(String.class), anyString(), anyString())).thenReturn(Mono.just(senderResult));

        ChangeEventProducerImpl producer = new ChangeEventProducerImpl(producerTemplate, new ObjectMapper(), "topic");

        StepVerifier.create(producer.sendMessage(TEST_CONSULTANT, TEST_CLIENT, TEST_FISCAL_YEAR_2021_START, TEST_BASE_VERSION, TEST_DELTA_VERSION))
                    .expectComplete()
                    .verify();

        List<ILoggingEvent> errorList = memoryAppender.search(KAFKA_CHANGE_EVENT_PRODUCER_ERROR, Level.WARN);
        assertThat(errorList).hasSize(1);
    }

    @Test
    void kafka_event_producer_should_return_empty_send_result_OK(){

        TopicPartition partition = new TopicPartition("empty", 3);
        RecordMetadata metadata = new RecordMetadata(partition,1,1,1L,1,1);
        SenderResult<Void> senderResult = mock(SenderResult.class);

        when(senderResult.recordMetadata()).thenReturn(metadata);
        when(senderResult.exception()).thenReturn(null);
        when(senderResult.correlationMetadata()).thenReturn(null);

        when(producerTemplate.send(any(String.class), anyString(), anyString())).thenReturn(Mono.just(senderResult));

        ChangeEventProducerImpl producer = new ChangeEventProducerImpl(producerTemplate,  new ObjectMapper(), "topic");

        StepVerifier.create(producer.sendMessage(TEST_CONSULTANT, TEST_CLIENT, TEST_FISCAL_YEAR_2021_START, TEST_BASE_VERSION, TEST_DELTA_VERSION))
                    .expectComplete()
                    .verify();
    }
}
