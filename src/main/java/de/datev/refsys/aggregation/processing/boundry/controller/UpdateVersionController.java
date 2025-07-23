package de.datev.refsys.aggregation.processing.boundry.controller;

import de.datev.refsys.aggregation.document.model.StateDoc;
import de.datev.refsys.aggregation.document.model.enums.StateDocState;
import de.datev.refsys.aggregation.processing.api.UpdateVersionApi;
import de.datev.refsys.aggregation.processing.api.model.DeltaInfo;
import de.datev.refsys.aggregation.processing.constant.ProcessingServiceConstants;
import de.datev.refsys.aggregation.processing.repository.StateDocRepository;
import de.datev.refsys.aggregation.processing.util.LoggingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class UpdateVersionController implements UpdateVersionApi {

    private final StateDocRepository stateDocRepository;

    @Override
    public Mono<DeltaInfo> updateVersion(Integer consultant, Integer client, Integer fiscalYear, Mono<DeltaInfo> requestBody, String xCorrelationId,
                                         String requestId, ServerWebExchange exchange) {
        log.info(LoggingUtil.MARKER, LoggingUtil.UPDATE_VERSION_START_LOG);
        return requestBody.flatMap(request -> updateStateDocVersionInfo(consultant, client, fiscalYear, exchange, request)
                .elapsed().map(LoggingUtil.logInfoWithDuration(LoggingUtil.UPDATE_VERSION_RESPONSE_LOG)));
    }

    @NotNull
    private Mono<DeltaInfo> updateStateDocVersionInfo(Integer consultant, Integer client, Integer fiscalYear, ServerWebExchange exchange, DeltaInfo request) {
        return stateDocRepository.findOneByBusinessKey(consultant, client, fiscalYear)
                                 .switchIfEmpty(Mono.defer(() -> {
                                     exchange.getResponse().setStatusCode(HttpStatus.NO_CONTENT);
                                     return Mono.just(true)
                                                .elapsed().map(LoggingUtil.logInfoWithDuration(LoggingUtil.UPDATE_VERSION_RESPONSE_LOG))
                                                .then(Mono.empty());
                                 }))
                                 .mapNotNull(stateDoc -> checkStateDoc(stateDoc, request))
                                 .flatMap(stateDoc -> stateDocRepository.updateVersionInfo(stateDoc, request.getBaseVersion(),
                                                                                           request.getDeltaVersion()))
                                 .map(updatedStatedoc -> new DeltaInfo().baseVersion(updatedStatedoc.getBaseVersion())
                                                                        .deltaVersion(updatedStatedoc.getDeltaVersion()));
    }

    /**
     * Checks the Statedoc if processing is possible.
     * @param stateDoc
     * @param deltaInfo
     * @throws ResponseStatusException if stateDoc is not in a processable ste
     * @return StateDoc if processing is ok; null if ignoring the request
     */
    private StateDoc checkStateDoc(StateDoc stateDoc, DeltaInfo deltaInfo) {
        if (stateDoc.getState() == StateDocState.INIT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                                              buildVersionInfo(stateDoc,
                                                               deltaInfo,
                                                               ProcessingServiceConstants.LOG_LEVEL_WARNING + " StateDocState in Init-State"));
        }

        if (stateDoc.getState() != StateDocState.DONE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                              buildVersionInfo(stateDoc,
                                                               deltaInfo,
                                                               ProcessingServiceConstants.LOG_LEVEL_WARNING + " StateDocState not in Done-State"));
        }

        if (stateDoc.getBaseVersion().equals(deltaInfo.getBaseVersion())) {
            if (stateDoc.getDeltaVersion() + 1 == deltaInfo.getDeltaVersion()) {
                return stateDoc;
            } else if (stateDoc.getDeltaVersion() >= deltaInfo.getDeltaVersion()) {
                log.info("Ignoring version - already higher delta version in DB");
                return null;
            }
        }

        if (stateDoc.getBaseVersion() > deltaInfo.getBaseVersion()) {
            log.info("Ignoring version - already higher base version in DB");
            return null;
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, buildVersionInfo(stateDoc, deltaInfo, "Global catch"));
    }

    private static String buildVersionInfo(StateDoc stateDoc, DeltaInfo deltaInfo, String additionalInfo) {
        return String.format("%s - StateDoc [baseVersion=%s, deltaVersion=%s] vs DeltaInfo [baseVersion=%s, deltaVersion=%s]",
                             additionalInfo,
                             stateDoc.getBaseVersion(), stateDoc.getDeltaVersion(),
                             deltaInfo.getBaseVersion(), deltaInfo.getDeltaVersion());
    }

}
