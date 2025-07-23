package de.datev.refsys.aggregation.processing.service;

import com.mongodb.client.result.UpdateResult;
import de.datev.refsys.aggregation.document.model.AccountValue;
import de.datev.refsys.aggregation.document.model.AdditionalParameters;
import de.datev.refsys.aggregation.document.model.CustomColumnStructureContent;
import de.datev.refsys.aggregation.document.model.CustomReportStructureContent;
import de.datev.refsys.aggregation.document.model.MasterDataContext;
import de.datev.refsys.aggregation.document.model.MovementDataMonth;
import de.datev.refsys.aggregation.document.model.MovementDataPersonGroupDay;
import de.datev.refsys.aggregation.document.model.MovementDataPersonGroupMonth;
import de.datev.refsys.aggregation.document.model.ShareholderRelation;
import de.datev.refsys.aggregation.document.model.StateDoc;
import de.datev.refsys.aggregation.processing.client.CustomStructuresClient;
import de.datev.refsys.aggregation.processing.client.MasterDataClient;
import de.datev.refsys.aggregation.processing.client.MovementDataClient;
import de.datev.refsys.aggregation.processing.config.InitialLoadConfiguration;
import de.datev.refsys.aggregation.processing.constant.MetricConstants;
import de.datev.refsys.aggregation.processing.constant.ProcessingErrorMessageConstants;
import de.datev.refsys.aggregation.processing.constant.ProcessingServiceConstants;
import de.datev.refsys.aggregation.processing.exception.AggregationProcessingBusinessException;
import de.datev.refsys.aggregation.processing.exception.InitialLoadFailedException;
import de.datev.refsys.aggregation.processing.mapper.AccountDbKeyFieldsMapper;
import de.datev.refsys.aggregation.processing.mapper.AccountSumDayMapper;
import de.datev.refsys.aggregation.processing.mapper.AccountValueMapper;
import de.datev.refsys.aggregation.processing.mapper.AdditionalParametersMapper;
import de.datev.refsys.aggregation.processing.mapper.AlternativeAccountTranslationMapper;
import de.datev.refsys.aggregation.processing.mapper.CollectiveAccountMapper;
import de.datev.refsys.aggregation.processing.mapper.CustomColumnStructureContentMapper;
import de.datev.refsys.aggregation.processing.mapper.CustomReportStructureContentMapper;
import de.datev.refsys.aggregation.processing.mapper.InventoryDbKeyFieldsMapper;
import de.datev.refsys.aggregation.processing.mapper.MasterDataContextMapper;
import de.datev.refsys.aggregation.processing.mapper.PreviousYearAccountTranslationMapper;
import de.datev.refsys.aggregation.processing.mapper.ShareholderAdditionMapper;
import de.datev.refsys.aggregation.processing.mapper.ShareholderAddressMapper;
import de.datev.refsys.aggregation.processing.mapper.ShareholderRelationMapper;
import de.datev.refsys.aggregation.processing.mapper.ShareholderTaxOfficeMapper;
import de.datev.refsys.aggregation.processing.model.AccountDbKeyFields;
import de.datev.refsys.aggregation.processing.model.CustomStructures;
import de.datev.refsys.aggregation.processing.model.DateCorrection;
import de.datev.refsys.aggregation.processing.model.ExtendedMovementdataInventory;
import de.datev.refsys.aggregation.processing.model.ImportData;
import de.datev.refsys.aggregation.processing.model.InventoryDbKeyFields;
import de.datev.refsys.aggregation.processing.model.MovementDataAccountValues;
import de.datev.refsys.aggregation.processing.model.MovementDataDayUpsert;
import de.datev.refsys.aggregation.processing.repository.CustomColumnStructureContentRepository;
import de.datev.refsys.aggregation.processing.repository.CustomReportStructureContentRepository;
import de.datev.refsys.aggregation.processing.repository.MasterDataAccountRepository;
import de.datev.refsys.aggregation.processing.repository.MasterDataRepository;
import de.datev.refsys.aggregation.processing.repository.MovementDataDayRepository;
import de.datev.refsys.aggregation.processing.repository.MovementDataInventoryRepository;
import de.datev.refsys.aggregation.processing.repository.MovementDataMonthRepository;
import de.datev.refsys.aggregation.processing.repository.MovementDataPersonGroupDayRepository;
import de.datev.refsys.aggregation.processing.repository.MovementDataPersonGroupMonthRepository;
import de.datev.refsys.aggregation.processing.repository.StateDocRepository;
import de.datev.refsys.aggregation.processing.util.ComparerUtil;
import de.datev.refsys.aggregation.processing.util.LoggingUtil;
import de.datev.refsys.aggregation.processing.util.Util;
import de.datev.refsys.generated.acds.api.model.AccountSumDay;
import de.datev.refsys.generated.acds.api.model.CollectiveAccount;
import de.datev.refsys.generated.acds.api.model.CustomColumnStructure;
import de.datev.refsys.generated.acds.api.model.CustomReportStructure;
import de.datev.refsys.generated.acds.api.model.MasterdataContext;
import de.datev.refsys.generated.acds.api.model.MasterdataInventory;
import de.datev.refsys.generated.acds.api.model.MovementdataInventory;
import de.datev.refsys.generated.acds.api.model.ShareholderData;
import de.datev.refsys.generated.acds.api.model.Translation;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.observability.micrometer.Micrometer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;


