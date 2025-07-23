package de.datev.refsys.aggregation.processing.repository;

import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoCollection;
import de.datev.refsys.aggregation.document.model.MovementDataInventory;
import de.datev.refsys.aggregation.document.model.constants.FieldConstants;
import de.datev.refsys.aggregation.processing.constant.MetricConstants;
import de.datev.refsys.aggregation.processing.constant.ProcessingErrorMessageConstants;
import de.datev.refsys.aggregation.processing.constant.ProcessingServiceConstants;
import de.datev.refsys.aggregation.processing.mapper.InventoryValueMapper;
import de.datev.refsys.aggregation.processing.model.ExtendedMovementdataInventory;
import de.datev.refsys.aggregation.processing.model.InventoryDbKeyFields;
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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static de.datev.refsys.aggregation.document.model.constants.CollectionConstants.MOVEMENT_DATA_INVENTORIES;

/**
 * Repository class for operations with the MovementDataInventories mongo collection
 */
@Slf4j
@Repository
public class MovementDataInventoryRepository {
    private final MongoCollection<MovementDataInventory> movementDataInventoriesCollection;
    private final InventoryValueMapper inventoryValueMapper;
    private final CircuitBreaker afterMovementDataCircuitBreaker;
    private final Retry mongoRetryInstance;
    private final MeterRegistry meterRegistry;

    public MovementDataInventoryRepository(final MongoClient insertMongoClient, final MeterRegistry meterRegistry,
                                           @Value("${spring.data.mongodb.database}") final String databaseName,
                                           final InventoryValueMapper inventoryValueMapper, final CircuitBreakerRegistry circuitBreakerRegistry,
                                           final RetryRegistry retryRegistry) {
        this.movementDataInventoriesCollection =
                insertMongoClient.getDatabase(databaseName).getCollection(MOVEMENT_DATA_INVENTORIES, MovementDataInventory.class);
        this.inventoryValueMapper = inventoryValueMapper;
        this.afterMovementDataCircuitBreaker = circuitBreakerRegistry.circuitBreaker(ProcessingServiceConstants.AFTER_MOVEMENT_DATA_CIRCUIT_BREAKER);
        this.mongoRetryInstance = retryRegistry.retry(ProcessingServiceConstants.MONGODB_RETRY_INSTANCE_NAME);
        this.meterRegistry = meterRegistry;
    }

    /**
     * Deletes many MovementDataInventories  for consultant, client and fiscal year key
     *
     * @param consultant consultant number
     * @param client     client number
     * @param fiscalYear fiscal year start
     * @return mongo DeleteResult object
     */
    public Mono<DeleteResult> deleteManyByBusinessKey(Integer consultant, Integer client, Integer fiscalYear) {
        return Mono.from(movementDataInventoriesCollection.deleteMany(QueryUtil.deleteMovementData(consultant, client, fiscalYear)))
                   .elapsed().map(LoggingUtil.logDebugWithDuration(LoggingUtil.MOVEMENT_DATA_INVENTORY_REPOSITORY_DELETE_MANY_LOG));
    }

    /**
     * Bulk upsert MovementDataInventories
     *
     * @param movementDataInventories map of ExtendedMovementdataInventory objects grouped by DB key
     * @return mongo BulkWriteResult object
     */
    public Mono<BulkWriteResult> bulkUpsert(Map<InventoryDbKeyFields, List<ExtendedMovementdataInventory>> movementDataInventories) {
        return Mono.from(movementDataInventoriesCollection.bulkWrite(movementDataInventories.entrySet()
                                                                                            .stream()
                                                                                            .map(entry -> new UpdateOneModel<MovementDataInventory>(
                                                                                                    getOneMovementDataInventory(entry.getKey()),
                                                                                                    getMovementDataInventoryUpdate(entry.getValue()),
                                                                                                    QueryUtil.UPSERT_UPDATE_OPTIONS))
                                                                                            .toList()))
                   .elapsed().map(LoggingUtil.logTraceWithDuration(LoggingUtil.MOVEMENT_DATA_INVENTORY_REPOSITORY_BULK_UPSERT_LOG))
                   .name(MetricConstants.METRIC_MONGO)
                   .tag(MetricConstants.REPOSITORY, MOVEMENT_DATA_INVENTORIES)
                   .tag(MetricConstants.METHOD, "bulkUpsert")
                   .tag(MetricConstants.METRIC_TYPE, MetricConstants.METRIC_WRITE)
                   .tap(Micrometer.metrics(meterRegistry))
                   .transformDeferred(CircuitBreakerOperator.of(afterMovementDataCircuitBreaker))
                   .transformDeferred(RetryOperator.of(mongoRetryInstance));
    }

    private static Bson getOneMovementDataInventory(InventoryDbKeyFields inventoryDbKeyFields) {
        return Filters.and(
                Filters.eq(FieldConstants.CONSULTANT, inventoryDbKeyFields.getConsultant()),
                Filters.eq(FieldConstants.CLIENT, inventoryDbKeyFields.getClient()),
                Filters.eq(FieldConstants.FISCAL_YEAR, inventoryDbKeyFields.getFiscalYear()),
                Filters.eq(FieldConstants.ACCOUNT_NUMBER, inventoryDbKeyFields.getAccountNumber()),
                Filters.eq(FieldConstants.ANLAG_ACCOUNTING_REASON, inventoryDbKeyFields.getAccountingReason()));
    }

    private Bson getMovementDataInventoryUpdate(List<ExtendedMovementdataInventory> movementDataInventories) {
        Set<String> inventoryNumbers = new HashSet<>();
        return Updates.addEachToSet(FieldConstants.INVENTORY_VALUES,
                                    movementDataInventories.stream().map(extendedInventory -> {
                                        String inventoryNumber = extendedInventory.getInventoryNumber();
                                        boolean isNumberUnique = inventoryNumbers.add(inventoryNumber);
                                        if (!isNumberUnique) {
                                            log.error(ProcessingErrorMessageConstants.DUPLICATE_INVENTORY_NUMBER_ERROR, inventoryNumber);
                                        }
                                        return inventoryValueMapper.mapApiToDbModel(extendedInventory);
                                    }).toList());
    }
}
