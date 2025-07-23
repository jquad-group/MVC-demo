package de.datev.refsys.aggregation.processing.repository;

import com.mongodb.client.model.Updates;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import com.mongodb.client.ClientSession;
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
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.bson.conversions.Bson;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;
import org.springframework.util.StopWatch;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import static de.datev.refsys.aggregation.document.model.constants.CollectionConstants.MASTER_DATA;

/**
 * Repository class for operations with the MasterData mongo collection (MVC version)
 */
@Slf4j
@Repository
public class MasterDataRepositoryMvc {
    protected final MeterRegistry meterRegistry;
    private final MongoTemplate mongoTemplate;
    private final CircuitBreaker afterMovementDataCircuitBreaker;
    private final Retry mongoRetryInstance;
    private final String databaseName;

    public MasterDataRepositoryMvc(final MongoTemplate mongoTemplate, final MeterRegistry meterRegistry,
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
                Query query = new Query()
                    .addCriteria(Criteria.where(FieldConstants.CONSULTANT).is(consultant))
                    .addCriteria(Criteria.where(FieldConstants.CLIENT).is(client))
                    .addCriteria(Criteria.where(FieldConstants.YEAR_BEGIN).is(fiscalYear));
                
                DeleteResult result = mongoTemplate.remove(query, MasterData.class);
                
                // Manual timing and logging
                LoggingUtil.logDebugWithDuration(LoggingUtil.MASTER_DATA_REPOSITORY_DELETE_ONE_LOG)
                          .accept(stopWatch.getTotalTimeMillis());
                return result;
            } finally {
                stopWatch.stop();
            }
        };
        
        // Apply circuit breaker and retry with synchronous decorators
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
    public UpdateResult upsertOne(MasterDataContext mdc, List<CollectiveAccount> collectiveAccounts,
                                  List<ShareholderRelation> shareholderRelations, List<ShareholderAddition> shareholderAdditions,
                                  List<ShareholderAddress> shareholderAddresses, List<ShareholderTaxOffice> shareholderTaxOffices,
                                  List<PreviousYearAccountTranslation> previousYearTranslations,
                                  List<AlternativeAccountTranslation> alternativeAccountTranslations,
                                  Set<Integer> individualPersonAccountNumbers) {
                                  
        Supplier<UpdateResult> upsertOperation = () -> {
            StopWatch stopWatch = StopWatch.createStarted();
            Timer.Sample sample = Timer.start(meterRegistry);
            
            try {
                Query query = new Query()
                    .addCriteria(Criteria.where(FieldConstants.CONSULTANT).is(mdc.getConsultant()))
                    .addCriteria(Criteria.where(FieldConstants.CLIENT).is(mdc.getClient()))
                    .addCriteria(Criteria.where(FieldConstants.YEAR_BEGIN).is(mdc.getYearBegin()));

                Update update = buildMasterDataUpdate(mdc, collectiveAccounts, shareholderRelations, shareholderAdditions,
                                                    shareholderAddresses, shareholderTaxOffices, previousYearTranslations,
                                                    alternativeAccountTranslations, individualPersonAccountNumbers);

                UpdateResult result = mongoTemplate.upsert(query, update, MasterData.class);
                
                // Manual timing and logging
                LoggingUtil.logDebugWithDuration(LoggingUtil.MASTER_DATA_REPOSITORY_UPSERT_ONE_LOG)
                          .accept(stopWatch.getTotalTimeMillis());
                          
                // Manual metrics recording
                sample.stop(Timer.builder(MetricConstants.METRIC_MONGO)
                          .tag(MetricConstants.REPOSITORY, MASTER_DATA)
                          .tag(MetricConstants.METHOD, "upsertOne")
                          .tag(MetricConstants.METRIC_TYPE, MetricConstants.METRIC_WRITE)
                          .register(meterRegistry));
                          
                return result;
            } finally {
                stopWatch.stop();
            }
        };
        
        return afterMovementDataCircuitBreaker.executeSupplier(mongoRetryInstance.executeSupplier(upsertOperation));
    }

    /**
     * Finds one master data by business key
     *
     * @param consultant consultant number
     * @param client     client number
     * @param fiscalYear fiscal year start
     * @return a MasterData object
     */
    public MasterData findOneByBusinessKey(Integer consultant, Integer client, Integer fiscalYear) {
        Supplier<MasterData> findOperation = () -> {
            StopWatch stopWatch = StopWatch.createStarted();
            Timer.Sample sample = Timer.start(meterRegistry);
            
            try {
                Query query = new Query()
                    .addCriteria(Criteria.where(FieldConstants.CONSULTANT).is(consultant))
                    .addCriteria(Criteria.where(FieldConstants.CLIENT).is(client))
                    .addCriteria(Criteria.where(FieldConstants.YEAR_BEGIN).is(fiscalYear));
                
                MasterData result = mongoTemplate.findOne(query, MasterData.class);
                
                LoggingUtil.logDebugWithDuration(LoggingUtil.MASTER_DATA_REPOSITORY_FIND_ONE_LOG)
                          .accept(stopWatch.getTotalTimeMillis());
                          
                sample.stop(Timer.builder(MetricConstants.METRIC_MONGO)
                          .tag(MetricConstants.REPOSITORY, MASTER_DATA)
                          .tag(MetricConstants.METHOD, "findOneByBusinessKey")
                          .tag(MetricConstants.METRIC_TYPE, MetricConstants.METRIC_READ)
                          .register(meterRegistry));
                          
                return result;
            } finally {
                stopWatch.stop();
            }
        };
        
        return afterMovementDataCircuitBreaker.executeSupplier(mongoRetryInstance.executeSupplier(findOperation));
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
    public UpdateResult updateFromSchemaVersionOneToFour(Integer consultant, Integer client, Integer fiscalYear, boolean containsNearTimeData,
                                                         Integer industryId, List<CustomReportStructureInfo> customReportStructureInfos,
                                                         List<CustomColumnStructureInfo> customColumnStructureInfos) {
        Supplier<UpdateResult> updateOperation = () -> {
            StopWatch stopWatch = StopWatch.createStarted();
            Timer.Sample sample = Timer.start(meterRegistry);
            
            try {
                Query query = new Query()
                    .addCriteria(Criteria.where(FieldConstants.CONSULTANT).is(consultant))
                    .addCriteria(Criteria.where(FieldConstants.CLIENT).is(client))
                    .addCriteria(Criteria.where(FieldConstants.YEAR_BEGIN).is(fiscalYear));

                Update update = new Update()
                    .set(FieldConstants.CONTAINS_NEAR_TIME_DATA_UPDATE, containsNearTimeData);
                    
                if (industryId != null) {
                    update.set(FieldConstants.INDUSTRY_ID_UPDATE, industryId);
                }
                if (!customReportStructureInfos.isEmpty()) {
                    update.set(FieldConstants.CUSTOM_REPORT_STRUCTURE_INFOS_UPDATE, customReportStructureInfos);
                }
                if (!customColumnStructureInfos.isEmpty()) {
                    update.set(FieldConstants.CUSTOM_COLUMN_STRUCTURE_INFOS_UPDATE, customColumnStructureInfos);
                }

                UpdateResult result = mongoTemplate.updateFirst(query, update, MasterData.class);
                
                LoggingUtil.logDebugWithDuration(LoggingUtil.MASTER_DATA_REPOSITORY_UPDATE_FROM_SCHEMA_VERSION_ONE_TO_TWO_LOG)
                          .accept(stopWatch.getTotalTimeMillis());
                          
                sample.stop(Timer.builder(MetricConstants.METRIC_MONGO)
                          .tag(MetricConstants.REPOSITORY, MASTER_DATA)
                          .tag(MetricConstants.METHOD, "updateFromSchemaVersionOneToFour")
                          .tag(MetricConstants.METRIC_TYPE, MetricConstants.METRIC_WRITE)
                          .register(meterRegistry));
                          
                return result;
            } finally {
                stopWatch.stop();
            }
        };
        
        return afterMovementDataCircuitBreaker.executeSupplier(mongoRetryInstance.executeSupplier(updateOperation));
    }

    // Helper method to build update operations
    private Update buildMasterDataUpdate(MasterDataContext mdc, List<CollectiveAccount> collectiveAccounts,
                                        List<ShareholderRelation> shareholderRelations, List<ShareholderAddition> shareholderAdditions,
                                        List<ShareholderAddress> shareholderAddresses, List<ShareholderTaxOffice> shareholderTaxOffices,
                                        List<PreviousYearAccountTranslation> pyatList, List<AlternativeAccountTranslation> aatList,
                                        Set<Integer> individualPersonAccountNumbers) {
        Update update = new Update()
            .setOnInsert(FieldConstants.YEAR_BEGIN, mdc.getYearBegin())
            .setOnInsert(FieldConstants.YEAR_END, mdc.getYearEnd())
            .set(FieldConstants.CONTEXT, mdc);
            
        if (collectiveAccounts != null && !collectiveAccounts.isEmpty()) {
            update.set(FieldConstants.COLLECTIVE_ACCOUNTS, collectiveAccounts);
        }
        if (shareholderRelations != null && !shareholderRelations.isEmpty()) {
            update.set(FieldConstants.SHAREHOLDER_RELATIONS, shareholderRelations);
        }
        if (shareholderAdditions != null && !shareholderAdditions.isEmpty()) {
            update.set(FieldConstants.SHAREHOLDER_ADDITIONS, shareholderAdditions);
        }
        if (shareholderAddresses != null && !shareholderAddresses.isEmpty()) {
            update.set(FieldConstants.SHAREHOLDER_ADDRESSES, shareholderAddresses);
        }
        if (shareholderTaxOffices != null && !shareholderTaxOffices.isEmpty()) {
            update.set(FieldConstants.SHAREHOLDER_TAX_OFFICES, shareholderTaxOffices);
        }
        if (pyatList != null && !pyatList.isEmpty()) {
            update.set(FieldConstants.PREVIOUS_YEAR_ACCOUNT_TRANSLATIONS, pyatList);
        }
        if (aatList != null && !aatList.isEmpty()) {
            update.set(FieldConstants.ALTERNATIVE_ACCOUNT_TRANSLATIONS, aatList);
        }
        if (individualPersonAccountNumbers != null && !individualPersonAccountNumbers.isEmpty()) {
            update.set(FieldConstants.INDIVIDUAL_PERSON_ACCOUNT_NUMBERS, individualPersonAccountNumbers);
        }
        
        return update;
    }
}