@Slf4j
@Service
public class ImportServiceImpl extends CommonService implements ImportService {
    public static final int MAX_ACCOUNT_SUM_DAYS_CONCURRENCY = 1;
    public static final int MAX_ACCOUNT_SUM_DAYS_PREFETCH = 1;
    private final StateDocRepository stateDocRepository;
    private final MasterDataRepository masterDataRepository;
    private final MasterDataAccountRepository masterDataAccountRepository;
    private final MovementDataDayRepository movementDataDayRepository;
    private final MovementDataMonthRepository movementDataMonthRepository;
    private final MovementDataPersonGroupDayRepository personGroupDayRepository;
    private final MovementDataPersonGroupMonthRepository personGroupMonthRepository;
    private final MovementDataInventoryRepository movementDataInventoryRepository;
    private final MeterRegistry meterRegistry;
    private final MasterDataContextMapper masterDataContextMapper;
    private final CollectiveAccountMapper collectiveAccountMapper;
    private final AlternativeAccountTranslationMapper alternativeAccountTranslationMapper;
    private final PreviousYearAccountTranslationMapper previousYearAccountTranslationMapper;
    private final ShareholderRelationMapper shareholderRelationMapper;
    private final ShareholderAdditionMapper shareholderAdditionMapper;
    private final ShareholderAddressMapper shareholderAddressMapper;
    private final ShareholderTaxOfficeMapper shareholderTaxOfficeMapper;
    private final MasterDataClient masterDataClient;
    private final MovementDataClient movementDataClient;
    private final InitialLoadConfiguration initialLoadConfiguration;
    private final InventoryDbKeyFieldsMapper inventoryDbKeyFieldsMapper;
    private final AccountSumDayMapper accountSumDayMapper;
    private final CustomStructuresService customStructuresService;

