package de.datev.refsys.aggregation.processing.repository;

import com.mongodb.client.model.Updates;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import com.mongodb.ClientSessionOptions;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import de.datev.refsys.aggregation.document.model.AlternativeAccountTranslation;
import de.datev.refsys.aggregation.document.model.CollectiveAccount;
import de.datev.refsys.aggregation.document.model.CustomColumnStructureInfo;
import de.datev.refsys.aggregation.document.model.CustomReportStructureInfo;
import de.datev.refsys.aggregation.document.model.MasterData;
import de.datev.refsys.aggregation.document.model.MasterDataContext;
import de.datev.refsys.aggregation.document.model.PreviousYearAccountTranslation;
import de.datev.refsys.aggregation.document.model.ShareholderAddition;
import de.datev.refsys.aggregation.document.model.ShareholderAddress;
import de.datev.refsys.aggregation.document.model.ShareholderRelation;
import de.datev.refsys.aggregation.document.model.ShareholderTaxOffice;
import de.datev.refsys.aggregation.document.model.constants.FieldConstants;
import de.datev.refsys.aggregation.processing.constant.MetricConstants;
import de.datev.refsys.aggregation.processing.constant.ProcessingServiceConstants;
import de.datev.refsys.aggregation.processing.util.LoggingUtil;
import de.datev.refsys.aggregation.processing.util.QueryUtil;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.bson.conversions.Bson;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.util.StopWatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static de.datev.refsys.aggregation.document.model.constants.CollectionConstants.MASTER_DATA;

/**
 * Repository class for operations with the MasterData mongo collection
 */
@Slf4j
@Repository
public class MasterDataRepository {
    protected final MeterRegistry meterRegistry;
    private final MongoTemplate mongoTemplate;
    private final CircuitBreaker afterMovementDataCircuitBreaker;
    private final Retry mongoRetryInstance;
    private final String databaseName;

    public MasterDataRepository(final MongoTemplate mongoTemplate, final MeterRegistry meterRegistry,
                                @Value("${spring.data.mongodb.database}") final String databaseName,
                                final CircuitBreakerRegistry circuitBreakerRegistry, final RetryRegistry retryRegistry) {
        this.databaseName = databaseName;
        this.mongoTemplate = mongoTemplate;
        this.afterMovementDataCircuitBreaker = circuitBreakerRegistry.circuitBreaker(ProcessingServiceConstants.AFTER_MOVEMENT_DATA_CIRCUIT_BREAKER);
        this.mongoRetryInstance = retryRegistry.retry(ProcessingServiceConstants.MONGODB_RETRY_INSTANCE_NAME);
        this.meterRegistry = meterRegistry;
    }

    /**
     * Deletes one MasterData for consultant, client and fiscal year key
     *
     * @param consultant consultant number
     * @param client     client number
     * @param fiscalYear fiscal year start
     * @return mongo DeleteResult object
     */
    public DeleteResult deleteOne(Integer consultant, Integer client, Integer fiscalYear) {
        Supplier<DeleteResult> deleteOperation = () -> {
            StopWatch stopWatch = StopWatch.createStarted();
            try {
                Query query = new Query().addCriteria(QueryUtil.getByMasterDataBusinessKey(consultant, client, fiscalYear));
                DeleteResult result = mongoTemplate.remove(query, MasterData.class);
                LoggingUtil.logDebugWithDuration(LoggingUtil.MASTER_DATA_REPOSITORY_DELETE_ONE_LOG)
                          .accept(stopWatch.getTotalTimeMillis());
                return result;
            } finally {
                stopWatch.stop();
            }
        };
        
        return afterMovementDataCircuitBreaker.executeSupplier(mongoRetryInstance.executeSupplier(deleteOperation));
    }

