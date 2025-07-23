package de.datev.refsys.aggregation.processing.repository;

import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.InsertOneModel;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.reactivestreams.client.ClientSession;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoCollection;
import de.datev.refsys.aggregation.document.model.MovementDataMonth;
import de.datev.refsys.aggregation.document.model.constants.FieldConstants;
import de.datev.refsys.aggregation.processing.constant.MetricConstants;
import de.datev.refsys.aggregation.processing.constant.ProcessingServiceConstants;
import de.datev.refsys.aggregation.processing.model.AccountDbKeyFields;
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
import org.bson.BsonValue;
import org.bson.conversions.Bson;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import reactor.core.observability.micrometer.Micrometer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static de.datev.refsys.aggregation.document.model.constants.CollectionConstants.MOVEMENT_DATA_MONTHS;

/**
 * Repository class for operations with the MovementDataMonths mongo collection
 */
@Slf4j
@Repository
public class MovementDataMonthRepository {
    private final MongoCollection<MovementDataMonth> insertMovementDataMonthCollection;
    private final MongoCollection<MovementDataMonth> updateMovementDataMonthCollection;
    private final CircuitBreaker importMovementDataCircuitBreaker;
    private final Retry mongoRetryInstance;
    private final MeterRegistry meterRegistry;
    private final MongoClient mongoClient;
    private final String databaseName;

    public MovementDataMonthRepository(final MongoClient insertMongoClient, final MongoClient updateMongoClient,
                                       final MeterRegistry meterRegistry,
                                       @Value("${spring.data.mongodb.database}") final String databaseName,
                                       final CircuitBreakerRegistry circuitBreakerRegistry, final RetryRegistry retryRegistry) {
        this.insertMovementDataMonthCollection = insertMongoClient.getDatabase(databaseName)
                .getCollection(MOVEMENT_DATA_MONTHS, MovementDataMonth.class);
        this.updateMovementDataMonthCollection = updateMongoClient.getDatabase(databaseName)
                .getCollection(MOVEMENT_DATA_MONTHS, MovementDataMonth.class);
        this.importMovementDataCircuitBreaker =
                circuitBreakerRegistry.circuitBreaker(ProcessingServiceConstants.IMPORT_MOVEMENT_DATA_CIRCUIT_BREAKER);
        this.mongoRetryInstance = retryRegistry.retry(ProcessingServiceConstants.MONGODB_RETRY_INSTANCE_NAME);
        this.meterRegistry = meterRegistry;
        this.mongoClient = insertMongoClient;
        this.databaseName = databaseName;
    }

    /**
     * Deletes many MovementDataMonths  for consultant, client and fiscal year key
     *
     * @param consultant consultant number
     * @param client     client number
     * @param fiscalYear fiscal year start
     * @return mongo DeleteResult object
     */
    public Mono<DeleteResult> deleteManyByBusinessKey(Integer consultant, Integer client, Integer fiscalYear) {
        return Mono.from(insertMovementDataMonthCollection.deleteMany(QueryUtil.deleteMovementData(consultant, client, fiscalYear)))
                   .elapsed().map(LoggingUtil.logDebugWithDuration(LoggingUtil.MOVEMENT_DATA_MONTH_REPOSITORY_DELETE_MANY_LOG))
                   .name(MetricConstants.METRIC_MONGO)
                   .tag(MetricConstants.REPOSITORY, MOVEMENT_DATA_MONTHS)
                   .tag(MetricConstants.METHOD, "deleteManyByBusinessKey")
                   .tag(MetricConstants.METRIC_TYPE, MetricConstants.METRIC_DELETE)
                   .tap(Micrometer.metrics(meterRegistry));
    }

    /**
     * Find all account numbers by oid
     *
     * @param idList oid list from bulk update
     * @return List of account numbers
     */
    public Mono<List<MovementDataMonth>> findAllById(List<BsonValue> idList) {
        return Flux.from(updateMovementDataMonthCollection.find(getMovementDataById(idList))
                                    .projection(getMovementDataByIdFieldsFilter())).collectList()
                   .elapsed().map(LoggingUtil.logDebugWithDuration(LoggingUtil.MOVEMENT_DATA_MONTH_REPOSITORY_FIND_ALL_LOG))
                   .name(MetricConstants.METRIC_MONGO)
                   .tag(MetricConstants.METRIC_TYPE, MetricConstants.METRIC_READ)
                   .tag(MetricConstants.REPOSITORY, MOVEMENT_DATA_MONTHS)
                   .tag(MetricConstants.METHOD, "findAllById")
                   .tap(Micrometer.metrics(meterRegistry));
    }

