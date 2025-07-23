package de.datev.refsys.aggregation.processing.repository;

import com.mongodb.client.model.Updates;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;
import com.mongodb.reactivestreams.client.ClientSession;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoCollection;
import de.datev.refsys.aggregation.document.model.ProcessingError;
import de.datev.refsys.aggregation.document.model.StateDoc;
import de.datev.refsys.aggregation.document.model.constants.FieldConstants;
import de.datev.refsys.aggregation.document.model.enums.StateDocState;
import de.datev.refsys.aggregation.processing.constant.MetricConstants;
import de.datev.refsys.aggregation.processing.constant.ProcessingServiceConstants;
import de.datev.refsys.aggregation.processing.util.LoggingUtil;
import de.datev.refsys.aggregation.processing.util.QueryUtil;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import org.bson.conversions.Bson;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import reactor.core.observability.micrometer.Micrometer;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;

import static de.datev.refsys.aggregation.document.model.constants.CollectionConstants.STATE_DOC;

/**
 * Repository class for operations with the StateDocs mongo collection
 */
@Repository
public class StateDocRepository {
    private final MongoCollection<StateDoc> stateDocCollection;
    private final MeterRegistry meterRegistry;
    private final CircuitBreaker stateDocCircuitBreaker;
    private final Retry mongoRetryInstance;
    private final String databaseName;
    private final int schemaVersion;

    public StateDocRepository(final MongoClient insertMongoClient, final MeterRegistry meterRegistry,
                              @Value("${spring.data.mongodb.database}") final String databaseName,
                              final CircuitBreakerRegistry circuitBreakerRegistry, final RetryRegistry retryRegistry,
                              @Value("${ref-sys.update-schema.schema-version}") final int schemaVersion) {
        this.schemaVersion = schemaVersion;
        this.stateDocCollection = insertMongoClient.getDatabase(databaseName).getCollection(STATE_DOC, StateDoc.class);
        this.meterRegistry = meterRegistry;
        this.databaseName = databaseName;
        this.stateDocCircuitBreaker = circuitBreakerRegistry.circuitBreaker(ProcessingServiceConstants.STATE_DOC_CIRCUIT_BREAKER);
        this.mongoRetryInstance = retryRegistry.retry(ProcessingServiceConstants.MONGODB_RETRY_INSTANCE_NAME);
    }

    /**
     * Deletes one StateDoc for consultant, client and fiscal year key
     *<pre>The order of the two transformDeferred is very important:
     *- If the circuit breaker is placed above the retry, every failure from each retry will be recorded and counted towards the circuit breaker
     * threshold.
     *- If the retry is placed above the circuit breaker, all the retries will be exhausted before a circuit breaker could record an exception.
     * </pre>
     * @param consultant consultant number
     * @param client     client number
     * @param fiscalYear fiscal year start
     * @return mongo DeleteResult object
     */
    public Mono<DeleteResult> deleteOne(Integer consultant, Integer client, Integer fiscalYear) {
        return Mono.from(stateDocCollection.deleteOne(QueryUtil.getByMasterDataBusinessKey(consultant, client, fiscalYear)))
                   .elapsed().map(LoggingUtil.logDebugWithDuration(LoggingUtil.STATE_DOC_REPOSITORY_DELETE_ONE_LOG))
                   .name(MetricConstants.METRIC_MONGO)
                   .tag(MetricConstants.REPOSITORY, STATE_DOC)
                   .tag(MetricConstants.METHOD, "deleteOne")
                   .tag(MetricConstants.METRIC_TYPE, MetricConstants.METRIC_DELETE)
                   .tap(Micrometer.metrics(meterRegistry))
                   .transformDeferred(CircuitBreakerOperator.of(stateDocCircuitBreaker))
                   .transformDeferred(RetryOperator.of(mongoRetryInstance));
    }

