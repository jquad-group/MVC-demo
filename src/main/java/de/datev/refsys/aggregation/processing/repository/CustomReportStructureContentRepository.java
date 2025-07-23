package de.datev.refsys.aggregation.processing.repository;

import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.model.InsertOneModel;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoCollection;
import de.datev.refsys.aggregation.document.model.CustomReportStructureContent;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import reactor.core.observability.micrometer.Micrometer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

import static de.datev.refsys.aggregation.document.model.constants.CollectionConstants.CUSTOM_REPORT_STRUCTURE_CONTENTS;

/**
 * Repository class for operations with the CustomReportStructureContents mongo collection
 */
@Slf4j
@Repository
public class CustomReportStructureContentRepository {
    private final MongoCollection<CustomReportStructureContent> insertCustomReportStructureCollection;
    private final CircuitBreaker afterMovementDataCircuitBreaker;
    private final Retry mongoRetryInstance;
    private final MeterRegistry meterRegistry;

    public CustomReportStructureContentRepository(final MongoClient insertMongoClient, final MeterRegistry meterRegistry,
                                                  @Value("${spring.data.mongodb.database}") final String databaseName,
                                                  final CircuitBreakerRegistry circuitBreakerRegistry, final RetryRegistry retryRegistry) {
        this.insertCustomReportStructureCollection =
                insertMongoClient.getDatabase(databaseName).getCollection(CUSTOM_REPORT_STRUCTURE_CONTENTS, CustomReportStructureContent.class);
        this.afterMovementDataCircuitBreaker = circuitBreakerRegistry.circuitBreaker(ProcessingServiceConstants.AFTER_MOVEMENT_DATA_CIRCUIT_BREAKER);
        this.mongoRetryInstance = retryRegistry.retry(ProcessingServiceConstants.MONGODB_RETRY_INSTANCE_NAME);
        this.meterRegistry = meterRegistry;
    }

    /**
     * Deletes many CustomReportStructures  for consultant, client, yearBegin key
     *
     * @param consultant consultant
     * @param client     client
     * @param yearBegin  yearBegin
     * @return Flux<DeleteResult>
     */
    public Mono<DeleteResult> deleteManyByBusinessKey(Integer consultant, Integer client, Integer yearBegin) {
        return Mono.from(insertCustomReportStructureCollection.deleteMany(QueryUtil.getByMasterDataBusinessKey(consultant, client, yearBegin)))
                   .elapsed().map(LoggingUtil.logDebugWithDuration(LoggingUtil.CUSTOM_REPORT_STRUCTURE_REPOSITORY_DELETE_MANY_LOG));
    }

    /**
     * Bulk insert CustomReportStructureContents
     *
     * @param customReportStructureContentSet Set of CustomReportStructureContents to be inserted
     * @return mongo BulkWriteResult object
     */
    public Mono<BulkWriteResult> bulkInsert(List<CustomReportStructureContent> customReportStructureContentSet) {
        return Mono.from(insertCustomReportStructureCollection.bulkWrite(customReportStructureContentSet.stream().map(InsertOneModel::new).toList()))
                   .elapsed().map(LoggingUtil.logDebugWithDuration(LoggingUtil.CUSTOM_REPORT_STRUCTURE_REPOSITORY_BULK_INSERT_LOG))
                   .name(MetricConstants.METRIC_MONGO)
                   .tag(MetricConstants.REPOSITORY, CUSTOM_REPORT_STRUCTURE_CONTENTS)
                   .tag(MetricConstants.METHOD, "bulkInsert")
                   .tag(MetricConstants.METRIC_TYPE, MetricConstants.METRIC_WRITE)
                   .tap(Micrometer.metrics(meterRegistry))
                   .transformDeferred(CircuitBreakerOperator.of(afterMovementDataCircuitBreaker))
                   .transformDeferred(RetryOperator.of(mongoRetryInstance));
    }
}
