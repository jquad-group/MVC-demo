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
import de.datev.refsys.aggregation.document.model.AccountDescription;
import de.datev.refsys.aggregation.document.model.Description;
import de.datev.refsys.aggregation.document.model.Inventory;
import de.datev.refsys.aggregation.document.model.MasterDataAccount;
import de.datev.refsys.aggregation.document.model.constants.FieldConstants;
import de.datev.refsys.aggregation.processing.constant.MetricConstants;
import de.datev.refsys.aggregation.processing.constant.ProcessingErrorMessageConstants;
import de.datev.refsys.aggregation.processing.constant.ProcessingServiceConstants;
import de.datev.refsys.aggregation.processing.exception.AggregationProcessingBusinessException;
import de.datev.refsys.aggregation.processing.mapper.AccountPurposeMapper;
import de.datev.refsys.aggregation.processing.mapper.InventoryMapper;
import de.datev.refsys.aggregation.processing.util.LoggingUtil;
import de.datev.refsys.aggregation.processing.util.QueryUtil;
import de.datev.refsys.generated.acds.api.model.AccountCaption;
import de.datev.refsys.generated.acds.api.model.AccountPurposeMapping;
import de.datev.refsys.generated.acds.api.model.MasterdataContext;
import de.datev.refsys.generated.acds.api.model.MasterdataInventory;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import org.bson.conversions.Bson;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import reactor.core.observability.micrometer.Micrometer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static de.datev.refsys.aggregation.document.model.constants.CollectionConstants.MASTER_DATA_ACCOUNTS;
import static java.util.stream.Collectors.groupingBy;

/**
 * Repository class for operations with the MasterDataAccounts mongo collection
 */
@Repository
public class MasterDataAccountRepository {
    private final MongoCollection<MasterDataAccount> insertMasterDataAccountCollection;
    private final MongoCollection<MasterDataAccount> updateMasterDataAccountCollection;
    private final CircuitBreaker afterMovementDataCircuitBreaker;
    private final Retry mongoRetryInstance;
    private final AccountPurposeMapper accountPurposeMapper;
    private final InventoryMapper inventoryMapper;
    private final MeterRegistry meterRegistry;

    public MasterDataAccountRepository(final MongoClient insertMongoClient,
                                       final MongoClient updateMongoClient,
                                       final MeterRegistry meterRegistry,
                                       final AccountPurposeMapper accountPurposeMapper,
                                       final InventoryMapper inventoryMapper,
                                       @Value("${spring.data.mongodb.database}") final String databaseName,
                                       final CircuitBreakerRegistry circuitBreakerRegistry, final RetryRegistry retryRegistry) {
        this.insertMasterDataAccountCollection = insertMongoClient.getDatabase(databaseName)
                .getCollection(MASTER_DATA_ACCOUNTS, MasterDataAccount.class);
        this.updateMasterDataAccountCollection = updateMongoClient.getDatabase(databaseName)
                .getCollection(MASTER_DATA_ACCOUNTS, MasterDataAccount.class);
        this.afterMovementDataCircuitBreaker = circuitBreakerRegistry.circuitBreaker(ProcessingServiceConstants.AFTER_MOVEMENT_DATA_CIRCUIT_BREAKER);
        this.mongoRetryInstance = retryRegistry.retry(ProcessingServiceConstants.MONGODB_RETRY_INSTANCE_NAME);
        this.accountPurposeMapper = accountPurposeMapper;
        this.inventoryMapper = inventoryMapper;
        this.meterRegistry = meterRegistry;
    }

    /**
     * Deletes many MasterDataAccounts for consultant, client and fiscal year key
     *
     * @param consultant consultant number
     * @param client     client number
     * @param fiscalYear fiscal year start
     * @return mongo DeleteResult flux
     */
    public Mono<DeleteResult> deleteManyByBusinessKey(Integer consultant, Integer client, Integer fiscalYear) {
        return Mono.from(insertMasterDataAccountCollection.deleteMany(QueryUtil.getByMasterDataBusinessKey(consultant, client, fiscalYear)))
                   .elapsed().map(LoggingUtil.logDebugWithDuration(LoggingUtil.MASTER_DATA_ACCOUNT_REPOSITORY_DELETE_MANY_LOG));
    }