    /**
     * Finds one StateDoc for consultant, client and fiscal year key
     *<pre>The order of the two transformDeferred is very important:
     *- If the circuit breaker is placed above the retry, every failure from each retry will be recorded and counted towards the circuit breaker
     * threshold.
     *- If the retry is placed above the circuit breaker, all the retries will be exhausted before a circuit breaker could record an exception.
     * </pre>
     * @param consultant consultant number
     * @param client     client number
     * @param fiscalYear fiscal year start
     * @return a StateDoc object
     */
    public Mono<StateDoc> findOneByBusinessKey(Integer consultant, Integer client, Integer fiscalYear) {
        return Mono.from(stateDocCollection.find(QueryUtil.getByMasterDataBusinessKey(consultant, client, fiscalYear)))
                   .elapsed().map(LoggingUtil.logDebugWithDuration(LoggingUtil.STATE_DOC_REPOSITORY_FIND_ONE_LOG))
                   .transformDeferred(CircuitBreakerOperator.of(stateDocCircuitBreaker))
                   .transformDeferred(RetryOperator.of(mongoRetryInstance))
                   .name(MetricConstants.METRIC_MONGO)
                   .tag(MetricConstants.REPOSITORY, STATE_DOC)
                   .tag(MetricConstants.METHOD, "findOneByBusinessKey")
                   .tag(MetricConstants.METRIC_TYPE, MetricConstants.METRIC_READ)
                   .tap(Micrometer.metrics(meterRegistry));
    }

    /**
     * updates and Finds one StateDoc for consultant, client and fiscal year key
     *<pre>The order of the two transformDeferred is very important:
     *- If the circuit breaker is placed above the retry, every failure from each retry will be recorded and counted towards the circuit breaker
     * threshold.
     *- If the retry is placed above the circuit breaker, all the retries will be exhausted before a circuit breaker could record an exception.
     * </pre>
     * @param baseVersion delta version
     * @param deltaVersion base version
     * @param actualStateDoc actual stateDoc
     * @return Mono of {@link StateDoc}
     */
    public Mono<StateDoc> updateToInitAndFindOne(Long baseVersion, Long deltaVersion, StateDoc actualStateDoc) {
        return Mono.from(stateDocCollection.findOneAndUpdate(QueryUtil.getByStateDoc(actualStateDoc),
                                                             getStateDocUpdateToInitAndFindOne(baseVersion, deltaVersion),
                                                             QueryUtil.UPDATE_FIND_OPTIONS))
                   .elapsed().map(LoggingUtil.logDebugWithDuration(LoggingUtil.STATE_DOC_REPOSITORY_UPDATE_TO_INIT_AND_FIND_ONE_LOG))
                   .transformDeferred(CircuitBreakerOperator.of(stateDocCircuitBreaker))
                   .transformDeferred(RetryOperator.of(mongoRetryInstance))
                   .name(MetricConstants.METRIC_MONGO)
                   .tag(MetricConstants.REPOSITORY, STATE_DOC)
                   .tag(MetricConstants.METHOD, "updateAndFindOne")
                   .tag(MetricConstants.METRIC_TYPE, MetricConstants.METRIC_WRITE)
                   .tap(Micrometer.metrics(meterRegistry));
    }

    /**
     * insert a statedoc
     * <pre>The order of the two transformDeferred is very important:
     * - If the circuit breaker is placed above the retry, every failure from each retry will be recorded and counted towards the circuit breaker
     * threshold.
     * - If the retry is placed above the circuit breaker, all the retries will be exhausted before a circuit breaker could record an exception.
     * </pre>
     * @param stateDoc stateDoc
     * @return Mono of {@link InsertOneResult}
     */
    public Mono<InsertOneResult> insertOne(StateDoc stateDoc) {
        return Mono.from(stateDocCollection.insertOne(stateDoc))
                   .elapsed().map(LoggingUtil.logDebugWithDuration(LoggingUtil.STATE_DOC_REPOSITORY_INSERT_ONE_LOG))
                   .transformDeferred(CircuitBreakerOperator.of(stateDocCircuitBreaker))
                   .transformDeferred(RetryOperator.of(mongoRetryInstance))
                   .name("importData-insertOne-" + stateDoc.getState().name())
                   .tag(MetricConstants.METRIC_TYPE, MetricConstants.METRIC_WRITE)
                   .tap(Micrometer.metrics(meterRegistry));
    }

