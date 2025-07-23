package de.datev.refsys.aggregation.processing.service;

import com.mongodb.ClientSessionOptions;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.reactivestreams.client.ClientSession;
import com.mongodb.reactivestreams.client.MongoClient;
import de.datev.refsys.aggregation.document.model.AccountDescription;
import de.datev.refsys.aggregation.document.model.AccountGroupValue;
import de.datev.refsys.aggregation.document.model.AccountValue;
import de.datev.refsys.aggregation.document.model.MasterDataAccount;
import de.datev.refsys.aggregation.document.model.MovementDataDay;
import de.datev.refsys.aggregation.document.model.MovementDataMonth;
import de.datev.refsys.aggregation.document.model.MovementDataPersonGroupDay;
import de.datev.refsys.aggregation.document.model.MovementDataPersonGroupMonth;
import de.datev.refsys.aggregation.document.model.StateDoc;
import de.datev.refsys.aggregation.document.model.enums.StateDocState;
import de.datev.refsys.aggregation.processing.api.model.AccountSumDayDelta;
import de.datev.refsys.aggregation.processing.api.model.DeltaInfo;
import de.datev.refsys.aggregation.processing.api.model.DeltaRequest;
import de.datev.refsys.aggregation.processing.constant.MetricConstants;
import de.datev.refsys.aggregation.processing.constant.ProcessingErrorMessageConstants;
import de.datev.refsys.aggregation.processing.constant.ProcessingServiceConstants;
import de.datev.refsys.aggregation.processing.exception.InitialLoadFailedException;
import de.datev.refsys.aggregation.processing.exception.RestWarnException;
import de.datev.refsys.aggregation.processing.functions.PersonGroupDeltaCalculation;
import de.datev.refsys.aggregation.processing.functions.processingResultDto.DeltaEventDifferenceResult;
import de.datev.refsys.aggregation.processing.functions.processingResultDto.PersonGroupDelta;
import de.datev.refsys.aggregation.processing.mapper.AccountDbKeyFieldsMapper;
import de.datev.refsys.aggregation.processing.mapper.AccountSumDayMapper;
import de.datev.refsys.aggregation.processing.mapper.AccountValueMapper;
import de.datev.refsys.aggregation.processing.mapper.AdditionalParametersMapper;
import de.datev.refsys.aggregation.processing.model.AccountDbKeyFields;
import de.datev.refsys.aggregation.processing.model.MovementDataAccountValues;
import de.datev.refsys.aggregation.processing.repository.MasterDataAccountRepository;
import de.datev.refsys.aggregation.processing.repository.MasterDataRepository;
import de.datev.refsys.aggregation.processing.repository.MovementDataDayRepository;
import de.datev.refsys.aggregation.processing.repository.MovementDataMonthRepository;
import de.datev.refsys.aggregation.processing.repository.MovementDataPersonGroupDayRepository;
import de.datev.refsys.aggregation.processing.repository.MovementDataPersonGroupMonthRepository;
import de.datev.refsys.aggregation.processing.repository.StateDocRepository;
import de.datev.refsys.aggregation.processing.util.ComparerUtil;
import de.datev.refsys.aggregation.processing.util.LoggingUtil;
import de.datev.refsys.generated.acds.api.model.AccountSumDay;
import de.datev.refsys.generated.acds.api.model.MasterdataContext;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.observability.micrometer.Micrometer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.datev.refsys.aggregation.processing.util.NumberUtils.nullSafeGet;
import static de.datev.refsys.aggregation.processing.util.NumberUtils.storeNullIfZero;

@Slf4j
@Service
public class DeltaEventProcessingServiceImpl extends CommonService implements DeltaEventProcessingService {
    private final MasterDataRepository masterDataRepository;
    private final ClientSessionOptions clientSessionOptions;
    private final MongoClient mongoClient;
    private final MeterRegistry meterRegistry;
    private final StateDocRepository stateDocRepository;
    private final MasterDataAccountRepository masterDataAccountRepository;
    private final MovementDataDayRepository movementDataDayRepository;
    private final MovementDataMonthRepository movementDataMonthRepository;
    private final MovementDataPersonGroupDayRepository personGroupDayRepository;
    private final MovementDataPersonGroupMonthRepository personGroupMonthRepository;
    private final AccountSumDayMapper accountSumDayMapper;

    @Value("${ref-sys.mongodb.transaction-timeout-in-ms}")
    protected Long transactionTimeout;