    /**
     * Returns MasterDataAccounts for updated account numbers and consultant, client and fiscal year key
     *
     * @param consultant     consultant number
     * @param client         client number
     * @param fiscalYear     fiscal year start
     * @param accountNumbers updated account numbers
     * @return MasterDataAccounts flux
     */
    public Flux<MasterDataAccount> findAllByBusinessKeyAndAccountNumbers(ClientSession session, Integer consultant, Integer client,
                                                                         Integer fiscalYear, Set<Integer> accountNumbers) {
        return Flux.from(updateMasterDataAccountCollection.find(session, getMasterDataAccountsForAccountNumbers(consultant, client, fiscalYear,
                                                                                                                accountNumbers)))
                   .elapsed().map(LoggingUtil.logDebugWithDuration(LoggingUtil.MASTER_DATA_ACCOUNT_REPOSITORY_FIND_ALL_LOG))
                   .name(MetricConstants.METRIC_MONGO)
                   .tag(MetricConstants.REPOSITORY, MASTER_DATA_ACCOUNTS)
                   .tag(MetricConstants.METHOD, "findAllByBusinessKeyAndAccountNumbers")
                   .tag(MetricConstants.METRIC_TYPE, MetricConstants.METRIC_READ)
                   .tap(Micrometer.metrics(meterRegistry));
    }

    /**
     * Bulk insert MasterDataAccounts
     *
     * @param mdc                    MasterdataContext object from ACDS
     * @param accountPurposeMappings AccountPurposeMapping list from ACDS
     * @param accountCaptions        AccountCaption list from ACDS
     * @param inventories            MasterdataInventory list from ACDS
     * @param usedAccountNumbers     booked account numbers list
     * @return mongo BulkWriteResult object
     */
    public Mono<BulkWriteResult> bulkInsert(MasterdataContext mdc, List<AccountPurposeMapping> accountPurposeMappings,
                                            List<AccountCaption> accountCaptions,
                                            List<MasterdataInventory> inventories,
                                            Set<Integer> usedAccountNumbers) {
        return Mono.from(insertMasterDataAccountCollection.bulkWrite(getMasterDataAccountInserts(mdc, accountPurposeMappings, accountCaptions,
                                                                                                 inventories, usedAccountNumbers)))
                   .elapsed().map(LoggingUtil.logDebugWithDuration(LoggingUtil.MASTER_DATA_ACCOUNT_REPOSITORY_BULK_INSERT_LOG))
                   .name(MetricConstants.METRIC_MONGO)
                   .tag(MetricConstants.REPOSITORY, MASTER_DATA_ACCOUNTS)
                   .tag(MetricConstants.METHOD, "bulkInsert")
                   .tag(MetricConstants.METRIC_TYPE, MetricConstants.METRIC_WRITE)
                   .tap(Micrometer.metrics(meterRegistry))
                   .transformDeferred(CircuitBreakerOperator.of(afterMovementDataCircuitBreaker))
                   .transformDeferred(RetryOperator.of(mongoRetryInstance));
    }

    /**
     * Updates account captions used flag to true for new account numbers
     *
     * @param masterDataAccounts list of MasterDataAccounts to be updated
     * @return mongo BulkWriteResult object
     */
    public Mono<BulkWriteResult> bulkUpdate(List<MasterDataAccount> masterDataAccounts) {
        return Mono.from(updateMasterDataAccountCollection.bulkWrite(masterDataAccounts.stream()
                                                                                       .map(mda -> new UpdateOneModel<MasterDataAccount>(
                                                                                               getOneMasterDataAccount(mda.getConsultant(),
                                                                                                                       mda.getClient(),
                                                                                                                       mda.getYearBegin(),
                                                                                                                       mda.getYearEnd(),
                                                                                                                       mda.getAccountNumberFrom(),
                                                                                                                       mda.getAccountNumberTo()),
                                                                                               updateMasterDataAccount(mda.getAccountCaptions())))
                                                                                       .toList()))
                   .elapsed().map(LoggingUtil.logDebugWithDuration(LoggingUtil.MASTER_DATA_ACCOUNT_REPOSITORY_BULK_UPDATE_LOG))
                   .name(MetricConstants.METRIC_MONGO)
                   .tag(MetricConstants.REPOSITORY, MASTER_DATA_ACCOUNTS)
                   .tag(MetricConstants.METHOD, "bulkUpdate")
                   .tag(MetricConstants.METRIC_TYPE, MetricConstants.METRIC_WRITE)
                   .tap(Micrometer.metrics(meterRegistry));
    }