    public ImportServiceImpl(final StateDocRepository stateDocRepository, final MasterDataRepository masterDataRepository,
                             final MovementDataDayRepository movementDataDayRepository, final MovementDataMonthRepository movementDataMonthRepository,
                             final MovementDataPersonGroupDayRepository personGroupDayRepository,
                             final MovementDataPersonGroupMonthRepository personGroupMonthRepository,
                             final MasterDataAccountRepository masterDataAccountRepository,
                             final MovementDataInventoryRepository movementDataInventoryRepository, final MeterRegistry meterRegistry,
                             final AdditionalParametersMapper additionalParametersMapper, final MasterDataContextMapper masterDataContextMapper,
                             final CollectiveAccountMapper collectiveAccountMapper,
                             final AlternativeAccountTranslationMapper alternativeAccountTranslationMapper,
                             final PreviousYearAccountTranslationMapper previousYearAccountTranslationMapper,
                             final ShareholderRelationMapper shareholderRelationMapper, final AccountDbKeyFieldsMapper accountDbKeyFieldsMapper,
                             final AccountValueMapper accountValueMapper, final ShareholderAdditionMapper shareholderAdditionMapper,
                             final ShareholderAddressMapper shareholderAddressMapper, final ShareholderTaxOfficeMapper shareholderTaxOfficeMapper,
                             final MasterDataClient masterDataClient, final MovementDataClient movementDataClient,
                             final InitialLoadConfiguration initialLoadConfiguration, final InventoryDbKeyFieldsMapper inventoryDbKeyFieldsMapper,
                             final AccountSumDayMapper accountSumDayMapper, final CustomStructuresService customStructuresService) {
        super(additionalParametersMapper, accountDbKeyFieldsMapper, accountValueMapper);
        this.stateDocRepository = stateDocRepository;
        this.masterDataRepository = masterDataRepository;
        this.masterDataAccountRepository = masterDataAccountRepository;
        this.movementDataDayRepository = movementDataDayRepository;
        this.movementDataMonthRepository = movementDataMonthRepository;
        this.personGroupDayRepository = personGroupDayRepository;
        this.personGroupMonthRepository = personGroupMonthRepository;
        this.movementDataInventoryRepository = movementDataInventoryRepository;
        this.meterRegistry = meterRegistry;
        this.shareholderAdditionMapper = shareholderAdditionMapper;
        this.shareholderAddressMapper = shareholderAddressMapper;
        this.shareholderTaxOfficeMapper = shareholderTaxOfficeMapper;
        this.masterDataContextMapper = masterDataContextMapper;
        this.collectiveAccountMapper = collectiveAccountMapper;
        this.alternativeAccountTranslationMapper = alternativeAccountTranslationMapper;
        this.previousYearAccountTranslationMapper = previousYearAccountTranslationMapper;
        this.shareholderRelationMapper = shareholderRelationMapper;
        this.masterDataClient = masterDataClient;
        this.movementDataClient = movementDataClient;
        this.initialLoadConfiguration = initialLoadConfiguration;
        this.inventoryDbKeyFieldsMapper = inventoryDbKeyFieldsMapper;
        this.accountSumDayMapper = accountSumDayMapper;
        this.customStructuresService = customStructuresService;
    }

    @Override
    public Mono<Boolean> executeFullImport(ImportData importData) {
        Set<Integer> usedAccountNumbers = new HashSet<>();
        Set<Integer> individualPersonAccountNumbers = new HashSet<>();
        Map<AccountDbKeyFields, MovementDataPersonGroupDay> personGroupDayMap = new LinkedHashMap<>();
        Map<AccountDbKeyFields, MovementDataPersonGroupMonth> personGroupMonthMap = new LinkedHashMap<>();
        AtomicInteger concurrentCounter = new AtomicInteger();
        AtomicInteger valueCounter = new AtomicInteger();
        log.debug(LoggingUtil.MOVEMENT_DATA_DAYS_BATCH_START_LOG);
        return movementDataClient.getAccountSumDays(importData.masterdataContext())
                                 .buffer(initialLoadConfiguration.getAccountSumDaysBufferSize())
                                 .flatMap(asdList -> bulkUpsertMovementDataDays(importData.masterdataContext(), asdList, importData.stateDoc(),
                                                                                concurrentCounter, valueCounter),
                                          MAX_ACCOUNT_SUM_DAYS_CONCURRENCY, MAX_ACCOUNT_SUM_DAYS_PREFETCH)
                                 .collectList()
                                 .elapsed().map(LoggingUtil.logDebugWithDuration(LoggingUtil.MOVEMENT_DATA_DAYS_BATCH_SUCCESS_LOG))
                                 .switchIfEmpty(Flux.defer(
                                         () -> afterMovementData(importData.masterdataContext(), usedAccountNumbers, personGroupDayMap,
                                                                 personGroupMonthMap,
                                                                 individualPersonAccountNumbers, importData.stateDoc()).then()).collectList())
                                 .doOnNext(voids ->  log.debug(LoggingUtil.MOVEMENT_DATA_MONTHS_BATCH_START_LOG))
                                 .flatMap(br -> getAccountSumDaysWithSameAccountNumber(importData.masterdataContext())
                                         .contextWrite(context -> {
                                             Optional<Object> optionalLoggingContext = context.getOrEmpty(LoggingUtil.LOGGING_CONTEXT_KEY);
                                             if (optionalLoggingContext.isPresent()) {
                                                 Map<String, String> loggingContext = (Map<String, String>) optionalLoggingContext.get();
                                                 loggingContext.put(LoggingUtil.MOVEMENTDATA_COUNT, String.valueOf(valueCounter.get()));
                                             } else {
                                                 log.warn("Could not find the logging context with key {}", LoggingUtil.LOGGING_CONTEXT_KEY);
                                             }
                                             return context;
                                         })
                                         .bufferUntilChanged(AccountSumDay::getAccountNumber, ComparerUtil::isSameIntegerValue)

                                         .flatMapSequential(asdList -> importMovementData(asdList, importData.masterdataContext(), usedAccountNumbers,
                                                                                          individualPersonAccountNumbers, personGroupDayMap,
                                                                                          personGroupMonthMap,
                                                                                          importData.stateDoc()))
                                         .collectList()
                                         .elapsed().map(LoggingUtil.logDebugWithDuration(LoggingUtil.MOVEMENT_DATA_MONTHS_BATCH_SUCCESS_LOG))
                                         .flatMap(result -> afterMovementData(importData.masterdataContext(), usedAccountNumbers, personGroupDayMap,
                                                                              personGroupMonthMap, individualPersonAccountNumbers,
                                                                              importData.stateDoc())));
    }