    public DeltaEventProcessingServiceImpl(final MongoClient updateMongoClient, final StateDocRepository stateDocRepository,
                                           final MasterDataRepository masterDataRepository,
                                           final MasterDataAccountRepository masterDataAccountRepository,
                                           final MovementDataDayRepository movementDataDayRepository,
                                           final MovementDataMonthRepository movementDataMonthRepository,
                                           final MovementDataPersonGroupDayRepository personGroupDayRepository,
                                           final MovementDataPersonGroupMonthRepository personGroupMonthRepository, final MeterRegistry meterRegistry,
                                           final ClientSessionOptions clientSessionOptions,
                                           final AdditionalParametersMapper additionalParametersMapper,
                                           final AccountDbKeyFieldsMapper accountDbKeyFieldsMapper, final AccountValueMapper accountValueMapper,
                                           final AccountSumDayMapper accountSumDayMapper) {
        super(additionalParametersMapper, accountDbKeyFieldsMapper, accountValueMapper);
        this.masterDataRepository = masterDataRepository;
        this.clientSessionOptions = clientSessionOptions;
        this.mongoClient = updateMongoClient;
        this.meterRegistry = meterRegistry;
        this.stateDocRepository = stateDocRepository;
        this.masterDataAccountRepository = masterDataAccountRepository;
        this.movementDataDayRepository = movementDataDayRepository;
        this.movementDataMonthRepository = movementDataMonthRepository;
        this.personGroupDayRepository = personGroupDayRepository;
        this.personGroupMonthRepository = personGroupMonthRepository;
        this.accountSumDayMapper = accountSumDayMapper;
    }

    @Override
    public Mono<DeltaInfo> processDeltaEvent(Integer consultant, Integer client, Integer fiscalYear, Long baseVersion, Long deltaVersion,
                                             Mono<DeltaRequest> deltaRequest) {
        return deltaRequest
                   .flatMap(dr -> stateDocRepository.findOneByBusinessKey(consultant, client, fiscalYear)
                                                    .flatMap(sd -> checkStateDocAndProcessDeltaEvent(dr, baseVersion, deltaVersion, sd)))
                   .name("delta_event")
                   .tag(MetricConstants.METRIC_TYPE, MetricConstants.METRIC_ENDPOINT)
                   .tap(Micrometer.metrics(meterRegistry));
    }

    private Mono<DeltaInfo> checkStateDocAndProcessDeltaEvent(DeltaRequest deltaRequest, Long baseVersion, Long deltaVersion, StateDoc sd) {
        if (sd.getState() != StateDocState.DONE) {
            if (sd.getState() == StateDocState.BAD || sd.getState() == StateDocState.INVALID_DATA) {
                return Mono.error(new RestWarnException(
                        String.format(ProcessingErrorMessageConstants.STATE_DOC_STATE_ERROR, sd.getState()),
                        HttpStatus.BAD_REQUEST.value(), ProcessingServiceConstants.STATE_DOC_STATE_ERROR_TYPE));
            } else {
                long importDuration = ChronoUnit.MILLIS.between(sd.getStateTimestamp(), OffsetDateTime.now());
                return Mono.error(new InitialLoadFailedException(
                        String.format(ProcessingErrorMessageConstants.ANOTHER_IMPORT_IN_PROGRESS, importDuration),
                        HttpStatus.CONFLICT.value(), ProcessingServiceConstants.IMPORT_IN_PROGRESS_TYPE));
            }
        }

        if (sd.getBaseVersion().equals(baseVersion)) {
            // if stateDoc.baseVersion and request.baseVersion are equal and
            if (sd.getDeltaVersion() + 1 == deltaVersion) {
                return processDeltaEventInTransaction(baseVersion, deltaVersion, deltaRequest, sd);
            } else if (sd.getDeltaVersion() + 1 > deltaVersion) {
                return logWarnAndGetDeltaInfo(sd);
            } else {
                return getRestWarnException(sd);
            }
        } else if (sd.getBaseVersion() > baseVersion) {
            return logWarnAndGetDeltaInfo(sd);
        } else {
            return getRestWarnException(sd);
        }
    }

    private static Mono<DeltaInfo> getRestWarnException(StateDoc sd) {
        String gapMessage =
                String.format(ProcessingErrorMessageConstants.BASE_OR_DELTA_VERSION_GAP_ERROR, sd.getBaseVersion(), sd.getDeltaVersion());
        log.warn(gapMessage);
        return Mono.error(new RestWarnException(gapMessage, HttpStatus.BAD_REQUEST.value(),
                                                ProcessingServiceConstants.STATE_DOC_BASE_OR_DELTA_VERSION_GAP_TYPE));
    }

    private static Mono<DeltaInfo> logWarnAndGetDeltaInfo(StateDoc sd) {
        log.warn(ProcessingErrorMessageConstants.BASE_OR_DELTA_VERSION_UP_TO_DATE, sd.getBaseVersion(), sd.getDeltaVersion());
        return Mono.just(new DeltaInfo(sd.getBaseVersion(), sd.getDeltaVersion()));
    }