    /**
     * Updates one MasterData for consultant, client and fiscal year key
     *
     * @param mdc                            MasterDataContext object
     * @param collectiveAccounts             CollectiveAccounts list
     * @param shareholderRelations           ShareholderRelations list
     * @param shareholderAdditions           ShareholderAdditions list
     * @param shareholderAddresses           ShareholderAddresses list
     * @param shareholderTaxOffices          ShareholderTaxOffices list
     * @param previousYearTranslations       PreviousYearAccountTranslation list
     * @param alternativeAccountTranslations AlternativeAccountTranslation list
     * @param individualPersonAccountNumbers individualPersonAccountNumbers list
     * @return mongo UpdateResult object
     */
    // TODO replace this method with insertOne method and use it during the initialLoad
    public Mono<UpdateResult> upsertOne(MasterDataContext mdc, List<CollectiveAccount> collectiveAccounts,
                                        List<ShareholderRelation> shareholderRelations, List<ShareholderAddition> shareholderAdditions,
                                        List<ShareholderAddress> shareholderAddresses, List<ShareholderTaxOffice> shareholderTaxOffices,
                                        List<PreviousYearAccountTranslation> previousYearTranslations,
                                        List<AlternativeAccountTranslation> alternativeAccountTranslations,
                                        Set<Integer> individualPersonAccountNumbers) {
        return Mono.from(
                           masterDataCollection.updateOne(QueryUtil.getByMasterDataBusinessKey(mdc.getConsultant(), mdc.getClient(),
                                                                                               mdc.getYearBegin()),
                                                          getMasterDataUpdate(mdc, collectiveAccounts, shareholderRelations, shareholderAdditions,
                                                                              shareholderAddresses, shareholderTaxOffices, previousYearTranslations,
                                                                              alternativeAccountTranslations, individualPersonAccountNumbers),
                                                          QueryUtil.UPSERT_UPDATE_OPTIONS))
                   .elapsed().map(LoggingUtil.logDebugWithDuration(LoggingUtil.MASTER_DATA_REPOSITORY_UPSERT_ONE_LOG))
                   .name(MetricConstants.METRIC_MONGO)
                   .tag(MetricConstants.REPOSITORY, MASTER_DATA)
                   .tag(MetricConstants.METHOD, "upsertOne")
                   .tag(MetricConstants.METRIC_TYPE, MetricConstants.METRIC_WRITE)
                   .tap(Micrometer.metrics(meterRegistry))
                   .transformDeferred(CircuitBreakerOperator.of(afterMovementDataCircuitBreaker))
                   .transformDeferred(RetryOperator.of(mongoRetryInstance));
    }

    public Mono<UpdateResult> writeNearTimeDataFlag(Integer consultant, Integer client, Integer fiscalYear, ClientSession clientSession,
                                                    MongoClient mongoClient,
                                                    boolean value) {
        // WORKAROUND - session uses different client - TODO: performance messen! Get Collection ist langsam.
        MongoCollection<MasterData> transactionEnabledmasterDataCollection =
                mongoClient.getDatabase(databaseName).getCollection(MASTER_DATA, MasterData.class);
        return Mono.from(
                           transactionEnabledmasterDataCollection.updateOne(clientSession, QueryUtil.getByMasterDataBusinessKey(consultant, client,
                                                                                                                                fiscalYear),
                                                                            //TODO: Update in Field Constants im Model
                                                                            Updates.set("context.contains_near_time_data", value)))
                   .elapsed().map(LoggingUtil.logDebugWithDuration(LoggingUtil.MASTER_DATA_REPOSITORY_WRITE_NEAR_TIME_DATA_FLAG_LOG))
                   .name(MetricConstants.METRIC_MONGO)
                   .tag(MetricConstants.REPOSITORY, MASTER_DATA)
                   .tag(MetricConstants.METHOD, "writeNearTimeDataFlag")
                   .tag(MetricConstants.METRIC_TYPE, MetricConstants.METRIC_WRITE)
                   .tap(Micrometer.metrics(meterRegistry));
    }