    private Mono<Void> bulkUpsertMovementDataDays(MasterdataContext mdc, List<AccountSumDay> asdList, StateDoc stateDoc,
                                                  AtomicInteger concurrentCounter, AtomicInteger valueCounter) {
        valueCounter.addAndGet(asdList.size());
        int counter = concurrentCounter.incrementAndGet();
        if (counter > 1) {
            log.error("{} concurrent operations detected for movementDataDays bulkUpsert call", counter);
        }
        if (asdList.isEmpty()) {
            return Mono.empty();
        }
        try {
            checkInitialLoadMaxImportTime(stateDoc);
        } catch (InitialLoadFailedException e) {
            return Mono.error(e);
        }
        List<MovementDataDayUpsert> movementDataDayUpserts = asdList.stream().map(asd -> mapToMovementDataDayUpsert(mdc, asd)).toList();
        return Flux.merge(movementDataDayRepository.bulkUpsert(mdc.getConsultant(), mdc.getClient(), mdc.getYearBegin(), movementDataDayUpserts),
                          checkAndUpdateParkingTime(mdc, stateDoc))
                   .collectList()
                   .flatMap(afterMerge -> {
                       concurrentCounter.decrementAndGet();
                       return Mono.empty();
                   });
    }

    private MovementDataDayUpsert mapToMovementDataDayUpsert(MasterdataContext mdc, AccountSumDay asd) {
        AdditionalParameters additionalParams = additionalParametersMapper.accountSumDayToAdditionalParameters(asd);
        AccountValue accountValue = accountValueMapper.accountSumDayToAccountValue(asd);
        DateCorrection dateCorrection = new DateCorrection(asd, mdc.getYearBegin(), mdc.getYearEnd());
        String dayKey = getDayKey(asd, dateCorrection);
        return new MovementDataDayUpsert(asd.getAccountingReasonId(), asd.getAccountNumber(), dayKey, additionalParams, accountValue);
    }

    private Flux<AccountSumDay> getAccountSumDaysWithSameAccountNumber(MasterdataContext mdc) {
        return Flux.from(movementDataDayRepository.findByBusinessKey(mdc.getConsultant(), mdc.getClient(), mdc.getYearBegin()))
                   .flatMapSequential(mdd -> Flux.fromIterable(accountSumDayMapper.mapDbToApiModel(mdd)));
    }

