package de.datev.refsys.aggregation.processing.service;

import de.datev.refsys.aggregation.processing.model.ImportData;
import reactor.core.publisher.Mono;

public interface ImportService {
    /**
     * Imports all data
     *
     * @param importData contains MasterdataContext and StateDoc
     * @return temporary return type until all importService has been completely refactored
     */
    Mono<Boolean> executeFullImport(ImportData importData);
}