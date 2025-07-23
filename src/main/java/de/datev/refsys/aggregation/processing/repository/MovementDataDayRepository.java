package de.datev.refsys.aggregation.processing.repository;

import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.InsertOneModel;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.reactivestreams.client.ClientSession;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoCollection;
import de.datev.refsys.aggregation.document.model.MovementDataDay;
import de.datev.refsys.aggregation.document.model.constants.FieldConstants;
import de.datev.refsys.aggregation.processing.constant.MetricConstants;
import de.datev.refsys.aggregation.processing.constant.ProcessingServiceConstants;
import de.datev.refsys.aggregation.processing.model.AccountDbKeyFields;
import de.datev.refsys.aggregation.processing.model.MovementDataDayUpsert;
import de.datev.refsys.aggregation.processing.util.LoggingUtil;
import de.datev.refsys.aggregation.processing.util.QueryUtil;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.bson.conversions.Bson;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import reactor.core.observability.micrometer.Micrometer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static de.datev.refsys.aggregation.document.model.constants.CollectionConstants.MOVEMENT_DATA_DAYS;

/**
 * Repository class for operations with the MovementDataDays mongo collection
 */
@Slf4j
@Repository
public class MovementDataDayRepository {
    private final MongoCollection<MovementDataDay> insertMovementDataDayCollection;
    private final MongoCollection<MovementDataDay> updateMovementDataDayCollection;
    private final CircuitBreaker importMovementDataCircuitBreaker;
    private final Retry mongoRetryInstance;
    private final MeterRegistry meterRegistry;

    public MovementDataDayRepository(final MongoClient insertMongoClient, final MongoClient updateMongoClient, final MeterRegistry meterRegistry,
                                     @Value("${spring.data.mongodb.database}") final String databaseName,
                                     final CircuitBreakerRegistry circuitBreakerRegistry, final RetryRegistry retryRegistry) {
        this.insertMovementDataDayCollection = insertMongoClient.getDatabase(databaseName).getCollection(MOVEMENT_DATA_DAYS, MovementDataDay.class);
        this.updateMovementDataDayCollection = updateMongoClient.getDatabase(databaseName).getCollection(MOVEMENT_DATA_DAYS, MovementDataDay.class);
        this.importMovementDataCircuitBreaker =
                circuitBreakerRegistry.circuitBreaker(ProcessingServiceConstants.IMPORT_MOVEMENT_DATA_CIRCUIT_BREAKER);
        this.mongoRetryInstance = retryRegistry.retry(ProcessingServiceConstants.MONGODB_RETRY_INSTANCE_NAME);
        this.meterRegistry = meterRegistry;
    }

    /**
     * Deletes many MovementDataDays  for consultant, client and fiscal year key
     *
     * @param consultant consultant number
     * @param client     client number
     * @param fiscalYear fiscal year start
     * @return mongo DeleteResult object
     */
    public Mono<DeleteResult> deleteManyByBusinessKey(Integer consultant, Integer client, Integer fiscalYear) {
        return Mono.from(insertMovementDataDayCollection.deleteMany(QueryUtil.deleteMovementData(consultant, client, fiscalYear)))
                   .elapsed().map(LoggingUtil.logDebugWithDuration(LoggingUtil.MOVEMENT_DATA_DAY_REPOSITORY_DELETE_MANY_LOG));
    }

    /**
     * Bulk insert MovementDataDays
     *
     * @param accountDayMap map of MovementDataDay documents
     * @return mongo BulkWriteResult object
     */
    public Mono<BulkWriteResult> bulkInsert(Map<AccountDbKeyFields, MovementDataDay> accountDayMap) {
        return Mono.from(insertMovementDataDayCollection.bulkWrite(accountDayMap.values().stream().map(InsertOneModel::new).toList()))
                   .elapsed().map(LoggingUtil.logDebugWithDuration(LoggingUtil.MOVEMENT_DATA_DAY_REPOSITORY_BULK_INSERT_LOG))
                   .name(MetricConstants.METRIC_MONGO)
                   .tag(MetricConstants.METRIC_TYPE, MetricConstants.METRIC_WRITE)
                   .tag(MetricConstants.REPOSITORY, MOVEMENT_DATA_DAYS)
                   .tag(MetricConstants.METHOD, "bulkInsert")
                   .tap(Micrometer.metrics(meterRegistry))
                   .transformDeferred(CircuitBreakerOperator.of(importMovementDataCircuitBreaker))
                   .transformDeferred(RetryOperator.of(mongoRetryInstance));
    }