    protected Mono<DeltaInfo> processDeltaEventInTransaction(Long baseVersion, Long deltaVersion, DeltaRequest deltaRequest, StateDoc stateDoc) {
        log.info("processing event in transaction");

        MasterdataContext masterdataContext =
                MasterdataContext.builder().consultant(stateDoc.getConsultant()).client(stateDoc.getClient())
                                 .yearBegin(stateDoc.getYearBegin()).yearEnd(stateDoc.getYearEnd()).build();

        if (deltaRequest.getAccountSumDayDeltas() == null || deltaRequest.getAccountSumDayDeltas().isEmpty()) {
            log.warn(ProcessingErrorMessageConstants.DELTA_REQUEST_HAS_NO_ACCOUNT_SUM_DAY_DELTAS);
            return Mono.from(mongoClient.startSession(clientSessionOptions))
                       .flatMap(cs -> updateVersionInfoInStateDoc(baseVersion, deltaVersion, stateDoc, cs).doFinally(signal -> cs.close()));

        }
        log.debug(LoggingUtil.DELTA_EVENT_BATCH_START_LOG);
        return Flux.fromIterable(deltaRequest.getAccountSumDayDeltas())
                   .bufferUntilChanged(AccountSumDayDelta::getAccountNumber, ComparerUtil::isSameIntegerValue)
                   .flatMap(accountSumDeltas -> {
                       // Convert AccountSumDiffs to AccountSumDay
                       List<AccountSumDay> deltaAccountSumDays = accountSumDeltas.stream().map(accountSumDayMapper::mapAccountSumDayDelta).toList();

                       List<Integer> accountNumbers = accountSumDeltas.stream().map(AccountSumDayDelta::getAccountNumber).distinct().toList();
                       //TODO: assert: Fehler bei mehr als 1 Account
                       if (accountNumbers.size() == 1 && accountNumbers.get(0) >= MIN_ACCOUNT_PERSON_GROUP_NUMBER) {
                           // Personenkonto -> Deltaberechnung für die Gruppen notwendig
                           return movementDataDayRepository.findAllMovementDataDayForAccount(stateDoc.getConsultant(), stateDoc.getClient(),
                                                                                             stateDoc.getYearBegin(), accountNumbers)
                                                           .flatMap(existingBookings -> calculateDifferencesToStoreInDatabase(
                                                                   existingBookings, masterdataContext, deltaAccountSumDays));
                       }
                       return doSimpleMovementAccountCalculation(deltaAccountSumDays, masterdataContext);
                   })
                   .collectList()
                   .elapsed().map(LoggingUtil.logDebugWithDuration(LoggingUtil.DELTA_EVENT_BATCH_SUCCESS_LOG))
                   .mapNotNull(this::mergeDifferenceResults)
                   .flatMap(differenceResult -> Mono.from(mongoClient.startSession(clientSessionOptions))
                                                    .flatMap(cs -> {
                                                        cs.startTransaction();
                                                        return Mono.from(writeDocumentDeltas(stateDoc,
                                                                                             cs,
                                                                                             differenceResult.resultMovementDataAccountValues(),
                                                                                             differenceResult.personGroupDelta()))
                                                                   .name("deltaEvent-transactionBulkIncrementMovementData")
                                                                   .tag(MetricConstants.METRIC_TYPE,
                                                                            MetricConstants.METRIC_WRITE)
                                                                   .tap(Micrometer.metrics(meterRegistry))
                                                                   //TODO: consider joining the two updates for the masterdata collection
                                                                   .then(updateIndividualPersonAccounts(cs,
                                                                                                        differenceResult.personGroupDelta().additionalIndividualPersonAccounts(),
                                                                                                        differenceResult.personGroupDelta().masterdataContext()))
                                                                   .then(writeNearTimeDataFlagInMasterDataContext(deltaRequest, stateDoc, cs))
                                                                   .then(updateVersionInfoInStateDoc(baseVersion, deltaVersion, stateDoc, cs))
                                                                   .then(Mono.defer(() -> {
                                                                       log.debug("Commiting transaction. Transaction state: {}",
                                                                                 cs.hasActiveTransaction());
                                                                       return Mono.from(cs.commitTransaction());
                                                                   }))
                                                                   .onErrorResume(e -> {
                                                                       log.debug("Aborting transaction. Transaction state: {}",
                                                                                 cs.hasActiveTransaction(), e);
                                                                       log.error("Error in Transaction. Rolling Back.");
                                                                       return Mono.from(cs.abortTransaction()).then(Mono.error(e));
                                                                   }) // TODO: write bad doc state if state doc not changed -> ack event. "))
                                                                   .doFinally(signal -> cs.close())
                                                                   .then(Mono.just(new DeltaInfo(baseVersion, deltaVersion)));
                                                    })
                   );
    }

