package de.datev.refsys.aggregation.processing.boundry.event;

import reactor.core.publisher.Mono;

public interface ChangeEventProducer {
    String CHANGED_EVENT_SUCCESS_LOG = "Sent ChangedEvent with producer offset : {}";

    Mono<Void> sendMessage(Integer consultant, Integer client, Integer fiscalYear, Long baseVersion, Long deltaVersion);
}