    /**
     * Bulk insert MovementDataMonths
     *
     * @param accountMonthMap map of MovementDataMonth documents
     * @return mongo BulkWriteResult object
     */
    public Mono<BulkWriteResult> bulkInsert(Map<AccountDbKeyFields, MovementDataMonth> accountMonthMap) {
        return Mono.from(insertMovementDataMonthCollection.bulkWrite(accountMonthMap.values().stream().map(InsertOneModel::new).toList()))
                   .elapsed().map(LoggingUtil.logTraceWithDuration(LoggingUtil.MOVEMENT_DATA_MONTH_REPOSITORY_BULK_INSERT_LOG))
                   .name(MetricConstants.METRIC_MONGO)
                   .tag(MetricConstants.METRIC_TYPE, MetricConstants.METRIC_WRITE)
                   .tag(MetricConstants.REPOSITORY, MOVEMENT_DATA_MONTHS)
                   .tag(MetricConstants.METHOD, "bulkInsert")
                   .tap(Micrometer.metrics(meterRegistry))
                   .transformDeferred(CircuitBreakerOperator.of(importMovementDataCircuitBreaker))
                   .transformDeferred(RetryOperator.of(mongoRetryInstance));
    }

    /**
     * Updates MovementDataMonths with deltas
     *
     * @param consultant      consultant number
     * @param client          client number
     * @param fiscalYear      fiscal year start
     * @param accountMonthMap MovementDataMonths to be updated
     * @param clientSession   mongo ClientSession for transaction
     * @return mongo BulkWriteResult object
     */
    public Mono<List<Integer>> bulkUpsert(Integer consultant, Integer client, Integer fiscalYear,
                                          Map<AccountDbKeyFields, MovementDataMonth> accountMonthMap, ClientSession clientSession) {
        log.debug("bulkUpdate {} documents for consultant={} client={} fiscalYear={} transaction={}", accountMonthMap.values().size(), consultant,
                  client, fiscalYear, clientSession.hasActiveTransaction());

        // 1) Baue zuerst eine geordnete Liste aller MD-Objekte und parallel ihre AccountNumbers
        List<MovementDataMonth> mdList = new ArrayList<>(accountMonthMap.values());
        List<Integer> accountNumbers = mdList.stream()
                                            .map(MovementDataMonth::getAccountNumber)
                                            .toList();

        List<UpdateOneModel<MovementDataMonth>> updateOneModelList =
            mdList
               .stream()
               .map(md -> {
                   // Filter wie gehabt
                   Bson filter = QueryUtil.getOneMovementDataDocument(
                           consultant, client, fiscalYear,
                           md.getAccountingReasonId(),
                           md.getAccountNumber(),
                           md.getAdditionalParams()
                                                                     );

                   // $inc-Teil (besteht schon)
                   Bson incUpdate = QueryUtil.incrementMovementData(md.getValues());

                   // $setOnInsert-Teil: alle Felder, mit denen ein neues Dokument initialisiert werden soll
                   Bson update = Updates.combine(
                           incUpdate,
                           Updates.setOnInsert("consultant", consultant),
                           Updates.setOnInsert("client", client),
                           Updates.setOnInsert("fiscalYear", fiscalYear),
                           Updates.setOnInsert("accountingReasonId", md.getAccountingReasonId()),
                           Updates.setOnInsert("accountNumber", md.getAccountNumber()),
                           Updates.setOnInsert("additionalParams", md.getAdditionalParams())
                                                     );

                   // UpdateOneModel mit upsert=true
                   return new UpdateOneModel<MovementDataMonth>(
                           filter,
                           update,
                           QueryUtil.UPSERT_UPDATE_OPTIONS   // enthält: new UpdateOptions().upsert(true)
                   );
               })
               .toList();
        if (updateOneModelList.isEmpty()) {
            return Mono.empty();
        }
        return Mono.from(updateMovementDataMonthCollection.bulkWrite(clientSession, updateOneModelList))
                   .elapsed()
                   .map(LoggingUtil.logDebugWithDuration(LoggingUtil.MOVEMENT_DATA_MONTH_REPOSITORY_BULK_UPDATE_LOG))
                   .name(MetricConstants.METRIC_MONGO)
                   .tag(MetricConstants.METRIC_TYPE, MetricConstants.METRIC_WRITE)
                   .tag(MetricConstants.REPOSITORY, MOVEMENT_DATA_MONTHS)
                   .tag(MetricConstants.METHOD, "bulkUpdate")
                   .tap(Micrometer.metrics(meterRegistry))
                   .map(bulkResult -> {
                       long modifiedCount = bulkResult.getModifiedCount();
                       int upsertCount    = bulkResult.getUpserts().size();
                       long totalTouched  = modifiedCount + upsertCount;

                       // Wenn nicht alle docs wirklich geändert oder neu angelegt wurden, dann gibt es ein log.warn
                       if (totalTouched != accountNumbers.size()) {
                           log.warn("Nicht alle Accounts wurden bearbeitet.");
                       }

                       return accountNumbers;
                   });
    }

    private static Bson getMovementDataById(List<BsonValue> idList) {
        return Filters.in(FieldConstants.ID, idList);
    }

    private static Bson getMovementDataByIdFieldsFilter() {
        return Projections.fields(Projections.include(FieldConstants.ACCOUNT_NUMBER));
    }
}
