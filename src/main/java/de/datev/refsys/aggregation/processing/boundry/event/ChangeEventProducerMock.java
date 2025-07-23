package de.datev.refsys.aggregation.processing.boundry.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@ConditionalOnProperty(value = "kafka-config.enabled", havingValue = "false")
/*
 * Diese Klasse ist notwendig um sicherzustellen, dass der Spring Context in den Tests korrekt geladen werden kann.
 *
 * DeleteInventoryController nutzt ChangeEventProducer (ChangeEventProducerImpl). Bean ChangeEventProducerImpl wird in
 * den Tests nicht geladen, da die Property kafka-config.enabled nicht gesetzt ist.
 *
 * Diese Klasse ist also nur ein Mock um den Spring Context in den Tests zu laden. Andere Lösungsmöglichkeiten wurden auch
 * validiert; sind aber teilweise aufwändiger.
 *
 */
public class ChangeEventProducerMock implements ChangeEventProducer {
    @Override
    public Mono<Void> sendMessage(Integer consultant, Integer client, Integer fiscalYear, Long baseVersion, Long deltaVersion) {
        return Mono.just(true)
                .doOnSuccess(success -> log.info(CHANGED_EVENT_SUCCESS_LOG, 0))
                .then();
    }
}