    private static Bson getMasterDataAccountsForAccountNumbers(Integer consultant, Integer client, Integer fiscalYear, Set<Integer> accountNumbers) {
        List<Bson> query = new ArrayList<>();
        query.add(Filters.eq(FieldConstants.CONSULTANT, consultant));
        query.add(Filters.eq(FieldConstants.CLIENT, client));
        query.add(Filters.eq(FieldConstants.YEAR_BEGIN, fiscalYear));
        query.add(Filters.or(accountNumbers.stream()
                .map(accountNumber -> Filters.and(Filters.lte(FieldConstants.ACCOUNT_NUMBER_FROM, accountNumber),
                        Filters.gte(FieldConstants.ACCOUNT_NUMBER_TO, accountNumber)))
                .toList()));
        return Filters.and(query);
    }

    private static Bson getOneMasterDataAccount(Integer consultant, Integer client, Integer yearBegin, Integer yearEnd,
                                                Integer accountNumberFrom, Integer accountNumberTo) {
        return Filters.and(List.of(
                Filters.eq(FieldConstants.CONSULTANT, consultant),
                Filters.eq(FieldConstants.CLIENT, client),
                Filters.eq(FieldConstants.YEAR_BEGIN, yearBegin),
                Filters.eq(FieldConstants.YEAR_END, yearEnd),
                Filters.eq(FieldConstants.ACCOUNT_NUMBER_FROM, accountNumberFrom),
                Filters.eq(FieldConstants.ACCOUNT_NUMBER_TO, accountNumberTo)));
    }

    private static Bson updateMasterDataAccount(List<AccountDescription> accountDescriptions) {
        return Updates.combine(Updates.set(FieldConstants.USED, true),
                Updates.set(FieldConstants.ACCOUNT_CAPTIONS, accountDescriptions));
    }

    private List<InsertOneModel<MasterDataAccount>> getMasterDataAccountInserts(MasterdataContext mdc, List<AccountPurposeMapping> apmList,
                                                                                List<AccountCaption> acList,
                                                                                List<MasterdataInventory> inventories,
                                                                                Set<Integer> usedAccountNumbers) {
        List<InsertOneModel<MasterDataAccount>> masterDataAccountInserts = new ArrayList<>();
        Map<Integer, List<AccountCaption>> groupedAccountCaptions = acList.stream().collect(groupingBy(AccountCaption::getAccountNumber));
        Map<Integer, List<MasterdataInventory>> groupedInventories = inventories.stream().collect(groupingBy(MasterdataInventory::getAccountNumber));

        apmList.forEach(apm -> addMasterDataAccountInserts(mdc, usedAccountNumbers, apm, groupedAccountCaptions, groupedInventories,
                                                           masterDataAccountInserts));
        if (!usedAccountNumbers.isEmpty()) {
            addNewAccountNumbers(usedAccountNumbers, masterDataAccountInserts);
        }
        return masterDataAccountInserts;
    }

    private static void addNewAccountNumbers(Set<Integer> usedAccountNumbers, List<InsertOneModel<MasterDataAccount>> masterDataAccountInserts) {
        masterDataAccountInserts.forEach(mda -> {
            List<Integer> usedAccountNumberList = usedAccountNumbers.stream().filter(uan -> accountNumberBetweenFromAndTo(uan,
                    mda.getDocument().getAccountNumberFrom(), mda.getDocument().getAccountNumberTo())).toList();
            if (!usedAccountNumberList.isEmpty()) {
                mda.getDocument().setUsed(true);
                mda.getDocument().getAccountCaptions().addAll(usedAccountNumberList.stream()
                        .map(usedAccountNumber -> createAccountDescription(usedAccountNumber, true)).toList()
                );
            }
        });
    }

