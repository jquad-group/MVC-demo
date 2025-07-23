package de.datev.refsys.aggregation.processing.service;

import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import de.datev.refsys.aggregation.processing.model.ImportData;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.context.ContextView;

/**
 * Service for starting and finishing data import and handling exceptions
 */
public interface ImportExecutionService {

    /**
     * Checks StateDoc for running imports, calls MasterdataContext API and sets the StateDocState to INIT. Creates a new StateDoc or updates an existing one
     *
     * @param consultant      consultant number
     * @param client          client number
     * @param fiscalYearStart fiscal year start
     * @param baseVersion     bas version
     * @param deltaVersion    delta version
     * @param contextView     reactive context
     * @return an ImportData object containing MasterdataContext and StateDoc
     */
    Mono<ImportData> initializeFullImport(Integer consultant, Integer client, Integer fiscalYearStart, Long baseVersion, Long deltaVersion,
                                          ContextView contextView);

    /**
     * Checks StateDoc for running imports and sets the StateDocState to INIT. Updates an existing StateDoc or returns httpStatus 204 if a StateDoc doesn't exist
     *
     * @param consultant      consultant number
     * @param client          client number
     * @param fiscalYearStart fiscal year start
     * @param baseVersion     bas version
     * @param deltaVersion    delta version
     * @param contextView     reactive context
     * @return an ImportData object containing MasterdataContext and StateDoc
     */
    Mono<ImportData> initializePartialImport(Integer consultant, Integer client, Integer fiscalYearStart, Long baseVersion, Long deltaVersion,
                                             ContextView contextView);

    /**
     * Executes the import action, updates the StateDocState to DONE and asynchronously sends a kafka event that the import data has changed
     *
     * @param importData     object containing MasterdataContext and StateDoc
     * @param importAction   the action to be executed
     * @param successMessage log message for success
     * @return UpdateResult of the StateDoc update
     */
    Mono<UpdateResult> executeImport(ImportData importData, Mono<Boolean> importAction, String successMessage);

    /**
     * @param consultant      consultant number
     * @param client          client number
     * @param fiscalYearStart fiscal year start
     * @param contextView     reactive context
     * @param errorLog        log message for errors
     * @param e               exception that occurred
     * @return UpdateResult of the StateDoc update
     */
    <T> Mono<T> handleExceptionAndUpdateStateDoc(Integer consultant, Integer client, Integer fiscalYearStart, ContextView contextView,
                                                 String errorLog, Throwable e);


    Flux<DeleteResult> deleteImportData(Integer consultant, Integer client, Integer fiscalYear);

}