    /**
     * Updates one StateDoc to Unsuccessful State (BAD or INVALID_DATA) for consultant, client and fiscal year key
     *<pre>The order of the two transformDeferred is very important:
     *- If the circuit breaker is placed above the retry, every failure from each retry will be recorded and counted towards the circuit breaker
     * threshold.
     *- If the retry is placed above the circuit breaker, all the retries will be exhausted before a circuit breaker could record an exception.
     * </pre>
     * @param consultant      consultant number
     * @param client          client number
     * @param yearBegin       fiscal year start
     * @param state           StateDocState
     * @param processingError processing error
     * @return mongo UpdateResult object
     */
    public Mono<UpdateResult> updateToUnsuccessfulState(Integer consultant, Integer client, Integer yearBegin,
                                                        StateDocState state, ProcessingError processingError) {
        return Mono.from(stateDocCollection.updateOne(QueryUtil.getByMasterDataBusinessKey(consultant, client, yearBegin),
                                                      getStateDocUpdateToUnsuccessfulState(state, processingError)))
                   .elapsed().map(LoggingUtil.logDebugWithDuration(LoggingUtil.STATE_DOC_REPOSITORY_UPDATE_TO_UNSUCCESSFUL_STATE_LOG))
                   .transformDeferred(CircuitBreakerOperator.of(stateDocCircuitBreaker))
                   .transformDeferred(RetryOperator.of(mongoRetryInstance))
                   .name(MetricConstants.METRIC_MONGO)
                   .tag(MetricConstants.REPOSITORY, STATE_DOC)
                   .tag(MetricConstants.METHOD, "updateToUnsuccessfulState")
                   .tag(MetricConstants.METRIC_TYPE, MetricConstants.METRIC_WRITE)
                   .tap(Micrometer.metrics(meterRegistry));
    }

    /**
     * Updates one StateDoc to successful State (DONE) for consultant, client and fiscal year key
     *<pre>The order of the two transformDeferred is very important:
     *- If the circuit breaker is placed above the retry, every failure from each retry will be recorded and counted towards the circuit breaker
     * threshold.
     *- If the retry is placed above the circuit breaker, all the retries will be exhausted before a circuit breaker could record an exception.
     * </pre>
     * @param consultant   consultant number
     * @param client       client number
     * @param yearBegin    fiscal year start
     * @param baseVersion  base version
     * @param deltaVersion delta version
     * @return mongo UpdateResult object
     */
    public Mono<UpdateResult> updateToSuccessfulState(Integer consultant, Integer client, Integer yearBegin, Long baseVersion, Long deltaVersion) {
        return Mono.from(stateDocCollection.updateOne(QueryUtil.getByMasterDataBusinessKey(consultant, client, yearBegin),
                                                      getStateDocUpdateToSuccessfulState(baseVersion, deltaVersion)))
                   .elapsed().map(LoggingUtil.logDebugWithDuration(LoggingUtil.STATE_DOC_REPOSITORY_UPDATE_TO_SUCCESSFUL_STATE_LOG))
                   .transformDeferred(CircuitBreakerOperator.of(stateDocCircuitBreaker))
                   .transformDeferred(RetryOperator.of(mongoRetryInstance))
                   .name(MetricConstants.METRIC_MONGO)
                   .tag(MetricConstants.REPOSITORY, STATE_DOC)
                   .tag(MetricConstants.METHOD, "updateToSuccessfulState")
                   .tag(MetricConstants.METRIC_TYPE, MetricConstants.METRIC_WRITE)
                   .tap(Micrometer.metrics(meterRegistry));
    }

    /**
     * Updates one StateDoc stateTimestamp for consultant, client and fiscal year key
     *<pre>The order of the two transformDeferred is very important:
     *- If the circuit breaker is placed above the retry, every failure from each retry will be recorded and counted towards the circuit breaker
     * threshold.
     *- If the retry is placed above the circuit breaker, all the retries will be exhausted before a circuit breaker could record an exception.
     * </pre>
     * @param consultant   consultant number
     * @param client       client number
     * @param yearBegin    fiscal year start
     * @param currentTimestamp current Timestamp
     * @return mongo UpdateResult object
     */
    public Mono<UpdateResult> updateTimestamp(Integer consultant, Integer client, Integer yearBegin, OffsetDateTime currentTimestamp) {
        return Mono.from(stateDocCollection.updateOne(QueryUtil.getByMasterDataBusinessKey(consultant, client, yearBegin),
                                                      Updates.set(FieldConstants.STATE_TIMESTAMP, currentTimestamp)))
                   .elapsed().map(LoggingUtil.logDebugWithDuration(LoggingUtil.STATE_DOC_REPOSITORY_UPDATE_TIMESTAMP_LOG))
                   .transformDeferred(CircuitBreakerOperator.of(stateDocCircuitBreaker))
                   .transformDeferred(RetryOperator.of(mongoRetryInstance))
                   .name(MetricConstants.METRIC_MONGO)
                   .tag(MetricConstants.REPOSITORY, STATE_DOC)
                   .tag(MetricConstants.METHOD, "updateTimestamp")
                   .tag(MetricConstants.METRIC_TYPE, MetricConstants.METRIC_WRITE)
                   .tap(Micrometer.metrics(meterRegistry));
    }