    /**
     * Finds one master data by business key
     *
     * @param consultant consultant number
     * @param client     client number
     * @param fiscalYear fiscal year start
     * @return a MasterData object
     */
    public Mono<MasterData> findOneByBusinessKey(Integer consultant, Integer client, Integer fiscalYear) {
        return Mono.from(masterDataCollection.find(QueryUtil.getByMasterDataBusinessKey(consultant, client, fiscalYear)))
                   .elapsed().map(LoggingUtil.logDebugWithDuration(LoggingUtil.MASTER_DATA_REPOSITORY_FIND_ONE_LOG))
                   .name(MetricConstants.METRIC_MONGO)
                   .tag(MetricConstants.REPOSITORY, MASTER_DATA)
                   .tag(MetricConstants.METHOD, "findOneByBusinessKey")
                   .tag(MetricConstants.METRIC_TYPE, MetricConstants.METRIC_READ)
                   .tap(Micrometer.metrics(meterRegistry));
    }

    /**
     * Update the value for the containsNearTimeData, industryId, customReportStructureInfos and customColumnStructureInfos fields in MasterDataContext
     *
     * @param consultant           consultant number
     * @param client               client number
     * @param fiscalYear           fiscal year start
     * @param containsNearTimeData boolean flag from master data context
     * @return mongo UpdateResult object
     */
    public Mono<UpdateResult> updateFromSchemaVersionOneToFour(Integer consultant, Integer client, Integer fiscalYear, boolean containsNearTimeData,
                                                               Integer industryId, List<CustomReportStructureInfo> customReportStructureInfos,
                                                               List<CustomColumnStructureInfo> customColumnStructureInfos) {
        List<Bson> updates = new ArrayList<>();
        updates.add(Updates.set(FieldConstants.CONTAINS_NEAR_TIME_DATA_UPDATE, containsNearTimeData));
        if (industryId != null) {
            updates.add(Updates.set(FieldConstants.INDUSTRY_ID_UPDATE, industryId));
        }
        if (!customReportStructureInfos.isEmpty()) {
            updates.add(Updates.set(FieldConstants.CUSTOM_REPORT_STRUCTURE_INFOS_UPDATE, customReportStructureInfos));
        }
        if (!customColumnStructureInfos.isEmpty()) {
            updates.add(Updates.set(FieldConstants.CUSTOM_COLUMN_STRUCTURE_INFOS_UPDATE, customColumnStructureInfos));
        }

        return Mono.from(masterDataCollection.updateOne(QueryUtil.getByMasterDataBusinessKey(consultant, client, fiscalYear), updates))
                   .elapsed()
                   .map(LoggingUtil.logDebugWithDuration(LoggingUtil.MASTER_DATA_REPOSITORY_UPDATE_FROM_SCHEMA_VERSION_ONE_TO_TWO_LOG))
                   .name(MetricConstants.METRIC_MONGO)
                   .tag(MetricConstants.REPOSITORY, MASTER_DATA)
                   .tag(MetricConstants.METHOD, "updateFromSchemaVersionOneToFour")
                   .tag(MetricConstants.METRIC_TYPE, MetricConstants.METRIC_WRITE)
                   .tap(Micrometer.metrics(meterRegistry));
    }