    private void addMasterDataAccountInserts(MasterdataContext mdc, Set<Integer> usedAccountNumbers, AccountPurposeMapping apm,
                                             Map<Integer, List<AccountCaption>> groupedAccountCaptions,
                                             Map<Integer, List<MasterdataInventory>> groupedInventories,
                                             List<InsertOneModel<MasterDataAccount>> masterDataAccountInserts) {
        if (apm.getAccountNumberFrom() > apm.getAccountNumberTo()) {
            throw new AggregationProcessingBusinessException(ProcessingErrorMessageConstants.ACCOUNT_NUMBER_CHECK_ERROR,
                                                             HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        MasterDataAccount.MasterDataAccountBuilder masterDataAccountBuilder = createMasterDataAccountsBuilder(mdc, apm);
        List<Integer> containedAccountNumbers = usedAccountNumbers.stream()
                .filter(accountNumber -> accountNumberBetweenFromAndTo(accountNumber, apm.getAccountNumberFrom(), apm.getAccountNumberTo()))
                .collect(Collectors.toCollection(ArrayList::new));
        if (!containedAccountNumbers.isEmpty()) {
            masterDataAccountBuilder.used(true);
            masterDataAccountBuilder.accountCaptions(
                    groupedAccountCaptions.entrySet()
                                          .stream()
                                          .filter(entry -> accountNumberBetweenFromAndTo(entry.getKey(), apm.getAccountNumberFrom(),
                                                                                         apm.getAccountNumberTo()))
                                          .map(entry -> mapAccountDescription(usedAccountNumbers, entry, containedAccountNumbers))
                                          .collect(Collectors.toCollection(ArrayList::new)));
            List<Inventory> inventories =
                    groupedInventories.entrySet()
                                      .stream()
                                      .filter(entry -> accountNumberBetweenFromAndTo(entry.getKey(), apm.getAccountNumberFrom(),
                                                                                     apm.getAccountNumberTo()))
                                      .map(this::mapInventories)
                                      .flatMap(Collection::stream)
                                      .collect(Collectors.toCollection(ArrayList::new));
            if (!inventories.isEmpty()) {
                masterDataAccountBuilder.inventories(inventories);
            }
        } else {
            List<AccountDescription> accountCaptions = groupedAccountCaptions.entrySet().stream()
                    .filter(entry -> accountNumberBetweenFromAndTo(entry.getKey(), apm.getAccountNumberFrom(), apm.getAccountNumberTo()))
                    .map(entry -> createAccountDescription(entry.getKey(), entry.getValue()))
                    .toList();
            if (!accountCaptions.isEmpty()) {
                masterDataAccountBuilder.accountCaptions(accountCaptions);
            }
        }
        masterDataAccountInserts.add(new InsertOneModel<>(masterDataAccountBuilder.build()));
    }

    private static AccountDescription mapAccountDescription(Set<Integer> usedAccountNumbers, Map.Entry<Integer, List<AccountCaption>> entry,
                                                            List<Integer> containedAccountNumbers) {
        if (containedAccountNumbers.contains(entry.getKey())) {
            usedAccountNumbers.remove(entry.getKey());
            return createAccountDescription(entry.getKey(),
                                            entry.getValue(), true);
        }
        return createAccountDescription(entry.getKey(),
                                        entry.getValue());
    }

    private List<Inventory> mapInventories(Map.Entry<Integer, List<MasterdataInventory>> entry) {
        return entry.getValue()
                    .stream()
                    .map(inventoryMapper::masterdataInventoryToInventory)
                    .toList();
    }

    private static boolean accountNumberBetweenFromAndTo(Integer accountNumber, Integer accountNumberFrom, Integer accountNumberTo) {
        return accountNumber >= accountNumberFrom && accountNumber <= accountNumberTo;
    }

    private MasterDataAccount.MasterDataAccountBuilder createMasterDataAccountsBuilder(MasterdataContext mdc, AccountPurposeMapping apm) {
        return MasterDataAccount.builder()
                .consultant(mdc.getConsultant())
                .client(mdc.getClient())
                .yearBegin(mdc.getYearBegin())
                .yearEnd(mdc.getYearEnd())
                .accountNumberFrom(apm.getAccountNumberFrom())
                .accountNumberTo(apm.getAccountNumberTo())
                .purpose(accountPurposeMapper.mapAcdsToDb(apm));
    }

    private static AccountDescription createAccountDescription(Integer accountNumber, List<AccountCaption> accountCaptions) {
        return createAccountDescription(accountNumber, accountCaptions, null);
    }

    private static AccountDescription createAccountDescription(Integer accountNumber, List<AccountCaption> accountCaptions, Boolean used) {
        return AccountDescription.builder()
                .accountNumber(accountNumber)
                .used(used)
                .captions(accountCaptions.stream().collect(Collectors.toMap(AccountCaption::getCultureCode, descriptionValueMapper)))
                .build();
    }

    private static AccountDescription createAccountDescription(Integer accountNumber, Boolean used) {
        return AccountDescription.builder()
                .accountNumber(accountNumber)
                .used(used)
                .build();
    }

    private static final Function<AccountCaption, Description> descriptionValueMapper = accountCaption ->
            Description.builder().caption(accountCaption.getCaption()).captionLong(accountCaption.getCaptionLong()).build();
}