    private Mono<Void> checkAndUpdateParkingTime(MasterdataContext mdc, StateDoc stateDoc) {
        OffsetDateTime currentTimestamp = OffsetDateTime.now();
        if (stateDoc.getStateTimestamp() != null
                && ChronoUnit.MILLIS.between(stateDoc.getStateTimestamp(), currentTimestamp) > initialLoadConfiguration.getParkingTimeInMs()) {
            stateDoc.setStateTimestamp(currentTimestamp);
            return stateDocRepository.updateTimestamp(mdc.getConsultant(), mdc.getClient(), mdc.getYearBegin(), currentTimestamp).then();
        }
        return Mono.empty();
    }

    private void checkInitialLoadMaxImportTime(StateDoc stateDoc) {
        if (stateDoc.getStateTimestamp() != null && ChronoUnit.MILLIS.between(stateDoc.getStateTimestamp(), OffsetDateTime.now())
                > initialLoadConfiguration.getMaxImportDurationInMs()) {
            throw new InitialLoadFailedException(ProcessingErrorMessageConstants.IMPORT_EXCEEDED_MAX_TIME, HttpStatus.CONFLICT.value(),
                                                 ProcessingServiceConstants.IMPORT_IN_PROGRESS_TYPE);
        }
    }

    /**
     * inserts documents in MasterData, MasterDataAccounts, MovementDataPersonGroupDays, MovementDataPersonGroupMonths collections
     *<pre>The order of the two transformDeferred is very important:
     *- If the circuit breaker is placed above the retry, every failure from each retry will be recorded and counted towards the circuit breaker
     * threshold.
     *- If the retry is placed above the circuit breaker, all the retries will be exhausted before a circuit breaker could record an exception.
     * </pre>
     * @param mdc master data context
     * @param usedAccountNumbers used account number set
     * @param personGroupDayMap Map of {@link AccountDbKeyFields} as key and {@link MovementDataPersonGroupDay} as value
     * @param personGroupMonthMap Map of {@link AccountDbKeyFields} as key and {@link MovementDataPersonGroupMonth} as value
     * @param individualPersonAccountNumbers individual person account number set
     * @param stateDoc state document {@link StateDoc}
     * @return Mono of {@link UpdateResult}
     */
    protected Mono<Boolean> afterMovementData(MasterdataContext mdc, Set<Integer> usedAccountNumbers,
                                              Map<AccountDbKeyFields, MovementDataPersonGroupDay> personGroupDayMap,
                                              Map<AccountDbKeyFields, MovementDataPersonGroupMonth> personGroupMonthMap,
                                              Set<Integer> individualPersonAccountNumbers, StateDoc stateDoc) {
        return Mono.zip(masterDataClient.getCollectiveAccounts(mdc), masterDataClient.getTranslation(mdc), masterDataClient.getInventories(mdc))
                   .flatMapMany(result -> {
                       result.getT1().forEach(ca -> usedAccountNumbers.add(ca.getAccountNumberFrom()));
                       if (result.getT2().getPreviousYearAccountTranslations() != null) {
                           result.getT2().getPreviousYearAccountTranslations().forEach(pat -> usedAccountNumbers.add(pat.getTargetValueInt()));
                       }
                       try {
                           checkInitialLoadMaxImportTime(stateDoc);
                       } catch (InitialLoadFailedException e) {
                           return Mono.error(e);
                       }
                       // group MasterDataInventories by wgId in a TreeMap for faster search in the MovementDataInventories business logic
                       Map<Integer, MasterdataInventory> groupedMasterDataInventories = result.getT3().stream().collect(
                               Collectors.toMap(MasterdataInventory::getWgId, Function.identity(), (mi1, mi2) -> mi2, TreeMap::new));
                       // add all accountNumbers from MasterDataInventories to the usedAccountNumbers, to set the used flag correctly
                       usedAccountNumbers.addAll(result.getT3().stream().map(MasterdataInventory::getAccountNumber).collect(Collectors.toSet()));
                       return Flux.merge(insertMovementDataPersonGroupDays(personGroupDayMap),
                                         insertMovementDataPersonGroupMonths(personGroupMonthMap),
                                         importMasterData(mdc, individualPersonAccountNumbers, result.getT1(), result.getT2()),
                                         importMasterDataAccounts(mdc, usedAccountNumbers, result.getT3()),
                                         importMovementDataInventories(mdc, groupedMasterDataInventories));
                   })
                   .name("importData-bulkInsertMasterDataAndMasterDataAccounts").tag(MetricConstants.METRIC_TYPE, MetricConstants.METRIC_WRITE)
                   .tap(Micrometer.metrics(meterRegistry))
                   .collectList()
                   .flatMap(afterCollect -> Mono.just(true));
    }

