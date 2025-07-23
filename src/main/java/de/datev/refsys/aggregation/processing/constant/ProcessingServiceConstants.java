package de.datev.refsys.aggregation.processing.constant;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ProcessingServiceConstants {
    public static final boolean NEAR_TIME_DATA = true;
    // CircuitBreaker and Retry constants
    public static final String MONGODB_RETRY_INSTANCE_NAME = "mongodb-retry";
    public static final String MONGODB_UPDATE_RETRY_INSTANCE_NAME = "mongodb-update-retry";
    public static final String HTTP_CLIENT_RETRY_INSTANCE_NAME = "http-client-retry";
    public static final String MASTER_DATA_CONTEXT_CIRCUIT_BREAKER = "masterDataContextCircuitBreaker";
    public static final String PURPOSE_MAPPINGS_CAPTIONS_MASTER_INVENTORIES_SHAREHOLDER_CIRCUIT_BREAKER =
            "purposeMappingsCaptionsMasterInventoriesAndShareholderCircuitBreaker";
    public static final String COLLECTIVE_ACCOUNTS_MOVEMENT_INVENTORIES_TRANSLATION_CIRCUIT_BREAKER =
            "collectiveAccountsMovementInventoriesAndTranslationCircuitBreaker";
    public static final String MOVEMENT_DATA_CLIENT_CIRCUIT_BREAKER = "movementDataCircuitBreaker";
    public static final String MOVEMENT_DATA_INVENTORIES_CIRCUIT_BREAKER = "movementDataInventoriesCircuitBreaker";
    public static final String STATE_DOC_CIRCUIT_BREAKER = "stateDocCircuitBreaker";
    public static final String IMPORT_MOVEMENT_DATA_CIRCUIT_BREAKER = "importMovementDataCircuitBreaker";
    public static final String AFTER_MOVEMENT_DATA_CIRCUIT_BREAKER = "afterMovementDataCircuitBreaker";
    // Problem type constants
    public static final String STATE_DOC_NOT_FOUND_TYPE = "STATE_DOC_NOT_FOUND";
    public static final String STATE_DOC_STATE_ERROR_TYPE = "STATE_DOC_STATE_ERROR";
    public static final String STATE_DOC_STATE_NOT_IN_DONE = "STATE_DOC_STATE_NOT_IN_DONE";
    public static final String STATE_DOC_BASE_OR_DELTA_VERSION_GAP_TYPE = "STATE_DOC_BASE_OR_DELTA_VERSION_GAP";
    public static final String IMPORT_IN_PROGRESS_TYPE = "IMPORT_IN_PROGRESS";
    // Log level constatns
    public static final String LOG_LEVEL_WARNING = "WARNING";
}