    /**
     * Update the version information (base and delta version for an existing statedoc
     * @param baseVersion the base version to be set
     * @param deltaVersion the delta version to be set
     * @return mongo UpdateResult object
     */
    public Mono<StateDoc> updateVersionInfo(StateDoc actualStateDoc, Long baseVersion, Long deltaVersion) {
        return Mono.from(stateDocCollection.findOneAndUpdate(QueryUtil.getByStateDoc(actualStateDoc),
                                                             Updates.combine(Updates.set(FieldConstants.STATE_TIMESTAMP, OffsetDateTime.now()),
                                                                             Updates.set(FieldConstants.BASE_VERSION, baseVersion),
                                                                             Updates.set(FieldConstants.DELTA_VERSION, deltaVersion)),
                                                             QueryUtil.UPDATE_FIND_OPTIONS))
                   .elapsed().map(LoggingUtil.logDebugWithDuration(LoggingUtil.STATE_DOC_REPOSITORY_UPDATE_VERSION_INFO_LOG))
                   .name("importData-updateStateDoc-updateVersionInfo")
                   .tag(MetricConstants.METRIC_TYPE, MetricConstants.METRIC_WRITE)
                   .tap(Micrometer.metrics(meterRegistry));
    }

    /**
     * Update the version information (base and delta version for an existing statedoc
     * @param consultant query parameter ("Beraternummer")
     * @param client query parameter ("Mandantennummer")
     * @param fiscalYear query parameter ("Wirtschaftsjahr")
     * @param clientSession session for transaction handling (must be open on the given mongo client)
     * @param mongoClient mongo client with an open session
     * @param baseVersion the base version to be set
     * @param deltaVersion the delta version to be set
     * @return mongo UpdateResult object
     */
    public Mono<UpdateResult> updateVersionInfo(Integer consultant, Integer client, Integer fiscalYear, ClientSession clientSession,
                                                MongoClient mongoClient, Long baseVersion, Long deltaVersion) {
        // WORKAROUND - session may use a different client than the one stored in the repository
        // It could be better not to expose the session, but spans multiple collections (repos)
        MongoCollection<StateDoc> transactionEnabledMongoCollection = mongoClient.getDatabase(databaseName).getCollection(STATE_DOC, StateDoc.class);
        return Mono.from(
                           transactionEnabledMongoCollection.updateOne(clientSession, QueryUtil.getByMasterDataBusinessKey(consultant, client,
                                                                                                                           fiscalYear),
                                                                       // TODO: Query by existing state Doc
                                                                       Updates.combine(Updates.set(FieldConstants.STATE_TIMESTAMP,
                                                                                                   OffsetDateTime.now()),
                                                                                       Updates.set(FieldConstants.BASE_VERSION, baseVersion),
                                                                                       Updates.set(FieldConstants.DELTA_VERSION, deltaVersion))))
                   .elapsed().map(LoggingUtil.logDebugWithDuration(LoggingUtil.STATE_DOC_REPOSITORY_UPDATE_VERSION_INFO_LOG))
                   .name("importData-updateStateDoc-updateVersionInfo")
                   .tag(MetricConstants.METRIC_TYPE, MetricConstants.METRIC_WRITE)
                   .tap(Micrometer.metrics(meterRegistry));
    }