    private Mono<DeltaEventDifferenceResult> doSimpleMovementAccountCalculation(List<AccountSumDay> accountSumDays,
                                                                                MasterdataContext masterdataContext) {
        MovementDataAccountValues resultMovementDataAccountValues =
                getMovementDataAccountValues(masterdataContext, accountSumDays,
                                             new HashSet<>(), new LinkedHashMap<>(),
                                             new LinkedHashMap<>());
        return Mono.just(new DeltaEventDifferenceResult(resultMovementDataAccountValues,
                                                        new PersonGroupDelta(new LinkedHashMap<>(), new LinkedHashMap<>(), new HashSet<>(),
                                                                             masterdataContext)));
    }

    private DeltaEventDifferenceResult mergeDifferenceResults(List<DeltaEventDifferenceResult> differenceResults) {
        return differenceResults.stream().reduce((d1, d2) -> new DeltaEventDifferenceResult(
                mergeMovementDataAccountValues(d1.resultMovementDataAccountValues(), d2.resultMovementDataAccountValues()),
                mergePersonGroupDelta(d1.personGroupDelta(), d2.personGroupDelta()))).get();
    }

    private PersonGroupDelta mergePersonGroupDelta(PersonGroupDelta personGroupDelta, PersonGroupDelta personGroupDelta1) {
        Map<AccountDbKeyFields, MovementDataPersonGroupDay> persongroupDayMap =
                Stream.concat(personGroupDelta.personGroupDayMap().entrySet().stream(), personGroupDelta1.personGroupDayMap().entrySet().stream())
                      .collect(Collectors.toMap(Map.Entry::getKey,
                                                Map.Entry::getValue,
                                                DeltaEventProcessingServiceImpl::addPersonGroupDay));
        Map<AccountDbKeyFields, MovementDataPersonGroupMonth> persongroupMonthMap =
                Stream.concat(personGroupDelta.personGroupMonthMap().entrySet().stream(), personGroupDelta1.personGroupMonthMap().entrySet().stream())
                      .collect(Collectors.toMap(Map.Entry::getKey,
                                                Map.Entry::getValue,
                                                DeltaEventProcessingServiceImpl::addPersonGroupMonth));
        Set<Integer> additionaLindividualPersonAccounts = Stream.concat(personGroupDelta.additionalIndividualPersonAccounts().stream(),
                                                                        personGroupDelta1.additionalIndividualPersonAccounts().stream()).collect(
                Collectors.toSet());
        MasterdataContext masterdataContext = personGroupDelta.masterdataContext();

        return new PersonGroupDelta(persongroupDayMap, persongroupMonthMap, additionaLindividualPersonAccounts, masterdataContext);
    }

    private static MovementDataPersonGroupDay addPersonGroupDay(MovementDataPersonGroupDay movementDataPersonGroupDay,
                                                                MovementDataPersonGroupDay movementDataPersonGroupDay1) {
        return MovementDataPersonGroupDay.builder()
                                         .consultant(movementDataPersonGroupDay.getConsultant())
                                         .client(movementDataPersonGroupDay.getClient())
                                         .fiscalYear(movementDataPersonGroupDay.getFiscalYear())
                                         .accountGroupNumber(movementDataPersonGroupDay.getAccountGroupNumber())
                                         .accountingReasonId(movementDataPersonGroupDay.getAccountingReasonId())
                                         .additionalParams(movementDataPersonGroupDay.getAdditionalParams())
                                         .values(Stream.concat(movementDataPersonGroupDay.getValues().entrySet().stream(),
                                                               movementDataPersonGroupDay1.getValues().entrySet().stream())
                                                       .collect(
                                                               Collectors.toMap(
                                                                       Map.Entry::getKey,
                                                                       Map.Entry::getValue,
                                                                       DeltaEventProcessingServiceImpl::addGroupValues)
                                                       ))
                                         .build();
    }

    private static MovementDataPersonGroupMonth addPersonGroupMonth(MovementDataPersonGroupMonth movementDataPersonGroupMonth,
                                                                    MovementDataPersonGroupMonth movementDataPersonGroupMonth1) {
        return MovementDataPersonGroupMonth.builder()
                                           .consultant(movementDataPersonGroupMonth.getConsultant())
                                           .client(movementDataPersonGroupMonth.getClient())
                                           .fiscalYear(movementDataPersonGroupMonth.getFiscalYear())
                                           .accountGroupNumber(movementDataPersonGroupMonth.getAccountGroupNumber())
                                           .accountingReasonId(movementDataPersonGroupMonth.getAccountingReasonId())
                                           .additionalParams(movementDataPersonGroupMonth.getAdditionalParams())
                                           .values(Stream.concat(movementDataPersonGroupMonth.getValues().entrySet().stream(),
                                                                 movementDataPersonGroupMonth1.getValues().entrySet().stream())
                                                         .collect(
                                                                 Collectors.toMap(
                                                                         Map.Entry::getKey,
                                                                         Map.Entry::getValue,
                                                                         DeltaEventProcessingServiceImpl::addGroupValues)
                                                         ))
                                           .build();
    }

