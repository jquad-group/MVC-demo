package de.datev.refsys.aggregation.processing.service;

import com.mongodb.client.result.UpdateResult;
import de.datev.refsys.aggregation.processing.model.ImportData;
import reactor.core.publisher.Mono;

public interface CommonImportService {

    /**
     * Executes a full import
     *
     * @param consultant      consultant number
     * @param client          client number
     * @param fiscalYearStart fiscal year start
     * @param baseVersion     bas version
     * @param deltaVersion    delta version
     * @return UpdateResult of the StateDoc update
     */
    Mono<UpdateResult> doFullImport(Integer consultant, Integer client, Integer fiscalYearStart, Long baseVersion, Long deltaVersion);

    /**
     * Triggers a full import as fire and forget (doesn't wait for the import result)
     *
     * @param consultant      consultant number
     * @param client          client number
     * @param fiscalYearStart fiscal year start
     * @param baseVersion     bas version
     * @param deltaVersion    delta version
     */
    Mono<ImportData> doFireAndForgetFullImport(Integer consultant, Integer client, Integer fiscalYearStart, Long baseVersion, Long deltaVersion);
}
