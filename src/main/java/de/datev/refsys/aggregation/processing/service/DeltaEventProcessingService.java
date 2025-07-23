package de.datev.refsys.aggregation.processing.service;

import de.datev.refsys.aggregation.processing.api.model.DeltaInfo;
import de.datev.refsys.aggregation.processing.api.model.DeltaRequest;
import de.datev.refsys.generated.acds.api.model.ChangedEvent;
import reactor.core.publisher.Mono;

public interface DeltaEventProcessingService {

    Mono<DeltaInfo> processDeltaEvent(Integer consultant, Integer client, Integer fiscalYear, Long baseVersion, Long deltaVersion,
                                      Mono<DeltaRequest> deltaRequest);

}