    private MovementDataAccountValues mergeMovementDataAccountValues(MovementDataAccountValues m1, MovementDataAccountValues m2) {
        MovementDataAccountValues mergedValues = new MovementDataAccountValues();
        mergedValues.setAccountDayMap(Stream.concat(m1.getAccountDayMap().entrySet().stream(), m2.getAccountDayMap().entrySet().stream())
                                            .collect(Collectors.toMap(Map.Entry::getKey,
                                                                      Map.Entry::getValue,
                                                                      DeltaEventProcessingServiceImpl::addMovementDataDy)));
        mergedValues.setAccountMonthMap(Stream.concat(m1.getAccountMonthMap().entrySet().stream(), m2.getAccountMonthMap().entrySet().stream())
                                              .collect(Collectors.toMap(Map.Entry::getKey,
                                                                        Map.Entry::getValue,
                                                                        DeltaEventProcessingServiceImpl::addMovementDataMonth)));
        mergedValues.setIndividualPersonAccountNumbers(
                Stream.concat(m1.getIndividualPersonAccountNumbers().stream(), m2.getIndividualPersonAccountNumbers().stream()).collect(
                        Collectors.toSet()));
        return mergedValues;
    }

    private static MovementDataDay addMovementDataDy(MovementDataDay movementDataDay, MovementDataDay movementDataDay1) {
        return MovementDataDay.builder()
                              .consultant(movementDataDay.getConsultant())
                              .client(movementDataDay.getClient())
                              .fiscalYear(movementDataDay.getFiscalYear())
                              .accountNumber(movementDataDay.getAccountNumber())
                              .accountingReasonId(movementDataDay.getAccountingReasonId())
                              .additionalParams(movementDataDay.getAdditionalParams())
                              .values(Stream.concat(movementDataDay.getValues().entrySet().stream(), movementDataDay1.getValues().entrySet().stream())
                                            .collect(
                                                    Collectors.toMap(
                                                            Map.Entry::getKey,
                                                            Map.Entry::getValue,
                                                            DeltaEventProcessingServiceImpl::addValues)
                                            ))
                              .build();
    }

    private static MovementDataMonth addMovementDataMonth(MovementDataMonth movementDataMonth, MovementDataMonth movementDataMonth1) {
        return MovementDataMonth.builder()
                                .consultant(movementDataMonth.getConsultant())
                                .client(movementDataMonth.getClient())
                                .fiscalYear(movementDataMonth.getFiscalYear())
                                .accountNumber(movementDataMonth.getAccountNumber())
                                .accountingReasonId(movementDataMonth.getAccountingReasonId())
                                .additionalParams(movementDataMonth.getAdditionalParams())
                                .values(Stream.concat(movementDataMonth.getValues().entrySet().stream(),
                                                      movementDataMonth1.getValues().entrySet().stream())
                                              .collect(
                                                      Collectors.toMap(
                                                              Map.Entry::getKey,
                                                              Map.Entry::getValue,
                                                              DeltaEventProcessingServiceImpl::addValues)
                                              ))
                                .build();
    }

    private static AccountValue addValues(AccountValue accountValue, AccountValue accountValue1) {
        return AccountValue.builder()
                           .amountCredit(storeNullIfZero(nullSafeGet(accountValue.getAmountCredit()) + nullSafeGet(accountValue1.getAmountCredit())))
                           .amountDebit(storeNullIfZero(nullSafeGet(accountValue.getAmountDebit()) + nullSafeGet(accountValue1.getAmountDebit())))
                           .quantityDebit(
                                   storeNullIfZero(nullSafeGet(accountValue.getQuantityDebit()) + nullSafeGet(accountValue1.getQuantityDebit())))
                           .quantityCredit(
                                   storeNullIfZero(nullSafeGet(accountValue.getQuantityCredit()) + nullSafeGet(accountValue1.getQuantityCredit())))
                           .weightDebit(storeNullIfZero(nullSafeGet(accountValue.getWeightDebit()) + nullSafeGet(accountValue1.getWeightDebit())))
                           .weightCredit(storeNullIfZero(nullSafeGet(accountValue.getWeightCredit()) + nullSafeGet(accountValue1.getWeightCredit())))
                           .build();

    }

