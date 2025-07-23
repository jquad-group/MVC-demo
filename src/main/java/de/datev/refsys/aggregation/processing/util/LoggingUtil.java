package de.datev.refsys.aggregation.processing.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import reactor.util.context.Context;
import reactor.util.function.Tuple2;

import java.util.Map;
import java.util.function.Function;

@UtilityClass
@Slf4j
public final class LoggingUtil {
    // LoggingContext keys
    public static final Marker MARKER = MarkerFactory.getMarker("REFSYS_SPLUNK_MARKER");
    public static final String LOGGING_CONTEXT_KEY = "logging_context_key";
    public static final String CONSULTANT_KEY = "consultant";
    public static final String CLIENT_KEY = "client";
    public static final String FISCAL_YEAR_KEY = "fiscal_year";
    public static final String BASE_VERSION_KEY = "base_version";
    public static final String DELTA_VERSION_KEY = "delta_version";
    public static final String REQUEST_ID_KEY = "request_id";
    public static final String CORRELATION_ID_KEY = "correlation_id";
    public static final String DATEV_CLIENT_ID_KEY = "datev_client_id";
    // InitialLoad
    public static final String INITIAL_LOAD_START_LOG = "StartInitialLoad request received";
    public static final String INITIAL_LOAD_RESPONSE_LOG = "StartInitialLoad request finished in {}ms";
    public static final String INITIAL_LOAD_ERROR_LOG = "An error occurred during StartInitialLoad";
    public static final String FULL_IMPORT_STARTED_LOG = "Full Import started";
    public static final String FULL_IMPORT_SUCCESS_LOG = "Full Import finished in {}ms";
    public static final String PARTIAL_IMPORT_STARTED_LOG = "Partial Import started";
    public static final String PARTIAL_IMPORT_SUCCESS_LOG = "Partial Import finished in {}ms";
    // DeleteInventory
    public static final String DELETE_INVENTORY_START_LOG = "DeleteInventory request received";
    public static final String DELETE_INVENTORY_RESPONSE_LOG = "DeleteInventory request finished in {}ms";
    // UpdateSchema
    public static final String UPDATE_SCHEMA_START_LOG = "UpdateSchema request received";
    public static final String UPDATE_SCHEMA_RESPONSE_LOG = "UpdateSchema request finished in {}ms";
    public static final String UPDATE_SCHEMA_NOT_NEEDED_LOG = "UpdateSchema is not needed";
    public static final String UPDATE_SCHEMA_ERROR_LOG = "An error occurred during UpdateSchema";
    // ProcessDelta
    public static final String PROCESS_DELTA_START_LOG = "ProcessDelta request received";
    public static final String PROCESS_DELTA_RESPONSE_LOG = "ProcessDelta request finished in {}ms";
    // UpdateVersion
    public static final String UPDATE_VERSION_START_LOG = "UpdateVersion request received";
    public static final String UPDATE_VERSION_RESPONSE_LOG = "UpdateVersion request finished in {}ms";
    // CustomColumnStructureContentRepository
    public static final String CUSTOM_COLUMN_STRUCTURE_REPOSITORY_DELETE_MANY_LOG = "DeleteManyByBusinessKey in CustomColumnStructureRepository finished in {}ms";
    public static final String CUSTOM_COLUMN_STRUCTURE_REPOSITORY_BULK_INSERT_LOG = "BulkInsert in CustomColumnStructureRepository finished in {}ms";
    // CustomReportStructureContentRepository
    public static final String CUSTOM_REPORT_STRUCTURE_REPOSITORY_DELETE_MANY_LOG = "DeleteManyByBusinessKey in CustomReportStructureRepository finished in {}ms";
    public static final String CUSTOM_REPORT_STRUCTURE_REPOSITORY_BULK_INSERT_LOG = "BulkInsert in CustomReportStructureRepository finished in {}ms";
    // MasterDataAccountRepository
    public static final String MASTER_DATA_ACCOUNT_REPOSITORY_DELETE_MANY_LOG = "DeleteManyByBusinessKey in MasterDataAccountRepository finished in {}ms";
    public static final String MASTER_DATA_ACCOUNT_REPOSITORY_FIND_ALL_LOG = "FindAllByBusinessKeyAndAccountNumbers in MasterDataAccountRepository finished in {}ms";
    public static final String MASTER_DATA_ACCOUNT_REPOSITORY_BULK_INSERT_LOG = "BulkInsert in MasterDataAccountRepository finished in {}ms";
    public static final String MASTER_DATA_ACCOUNT_REPOSITORY_BULK_UPDATE_LOG = "BulkUpdate in MasterDataAccountRepository finished in {}ms";
    // MasterDataRepository
    public static final String MASTER_DATA_REPOSITORY_DELETE_ONE_LOG = "DeleteOne in MasterDataRepository finished in {}ms";
    public static final String MASTER_DATA_REPOSITORY_UPSERT_ONE_LOG = "UpsertOne in MasterDataRepository finished in {}ms";
    public static final String MASTER_DATA_REPOSITORY_WRITE_NEAR_TIME_DATA_FLAG_LOG = "WriteNearTimeDataFlag in MasterDataRepository finished in {}ms";
    public static final String MASTER_DATA_REPOSITORY_FIND_ONE_LOG = "FindOneByBusinessKey in MasterDataRepository finished in {}ms";
    public static final String MASTER_DATA_REPOSITORY_UPDATE_FROM_SCHEMA_VERSION_ONE_TO_TWO_LOG = "UpdateFromSchemaVersionOneToTwo in MasterDataRepository finished in {}ms";
    public static final String MASTER_DATA_REPOSITORY_ADD_INDIVIDUAL_PERSON_ACCOUNTS_LOG = "AddIndividualPersonAccounts in MasterDataRepository finished in {}ms";
    public static final String MASTER_DATA_REPOSITORY_UNSET_INDIVIDUAL_PERSON_ACCOUNTS_LOG = "UnsetIndividualPersonAccounts in MasterDataRepository finished in {}ms";
    // MovementDataDayRepository
    public static final String MOVEMENT_DATA_DAY_REPOSITORY_DELETE_MANY_LOG = "DeleteMany in MovementDataDayRepository finished in {}ms";
    public static final String MOVEMENT_DATA_DAY_REPOSITORY_BULK_INSERT_LOG = "BulkInsert in MovementDataDayRepository finished in {}ms";
    public static final String MOVEMENT_DATA_DAY_REPOSITORY_BULK_UPDATE_LOG = "BulkUpdate in MovementDataDayRepository finished in {}ms";
    public static final String MOVEMENT_DATA_DAY_REPOSITORY_BULK_UPSERT_LOG = "BulkUpsert in MovementDataDayRepository finished in {}ms";
    public static final String MOVEMENT_DATA_DAY_REPOSITORY_FIND_ALL_LOG = "FindAllMovementDataDayForAccount in MovementDataDayRepository finished in {}ms";
    // MovementDataInventoryRepository
    public static final String MOVEMENT_DATA_INVENTORY_REPOSITORY_DELETE_MANY_LOG = "DeleteMany in MovementDataInventoryRepository finished in {}ms";
    public static final String MOVEMENT_DATA_INVENTORY_REPOSITORY_BULK_UPSERT_LOG = "BulkUpsert in MovementDataInventoryRepository finished in {}ms";
    // MovementDataMonthRepository
    public static final String MOVEMENT_DATA_MONTH_REPOSITORY_DELETE_MANY_LOG = "DeleteMany in MovementDataMonthRepository finished in {}ms";
    public static final String MOVEMENT_DATA_MONTH_REPOSITORY_FIND_ALL_LOG = "FindAllById in MovementDataMonthRepository finished in {}ms";
    public static final String MOVEMENT_DATA_MONTH_REPOSITORY_BULK_INSERT_LOG = "BulkInsert in MovementDataMonthRepository finished in {}ms";
    public static final String MOVEMENT_DATA_MONTH_REPOSITORY_BULK_UPDATE_LOG = "BulkUpdate in MovementDataMonthRepository finished in {}ms";
    // MovementDataPersonGroupDayRepository
    public static final String MOVEMENT_DATA_PERSON_GROUP_DAY_REPOSITORY_DELETE_MANY_LOG = "DeleteMany in MovementDataPersonGroupDayRepository finished in {}ms";
    public static final String MOVEMENT_DATA_PERSON_GROUP_DAY_REPOSITORY_BULK_INSERT_LOG = "BulkInsert in MovementDataPersonGroupDayRepository finished in {}ms";
    public static final String MOVEMENT_DATA_PERSON_GROUP_DAY_REPOSITORY_BULK_UPDATE_LOG = "BulkUpdate in MovementDataPersonGroupDayRepository finished in {}ms";
    // MovementDataPersonGroupMonthRepository
    public static final String MOVEMENT_DATA_PERSON_GROUP_MONTH_REPOSITORY_DELETE_MANY_LOG = "DeleteMany in MovementDataPersonGroupMonthRepository finished in {}ms";
    public static final String MOVEMENT_DATA_PERSON_GROUP_MONTH_REPOSITORY_BULK_INSERT_LOG = "BulkInsert in MovementDataPersonGroupMonthRepository finished in {}ms";
    public static final String MOVEMENT_DATA_PERSON_GROUP_MONTH_REPOSITORY_BULK_UPDATE_LOG = "BulkUpdate in MovementDataPersonGroupMonthRepository finished in {}ms";
    // StateDocRepository
    public static final String STATE_DOC_REPOSITORY_DELETE_ONE_LOG = "DeleteOne in StateDocRepository finished in {}ms";
    public static final String STATE_DOC_REPOSITORY_FIND_ONE_LOG = "FindOneByBusinessKey in StateDocRepository finished in {}ms";
    public static final String STATE_DOC_REPOSITORY_UPDATE_TO_INIT_AND_FIND_ONE_LOG = "UpdateToInitAndFindOne in StateDocRepository finished in {}ms";
    public static final String STATE_DOC_REPOSITORY_INSERT_ONE_LOG = "InsertOne in StateDocRepository finished in {}ms";
    public static final String STATE_DOC_REPOSITORY_UPDATE_TO_UNSUCCESSFUL_STATE_LOG = "UpdateToUnsuccessfulState in StateDocRepository finished in {}ms";
    public static final String STATE_DOC_REPOSITORY_UPDATE_TO_SUCCESSFUL_STATE_LOG = "UpdateToSuccessfulState in StateDocRepository finished in {}ms";
    public static final String STATE_DOC_REPOSITORY_UPDATE_TIMESTAMP_LOG = "UpdateTimestamp in StateDocRepository finished in {}ms";
    public static final String STATE_DOC_REPOSITORY_UPDATE_VERSION_INFO_LOG = "UpdateVersionInfo in StateDocRepository finished in {}ms";
    public static final String STATE_DOC_REPOSITORY_UPDATE_FROM_SCHEMA_VERSION_ONE_TO_TWO_LOG = "UpdateFromSchemaVersionOneToTwo in StateDocRepository finished in {}ms";
    // ACDS calls
    public static final String ACDS_GET_ACCOUNT_CAPTIONS_LOG = "ACDS call getAccountCaptions finished in {}ms";
    public static final String ACDS_GET_ACCOUNT_PURPOSE_MAPPINGS_LOG = "ACDS call getAccountPurposeMappings finished in {}ms";
    public static final String ACDS_GET_COLLECTIVE_ACCOUNTS_LOG = "ACDS call getCollectiveAccounts finished in {}ms";
    public static final String ACDS_GET_CUSTOM_COLUMN_STRUCTURES_LOG = "ACDS call getCustomColumnStructures finished in {}ms";
    public static final String ACDS_GET_CUSTOM_REPORT_STRUCTURES_LOG = "ACDS call getCustomReportStructures finished in {}ms";
    public static final String ACDS_GET_MASTERDATA_CONTEXT_LOG = "ACDS call getMasterdataContext finished in {}ms";
    public static final String ACDS_GET_MASTERDATA_INVENTORIES_LOG = "ACDS call getMasterdataInventories finished in {}ms";
    public static final String ACDS_GET_SHAREHOLDERS_LOG = "ACDS call getShareholders finished in {}ms";
    public static final String ACDS_GET_TRANSLATIONS_LOG = "ACDS call getTranslations finished in {}ms";
    // Batches
    public static final String MOVEMENT_DATA_DAYS_BATCH_START_LOG = "Starting getAccountSumDays and MovementDataDaysUpsert batch processing";
    public static final String MOVEMENT_DATA_DAYS_BATCH_SUCCESS_LOG = "Batch processing for getAccountSumDays and MovementDataDaysUpsert finished in {}ms";