    /**
     * Updates MovementDataDays with deltas
     *
     * @param consultant    consultant number
     * @param client        client number
     * @param fiscalYear    fiscal year start
     * @param accountDayMap MovementDataDays to be updated
     * @param clientSession mongo ClientSession for transaction
     * @return mongo BulkWriteResult object
     */
    public Mono<BulkWriteResult> bulkUpsert(Integer consultant, Integer client, Integer fiscalYear,
                                            Map<AccountDbKeyFields, MovementDataDay> accountDayMap, ClientSession clientSession) {
        log.debug("bulkUpdate {} documents for consultant={} client={} fiscalYear={} transaction={}", accountDayMap.values().size(), consultant,
                  client, fiscalYear, clientSession.hasActiveTransaction());
        List<UpdateOneModel<MovementDataDay>> updateOneModelList =
                accountDayMap.values()
                             .stream()
                             .map(md -> {
                                 // a) Filter auf Business-Keys + additionalParams
                                 Bson filter = QueryUtil.getOneMovementDataDocument(
                                         consultant, client, fiscalYear,
                                         md.getAccountingReasonId(),
                                         md.getAccountNumber(),
                                         md.getAdditionalParams()
                                                                                   );

                                 // b) $inc-Teil
                                 Bson incUpdate = QueryUtil.incrementMovementData(md.getValues());

                                 // c) $setOnInsert-Teil für neue Dokumente
                                 Bson upsert = Updates.combine(
                                         incUpdate,
                                         Updates.setOnInsert("consultant", consultant),
                                         Updates.setOnInsert("client", client),
                                         Updates.setOnInsert("fiscalYear", fiscalYear),
                                         Updates.setOnInsert("accountingReasonId", md.getAccountingReasonId()),
                                         Updates.setOnInsert("accountNumber", md.getAccountNumber()),
                                         Updates.setOnInsert("additionalParams", md.getAdditionalParams())
                                                                   );
                                 return new UpdateOneModel<MovementDataDay>(
                                         filter,
                                         upsert,
                                         QueryUtil.UPSERT_UPDATE_OPTIONS
                                 );
                             })
                             .toList();
        if (updateOneModelList.isEmpty()) {
            return Mono.empty();
        }
        return Mono.from(updateMovementDataDayCollection.bulkWrite(clientSession, updateOneModelList))
                   .elapsed().map(LoggingUtil.logDebugWithDuration(LoggingUtil.MOVEMENT_DATA_DAY_REPOSITORY_BULK_UPDATE_LOG))
                   .name(MetricConstants.METRIC_MONGO)
                   .tag(MetricConstants.METRIC_TYPE, MetricConstants.METRIC_WRITE)
                   .tag(MetricConstants.REPOSITORY, MOVEMENT_DATA_DAYS)
                   .tag(MetricConstants.METHOD, "bulkUpdate")
                   .tap(Micrometer.metrics(meterRegistry));
    }

    /**
     * Upserts MovementDataDays from AccountSumDays
     *
     * @param consultant        consultant number
     * @param client            client number
     * @param fiscalYear        fiscal year start
     * @param movementDataDayUpserts list containing AccountSumDays data to execute an update
     * @return mongo BulkWriteResult object
     */
    public Mono<BulkWriteResult> bulkUpsert(Integer consultant, Integer client, Integer fiscalYear,
                                            List<MovementDataDayUpsert> movementDataDayUpserts) {
        return Mono.from(insertMovementDataDayCollection.bulkWrite(
                           movementDataDayUpserts.stream().map(mdd -> createUpsertModel(consultant, client, fiscalYear, mdd)).toList()))
                   .elapsed().map(LoggingUtil.logTraceWithDuration(LoggingUtil.MOVEMENT_DATA_DAY_REPOSITORY_BULK_UPSERT_LOG))
                   .name(MetricConstants.METRIC_MONGO)
                   .tag(MetricConstants.METRIC_TYPE, MetricConstants.METRIC_WRITE)
                   .tag(MetricConstants.REPOSITORY, MOVEMENT_DATA_DAYS)
                   .tag(MetricConstants.METHOD, "bulkUpsert")
                   .tap(Micrometer.metrics(meterRegistry));
    }