    private static AccountGroupValue addGroupValues(AccountGroupValue accountValue, AccountGroupValue accountValue1) {
        return AccountGroupValue.builder()
                                .amountCreditUsual(storeNullIfZero(
                                        nullSafeGet(accountValue.getAmountCreditUsual()) + nullSafeGet(accountValue1.getAmountCreditUsual())))
                                .amountCreditUnusual(storeNullIfZero(
                                        nullSafeGet(accountValue.getAmountCreditUnusual()) + nullSafeGet(accountValue1.getAmountCreditUnusual())))
                                .amountDebitUsual(storeNullIfZero(
                                        nullSafeGet(accountValue.getAmountDebitUsual()) + nullSafeGet(accountValue1.getAmountDebitUsual())))
                                .amountDebitUnusual(storeNullIfZero(
                                        nullSafeGet(accountValue.getAmountDebitUnusual()) + nullSafeGet(accountValue1.getAmountDebitUnusual())))
                                .quantityDebit(
                                        storeNullIfZero(nullSafeGet(accountValue.getQuantityDebit()) + nullSafeGet(accountValue1.getQuantityDebit())))
                                .quantityCredit(storeNullIfZero(
                                        nullSafeGet(accountValue.getQuantityCredit()) + nullSafeGet(accountValue1.getQuantityCredit())))
                                .weightDebit(
                                        storeNullIfZero(nullSafeGet(accountValue.getWeightDebit()) + nullSafeGet(accountValue1.getWeightDebit())))
                                .weightCredit(
                                        storeNullIfZero(nullSafeGet(accountValue.getWeightCredit()) + nullSafeGet(accountValue1.getWeightCredit())))
                                .build();

    }

    // besserer Name
    private Mono<DeltaEventDifferenceResult> calculateDifferencesToStoreInDatabase(List<MovementDataDay> existingBookings,
                                                                                   MasterdataContext masterdataContext,
                                                                                   List<AccountSumDay> deltaAccountSumDays) {
        // Convert existingBookings to AccountSumDay
        List<AccountSumDay> existingAccountSumDays = existingBookings.stream()
                                                                     .flatMap(movementDataDay -> accountSumDayMapper.mapDbToApiModel(movementDataDay)
                                                                                                                    .stream())
                                                                     .toList();

        // Calculate Delta Movementdata (first result)
        //TODO: Refactoring - should not do the group calculation; we dont need that.
        MovementDataAccountValues resultMovementDataAccountValues =
                getMovementDataAccountValues(masterdataContext, deltaAccountSumDays,
                                             new HashSet<>(), new LinkedHashMap<>(),
                                             new LinkedHashMap<>());

        // Calculate Difference for Persongroups
        PersonGroupDelta personGroupDelta = calculateDifferenceForPersongroup(masterdataContext, existingAccountSumDays, deltaAccountSumDays);

        return Mono.just(new DeltaEventDifferenceResult(resultMovementDataAccountValues, personGroupDelta));
    }

    private PersonGroupDelta calculateDifferenceForPersongroup(MasterdataContext masterdataContext,
                                                               List<AccountSumDay> existingAccountSumDays,
                                                               List<AccountSumDay> deltaValues) {
        Map<AccountDbKeyFields, MovementDataPersonGroupDay> existingPersonGroupDayMap = new LinkedHashMap<>();
        Map<AccountDbKeyFields, MovementDataPersonGroupMonth> existingPersonGroupMonthMap = new LinkedHashMap<>();
        MovementDataAccountValues existingMoventDataAccountValues =
                getMovementDataAccountValues(masterdataContext, existingAccountSumDays,
                                             new HashSet<>(), existingPersonGroupDayMap,
                                             existingPersonGroupMonthMap);

        Map<AccountDbKeyFields, MovementDataPersonGroupDay> afterDeltaPersonGroupDayMap = new LinkedHashMap<>();
        Map<AccountDbKeyFields, MovementDataPersonGroupMonth> afterDeltaPersonGroupMonthMap = new LinkedHashMap<>();
        List<AccountSumDay> extendedAccountSumDays = Stream.concat(existingAccountSumDays.stream(), deltaValues.stream()).sorted(
                Comparator.comparing(AccountSumDay::getAccountNumber)
                          .thenComparing(AccountSumDay::getDate)).toList();

        //HACK: to group the day Values we use the whole calculation -> should be easier but needs to separate the calculation logic.
        MovementDataAccountValues tempMovementAccountValues =
                getMovementDataAccountValues(masterdataContext, extendedAccountSumDays,
                                             new HashSet<>(), new LinkedHashMap<>(),
                                             new LinkedHashMap<>());

        List<AccountSumDay> aggregatedAccountSumDays = tempMovementAccountValues.getAccountDayMap().values().stream()
                                 .flatMap(movementDataDay -> accountSumDayMapper.mapDbToApiModel(movementDataDay)
                                                                                .stream())
                                 .toList();

        MovementDataAccountValues afterDeltaMovementAccountValues =
                getMovementDataAccountValues(masterdataContext,aggregatedAccountSumDays,
                                             new HashSet<>(), afterDeltaPersonGroupDayMap,
                                             afterDeltaPersonGroupMonthMap);


        return new PersonGroupDelta(PersonGroupDeltaCalculation.subtractDays(afterDeltaPersonGroupDayMap, existingPersonGroupDayMap),
                                    PersonGroupDeltaCalculation.subtractMonths(afterDeltaPersonGroupMonthMap, existingPersonGroupMonthMap),
                                    PersonGroupDeltaCalculation.subtractIndividualPersonAccounts(afterDeltaMovementAccountValues.getIndividualPersonAccountNumbers(),
                                                                                                 existingMoventDataAccountValues.getIndividualPersonAccountNumbers()),
                                    masterdataContext);
    }