    protected Mono<Void> importMasterData(MasterdataContext mdc, Set<Integer> individualPersonAccountNumbers,
                                                  List<CollectiveAccount> collectiveAccounts, Translation translation) {
        return Mono.zip(masterDataClient.getShareholderData(mdc), customStructuresService.getCustomStructures(mdc))
                   .flatMap(tuple2 -> insertMasterData(mdc, individualPersonAccountNumbers, collectiveAccounts, translation, tuple2.getT1(),
                                                       tuple2.getT2()));
    }

    private Mono<Void> insertMasterData(MasterdataContext mdc, Set<Integer> individualPersonAccountNumbers,
                                        List<CollectiveAccount> collectiveAccounts, Translation translation, ShareholderData sd,
                                        CustomStructures customStructures) {
        try {
            MasterDataContext masterDataContext = masterDataContextMapper.mapAcdsToDb(mdc);
            if (!customStructures.customColumnStructureInfos().isEmpty()) {
                masterDataContext.setCustomColumnStructureInfos(customStructures.customColumnStructureInfos());
            }
            if (!customStructures.customReportStructureInfos().isEmpty()) {
                masterDataContext.setCustomReportStructureInfos(customStructures.customReportStructureInfos());
            }
            List<ShareholderRelation> shareholderRelations = shareholderRelationMapper.mapAcdsToDb(sd.getShareholderRelations());
            return Flux.merge(masterDataRepository.upsertOne(masterDataContext,
                                                             collectiveAccountMapper.mapAcdsToDbList(collectiveAccounts),
                                                             shareholderRelations,
                                                             shareholderAdditionMapper.mapAcdsToDb(sd.getShareholderAdditions()),
                                                             shareholderAddressMapper.mapAcdsToDb(sd.getShareholderAddressees()),
                                                             shareholderTaxOfficeMapper.mapAcdsToDb(sd.getShareholderTaxOffices()),
                                                             previousYearAccountTranslationMapper.mapAcdsToDbList(
                                                                     translation.getPreviousYearAccountTranslations()),
                                                             alternativeAccountTranslationMapper.mapAcdsToDbList(
                                                                     translation.getAlternativeAccountTranslations()),
                                                             individualPersonAccountNumbers),
                              customStructuresService.insertCustomReportStructureContents(customStructures.customReportStructureContents()),
                              customStructuresService.insertCustomColumnStructureContents(customStructures.customColumnStructureContents()))
                       .collectList()
                       .flatMap(afterInsert -> Mono.empty());
        } catch (Exception e) {
            return Mono.error(new AggregationProcessingBusinessException(
                    String.format(ProcessingErrorMessageConstants.MASTER_DATA_MAPPING_ERROR, e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR.value(), e));
        }
    }

    private Mono<Void> importMasterDataAccounts(MasterdataContext mdc, Set<Integer> usedAccountNumbers,
                                                List<MasterdataInventory> masterdataInventories) {
        return Mono.zip(masterDataClient.getAccountPurposeMappings(mdc), masterDataClient.getAccountCaptions(mdc)).flatMap(
                           mdaList -> mdaList.getT1().isEmpty() ?
                                   Mono.empty() :
                                   masterDataAccountRepository.bulkInsert(mdc, mdaList.getT1(), mdaList.getT2(), masterdataInventories,
                                                                          usedAccountNumbers))
                   .then();
    }

    private Mono<Void> importMovementDataInventories(MasterdataContext mdc, Map<Integer, MasterdataInventory> groupedMasterDataInventories) {
        log.debug(LoggingUtil.MOVEMENT_DATA_INVENTORIES_BATCH_START_LOG);
        return bufferMovementDataInventoriesByWgId(mdc, groupedMasterDataInventories)
                // buffer again with a larger buffer size to not save in the DB on every wgId change
                .buffer(initialLoadConfiguration.getInventoriesBufferSize())
                .flatMap(bufferedInventories -> insertMovementDataInventories(mdc, bufferedInventories))
                .collectList()
                .elapsed().map(LoggingUtil.logDebugWithDuration(LoggingUtil.MOVEMENT_DATA_INVENTORIES_BATCH_SUCCESS_LOG))
                .flatMap(afterMerge -> Mono.empty());
    }

    protected Mono<Void> insertMovementDataInventories(MasterdataContext mdc, List<ExtendedMovementdataInventory> bufferedInventories) {
        Map<InventoryDbKeyFields, List<ExtendedMovementdataInventory>> groupedMovementDataInventoriesByDbKey =
                bufferedInventories.stream().collect(Collectors.groupingBy(
                                           extendedInventory -> inventoryDbKeyFieldsMapper.mapToDbModel(mdc, extendedInventory),
                                           Collectors.toList()));
        return groupedMovementDataInventoriesByDbKey.isEmpty() ?
                Mono.empty() :
                movementDataInventoryRepository.bulkUpsert(groupedMovementDataInventoriesByDbKey).then();
    }

    private Flux<ExtendedMovementdataInventory> bufferMovementDataInventoriesByWgId(MasterdataContext mdc,
                                                                                    Map<Integer, MasterdataInventory> groupedMasterDataInventories) {
        return movementDataClient.getMovementDataInventories(mdc)
                                 // buffer until wgId changes
                                 .bufferUntilChanged(MovementdataInventory::getWgId, ComparerUtil::isSameIntegerValue)
                                 .flatMap(inventories -> filterMovementDataInventories(groupedMasterDataInventories, inventories, mdc));
    }

    protected Flux<ExtendedMovementdataInventory> filterMovementDataInventories(Map<Integer, MasterdataInventory> groupedMasterDataInventories,
                                                                                List<MovementdataInventory> inventoriesWithSameWgId,
                                                                                MasterdataContext mdc) {
        if (groupedMasterDataInventories.containsKey(inventoriesWithSameWgId.get(0).getWgId())) {
            MasterdataInventory masterdataInventory = groupedMasterDataInventories.get(inventoriesWithSameWgId.get(0).getWgId());
            List<ExtendedMovementdataInventory> movementdataInventories =
                    inventoriesWithSameWgId.stream()
                                           .filter(inventory -> shouldTakeInventory(inventory, masterdataInventory.getFlagAccountTransfer(),
                                                                                    mdc.getYearBegin(), mdc.getYearEnd()))
                                           .map(filteredInventory ->
                                                        ExtendedMovementdataInventory.builder()
                                                                                     .inventoryNumber(masterdataInventory.getInventoryNumber())
                                                                                     .movementdataInventory(filteredInventory)
                                                                                     .build())
                                           .toList();
            return Flux.fromIterable(movementdataInventories);
        } else {
            return Flux.error(new AggregationProcessingBusinessException(
                    String.format(ProcessingErrorMessageConstants.MISSING_MASTER_DATA_INVENTORIES_ERROR, inventoriesWithSameWgId.get(0).getWgId()),
                    HttpStatus.INTERNAL_SERVER_ERROR.value()));
        }
    }

    private static boolean shouldTakeInventory(MovementdataInventory inventory, Integer flagAccountTransfer, Integer yearBegin, Integer yearEnd) {
        boolean isSameType = flagAccountTransfer.equals(inventory.getInvsutyp());
        if (inventory.getValidFrom() != null) {
            return isSameType && Util.isDateBetweenYearBeginAndEnd(inventory.getValidFrom(), yearBegin, yearEnd);
        } else {
            log.warn(ProcessingErrorMessageConstants.INVENTORY_VALID_FROM_IS_NULL, inventory.getWgId());
            return isSameType;
        }
    }

    /**
     * inserts documents in MovementDataDays, MovementDataMonths collections
     *<pre>The order of the two transformDeferred is very important:
     *- If the circuit breaker is placed above the retry, every failure from each retry will be recorded and counted towards the circuit breaker
     * threshold.
     *- If the retry is placed above the circuit breaker, all the retries will be exhausted before a circuit breaker could record an exception.
     * </pre>
     * @param accountSumDays {@link AccountSumDay} list
     * @param mdc master data context
     * @param usedAccountNumbers used account number set
     * @param individualPersonAccountNumbers individual person account number set
     * @param personGroupDayMap Map of {@link AccountDbKeyFields} as key and {@link MovementDataPersonGroupDay} as value
     * @param personGroupMonthMap Map of {@link AccountDbKeyFields} as key and {@link MovementDataPersonGroupMonth} as value
     * @param stateDoc state document {@link StateDoc}
     * @return Mono of Void
     */
    protected Mono<Void> importMovementData(List<AccountSumDay> accountSumDays, MasterdataContext mdc, Set<Integer> usedAccountNumbers,
                                            Set<Integer> individualPersonAccountNumbers,
                                            Map<AccountDbKeyFields, MovementDataPersonGroupDay> personGroupDayMap,
                                            Map<AccountDbKeyFields, MovementDataPersonGroupMonth> personGroupMonthMap,
                                            StateDoc stateDoc) {
        if (!accountSumDays.isEmpty()) {
            MovementDataAccountValues movementDataAccountValues;
            try {
                if (usedAccountNumbers.contains(accountSumDays.get(0).getAccountNumber())) {
                    throw new AggregationProcessingBusinessException(
                            String.format(ProcessingErrorMessageConstants.ACCOUNT_SUM_DAY_PROCESSING_ERROR, accountSumDays.get(0).getAccountNumber()),
                            HttpStatus.INTERNAL_SERVER_ERROR.value());
                }
                movementDataAccountValues = getMovementDataAccountValues(mdc,
                                                                         accountSumDays, usedAccountNumbers, personGroupDayMap, personGroupMonthMap);
                individualPersonAccountNumbers.addAll(movementDataAccountValues.getIndividualPersonAccountNumbers());
                checkInitialLoadMaxImportTime(stateDoc);
            } catch (AggregationProcessingBusinessException | InitialLoadFailedException e) {
                return Mono.error(e);
            } catch (Exception e) {
                return Mono.error(new AggregationProcessingBusinessException(
                        String.format(ProcessingErrorMessageConstants.MOVEMENT_DATA_MAPPING_ERROR, e.getMessage()),
                        HttpStatus.INTERNAL_SERVER_ERROR.value(), e));
            }
            return Flux.merge(insertMovementDataMonths(movementDataAccountValues.getAccountMonthMap()),
                              checkAndUpdateParkingTime(mdc, stateDoc))
                       .name("importData-bulkInsertMovementDataMonths")
                       .tag(MetricConstants.METRIC_TYPE, MetricConstants.METRIC_WRITE)
                       .tap(Micrometer.metrics(meterRegistry))
                       .collectList()
                       .flatMap(afterMerge -> Mono.empty());
        }
        return Mono.empty();
    }

    private Mono<Void> insertMovementDataMonths(Map<AccountDbKeyFields, MovementDataMonth> accountMonthMap) {
        return accountMonthMap.values().isEmpty() ? Mono.empty() : movementDataMonthRepository.bulkInsert(accountMonthMap).then();
    }

    private Mono<Void> insertMovementDataPersonGroupDays(Map<AccountDbKeyFields, MovementDataPersonGroupDay> accountPersonGroupDayMap) {
        return accountPersonGroupDayMap.values().isEmpty() ? Mono.empty() : personGroupDayRepository.bulkInsert(accountPersonGroupDayMap).then();
    }

    private Mono<Void> insertMovementDataPersonGroupMonths(Map<AccountDbKeyFields, MovementDataPersonGroupMonth> accountPGMonthMap) {
        return accountPGMonthMap.values().isEmpty() ? Mono.empty() : personGroupMonthRepository.bulkInsert(accountPGMonthMap).then();
    }
}