    /**
     * Returns all MovementDataDay Documents for the given consultant, client and fiscal year sorted by fiscal year descending and account number
     * ascending
     *
     * @param consultant consultant number
     * @param client     client number
     * @param fiscalYear requested fiscal year
     * @return List of MovementDataDay Documents
     */
    public Flux<MovementDataDay> findByBusinessKey(Integer consultant, Integer client, Integer fiscalYear) {
        return Flux.from(insertMovementDataDayCollection.find(getMovementDataByBusinessKey(consultant, client, fiscalYear))
                                                        .sort(QueryUtil.MOVEMENT_DATA_SORT))
                   .name(MetricConstants.METRIC_MONGO)
                   .tag(MetricConstants.REPOSITORY, MOVEMENT_DATA_DAYS)
                   .tag(MetricConstants.METHOD, "findAllMovementDataDayForAccount")
                   .tag(MetricConstants.METRIC_TYPE, MetricConstants.METRIC_READ)
                   .tap(Micrometer.metrics(meterRegistry));
    }

    /**
     * Returns all MovementDataDay Documents for the given Account
     *
     * @param consultant       consultant number
     * @param client           client number
     * @param fiscalYear       requested fiscal year
     * @param accountNumbers   account numbers list
     * @return List of MovementDataDay Documents
     */
    public Mono<List<MovementDataDay>> findAllMovementDataDayForAccount(Integer consultant, Integer client, Integer fiscalYear,
                                                                        List<Integer> accountNumbers) {
        Bson movementDataFindQuery = QueryUtil.getMovementDataPerAccountQuery(consultant, client, fiscalYear, accountNumbers);
        return Flux.from(insertMovementDataDayCollection.find(movementDataFindQuery).sort(QueryUtil.MOVEMENT_DATA_SORT))
                   .collectList()
                   .elapsed().map(LoggingUtil.logTraceWithDuration(LoggingUtil.MOVEMENT_DATA_DAY_REPOSITORY_FIND_ALL_LOG))
                   .name(MetricConstants.METRIC_MONGO)
                   .tag(MetricConstants.REPOSITORY, MOVEMENT_DATA_DAYS)
                   .tag(MetricConstants.METHOD, "findAllMovementDataDayForAccount")
                   .tag(MetricConstants.METRIC_TYPE, MetricConstants.METRIC_READ)
                   .tap(Micrometer.metrics(meterRegistry));
    }

    private static UpdateOneModel<MovementDataDay> createUpsertModel(Integer consultant, Integer client, Integer fiscalYear,
                                                                     MovementDataDayUpsert mdd) {
        String dayKeyIncrement = FieldConstants.INC_VALUES + mdd.dayKey();
        return new UpdateOneModel<>(
                QueryUtil.getOneMovementDataDocument(consultant, client, fiscalYear, mdd.accountingReasonId(), mdd.accountNumber(),
                                                     mdd.additionalParameters()),
                QueryUtil.incrementMovementData(mdd.accountValue(), dayKeyIncrement), QueryUtil.UPSERT_UPDATE_OPTIONS);
    }

    private static Bson getMovementDataByBusinessKey(Integer consultant, Integer client, Integer fiscalYear) {
        return Filters.and(Filters.eq(FieldConstants.CONSULTANT, consultant), Filters.eq(FieldConstants.CLIENT, client),
                           Filters.eq(FieldConstants.FISCAL_YEAR, fiscalYear));
    }
}