    /**
     * Update schema version from version 1 to version 4 and sets the value for the forceReftabCurrentYear field
     *
     * @param consultant             consultant number
     * @param client                 client number
     * @param fiscalYear             fiscal year start
     * @param forceReftabCurrentYear boolean flag from master data context
     * @return mongo UpdateResult object
     */
    public Mono<UpdateResult> updateFromSchemaVersionOneToFour(Integer consultant, Integer client, Integer fiscalYear,
                                                               boolean forceReftabCurrentYear) {
        return Mono.from(stateDocCollection.updateOne(QueryUtil.getByMasterDataBusinessKey(consultant, client, fiscalYear),
                                                      Updates.combine(Updates.set(FieldConstants.FORCE_REFTAB_CURRENT_YEAR, forceReftabCurrentYear))))
                   .elapsed().map(LoggingUtil.logDebugWithDuration(LoggingUtil.STATE_DOC_REPOSITORY_UPDATE_FROM_SCHEMA_VERSION_ONE_TO_TWO_LOG))
                   .name(MetricConstants.METRIC_MONGO)
                   .tag(MetricConstants.REPOSITORY, STATE_DOC)
                   .tag(MetricConstants.METHOD, "updateFromSchemaVersionOneToFour")
                   .tag(MetricConstants.METRIC_TYPE, MetricConstants.METRIC_WRITE)
                   .tap(Micrometer.metrics(meterRegistry));
    }

    /**
     * Creates a new stateDoc
     *
     * @param consultant   consultant number
     * @param client       client number
     * @param yearBegin    fiscal year start
     * @param yearEnd      fiscal year end
     * @param baseVersion  base version
     * @param deltaVersion delta version
     * @return a StateDoc instance
     */
    public StateDoc createNewStateDoc(Integer consultant, Integer client, Integer yearBegin, Integer yearEnd, Long baseVersion, Long deltaVersion,
                                      boolean forceReftabCurrentYear) {
        OffsetDateTime currentTimestamp = OffsetDateTime.now();
        return StateDoc.builder()
                       .consultant(consultant)
                       .client(client)
                       .yearBegin(yearBegin)
                       .yearEnd(yearEnd)
                       .state(StateDocState.INIT)
                       .baseVersion(baseVersion)
                       .deltaVersion(deltaVersion)
                       .schemaVersion(schemaVersion)
                       .stateTimestamp(currentTimestamp)
                       .createdTimestamp(currentTimestamp)
                       .forceReftabCurrentYear(forceReftabCurrentYear)
                       .build();
    }

    private Bson getStateDocUpdateToInitAndFindOne(Long baseVersion, Long deltaVersion) {
        return Updates.combine(Updates.set(FieldConstants.STATE, StateDocState.INIT),
                               Updates.set(FieldConstants.STATE_TIMESTAMP, OffsetDateTime.now()),
                               Updates.set(FieldConstants.CREATED_TIMESTAMP, OffsetDateTime.now()),
                               Updates.set(FieldConstants.BASE_VERSION, baseVersion),
                               Updates.set(FieldConstants.DELTA_VERSION, deltaVersion));
    }

    private Bson getStateDocUpdateToSuccessfulState(Long baseVersion, Long deltaVersion) {
        return Updates.combine(Updates.set(FieldConstants.STATE, StateDocState.DONE),
                               Updates.set(FieldConstants.STATE_TIMESTAMP, OffsetDateTime.now()),
                               Updates.set(FieldConstants.BASE_VERSION, baseVersion),
                               Updates.set(FieldConstants.DELTA_VERSION, deltaVersion),
                               Updates.set(FieldConstants.SCHEMA_VERSION, schemaVersion),
                               Updates.unset(FieldConstants.PROCESSING_ERROR));
    }

    private Bson getStateDocUpdateToUnsuccessfulState(StateDocState state, ProcessingError processingError) {
        return Updates.combine(Updates.set(FieldConstants.STATE, state),
                               Updates.set(FieldConstants.STATE_TIMESTAMP, OffsetDateTime.now()),
                               Updates.set(FieldConstants.PROCESSING_ERROR_SOURCE, processingError.getSource()),
                               Updates.set(FieldConstants.PROCESSING_ERROR_PROBLEM_INFO, processingError.getProblemInfo()),
                               Updates.set(FieldConstants.PROCESSING_ERROR_SOURCE_STATUS_CODE, processingError.getSourceStatusCode()),
                               Updates.set(FieldConstants.PROCESSING_ERROR_SOURCE_ENDPOINT, processingError.getSourceEndpoint()),
                               Updates.set(FieldConstants.PROCESSING_ERROR_CORRELATION_ID, processingError.getCorrelationId()),
                               Updates.inc(FieldConstants.PROCESSING_ERROR_RETRY_COUNT, 1));
    }
}
