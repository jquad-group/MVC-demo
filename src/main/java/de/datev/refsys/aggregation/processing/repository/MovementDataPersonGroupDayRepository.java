package de.datev.refsys.aggregation.processing.repository;

import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.model.InsertOneModel;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.reactivestreams.client.ClientSession;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoCollection;
import de.datev.refsys.aggregation.document.model.MovementDataPersonGroupDay;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import reactor.core.observability.micrometer.Micrometer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static de.datev.refsys.aggregation.document.model.constants.CollectionConstants.MOVEMENT_DATA_PERSON_GROUP_DAYS;

/**
 * Repository class for operations with the MovementDataPersonGroupDays mongo collection
 */
@Slf4j
@Repository
public class MovementDataPersonGroupDayRepository {
    private final MongoCollection<MovementDataPersonGroupDay> insertPersonGroupDayCollection;
    private final MongoCollection<MovementDataPersonGroupDay> updatePersonGroupDayCollection;
    private final CircuitBreaker afterMovementDataCircuitBreaker;
    private final Retry mongoRetryInstance;
    private final MeterRegistry meterRegistry;

    public MovementDataPersonGroupDayRepository(final MongoClient insertMongoClient, final MongoClient updateMongoClient,
                                                final MeterRegistry meterRegistry,
                                                @Value("${spring.data.mongodb.database}") final String databaseName,
                                                final CircuitBreakerRegistry circuitBreakerRegistry, final RetryRegistry retryRegistry) {
        this.insertPersonGroupDayCollection = insertMongoClient.getDatabase(databaseName)
                .getCollection(MOVEMENT_DATA_PERSON_GROUP_DAYS, MovementDataPersonGroupDay.class);
        this.updatePersonGroupDayCollection = updateMongoClient.getDatabase(databaseName)
                .getCollection(MOVEMENT_DATA_PERSON_GROUP_DAYS, MovementDataPersonGroupDay.class);
        this.afterMovementDataCircuitBreaker = circuitBreakerRegistry.circuitBreaker(ProcessingServiceConstants.AFTER_MOVEMENT_DATA_CIRCUIT_BREAKER);
        this.mongoRetryInstance = retryRegistry.retry(ProcessingServiceConstants.MONGODB_RETRY_INSTANCE_NAME);
        this.meterRegistry = meterRegistry;
    }

    /**
     * Deletes many MovementDataPersonGroupDays  for consultant, client and fiscal year key
     *
     * @param consultant consultant number
     * @param client     client number
     * @param fiscalYear fiscal year start
     * @return mongo DeleteResult object
     */
    public Mono<DeleteResult> deleteManyByBusinessKey(Integer consultant, Integer client, Integer fiscalYear) {
        return Mono.from(insertPersonGroupDayCollection.deleteMany(QueryUtil.deleteMovementData(consultant, client, fiscalYear)))
                   .elapsed().map(LoggingUtil.logDebugWithDuration(LoggingUtil.MOVEMENT_DATA_PERSON_GROUP_DAY_REPOSITORY_DELETE_MANY_LOG));
    }

    /**
     * Bulk insert MovementDataPersonGroupDays
     *
     * @param accountPersonGroupDayMap map of MovementDataPersonGroupDay documents
     * @return mongo BulkWriteResult object
     */
    public Mono<BulkWriteResult> bulkInsert(Map<AccountDbKeyFields, MovementDataPersonGroupDay> accountPersonGroupDayMap) {
        return Mono.from(insertPersonGroupDayCollection.bulkWrite(accountPersonGroupDayMap.values().stream().map(InsertOneModel::new).toList()))
                   .elapsed().map(LoggingUtil.logDebugWithDuration(LoggingUtil.MOVEMENT_DATA_PERSON_GROUP_DAY_REPOSITORY_BULK_INSERT_LOG))
                   .name(MetricConstants.METRIC_MONGO)
                   .tag(MetricConstants.REPOSITORY, MOVEMENT_DATA_PERSON_GROUP_DAYS)
                   .tag(MetricConstants.METHOD, "bulkInsert")
                   .tag(MetricConstants.METRIC_TYPE, MetricConstants.METRIC_WRITE)
                   .tap(Micrometer.metrics(meterRegistry))
                   .transformDeferred(CircuitBreakerOperator.of(afterMovementDataCircuitBreaker))
                   .transformDeferred(RetryOperator.of(mongoRetryInstance));
    }

    /**
     * Updates MovementDataPersonGroupDays with deltas
     *
     * @param consultant      consultant number
     * @param client          client number
     * @param fiscalYear      fiscal year start
     * @param accountDayMap MovementDataPersonGroupDays to be updated
     * @param clientSession   mongo ClientSession for transaction
     * @return mongo BulkWriteResult object
     */
    public Mono<BulkWriteResult> bulkUpdate(Integer consultant, Integer client, Integer fiscalYear,
                                            Map<AccountDbKeyFields, MovementDataPersonGroupDay> accountDayMap, ClientSession clientSession) {
        log.debug("bulkUpdate {} documents for consultant={} client={} fiscalYear={} transaction={}", accountDayMap.values().size(), consultant,
                  client, fiscalYear, clientSession.hasActiveTransaction());

        List<UpdateOneModel<MovementDataPersonGroupDay>> updateOneModelList =
                accountDayMap.values()
                             .stream()
                             .map(md -> new UpdateOneModel<MovementDataPersonGroupDay>(
                                     QueryUtil.getOneMovementDataPersonGroupDocument(consultant, client, fiscalYear, md.getAccountingReasonId(),
                                                                                     md.getAccountGroupNumber(), md.getAdditionalParams()),
                                     QueryUtil.incrementMovementDataPersonGroup(md.getValues()), QueryUtil.UPSERT_UPDATE_OPTIONS))
                             .filter(x -> !x.getUpdate().toBsonDocument().isEmpty())
                             .toList();
        if (updateOneModelList.isEmpty()) {
            return Mono.empty();
        }
        return Mono.from(updatePersonGroupDayCollection.bulkWrite(clientSession, updateOneModelList))
                   .elapsed().map(LoggingUtil.logDebugWithDuration(LoggingUtil.MOVEMENT_DATA_PERSON_GROUP_DAY_REPOSITORY_BULK_UPDATE_LOG))
                   .name(MetricConstants.METRIC_MONGO)
                   .tag(MetricConstants.REPOSITORY, MOVEMENT_DATA_PERSON_GROUP_DAYS)
                   .tag(MetricConstants.METHOD, "bulkUpdate")
                   .tag(MetricConstants.METRIC_TYPE, MetricConstants.METRIC_WRITE)
                   .tap(Micrometer.metrics(meterRegistry));
    }
}
