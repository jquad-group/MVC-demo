package de.datev.refsys.aggregation.processing.constant;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ProcessingErrorMessageConstants {
    public static final String EVENT_TRANSACTION_ERROR = "Error during event transaction";
    public static final String IMPORT_ERROR = "Error during import";
    public static final String MOVEMENT_DATA_MAPPING_ERROR = "Error while mapping movement data values. Error Message: %s";
    public static final String MASTER_DATA_MAPPING_ERROR = "Error while mapping master data values. Error Message: %s";
    public static final String MOVEMENT_DATA_INVALID_DATE_ERROR = "Given date %d is outside of the fiscal year during the import of %s";
    public static final String MOVEMENT_DATA_INVALID_FISCAL_MONTH_ERROR =
            "Given fiscal month %d is outside of the fiscal year during the import of %s";
    public static final String MOVEMENT_DATA_INVALID_FISCAL_MONTH_VALUE_ERROR =
            "Given month %d is not a valid fiscal month value during the import of %s";
    public static final String ACCOUNT_SUM_DAY_PROCESSING_ERROR =
            "All accounts with the accountNumber %d were already processed. The AccountSumDays list in ACDS is not sorted by accountNumber";
    public static final String NO_MASTER_DATA_CONTEXT_IN_ACDS = "No data was found when calling the masterdata-context endpoint.";
    public static final String ACDS_TECHNICAL_ERROR = "A technical error occurred when calling the ACDS endpoint %s.";
    public static final String ACDS_BUSINESS_ERROR = "A business error occurred when calling the ACDS endpoint %s.";
    public static final String ANOTHER_IMPORT_IN_PROGRESS =
            "Another import is in progress, last state change was before %s milliseconds. The current request will be ignored.";
    public static final String PARALLEL_IMPORT = "Parallel Import just started";
    public static final String IMPORT_EXCEEDED_MAX_TIME = "Import exceeded the maximum time. Data is Ignored.";
    public static final String UNEXPECTED_CLIENT_ERROR = "An unexpected error happened when calling %s";
    public static final String NO_STATE_DOC_EXISTS_ERROR =
            "An error occurred before a state doc was created, the error data could not be saved in the state doc";
    public static final String KAFKA_SERIALIZATION_ERROR = "Change event serialization error";
    public static final String KAFKA_CHANGE_EVENT_PRODUCER_ERROR = "Kafka event wasn't send due error";
    public static final String MONGODB_WRITE_UNACKNOWLEDGED = "MongoDB returned an unacknowledged write operation";
    public static final String MISSING_MASTER_DATA_INVENTORIES_ERROR =
            "At least one MovementDataInventory was found with the wgId %s which doesn't exist in the MasterDataInventories";
    public static final String UNEXPECTED_VALUE_ERROR = "Unexpected value '";
    public static final String DUPLICATE_INVENTORY_NUMBER_ERROR =
            "A duplicate InventoryNumber occurred when writing into movementDataInventories collection: {}.";
    public static final String INVENTORY_VALID_FROM_IS_NULL = "The field 'valid_from' is null for the wgId {}";
    public static final String UNEXPECTED_ERROR_IN_EXCEPTION_HANDLING = "An unexpected error occurred in the exception handling";
    public static final String BASE_OR_DELTA_VERSION_GAP_ERROR =
            "A gap was found in the base or delta version when comparing to the stateDoc.baseVersion = %s and stateDoc.deltaVersion = %s";
    public static final String BASE_OR_DELTA_VERSION_UP_TO_DATE =
            "The stateDoc baseVersion = {} and deltaVersion = {} are already up to date. The delta request will be ignored";
    public static final String STATE_DOC_STATE_ERROR =
            "The delta request could not be processed because the stateDoc state is %s. Please import the data again";
    public static final String STATE_DOC_NOT_IN_DONE_STATE = "StateDoc was not in DONE state";
    public static final String DELTA_REQUEST_HAS_NO_ACCOUNT_SUM_DAY_DELTAS = "The delta request doesn't contain account sum day deltas";
    public static final String RETRY_EXCEPTION_OCCURRED = "An error was caught in the retry predicate";
    public static final String COLUMN_STRUCTURE_REQUIRED_FIELDS_ARE_NULL_ERROR =
            "The response for CustomColumnStructure is missing %s fields which are required";
    public static final String REPORT_STRUCTURE_REQUIRED_FIELDS_ARE_NULL_ERROR =
            "The response for CustomReportStructure is missing %s fields which are required";
    public static final String ACCOUNT_NUMBER_CHECK_ERROR = "The value of accountNumberFrom is bigger than the value of accountNumberTo";
}