    public static final String MOVEMENT_DATA_MONTHS_BATCH_START_LOG = "Starting MovementDataDaysFindBusinessKey and MovementDataMonthBulkUpsert batch processing";
    public static final String MOVEMENT_DATA_MONTHS_BATCH_SUCCESS_LOG = "Batch processing for MovementDataDaysFindBusinessKey and MovementDataMonthBulkUpsert finished in {}ms";

    public static final String MOVEMENT_DATA_INVENTORIES_BATCH_START_LOG = "Starting getMovementDataInventories and MovementDataInventoriesBulkUpsert batch processing";
    public static final String MOVEMENT_DATA_INVENTORIES_BATCH_SUCCESS_LOG = "Batch processing for getMovementDataInventories and MovementDataInventoriesBulkUpsert finished in {}ms";

    public static final String DELTA_EVENT_BATCH_START_LOG = "Starting DeltaEvent batch processing";
    public static final String DELTA_EVENT_BATCH_SUCCESS_LOG = "Batch processing for DeltaEvent finished in {}ms";
    public static final String MOVEMENTDATA_COUNT = "movementdata_count";

    /**
     * log debug level with duration.
     *
     * @param message message to be logged
     */
    @NotNull
    public static <T> Function<Tuple2<Long, T>, T> logDebugWithDuration(String message) {
        return objects -> {
            log.debug(message, objects.getT1());
            return objects.getT2();
        };
    }

    /**
     * log info level with duration.
     *
     * @param message message to be logged
     */
    @NotNull
    public static <T> Function<Tuple2<Long, T>, T> logInfoWithDuration(String message) {
        return objects -> {
            log.info(message, objects.getT1());
            return objects.getT2();
        };
    }

    /**
     * log trace level with duration.
     *

     */
    @NotNull
    public static <T> Function<Tuple2<Long, T>, T> logTraceWithDuration(String message) {
        return objects -> {
            log.trace(message, objects.getT1());
            return objects.getT2();
        };
    }

    /**
     * Creates an initial context for logging custom properties
     *
     * @param map           a map with context properties
     * @param correlationId correlationId
     * @return a Context for logging custom properties
     */
    public static Context createInitialContext(Map<String, String> map, String correlationId) {
        return Context.of(LOGGING_CONTEXT_KEY, map, CORRELATION_ID_KEY, correlationId);
    }
}