    // TODO remove after insertOne method has been implemented
    private static Bson getMasterDataUpdate(MasterDataContext mdc, List<CollectiveAccount> collectiveAccounts,
                                            List<ShareholderRelation> shareholderRelations, List<ShareholderAddition> shareholderAdditions,
                                            List<ShareholderAddress> shareholderAddresses, List<ShareholderTaxOffice> shareholderTaxOffices,
                                            List<PreviousYearAccountTranslation> pyatList, List<AlternativeAccountTranslation> aatList,
                                            Set<Integer> individualPersonAccountNumbers) {
        List<Bson> updates = new ArrayList<>();
        updates.add(Updates.setOnInsert(FieldConstants.YEAR_BEGIN, mdc.getYearBegin()));
        updates.add(Updates.setOnInsert(FieldConstants.YEAR_END, mdc.getYearEnd()));
        updates.add(Updates.set(FieldConstants.CONTEXT, mdc));
        if (collectiveAccounts != null && !collectiveAccounts.isEmpty()) {
            updates.add(Updates.set(FieldConstants.COLLECTIVE_ACCOUNTS, collectiveAccounts));
        }
        if (shareholderRelations != null && !shareholderRelations.isEmpty()) {
            updates.add(Updates.set(FieldConstants.SHAREHOLDER_RELATIONS, shareholderRelations));
        }
        if (shareholderAdditions != null && !shareholderAdditions.isEmpty()) {
            updates.add(Updates.set(FieldConstants.SHAREHOLDER_ADDITIONS, shareholderAdditions));
        }
        if (shareholderAddresses != null && !shareholderAddresses.isEmpty()) {
            updates.add(Updates.set(FieldConstants.SHAREHOLDER_ADDRESSES, shareholderAddresses));
        }
        if (shareholderTaxOffices != null && !shareholderTaxOffices.isEmpty()) {
            updates.add(Updates.set(FieldConstants.SHAREHOLDER_TAX_OFFICES, shareholderTaxOffices));
        }
        if (pyatList != null && !pyatList.isEmpty()) {
            updates.add(Updates.set(FieldConstants.PREVIOUS_YEAR_ACCOUNT_TRANSLATIONS, pyatList));
        }
        if (aatList != null && !aatList.isEmpty()) {
            updates.add(Updates.set(FieldConstants.ALTERNATIVE_ACCOUNT_TRANSLATIONS, aatList));
        }
        if (individualPersonAccountNumbers != null && !individualPersonAccountNumbers.isEmpty()) {
            updates.add(Updates.set(FieldConstants.INDIVIDUAL_PERSON_ACCOUNT_NUMBERS, individualPersonAccountNumbers));
        }
        return Updates.combine(updates);
    }

    public Mono<UpdateResult> addIndividualPersonAccounts(ClientSession session, Integer consultant, Integer client, Integer fiscalYear,
                                                          Set<Integer> additionalIndividualPersonAccounts) {
        return Mono.from(transactionalMasterDataCollection.updateOne(session, QueryUtil.getByMasterDataBusinessKey(consultant, client, fiscalYear),
                                                                     Updates.addEachToSet(FieldConstants.INDIVIDUAL_PERSON_ACCOUNT_NUMBERS,
                                                                                          additionalIndividualPersonAccounts.stream().toList())))
                   .elapsed()
                   .map(LoggingUtil.logDebugWithDuration(LoggingUtil.MASTER_DATA_REPOSITORY_ADD_INDIVIDUAL_PERSON_ACCOUNTS_LOG))
                   .name("addIndividualPersonAccounts")
                   .tag(MetricConstants.METRIC_TYPE, MetricConstants.METRIC_WRITE)
                   .tap(Micrometer.metrics(meterRegistry));
    }

    /**
     * Unsets the individual_person_account_numbers field
     *
     * @param clientSession mongo client session
     * @param consultant    consultant number
     * @param client        client number
     * @param fiscalYear    fiscal year start
     * @return mongo UpdateResult object
     */
    public Mono<UpdateResult> unsetIndividualPersonAccounts(ClientSession clientSession, Integer consultant, Integer client, Integer fiscalYear) {
        return Mono.from(
                           transactionalMasterDataCollection.updateOne(clientSession, QueryUtil.getByMasterDataBusinessKey(consultant, client,
                                                                                                                           fiscalYear),
                                                                       Updates.unset(FieldConstants.INDIVIDUAL_PERSON_ACCOUNT_NUMBERS)))
                   .elapsed()
                   .map(LoggingUtil.logDebugWithDuration(LoggingUtil.MASTER_DATA_REPOSITORY_UNSET_INDIVIDUAL_PERSON_ACCOUNTS_LOG))
                   .name("unsetIndividualPersonAccounts")
                   .tag(MetricConstants.METRIC_TYPE, MetricConstants.METRIC_WRITE)
                   .tap(Micrometer.metrics(meterRegistry));
    }
}