    private Mono<List<BulkWriteResult>> writeDocumentDeltas(StateDoc stateDoc, ClientSession cs,
                                                            MovementDataAccountValues movementDataAccountValues,
                                                            PersonGroupDelta personGroupDifference) {
        return Flux.concat(incrementMovementDataDays(movementDataAccountValues.getAccountDayMap(), stateDoc, cs),
                           incrementMovementDataMonths(movementDataAccountValues.getAccountMonthMap(), stateDoc, cs)
                                   .flatMap(accountNummbers -> checkNonExistingAccounts(cs, stateDoc, accountNummbers)),
                           incrementMovementDataPersonGroupDays(personGroupDifference.personGroupDayMap(), stateDoc, cs),
                           incrementMovementDataPersonGroupMonths(personGroupDifference.personGroupMonthMap(), stateDoc, cs))
                   .collectList();
    }

    private Mono<Void> updateIndividualPersonAccounts(ClientSession session, Set<Integer> additionalIndividualPersonAccounts,
                                                      MasterdataContext masterdataContext) {
        if (additionalIndividualPersonAccounts != null && !additionalIndividualPersonAccounts.isEmpty()) {
            return masterDataRepository.addIndividualPersonAccounts(session, masterdataContext.getConsultant(), masterdataContext.getClient(),
                                                                    masterdataContext.getYearBegin(), additionalIndividualPersonAccounts).then();
        } else {
            return masterDataRepository.unsetIndividualPersonAccounts(session, masterdataContext.getConsultant(), masterdataContext.getClient(),
                                                                      masterdataContext.getYearBegin()).then();
        }
    }

    private Mono<DeltaInfo> updateVersionInfoInStateDoc(Long baseVersion, Long deltaVersion, StateDoc stateDoc, ClientSession cs) {
        Integer consultant = stateDoc.getConsultant();
        Integer fiscalYear = stateDoc.getYearBegin();
        Integer client = stateDoc.getClient();
        log.debug("Writing version Info in State Doc. Transaction state: {}", cs.hasActiveTransaction());
        return stateDocRepository.updateVersionInfo(consultant, client, fiscalYear, cs, mongoClient,
                                                    baseVersion, deltaVersion)
                                 .doOnNext(updateResult -> log.info(
                                         "Updated version info in state doc for consultant: {}, client: {}, fiscal year: {} - {} documents updated",
                                         consultant, client, fiscalYear, updateResult.getModifiedCount()))
                                 .doOnError(
                                         throwable -> log.error(
                                                 "Error updating version info in state doc for consultant: {}, client: {}, fiscal year: {}",
                                                 consultant, client, fiscalYear, throwable))
                                 .flatMap(afterUpdate -> Mono.just(new DeltaInfo(baseVersion, deltaVersion)));
    }

    private Mono<Void> writeNearTimeDataFlagInMasterDataContext(DeltaRequest deltaRequest, StateDoc stateDoc, ClientSession clientSession) {
        Integer consultant = stateDoc.getConsultant();
        Integer client = stateDoc.getClient();
        Integer fiscalYear = stateDoc.getYearBegin();
        log.debug("Writing near time data flag. Transaction state: {}", clientSession.hasActiveTransaction());
        return masterDataRepository.writeNearTimeDataFlag(consultant, client, fiscalYear, clientSession, mongoClient, deltaRequest.getContainsNearTimeData())
                                   .doOnNext(updateResult -> log.debug(
                                     "Updated near time data flag for consultant: {}, client: {}, fiscal year: {} - {} documents updated",
                                     consultant, client, fiscalYear, updateResult.getModifiedCount()))
                                   .doOnError(throwable -> log.error(
                                           "Error updating near time data flag for consultant: {}, client: {}, fiscal year: {}",
                                                               consultant, client, fiscalYear, throwable))
                                   .then();
    }

