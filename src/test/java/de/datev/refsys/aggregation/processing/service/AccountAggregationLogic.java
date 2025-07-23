package de.datev.refsys.aggregation.processing.service;

import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoCollection;
import com.mongodb.reactivestreams.client.MongoDatabase;
import de.datev.refsys.aggregation.processing.client.CustomStructuresClient;
import de.datev.refsys.aggregation.processing.mapper.CustomColumnStructureContentMapper;
import de.datev.refsys.aggregation.processing.mapper.CustomColumnStructureInfoMapper;
import de.datev.refsys.aggregation.processing.mapper.CustomReportStructureContentMapper;
import de.datev.refsys.aggregation.processing.mapper.CustomReportStructureInfoMapper;
import de.datev.refsys.aggregation.processing.repository.CustomColumnStructureContentRepository;
import de.datev.refsys.aggregation.processing.repository.CustomReportStructureContentRepository;
import de.datev.refsys.generated.acds.api.model.AccountSumDay;
import de.datev.refsys.generated.acds.api.model.AccountSumMonth;
import de.datev.refsys.generated.acds.api.model.MasterdataContext;
import de.datev.refsys.generated.acds.api.model.ShareholderData;
import de.datev.refsys.generated.acds.api.model.Translation;
import de.datev.refsys.aggregation.document.model.AccountGroupValue;
import de.datev.refsys.aggregation.document.model.AccountValue;
import de.datev.refsys.aggregation.document.model.AdditionalParameters;
import de.datev.refsys.aggregation.document.model.MasterData;
import de.datev.refsys.aggregation.document.model.MovementDataDay;
import de.datev.refsys.aggregation.document.model.MovementDataMonth;
import de.datev.refsys.aggregation.document.model.MovementDataPersonGroupDay;
import de.datev.refsys.aggregation.document.model.MovementDataPersonGroupMonth;
import de.datev.refsys.aggregation.document.model.StateDoc;
import de.datev.refsys.aggregation.processing.client.MasterDataClient;
import de.datev.refsys.aggregation.processing.client.MovementDataClient;
import de.datev.refsys.aggregation.processing.config.InitialLoadConfiguration;
import de.datev.refsys.aggregation.processing.mapper.AccountDbKeyFieldsMapper;
import de.datev.refsys.aggregation.processing.mapper.AccountSumDayMapper;
import de.datev.refsys.aggregation.processing.mapper.AccountValueMapper;
import de.datev.refsys.aggregation.processing.mapper.AdditionalParametersMapper;
import de.datev.refsys.aggregation.processing.mapper.AlternativeAccountTranslationMapper;
import de.datev.refsys.aggregation.processing.mapper.CollectiveAccountMapper;
import de.datev.refsys.aggregation.processing.mapper.InventoryDbKeyFieldsMapper;
import de.datev.refsys.aggregation.processing.mapper.MasterDataContextMapper;
import de.datev.refsys.aggregation.processing.mapper.PreviousYearAccountTranslationMapper;
import de.datev.refsys.aggregation.processing.mapper.ProblemInfoMapper;
import de.datev.refsys.aggregation.processing.mapper.ShareholderAdditionMapper;
import de.datev.refsys.aggregation.processing.mapper.ShareholderAddressMapper;
import de.datev.refsys.aggregation.processing.mapper.ShareholderRelationMapper;
import de.datev.refsys.aggregation.processing.mapper.ShareholderTaxOfficeMapper;
import de.datev.refsys.aggregation.processing.model.ImportData;
import de.datev.refsys.aggregation.processing.repository.MasterDataAccountRepository;
import de.datev.refsys.aggregation.processing.repository.MasterDataRepository;
import de.datev.refsys.aggregation.processing.repository.MovementDataDayRepository;
import de.datev.refsys.aggregation.processing.repository.MovementDataInventoryRepository;
import de.datev.refsys.aggregation.processing.repository.MovementDataMonthRepository;
import de.datev.refsys.aggregation.processing.repository.MovementDataPersonGroupDayRepository;
import de.datev.refsys.aggregation.processing.repository.MovementDataPersonGroupMonthRepository;
import de.datev.refsys.aggregation.processing.repository.StateDocRepository;
import de.datev.refsys.aggregation.processing.util.DatabaseUtils;
import de.datev.refsys.aggregation.processing.util.TestDataLoader;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.Before;
import io.cucumber.java.DataTableType;
import io.cucumber.java.de.Angenommen;
import io.cucumber.java.de.Dann;
import io.cucumber.java.de.Und;
import io.cucumber.java.de.Wenn;
import io.micrometer.core.instrument.MeterRegistry;
import org.awaitility.Awaitility;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Value;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static de.datev.refsys.aggregation.document.model.constants.CollectionConstants.DAY_PREFIX;
import static de.datev.refsys.aggregation.document.model.constants.CollectionConstants.MASTER_DATA;
import static de.datev.refsys.aggregation.document.model.constants.CollectionConstants.MONTH_PREFIX;
import static de.datev.refsys.aggregation.document.model.constants.CollectionConstants.MOVEMENT_DATA_DAYS;
import static de.datev.refsys.aggregation.document.model.constants.CollectionConstants.MOVEMENT_DATA_MONTHS;
import static de.datev.refsys.aggregation.document.model.constants.CollectionConstants.MOVEMENT_DATA_PERSON_GROUP_DAYS;
import static de.datev.refsys.aggregation.document.model.constants.CollectionConstants.MOVEMENT_DATA_PERSON_GROUP_MONTHS;
import static de.datev.refsys.aggregation.document.model.constants.CollectionConstants.OPENING_BALANCE;
import static de.datev.refsys.aggregation.processing.mapper.AccountValueMapper.doubleToLongAmountInCent;
import static de.datev.refsys.aggregation.processing.util.QueryUtil.deleteMovementData;
import static de.datev.refsys.aggregation.processing.util.QueryUtil.getByMasterDataBusinessKey;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_CLIENT;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_CONSULTANT;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_FISCAL_YEAR_2021_START;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class AccountAggregationLogic {
    private static final long WAIT_FOR_STORAGE_IN_MONGO_IN_SECONDS = 2L;
    protected final MongoCollection<MovementDataDay> movementDataDayCollection;
    protected final MongoCollection<MovementDataMonth> movementDataMonthCollection;
    protected final MongoCollection<MovementDataPersonGroupDay> movementDataPersonGroupDayCollection;
    protected final MongoCollection<MovementDataPersonGroupMonth> movementDataPersonGroupMonthCollection;
    private final ImportServiceImpl importService;
    private final MongoCollection<MasterData> masterDataCollection;
    private final MovementDataClient movementDataClient;
    private final MongoDatabase mongoDatabase;
    private ImportData importData;
    private Integer fiscalYearStart = TEST_FISCAL_YEAR_2021_START;

    public AccountAggregationLogic(final MongoClient insertMongoClient, @Value("${spring.data.mongodb.database}") final String databaseName,
                                   final StateDocRepository stateDocRepository, final MasterDataRepository masterDataRepository,
                                   final MovementDataDayRepository movementDataDayRepository,
                                   final MovementDataMonthRepository movementDataMonthRepository,
                                   final MovementDataPersonGroupDayRepository personGroupDayRepository,
                                   final MovementDataPersonGroupMonthRepository personGroupMonthRepository,
                                   final MasterDataAccountRepository masterDataAccountRepository,
                                   final MovementDataInventoryRepository movementDataInventoryRepository,
                                   final MeterRegistry meterRegistry, final AdditionalParametersMapper additionalParametersMapper,
                                   final MasterDataContextMapper masterDataContextMapper, final CollectiveAccountMapper collectiveAccountMapper,
                                   final AlternativeAccountTranslationMapper alternativeAccountTranslationMapper,
                                   final PreviousYearAccountTranslationMapper previousYearAccountTranslationMapper,
                                   final ShareholderRelationMapper shareholderRelationMapper,
                                   final ShareholderAdditionMapper shareholderAdditionMapper,
                                   final ShareholderAddressMapper shareholderAddressMapper,
                                   final ShareholderTaxOfficeMapper shareholderTaxOfficeMapper,
                                   final AccountDbKeyFieldsMapper accountDbKeyFieldsMapper, final AccountValueMapper accountValueMapper,
                                   final InitialLoadConfiguration initialLoadConfiguration,
                                   final InventoryDbKeyFieldsMapper inventoryDbKeyFieldsMapper,
                                   final AccountSumDayMapper accountSumDayMapper,
                                   final CustomReportStructureContentMapper customReportStructureContentMapper,
                                   final CustomColumnStructureContentMapper customColumnStructureContentMapper,
                                   final CustomReportStructureInfoMapper customReportStructureInfoMapper,
                                   final CustomColumnStructureInfoMapper customColumnStructureInfoMapper,
                                   final CustomReportStructureContentRepository customReportStructureContentRepository,
                                   final CustomColumnStructureContentRepository customColumnStructureContentRepository) {

        MasterDataClient masterDataClient = Mockito.mock(MasterDataClient.class);
        this.movementDataClient = Mockito.mock(MovementDataClient.class);
        CustomStructuresClient customStructuresClient = Mockito.mock(CustomStructuresClient.class);
        when(customStructuresClient.getCustomReportStructuresList(any())).thenReturn(Mono.just(new ArrayList<>()));
        when(customStructuresClient.getCustomColumnStructuresList(any())).thenReturn(Mono.just(new ArrayList<>()));
        CustomStructuresService customStructuresService =
                new CustomStructuresServiceImpl(customStructuresClient, customReportStructureContentRepository,
                                                customColumnStructureContentRepository, customReportStructureContentMapper,
                                                customColumnStructureContentMapper, customReportStructureInfoMapper, customColumnStructureInfoMapper);
        this.importService = new ImportServiceImpl(stateDocRepository, masterDataRepository, movementDataDayRepository, movementDataMonthRepository,
                                                   personGroupDayRepository, personGroupMonthRepository, masterDataAccountRepository,
                                                   movementDataInventoryRepository, meterRegistry,
                                                   additionalParametersMapper, masterDataContextMapper,
                                                   collectiveAccountMapper, alternativeAccountTranslationMapper, previousYearAccountTranslationMapper,
                                                   shareholderRelationMapper, accountDbKeyFieldsMapper, accountValueMapper, shareholderAdditionMapper,
                                                   shareholderAddressMapper, shareholderTaxOfficeMapper,
                                                   masterDataClient, movementDataClient, initialLoadConfiguration,
                                                   inventoryDbKeyFieldsMapper, accountSumDayMapper, customStructuresService);
        MasterdataContext masterdataContext = TestDataLoader.load("json/acds-responses/masterDataContext.json", MasterdataContext.class);
        this.importData = new ImportData(masterdataContext, StateDoc.builder().stateTimestamp(OffsetDateTime.now()).build());
        this.mongoDatabase = insertMongoClient.getDatabase(databaseName);
        this.movementDataDayCollection = mongoDatabase.getCollection(MOVEMENT_DATA_DAYS, MovementDataDay.class);
        this.movementDataMonthCollection = mongoDatabase.getCollection(MOVEMENT_DATA_MONTHS, MovementDataMonth.class);
        this.movementDataPersonGroupDayCollection = mongoDatabase
                                                                     .getCollection(MOVEMENT_DATA_PERSON_GROUP_DAYS,
                                                                                    MovementDataPersonGroupDay.class);
        this.movementDataPersonGroupMonthCollection = mongoDatabase
                                                                       .getCollection(MOVEMENT_DATA_PERSON_GROUP_MONTHS,
                                                                                      MovementDataPersonGroupMonth.class);
        this.masterDataCollection = mongoDatabase.getCollection(MASTER_DATA, MasterData.class);
        when(masterDataClient.getAccountCaptions(any())).thenReturn(Mono.just(new ArrayList<>()));
        when(masterDataClient.getAccountPurposeMappings(any())).thenReturn(Mono.just(new ArrayList<>()));
        when(masterDataClient.getCollectiveAccounts(any())).thenReturn(Mono.just(new ArrayList<>()));
        when(masterDataClient.getShareholderData(any())).thenReturn(Mono.just(new ShareholderData()));
        when(masterDataClient.getTranslation(any())).thenReturn(Mono.just(new Translation()));
        when(masterDataClient.getInventories(any())).thenReturn(Mono.just(new ArrayList<>()));
        when(movementDataClient.getMovementDataInventories(any())).thenReturn(Flux.fromIterable(new ArrayList<>()));
    }

    @Before
    public void setUp() {
        DatabaseUtils.clearDatabase(mongoDatabase);
        DatabaseUtils.createIndexes(mongoDatabase);
    }

    @Angenommen("folgende Tagessummen")
    public void folgendeTagessummen(List<AccountSumDay> accountSumDays) {
        when(movementDataClient.getAccountSumDays(any())).thenReturn(Flux.fromIterable(accountSumDays));
    }

    @Wenn("Tagessummen verarbeitet werden")
    public void tagessummenVerarbeitetWerden() {
        importService.executeFullImport(importData).block();
    }

    @Dann("werden folgende Tageswerte ermittelt")
    public void werdenFolgendeTageswerteErmittelt(DataTable dataTable) {
        Awaitility.await().timeout(WAIT_FOR_STORAGE_IN_MONGO_IN_SECONDS, TimeUnit.SECONDS).untilAsserted(() -> {
            List<MovementDataDay> movementDataDays =
                    Flux.from(movementDataDayCollection.find(deleteMovementData(TEST_CONSULTANT, TEST_CLIENT, fiscalYearStart))).collectList()
                        .block();
            assertThat(movementDataDays).isNotNull();
            dataTable.asMaps().forEach(row -> {
                Integer accountNumber = Integer.valueOf(row.get("Kontonummer"));
                Integer accountingReasonId = Integer.valueOf(row.get("Bereichsnummer"));
                Integer recordType =
                        row.get("Buchungstyp") == null ? null : AdditionalParametersMapper.checkRecordType(Integer.valueOf(row.get("Buchungstyp")));
                Optional<MovementDataDay> movementDataDay = movementDataDays.stream()
                                                                            .filter(mdd -> {
                                                                                AdditionalParameters additionalParams =
                                                                                        mdd.getAdditionalParams();
                                                                                Integer actualRecordType = additionalParams == null ? null :
                                                                                        additionalParams.getRecordType();
                                                                                return accountNumber.equals(mdd.getAccountNumber())
                                                                                        && accountingReasonId.equals(
                                                                                        mdd.getAccountingReasonId())
                                                                                        && Objects.equals(recordType, actualRecordType);
                                                                            })
                                                                            .findFirst();
                String rawDay = row.get("Buchungstag");
                assertThat(movementDataDay).as("Movement Data for %s/%s should have a value", accountNumber, rawDay).isPresent();
                assertThat(movementDataDay.get().getAccountNumber()).isEqualTo(accountNumber);
                AccountValue accountValue;
                if (rawDay.equals(OPENING_BALANCE)) {
                    accountValue = movementDataDay.get().getValues().get(rawDay);
                } else {
                    int day = Integer.parseInt(rawDay);
                    accountValue = movementDataDay.get().getValues().get(DAY_PREFIX + day);
                }
                assertThat(accountValue).as("Account for %s/%s should have a value", accountNumber, rawDay).isNotNull();
                String amountCredit = row.get("SummeHaben");
                String amountDebit = row.get("SummeSoll");
                assertThat(accountValue.getAmountCredit()).as("Amount Credit for %s/%s should be correct", accountNumber, rawDay)
                                                          .isEqualTo(amountCredit != null ?
                                                                             doubleToLongAmountInCent(Double.valueOf(amountCredit)) :
                                                                             null);
                assertThat(accountValue.getAmountDebit()).as("Amount Debit for %s/%s should be correct", accountNumber, rawDay)
                                                         .isEqualTo(
                                                                 amountDebit != null ? doubleToLongAmountInCent(Double.valueOf(amountDebit)) : null);
                String accountingCommited = row.get("Festschreibung");
                Boolean expectedAccountingCommitted = accountingCommited == null ? null : Boolean.valueOf(accountingCommited);
                if (expectedAccountingCommitted != null) {
                    AdditionalParameters additionalParams =
                            movementDataDay.get().getAdditionalParams();
                    Boolean actualAccountingCommitted = additionalParams == null ? null :
                            additionalParams.getAccountingCommitted();
                    assertThat(actualAccountingCommitted).as("Festschreibung should be correct").isEqualTo(expectedAccountingCommitted);
                }
            });
        });
    }

    @Und("werden folgende Monatswerte ermittelt")
    public void werdenFolgendeMonatswerteErmittelt(DataTable dataTable) {
        List<MovementDataMonth> movementDataMonths =
                Flux.from(movementDataMonthCollection.find(deleteMovementData(TEST_CONSULTANT, TEST_CLIENT, fiscalYearStart)))
                    .collectList().block();
        assertThat(movementDataMonths).isNotNull();
        dataTable.asMaps().forEach(row -> {
            Integer accountNumber = Integer.valueOf(row.get("Kontonummer"));
            Integer accountingReasonId = Integer.valueOf(row.get("Bereichsnummer"));
            Integer recordType =
                    row.get("Buchungstyp") == null ? null : AdditionalParametersMapper.checkRecordType(Integer.valueOf(row.get("Buchungstyp")));
            Optional<MovementDataMonth> movementDataMonth = movementDataMonths.stream()
                                                                              .filter(mdm ->
                                                                                      {
                                                                                          AdditionalParameters additionalParams =
                                                                                                  mdm.getAdditionalParams();
                                                                                          Integer actualRecordType =
                                                                                                  additionalParams == null ? null :
                                                                                                          additionalParams.getRecordType();
                                                                                          return accountNumber.equals(mdm.getAccountNumber())
                                                                                                  && accountingReasonId.equals(
                                                                                                  mdm.getAccountingReasonId())
                                                                                                  && Objects.equals(recordType, actualRecordType);
                                                                                      }
                                                                              )
                                                                              .findFirst();
            String rawMonth = row.get("WJ-Monat");
            assertThat(movementDataMonth).as("Movement Data for %s/%s should have a value", accountNumber, rawMonth).isPresent();
            assertThat(movementDataMonth.get().getAccountNumber()).isEqualTo(accountNumber);
            AccountValue accountValue;
            if (rawMonth.equals(OPENING_BALANCE)) {
                accountValue = movementDataMonth.get().getValues().get(rawMonth);
            } else {
                int month = Integer.parseInt(rawMonth);
                accountValue = movementDataMonth.get().getValues().get(MONTH_PREFIX + month);
            }
            assertThat(accountValue).isNotNull();
            String amountCredit = row.get("SummeHaben");
            String amountDebit = row.get("SummeSoll");
            assertThat(accountValue.getAmountCredit()).isEqualTo(
                    amountCredit != null ? doubleToLongAmountInCent(Double.valueOf(amountCredit)) : null);
            assertThat(accountValue.getAmountDebit()).isEqualTo(
                    amountDebit != null ? doubleToLongAmountInCent(Double.valueOf(amountDebit)) : null);
        });
    }

    @Und("werden folgende Tageswerte der Personenkontengruppen ermittelt")
    public void werdenFolgendeTageswerteDerPersonenkontengruppenErmittelt(DataTable dataTable) {
        List<MovementDataPersonGroupDay> movementDataPersonGroupDays = Flux.from(movementDataPersonGroupDayCollection
                                                                                         .find(deleteMovementData(TEST_CONSULTANT, TEST_CLIENT,
                                                                                                                  fiscalYearStart)))
                                                                           .collectList().block();
        assertThat(movementDataPersonGroupDays).isNotNull();
        if (dataTable.asMaps().isEmpty()) {
            assertThat(movementDataPersonGroupDays).isEmpty();
        } else {
            dataTable.asMaps().forEach(row -> {
                Integer accountNumber = Integer.valueOf(row.get("Kontogruppennummer"));
                Integer accountingReasonId = Integer.valueOf(row.get("Bereichsnummer"));
                Integer recordType =
                        row.get("Buchungstyp") == null ? null : AdditionalParametersMapper.checkRecordType(Integer.valueOf(row.get("Buchungstyp")));
                Optional<MovementDataPersonGroupDay> personGroupDay = movementDataPersonGroupDays.stream()
                                                                                                 .filter(pgd -> accountNumber.equals(
                                                                                                         pgd.getAccountGroupNumber())
                                                                                                         && accountingReasonId.equals(
                                                                                                         pgd.getAccountingReasonId())
                                                                                                         && Objects.equals(recordType,
                                                                                                                           pgd.getAdditionalParams()
                                                                                                                              .getRecordType()))
                                                                                                 .findFirst();
                assertThat(personGroupDay).isPresent();
                assertThat(personGroupDay.get().getAccountGroupNumber()).isEqualTo(accountNumber);
                String rawDay = row.get("Buchungstag");
                AccountGroupValue accountGroupValue;
                if (rawDay.equals(OPENING_BALANCE)) {
                    accountGroupValue = personGroupDay.get().getValues().get(rawDay);
                } else {
                    int day = Integer.parseInt(rawDay);
                    accountGroupValue = personGroupDay.get().getValues().get(DAY_PREFIX + day);
                }
                assertThat(accountGroupValue).isNotNull();
                String amountCreditUsual = row.get("SummeHabenTypisch");
                String amountCreditUnusual = row.get("SummeHabenUntypisch");
                String amountDebitUsual = row.get("SummeSollTypisch");
                String amountDebitUnusual = row.get("SummeSollUntypisch");
                assertThat(accountGroupValue.getAmountCreditUsual()).as("Usual credit amount for %s/%s should be correct", accountNumber, rawDay)
                                                                    .isEqualTo(
                                                                            amountCreditUsual != null ?
                                                                                    doubleToLongAmountInCent(Double.valueOf(amountCreditUsual)) :
                                                                                    null);
                assertThat(accountGroupValue.getAmountCreditUnusual()).isEqualTo(
                        amountCreditUnusual != null ? doubleToLongAmountInCent(Double.valueOf(amountCreditUnusual)) : null);
                assertThat(accountGroupValue.getAmountDebitUsual()).isEqualTo(
                        amountDebitUsual != null ? doubleToLongAmountInCent(Double.valueOf(amountDebitUsual)) : null);
                assertThat(accountGroupValue.getAmountDebitUnusual()).isEqualTo(
                        amountDebitUnusual != null ? doubleToLongAmountInCent(Double.valueOf(amountDebitUnusual)) : null);
            });
        }
    }

    @Und("werden folgende Monatswerte der Personenkontengruppen ermittelt")
    public void werdenFolgendeMonatswerteDerPersonenkontengruppenErmittelt(DataTable dataTable) {
        List<MovementDataPersonGroupMonth> movementDataPersonGroupMonths = Flux.from(movementDataPersonGroupMonthCollection
                                                                                             .find(deleteMovementData(TEST_CONSULTANT, TEST_CLIENT,
                                                                                                                      fiscalYearStart)))
                                                                               .collectList().block();
        assertThat(movementDataPersonGroupMonths).isNotNull();
        if (dataTable.asMaps().isEmpty()) {
            assertThat(movementDataPersonGroupMonths).isEmpty();
        } else {
            dataTable.asMaps().forEach(row -> {
                Integer accountNumber = Integer.valueOf(row.get("Kontogruppennummer"));
                Integer accountingReasonId = Integer.valueOf(row.get("Bereichsnummer"));
                Integer recordType =
                        row.get("Buchungstyp") == null ? null : AdditionalParametersMapper.checkRecordType(Integer.valueOf(row.get("Buchungstyp")));
                Optional<MovementDataPersonGroupMonth> personGroupMonth = movementDataPersonGroupMonths.stream()
                                                                                                       .filter(pgm -> accountNumber.equals(
                                                                                                               pgm.getAccountGroupNumber())
                                                                                                               && accountingReasonId.equals(
                                                                                                               pgm.getAccountingReasonId())
                                                                                                               && Objects.equals(recordType,
                                                                                                                                 pgm.getAdditionalParams()
                                                                                                                                    .getRecordType()))
                                                                                                       .findFirst();
                assertThat(personGroupMonth).isPresent();
                assertThat(personGroupMonth.get().getAccountGroupNumber()).isEqualTo(Integer.valueOf(row.get("Kontogruppennummer")));
                String rawMonth = row.get("WJ-Monat");
                AccountGroupValue accountGroupValue;
                if (rawMonth.equals(OPENING_BALANCE)) {
                    accountGroupValue = personGroupMonth.get().getValues().get(rawMonth);
                } else {
                    int month = Integer.parseInt(rawMonth);
                    accountGroupValue = personGroupMonth.get().getValues().get(MONTH_PREFIX + month);
                }
                assertThat(accountGroupValue).isNotNull();
                String amountCreditUsual = row.get("SummeHabenTypisch");
                String amountCreditUnusual = row.get("SummeHabenUntypisch");
                String amountDebitUsual = row.get("SummeSollTypisch");
                String amountDebitUnusual = row.get("SummeSollUntypisch");
                assertThat(accountGroupValue.getAmountCreditUsual()).isEqualTo(
                        amountCreditUsual != null ? doubleToLongAmountInCent(Double.valueOf(amountCreditUsual)) : null);
                assertThat(accountGroupValue.getAmountCreditUnusual()).isEqualTo(
                        amountCreditUnusual != null ? doubleToLongAmountInCent(Double.valueOf(amountCreditUnusual)) : null);
                assertThat(accountGroupValue.getAmountDebitUsual()).isEqualTo(
                        amountDebitUsual != null ? doubleToLongAmountInCent(Double.valueOf(amountDebitUsual)) : null);
                assertThat(accountGroupValue.getAmountDebitUnusual()).isEqualTo(
                        amountDebitUnusual != null ? doubleToLongAmountInCent(Double.valueOf(amountDebitUnusual)) : null);
            });
        }
    }

    @Und("folgende Personenkonten werden individuel betrachtet")
    public void folgendePersonenkontenWerdenIndividuelBetrachtet(DataTable dataTable) {
        MasterData masterData =
                Mono.from(masterDataCollection.find(getByMasterDataBusinessKey(TEST_CONSULTANT, TEST_CLIENT, fiscalYearStart))).block();
        if (dataTable.asMaps().isEmpty()) {
            assertThat(masterData).isNotNull();
            assertThat(masterData.getIndividualPersonAccountNumbers()).isNull();
        } else {
            assertThat(masterData).isNotNull();
            assertThat(dataTable.asMaps()).hasSameSizeAs(masterData.getIndividualPersonAccountNumbers());
            dataTable.asMaps(String.class, Integer.class).forEach(ipa -> {
                assertThat(masterData.getIndividualPersonAccountNumbers()).contains(ipa.get("Kontonummer"));
            });

        }
    }

    @DataTableType
    public AccountSumDay accountSumDayTransformer(Map<String, String> inputRow) {
        String amountCredit = inputRow.get("SummeHaben");
        String amountDebit = inputRow.get("SummeSoll");
        return new AccountSumDay().accountNumber(Integer.valueOf(inputRow.get("Kontonummer")))
                                  .accountingReasonId(Integer.valueOf(inputRow.get("Bereichsnummer")))
                                  .date(Integer.valueOf(inputRow.get("Buchungstag")))
                                  .month(Integer.valueOf(inputRow.get("WJ-Monat")))
                                  .recordType(Integer.valueOf(inputRow.get("Buchungstyp")))
                                  .amountCredit(amountCredit != null ? Double.valueOf(amountCredit) : null)
                                  .amountDebit(amountDebit != null ? Double.valueOf(amountDebit) : null);
    }

    @DataTableType
    public AccountSumMonth accountSumMonthTransformer(Map<String, String> inputRow) {
        String amountCredit = inputRow.get("SummeHaben");
        String amountDebit = inputRow.get("SummeSoll");
        return new AccountSumMonth().accountNumber(Integer.valueOf(inputRow.get("Kontonummer")))
                                    .accountingReasonId(Integer.valueOf(inputRow.get("Bereichsnummer")))
                                    .month(Integer.valueOf(inputRow.get("WJ-Monat")))
                                    .recordType(Integer.valueOf(inputRow.get("Buchungstyp")))
                                    .amountCredit(amountCredit != null ? Double.valueOf(amountCredit) : null)
                                    .amountDebit(amountDebit != null ? Double.valueOf(amountDebit) : null);
    }

    @Angenommen("Bestand enthaelt kmvz")
    public void bestandEnthaeltKmvz() {
        MasterdataContext masterdataContext = TestDataLoader.load("json/acds-responses/masterDataContextKmvz.json", MasterdataContext.class);
        this.importData = new ImportData(masterdataContext, StateDoc.builder().stateTimestamp(OffsetDateTime.now()).build());
    }

    @Angenommen("Wirtschaftsjahr beginnt am {int}")
    public void wirtschaftsjahrBeginnt(int wirtschaftsjahrStart) {

        this.importData.masterdataContext().setYearBegin(wirtschaftsjahrStart);
        this.fiscalYearStart = wirtschaftsjahrStart;
    }

    @Angenommen("Wirtschaftsjahr endet am {int}")
    public void wirtschaftsjahrEndet(int wirtschaftsjahrEnde) {

        this.importData.masterdataContext().setYearEnd(wirtschaftsjahrEnde);
    }
}
