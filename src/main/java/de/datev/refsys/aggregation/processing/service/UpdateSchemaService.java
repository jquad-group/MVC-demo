package de.datev.refsys.aggregation.processing.service;

import reactor.core.publisher.Mono;

public interface UpdateSchemaService {
    Mono<Boolean> updateSchemaVersion(Integer consultant, Integer client, Integer fiscalYear, Long baseVersion, Long deltaVersion);
}