    private Mono<BulkWriteResult> checkNonExistingAccounts(ClientSession session, StateDoc stateDoc, List<Integer> accountNumbersList) {
        if (accountNumbersList == null || accountNumbersList.isEmpty()) {
            return Mono.empty();
        }
        return findAndUpdateMasterDataAccounts(session, stateDoc, accountNumbersList);
    }

    private Mono<BulkWriteResult> findAndUpdateMasterDataAccounts(ClientSession session, StateDoc stateDoc, List<Integer> accountNumbersList) {
        Set<Integer> newAccountNumbers = new HashSet<>(accountNumbersList);
        return masterDataAccountRepository.findAllByBusinessKeyAndAccountNumbers(session, stateDoc.getConsultant(), stateDoc.getClient(),
                                                                                 stateDoc.getYearBegin(), newAccountNumbers)
                                          .map(masterDataAccount -> {
                                              updateAccountCaptions(masterDataAccount, newAccountNumbers);
                                              masterDataAccount.setUsed(true);
                                              return masterDataAccount;
                                          })
                                          .collectList()
                                          .flatMap(list -> {
                                              if (list.isEmpty()) {
                                                  // keine Accounts gefunden → komplettes Mono empty
                                                  return Mono.empty();
                                              }
                                              // sonst bulkUpdate aufrufen
                                              return masterDataAccountRepository.bulkUpdate(list);
                                          });
    }

    private void updateAccountCaptions(MasterDataAccount masterDataAccount, Set<Integer> accountNumbers) {
        accountNumbers.forEach(accountNumber -> {
            if (accountNumber >= masterDataAccount.getAccountNumberFrom() && accountNumber <= masterDataAccount.getAccountNumberTo()) {
                Optional<AccountDescription> accountDescription = masterDataAccount.getAccountCaptions()
                                                                                   .stream()
                                                                                   .filter(ad -> ad.getAccountNumber().equals(accountNumber))
                                                                                   .findFirst();
                if (accountDescription.isPresent()) {
                    accountDescription.get().setUsed(true);
                } else {
                    masterDataAccount.getAccountCaptions().add(AccountDescription.builder()
                                                          .accountNumber(accountNumber)
                                                          .used(true).build());
                }
            }

        });
    }

    private Mono<BulkWriteResult> incrementMovementDataDays(Map<AccountDbKeyFields, MovementDataDay> accountDayMap,
                                                            StateDoc stateDoc, ClientSession clientSession) {
        log.debug("incrementMovementDataDays transaction={}", clientSession.hasActiveTransaction());
        return accountDayMap.values().isEmpty() ? Mono.empty() : movementDataDayRepository.bulkUpsert(stateDoc.getConsultant(),
                                                                                                      stateDoc.getClient(),
                                                                                                      stateDoc.getYearBegin(),
                                                                                                      accountDayMap, clientSession);
    }

    protected Mono<List<Integer>> incrementMovementDataMonths(Map<AccountDbKeyFields, MovementDataMonth> accountMonthMap,
                                                                StateDoc stateDoc, ClientSession clientSession) {
        log.debug("incrementMovementDataMonths transaction={}", clientSession.hasActiveTransaction());
        return accountMonthMap.values().isEmpty() ? Mono.empty() : movementDataMonthRepository.bulkUpsert(stateDoc.getConsultant(),
                                                                                                          stateDoc.getClient(),
                                                                                                          stateDoc.getYearBegin(),
                                                                                                          accountMonthMap, clientSession);
    }

    private Mono<BulkWriteResult> incrementMovementDataPersonGroupDays(Map<AccountDbKeyFields, MovementDataPersonGroupDay> accountPersonGroupDayMap,
                                                                       StateDoc stateDoc, ClientSession clientSession) {
        log.debug("incrementMovementPersonGroupDays transaction={}", clientSession.hasActiveTransaction());
        return accountPersonGroupDayMap.values().isEmpty() ? Mono.empty() : personGroupDayRepository.bulkUpdate(stateDoc.getConsultant(),
                                                                                                                stateDoc.getClient(),
                                                                                                                stateDoc.getYearBegin(),
                                                                                                                accountPersonGroupDayMap,
                                                                                                                clientSession);
    }

    private Mono<BulkWriteResult> incrementMovementDataPersonGroupMonths(
            Map<AccountDbKeyFields, MovementDataPersonGroupMonth> accountPersonGroupMonthMap,
            StateDoc stateDoc, ClientSession clientSession) {
        log.debug("incrementMovementDataPersonGroupMonths transaction={}", clientSession.hasActiveTransaction());
        return accountPersonGroupMonthMap.values().isEmpty() ? Mono.empty() : personGroupMonthRepository.bulkUpdate(stateDoc.getConsultant(),
                                                                                                                    stateDoc.getClient(),
                                                                                                                    stateDoc.getYearBegin(),
                                                                                                                    accountPersonGroupMonthMap,
                                                                                                                    clientSession);
    }
}
