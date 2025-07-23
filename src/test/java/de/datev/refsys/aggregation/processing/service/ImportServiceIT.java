package de.datev.refsys.aggregation.processing.service;

import ch.qos.logback.classic.Level;
import com.fasterxml.jackson.core.type.TypeReference;
import de.datev.refsys.aggregation.document.model.AdditionalParameters;
import de.datev.refsys.aggregation.document.model.CustomColumnStructureContent;
import de.datev.refsys.aggregation.document.model.CustomReportStructureContent;
import de.datev.refsys.aggregation.document.model.MasterData;
import de.datev.refsys.aggregation.document.model.MasterDataAccount;
import de.datev.refsys.aggregation.document.model.MovementDataDay;
import de.datev.refsys.aggregation.document.model.MovementDataInventory;
import de.datev.refsys.aggregation.document.model.StateDoc;
import de.datev.refsys.aggregation.document.model.enums.StateDocState;
import de.datev.refsys.aggregation.processing.client.CustomStructuresClient;
import de.datev.refsys.aggregation.processing.client.MasterDataClient;
import de.datev.refsys.aggregation.processing.client.MovementDataClient;
import de.datev.refsys.aggregation.processing.config.InitialLoadConfiguration;
import de.datev.refsys.aggregation.processing.config.TestMeterConfiguration;
import de.datev.refsys.aggregation.processing.config.TestResilienceConfiguration;
import de.datev.refsys.aggregation.processing.config.mongo.MongoSharedConfiguration;
import de.datev.refsys.aggregation.processing.configuration.ImportServiceConfiguration;
import de.datev.refsys.aggregation.processing.configuration.TestMongoClientConfiguration;
import de.datev.refsys.aggregation.processing.configuration.TestcontainersConfiguration;
import de.datev.refsys.aggregation.processing.constant.ProcessingErrorMessageConstants;
import de.datev.refsys.aggregation.processing.exception.InitialLoadFailedException;
import de.datev.refsys.aggregation.processing.mapper.AccountDbKeyFieldsMapper;
import de.datev.refsys.aggregation.processing.mapper.AccountDescriptionMapperImpl;
import de.datev.refsys.aggregation.processing.mapper.AccountPurposeMapperImpl;
import de.datev.refsys.aggregation.processing.mapper.AccountSumDayMapper;
import de.datev.refsys.aggregation.processing.mapper.AccountValueMapper;
import de.datev.refsys.aggregation.processing.mapper.AdditionalParametersMapper;
import de.datev.refsys.aggregation.processing.mapper.AlternativeAccountTranslationMapper;
import de.datev.refsys.aggregation.processing.mapper.CollectiveAccountMapper;
import de.datev.refsys.aggregation.processing.mapper.CustomColumnStructureContentMapper;
import de.datev.refsys.aggregation.processing.mapper.CustomColumnStructureInfoMapper;
import de.datev.refsys.aggregation.processing.mapper.CustomReportStructureContentMapper;
import de.datev.refsys.aggregation.processing.mapper.CustomReportStructureInfoMapper;
import de.datev.refsys.aggregation.processing.mapper.InventoryDbKeyFieldsMapper;
import de.datev.refsys.aggregation.processing.mapper.MasterDataContextMapper;
import de.datev.refsys.aggregation.processing.mapper.PreviousYearAccountTranslationMapper;
import de.datev.refsys.aggregation.processing.mapper.ShareholderAdditionMapper;
import de.datev.refsys.aggregation.processing.mapper.ShareholderAddressMapper;
import de.datev.refsys.aggregation.processing.mapper.ShareholderRelationMapper;
import de.datev.refsys.aggregation.processing.mapper.ShareholderTaxOfficeMapper;
import de.datev.refsys.aggregation.processing.model.ImportData;
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
import de.datev.refsys.aggregation.processing.util.ClearDatabaseAnCreateIndexesBeforeEachTest;
import de.datev.refsys.aggregation.processing.util.MemoryAppender;
import de.datev.refsys.aggregation.processing.util.MongoHelperService;
import de.datev.refsys.aggregation.processing.util.ResetResilienceAfterEachTest;
import de.datev.refsys.aggregation.processing.util.TestDataLoader;
import de.datev.refsys.generated.acds.api.CustomColumnStructuresApi;
import de.datev.refsys.generated.acds.api.CustomReportStructuresApi;
import de.datev.refsys.generated.acds.api.model.AccountPurposeMapping;
import de.datev.refsys.generated.acds.api.model.AccountSumDay;
import de.datev.refsys.generated.acds.api.model.CollectiveAccount;
import de.datev.refsys.generated.acds.api.model.CustomColumnStructure;
import de.datev.refsys.generated.acds.api.model.CustomReportStructure;
import de.datev.refsys.generated.acds.api.model.MasterdataContext;
import de.datev.refsys.generated.acds.api.model.MasterdataInventory;
import de.datev.refsys.generated.acds.api.model.MovementdataInventory;
import de.datev.refsys.generated.acds.api.model.ShareholderData;
import de.datev.refsys.generated.acds.api.model.Translation;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import org.assertj.core.api.ThrowableAssert;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.mockito.internal.stubbing.answers.AnswersWithDelay;
import org.mockito.internal.stubbing.answers.Returns;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static de.datev.refsys.aggregation.document.model.constants.FieldConstants.ADDITIONAL_PARAMS;
import static de.datev.refsys.aggregation.document.model.constants.FieldConstants.ALTERNATIVE_ACCOUNT_TRANSLATIONS;
import static de.datev.refsys.aggregation.document.model.constants.FieldConstants.COLLECTIVE_ACCOUNTS;
import static de.datev.refsys.aggregation.document.model.constants.FieldConstants.CONTEXT;
import static de.datev.refsys.aggregation.document.model.constants.FieldConstants.PREVIOUS_YEAR_ACCOUNT_TRANSLATIONS;
import static de.datev.refsys.aggregation.document.model.constants.FieldConstants.SHAREHOLDER_ADDITIONS;
import static de.datev.refsys.aggregation.document.model.constants.FieldConstants.SHAREHOLDER_ADDRESSES;
import static de.datev.refsys.aggregation.document.model.constants.FieldConstants.SHAREHOLDER_RELATIONS;
import static de.datev.refsys.aggregation.document.model.constants.FieldConstants.SHAREHOLDER_TAX_OFFICES;
import static de.datev.refsys.aggregation.processing.constant.ProcessingErrorMessageConstants.IMPORT_EXCEEDED_MAX_TIME;
import static de.datev.refsys.aggregation.processing.constant.ProcessingServiceConstants.STATE_DOC_CIRCUIT_BREAKER;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_CLIENT;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_CONSULTANT;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_FISCAL_YEAR_2021_END;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_FISCAL_YEAR_2021_START;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_PROFILE;
import static de.datev.refsys.aggregation.processing.util.TestUtil.createStateDoc;
import static de.datev.refsys.aggregation.processing.util.TestUtil.setupMemoryAppender;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.catchThrowable;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ImportAutoConfiguration(TestMongoClientConfiguration.class)
@ContextConfiguration(classes = { ImportServiceConfiguration.class, TestResilienceConfiguration.class, TestMeterConfiguration.class })
@Import({ TestcontainersConfiguration.class, MongoSharedConfiguration.class, StateDocRepository.class,
        MasterDataRepository.class, MasterDataAccountRepository.class, MovementDataDayRepository.class, MovementDataMonthRepository.class,
        MovementDataPersonGroupDayRepository.class, MovementDataPersonGroupMonthRepository.class, MovementDataInventoryRepository.class, CustomReportStructureContentRepository.class, CustomColumnStructureContentRepository.class,
        AccountDescriptionMapperImpl.class, AccountPurposeMapperImpl.class, InitialLoadConfiguration.class, MongoHelperService.class
})
@ResetResilienceAfterEachTest
@DataMongoTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ActiveProfiles(TEST_PROFILE)
@ClearDatabaseAnCreateIndexesBeforeEachTest
class ImportServiceIT {

    @Autowired
    private AdditionalParametersMapper additionalParametersMapper;

    @Autowired
    private MasterDataContextMapper masterDataContextMapper;

    @Autowired
    private CollectiveAccountMapper collectiveAccountMapper;

    @Autowired
    private AlternativeAccountTranslationMapper alternativeAccountTranslationMapper;

    @Autowired
    private PreviousYearAccountTranslationMapper previousYearAccountTranslationMapper;

    @Autowired
    private ShareholderRelationMapper shareholderRelationMapper;

    @Autowired
    private ShareholderAdditionMapper shareholderAdditionMapper;

    @Autowired
    private ShareholderAddressMapper shareholderAddressMapper;

    @Autowired
    private ShareholderTaxOfficeMapper shareholderTaxOfficeMapper;

    @Autowired
    private AccountValueMapper accountValueMapper;

    @Autowired
    private InventoryDbKeyFieldsMapper inventoryDbKeyFieldsMapper;

    @Autowired
    private AccountSumDayMapper accountSumDayMapper;

    @Autowired
    private CustomReportStructureContentMapper customReportStructureContentMapper;

    @Autowired
    private CustomColumnStructureContentMapper customColumnStructureContentMapper;

    @Autowired
    private CustomReportStructureInfoMapper customReportStructureInfoMapper;

    @Autowired
    private CustomColumnStructureInfoMapper customColumnStructureInfoMapper;

    @Autowired
    private MeterRegistry meterRegistry;

    @Autowired
    private MasterDataAccountRepository masterDataAccountRepository;

    @Autowired
    private MovementDataDayRepository movementDataDayRepository;

    @Autowired
    private MovementDataMonthRepository movementDataMonthRepository;

    @Autowired
    private MovementDataPersonGroupDayRepository personGroupDayRepository;

    @Autowired
    private MovementDataPersonGroupMonthRepository personGroupMonthRepository;

    @Autowired
    private MovementDataInventoryRepository movementDataInventoryRepository;

    @Autowired
    private CustomReportStructureContentRepository customReportStructureContentRepository;

    @Autowired
    private CustomColumnStructureContentRepository customColumnStructureContentRepository;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Autowired
    private InitialLoadConfiguration initialLoadConfiguration;

    @Autowired
    private MongoHelperService mongoHelperService;

    @Autowired
    private MasterDataRepository masterDataRepository;

    @MockitoSpyBean
    private StateDocRepository stateDocRepository;

    @MockitoSpyBean
    private AccountDbKeyFieldsMapper accountDbKeyFieldsMapper;

    @Value("${ref-sys.update-schema.schema-version}")
    private int schemaVersion;

    private ImportServiceImpl importService;
    private MasterDataClient masterDataClient;
    private MovementDataClient movementDataClient;
    private MemoryAppender memoryAppender;

    @BeforeEach
    void setup() {
        this.memoryAppender = setupMemoryAppender(memoryAppender, ImportServiceImpl.class, Level.DEBUG);
        this.masterDataClient = Mockito.mock(MasterDataClient.class);
        this.movementDataClient = Mockito.mock(MovementDataClient.class);
        CustomStructuresClient customStructuresClient = Mockito.mock(CustomStructuresClient.class);
        when(customStructuresClient.getCustomColumnStructuresList(any())).thenReturn(Mono.just(new ArrayList<>()));
        when(customStructuresClient.getCustomReportStructuresList(any())).thenReturn(Mono.just(new ArrayList<>()));
        CustomStructuresService customStructuresService =
                new CustomStructuresServiceImpl(customStructuresClient, customReportStructureContentRepository,
                                                customColumnStructureContentRepository, customReportStructureContentMapper,
                                                customColumnStructureContentMapper, customReportStructureInfoMapper, customColumnStructureInfoMapper);
        this.importService =
                Mockito.spy(new ImportServiceImpl(stateDocRepository, masterDataRepository, movementDataDayRepository, movementDataMonthRepository,
                                                  personGroupDayRepository, personGroupMonthRepository, masterDataAccountRepository,
                                                  movementDataInventoryRepository, meterRegistry,
                                                  additionalParametersMapper, masterDataContextMapper, collectiveAccountMapper,
                                                  alternativeAccountTranslationMapper, previousYearAccountTranslationMapper,
                                                  shareholderRelationMapper, accountDbKeyFieldsMapper, accountValueMapper,
                                                  shareholderAdditionMapper, shareholderAddressMapper, shareholderTaxOfficeMapper,
                                                  masterDataClient, movementDataClient,
                                                  initialLoadConfiguration, inventoryDbKeyFieldsMapper, accountSumDayMapper,
                                                  customStructuresService));
        this.circuitBreakerRegistry.circuitBreaker(STATE_DOC_CIRCUIT_BREAKER).transitionToDisabledState();
        mockAllAcdsEndpointsToEmptyResponses();
    }

    @ParameterizedTest
    @MethodSource("dataForTestOne")
    @DisplayName("MasterData Null Values Integration Test")
    @Order(1)
    void should_not_save_null_values_in_master_data_when_response_is_empty_or_null(Set<Integer> individualPersonAccounts,
                                                                                   List<CollectiveAccount> collectiveAccounts,
                                                                                   ShareholderData shareholderData, Translation translation) {
        when(masterDataClient.getShareholderData(any())).thenReturn(Mono.just(shareholderData));
        MasterdataContext masterDataContext = TestDataLoader.load("json/acds-responses/masterDataContext.json", MasterdataContext.class);
        importService.importMasterData(masterDataContext, individualPersonAccounts, collectiveAccounts, translation).block();
        mongoHelperService.findAllMasterDataAccounts();
        List<Document> resultMasterDataDocuments = mongoHelperService.findAllMasterDataDocument();
        assertThat(resultMasterDataDocuments).isNotNull().hasSize(1);
        assertThat(resultMasterDataDocuments.get(0))
                .as("Master Data without set fields should be returned. Sometimes randomly fails on Jenkins because of timing issues.")
                .isNotNull()
                .doesNotContainKey(COLLECTIVE_ACCOUNTS)
                .doesNotContainKey(ALTERNATIVE_ACCOUNT_TRANSLATIONS)
                .doesNotContainKey(PREVIOUS_YEAR_ACCOUNT_TRANSLATIONS)
                .doesNotContainKey(SHAREHOLDER_ADDITIONS)
                .doesNotContainKey(SHAREHOLDER_ADDRESSES)
                .doesNotContainKey(SHAREHOLDER_RELATIONS)
                .doesNotContainKey(SHAREHOLDER_TAX_OFFICES)
                .doesNotContainKey(CONTEXT + ".custom_column_structure_infos")
                .doesNotContainKey(CONTEXT + ".custom_report_structure_infos");
    }

    @Test
    @Order(2)
    @DisplayName("MovementData with default values AccountSumDay")
    void should_not_write_additional_parameters_null_into_db_when_accountSumDay_has_all_default_values() {
        List<AccountSumDay> accountSumDays =
                TestDataLoader.loadNdJsonList("json/acds-responses/accountSumDaysDefaultValues.ndjson", AccountSumDay.class);
        MasterdataContext masterDataContext = TestDataLoader.load("json/acds-responses/masterDataContext.json", MasterdataContext.class);
        when(movementDataClient.getAccountSumDays(any())).thenReturn(Flux.fromIterable(accountSumDays));
        ImportData importData = new ImportData(masterDataContext, StateDoc.builder().stateTimestamp(OffsetDateTime.now()).build());
        importService.executeFullImport(importData).block();
        List<Document> movementDataDays = mongoHelperService.findAllMovementDataDaysDocument();
        assertThat(movementDataDays).isNotNull().hasSize(2);
        assertThat(movementDataDays.get(0))
                .as("AccountSum defaults should not be written on data days. Sometimes randomly fails on Jenkins because of timing issues.")
                .isNotNull()
                .doesNotContainKey(ADDITIONAL_PARAMS);
        assertThat(movementDataDays.get(1))
                .as("AccountSum defaults should not be written on data days. Sometimes randomly fails on Jenkins because of timing issues.")
                .isNotNull()
                .doesNotContainKey(ADDITIONAL_PARAMS);
        List<Document> movementDataMonths = mongoHelperService.findAllMovementDataMonthsDocument();
        assertThat(movementDataMonths).isNotNull().hasSize(2);
        assertThat(movementDataMonths.get(0))
                .as("AccountSum defaults should not be written on data months. Sometimes randomly fails on Jenkins because of timing issues.")
                .isNotNull()
                .doesNotContainKey(ADDITIONAL_PARAMS);
        assertThat(movementDataMonths.get(1))
                .as("AccountSum defaults should not be written on data months. Sometimes randomly fails on Jenkins because of timing issues.")
                .isNotNull()
                .doesNotContainKey(ADDITIONAL_PARAMS);
        List<Document> personGroupDays = mongoHelperService.findAllMovementDataPersonGroupDaysDocument();
        assertThat(personGroupDays).isNotNull().hasSize(1);
        assertThat(personGroupDays.get(0))
                .as("AccountSum defaults should not be written on person group days. Sometimes randomly fails on Jenkins because of timing "
                            + "issues.")
                .isNotNull()
                .doesNotContainKey(ADDITIONAL_PARAMS);
        List<Document> personGroupMonths = mongoHelperService.findAllMovementDataPersonGroupMonthsDocument();
        assertThat(personGroupMonths).isNotNull().hasSize(1);
        assertThat(personGroupMonths.get(0))
                .as("AccountSum defaults should not be written on person group months. Sometimes randomly fails on Jenkins because of timing "
                            + "issues.")
                .isNotNull()
                .doesNotContainKey(ADDITIONAL_PARAMS);
    }

    @Test
    @Order(3)
    @DisplayName("MovementData with same AccountSumDay")
    void should_aggregate_debit_and_credit_when_same_accountSumDay_and_date() {
        List<AccountSumDay> accountSumDays =
                TestDataLoader.loadNdJsonList("json/acds-responses/accountSumDaysSameAccountNumberAndDate.ndjson", AccountSumDay.class);
        MasterdataContext masterDataContext = TestDataLoader.load("json/acds-responses/masterDataContext.json", MasterdataContext.class);
        when(movementDataClient.getAccountSumDays(any())).thenReturn(Flux.fromIterable(accountSumDays));
        ImportData importData = new ImportData(masterDataContext, StateDoc.builder().stateTimestamp(OffsetDateTime.now()).build());
        importService.executeFullImport(importData).block();
        AdditionalParameters expectedAdditionalParameters = additionalParametersMapper.accountSumDayToAdditionalParameters(accountSumDays.get(0));
        List<MovementDataDay> movementDataDayList = mongoHelperService.findAllMovementDataDays();
        assertThat(movementDataDayList).hasSize(1);
        MovementDataDay movementDataDay = movementDataDayList.get(0);
        assertThat(movementDataDay)
                .as("Debit and credit should be aggregated. Sometimes randomly fails on Jenkins because of timing issues.")
                .isNotNull()
                .hasFieldOrPropertyWithValue("consultant", TEST_CONSULTANT)
                .hasFieldOrPropertyWithValue("client", TEST_CLIENT)
                .hasFieldOrPropertyWithValue("fiscalYear", TEST_FISCAL_YEAR_2021_START)
                .hasFieldOrPropertyWithValue("accountingReasonId", 0)
                .hasFieldOrPropertyWithValue("additionalParams", expectedAdditionalParameters);
        assertThat(movementDataDay.getValues())
                .hasSize(1)
                .hasEntrySatisfying("d20210501", accountValue -> {
                    assertThat(accountValue.getAmountDebit()).isEqualTo(44356);
                    assertThat(accountValue.getAmountCredit()).isEqualTo(44356);
                });
    }

    @Test
    @Order(4)
    @DisplayName("do not update timestamp stateDoc when import accountSumDays is during parking time")
    void should_not_update_stateDoc_timestamp_when_import_accountSumDays_is_during_parking_time() {
        // Prepare
        List<AccountSumDay> accountSumDays =
                TestDataLoader.loadNdJsonList("json/acds-responses/accountSumDaysSameParkingTimeTesting.ndjson", AccountSumDay.class);
        MasterdataContext masterdataContext = TestDataLoader.load("json/acds-responses/masterDataContext.json", MasterdataContext.class);

        when(masterDataClient.getMasterDataContext(any(), any(), any(), any(), any(), any())).thenReturn(Mono.just(masterdataContext));
        long delayInMilliseconds = 10;
        final var answersWithDelay = new AnswersWithDelay(delayInMilliseconds, new Returns(Flux.fromIterable(accountSumDays)));
        Mockito.doAnswer(answersWithDelay).when(movementDataClient).getAccountSumDays(any());

        StateDoc stateDoc = createStateDoc(StateDocState.INIT, TEST_FISCAL_YEAR_2021_START, TEST_FISCAL_YEAR_2021_END, schemaVersion);
        mongoHelperService.insertOneStateDoc(stateDoc);

        ImportData importData = new ImportData(masterdataContext, stateDoc);

        // Act
        OffsetDateTime timestampBefore = OffsetDateTime.now(Clock.tickSeconds(ZoneId.systemDefault())).minusSeconds(1);
        assertDoesNotThrow(() -> importService.executeFullImport(importData).block());
        OffsetDateTime timestampAfter = OffsetDateTime.now(Clock.tickSeconds(ZoneId.systemDefault())).plusSeconds(1);

        // Assert
        List<StateDoc> resultStateDocList = mongoHelperService.findAllStateDocs();
        assertThat(resultStateDocList).hasSize(1);
        StateDoc resultStateDoc = resultStateDocList.get(0);
        assertThat(resultStateDoc).isNotNull();
        assertThat(resultStateDoc.getProcessingError()).isNull();
        assertThat(resultStateDoc).usingRecursiveComparison().ignoringFieldsOfTypes(OffsetDateTime.class).isEqualTo(stateDoc);
        assertThat(resultStateDoc.getStateTimestamp().withNano(0)).isBetween(timestampBefore, timestampAfter);
        assertThat(resultStateDoc.getCreatedTimestamp().withNano(0)).isBetween(timestampBefore, timestampAfter);

        verify(stateDocRepository, never()).updateTimestamp(any(), any(), any(), any());
    }

    @Test
    @Order(5)
    @DisplayName("update timestamp stateDoc when import accountSumDays exceeds parking time")
    void should_update_timestamp_stateDoc_when_import_accountSumDays_exceeds_parking_time() {
        // Prepare
        List<AccountSumDay> accountSumDays =
                TestDataLoader.loadNdJsonList("json/acds-responses/accountSumDaysSameParkingTimeTesting.ndjson", AccountSumDay.class);
        MasterdataContext masterdataContext = TestDataLoader.load("json/acds-responses/masterDataContext.json", MasterdataContext.class);

        when(masterDataClient.getMasterDataContext(any(), any(), any(), any(), any(), any())).thenReturn(Mono.just(masterdataContext));
        long accountSumDaysDelayMs = initialLoadConfiguration.getParkingTimeInMs() + 300L;
        final var answersWithDelay = new AnswersWithDelay(accountSumDaysDelayMs, new Returns(Flux.fromIterable(accountSumDays)));
        Mockito.doAnswer(answersWithDelay).when(movementDataClient).getAccountSumDays(any());

        StateDoc stateDoc = createStateDoc(StateDocState.INIT, TEST_FISCAL_YEAR_2021_START, TEST_FISCAL_YEAR_2021_END, schemaVersion);
        mongoHelperService.insertOneStateDoc(stateDoc);

        ImportData importData = new ImportData(masterdataContext, stateDoc);

        // Act
        assertDoesNotThrow(() -> importService.executeFullImport(importData).block());

        // Assert
        List<StateDoc> resultStateDocList = mongoHelperService.findAllStateDocs();
        assertThat(resultStateDocList).hasSize(1);
        StateDoc resultStateDoc = resultStateDocList.get(0);
        assertThat(resultStateDoc).isNotNull();
        assertThat(resultStateDoc.getProcessingError()).isNull();
        assertThat(resultStateDoc).usingRecursiveComparison().ignoringFieldsOfTypes(OffsetDateTime.class).isEqualTo(stateDoc);
        verify(stateDocRepository, times(1)).updateTimestamp(any(), any(), any(), any());
    }

    @Test
    @Order(6)
    @DisplayName("abort import if getAccountSumDays exceeds the maximum import time")
    void should_abort_import_if_getAccountSumDays_exceeds_the_maximum_import_time() {
        // Prepare
        List<AccountSumDay> accountSumDays =
                TestDataLoader.loadNdJsonList("json/acds-responses/accountSumDaysSameParkingTimeTesting.ndjson", AccountSumDay.class);
        MasterdataContext masterdataContext = TestDataLoader.load("json/acds-responses/masterDataContext.json", MasterdataContext.class);

        when(masterDataClient.getMasterDataContext(any(), any(), any(), any(), any(), any())).thenReturn(Mono.just(masterdataContext));
        long accountSumDaysDelayMs = initialLoadConfiguration.getMaxImportDurationInMs() + 1000L;
        final var answersWithDelay = new AnswersWithDelay(accountSumDaysDelayMs, new Returns(Flux.fromIterable(accountSumDays)));
        Mockito.doAnswer(answersWithDelay).when(movementDataClient).getAccountSumDays(any());

        StateDoc stateDoc = createStateDoc(StateDocState.INIT, TEST_FISCAL_YEAR_2021_START, TEST_FISCAL_YEAR_2021_END, schemaVersion);
        mongoHelperService.insertOneStateDoc(stateDoc);

        ImportData importData = new ImportData(masterdataContext, stateDoc);

        // Act
        Throwable throwable = ThrowableAssert.catchThrowable(() -> importService.executeFullImport(importData).block());

        // Assert
        assertThat(throwable).isInstanceOf(InitialLoadFailedException.class).hasMessage(IMPORT_EXCEEDED_MAX_TIME);
        InitialLoadFailedException initialLoadException = (InitialLoadFailedException) throwable;
        assertThat(initialLoadException.getHttpStatusCode()).isEqualTo(HttpStatus.CONFLICT.value());
        List<StateDoc> resultStateDocList = mongoHelperService.findAllStateDocs();
        assertThat(resultStateDocList).hasSize(1);
        StateDoc resultStateDoc = resultStateDocList.get(0);
        assertThat(resultStateDoc).isNotNull();
        assertThat(resultStateDoc.getProcessingError()).isNull();
        assertThat(resultStateDoc).usingRecursiveComparison().ignoringFieldsOfTypes(OffsetDateTime.class).isEqualTo(stateDoc);

        List<Document> movementDataDays = mongoHelperService.findAllMovementDataDaysDocument();
        assertThat(movementDataDays).isEmpty();

        List<Document> movementDataMonths = mongoHelperService.findAllMovementDataMonthsDocument();
        assertThat(movementDataMonths).isEmpty();

        List<Document> masterDataAccount = mongoHelperService.findAllMasterDataAccountsDocument();
        assertThat(masterDataAccount).isEmpty();

        List<Document> masterData = mongoHelperService.findAllMasterDataDocument();
        assertThat(masterData).isEmpty();

        List<Document> personGroupDays = mongoHelperService.findAllMovementDataPersonGroupDaysDocument();
        assertThat(personGroupDays).isEmpty();

        List<Document> personGroupMonths = mongoHelperService.findAllMovementDataPersonGroupMonthsDocument();
        assertThat(personGroupMonths).isEmpty();
        verify(stateDocRepository, never()).updateTimestamp(any(), any(), any(), any());

    }

    @Test
    @Order(7)
    @DisplayName("abort import if movementdata exceeds the maximum import time")
    void should_abort_import_if_movementdata_exceeds_the_maximum_import_time() {
        // Prepare
        List<AccountSumDay> accountSumDays =
                TestDataLoader.loadNdJsonList("json/acds-responses/accountSumDaysSameParkingTimeTesting.ndjson", AccountSumDay.class);
        when(movementDataClient.getAccountSumDays(any())).thenReturn(Flux.fromIterable(accountSumDays));

        MasterdataContext masterDataContext = TestDataLoader.load("json/acds-responses/masterDataContext.json", MasterdataContext.class);
        StateDoc stateDoc = createStateDoc(StateDocState.INIT, TEST_FISCAL_YEAR_2021_START, TEST_FISCAL_YEAR_2021_END, schemaVersion);
        mongoHelperService.insertOneStateDoc(stateDoc);

        // delay mapping account sum day in order to delay importMovementData.
        long delayTime = initialLoadConfiguration.getMaxImportDurationInMs() + 1000L;
        final var answersWithDelay = new AnswersWithDelay(delayTime, new Returns(Flux.fromIterable(accountSumDays)));
        Mockito.doAnswer(answersWithDelay).when(movementDataClient).getAccountSumDays(any());
        ImportData importData = new ImportData(masterDataContext, stateDoc);

        // Act
        OffsetDateTime timestampBefore = OffsetDateTime.now(Clock.tickSeconds(ZoneId.systemDefault())).minusSeconds(1);
        Throwable throwable = catchThrowable(() -> importService.executeFullImport(importData).block());
        OffsetDateTime timestampAfter = OffsetDateTime.now(Clock.tickSeconds(ZoneId.systemDefault())).plusSeconds(1);

        // Assert
        assertThat(throwable).isInstanceOf(InitialLoadFailedException.class).hasMessage(IMPORT_EXCEEDED_MAX_TIME);
        InitialLoadFailedException initialLoadException = (InitialLoadFailedException) throwable;
        assertThat(initialLoadException.getHttpStatusCode()).isEqualTo(HttpStatus.CONFLICT.value());
        List<StateDoc> resultStateDocList = mongoHelperService.findAllStateDocs();
        assertThat(resultStateDocList).hasSize(1);
        StateDoc resultStateDoc = resultStateDocList.get(0);
        assertThat(resultStateDoc).isNotNull();
        assertThat(resultStateDoc.getProcessingError()).isNull();
        assertThat(resultStateDoc).usingRecursiveComparison().ignoringFieldsOfTypes(OffsetDateTime.class).isEqualTo(stateDoc);
        assertThat(resultStateDoc.getStateTimestamp().withNano(0)).isBetween(timestampBefore, timestampAfter);
        assertThat(resultStateDoc.getCreatedTimestamp().withNano(0)).isBetween(timestampBefore, timestampAfter);

        List<Document> movementDataDays = mongoHelperService.findAllMovementDataDaysDocument();
        assertThat(movementDataDays).isEmpty();

        List<Document> movementDataMonths = mongoHelperService.findAllMovementDataMonthsDocument();
        assertThat(movementDataMonths).isEmpty();

        List<Document> masterDataAccount = mongoHelperService.findAllMasterDataAccountsDocument();
        assertThat(masterDataAccount).isEmpty();

        List<Document> masterData = mongoHelperService.findAllMasterDataDocument();
        assertThat(masterData).isEmpty();

        List<Document> personGroupDays = mongoHelperService.findAllMovementDataPersonGroupDaysDocument();
        assertThat(personGroupDays).isEmpty();

        List<Document> personGroupMonths = mongoHelperService.findAllMovementDataPersonGroupMonthsDocument();
        assertThat(personGroupMonths).isEmpty();
        verify(stateDocRepository, never()).updateTimestamp(any(), any(), any(), any());

    }

    @Test
    @Order(8)
    @DisplayName("MasterData and MovementData Inventories should be imported and MovementDataInventories should be processed in batches")
    void should_import_inventories_and_process_them_in_batches() {
        MasterdataContext masterDataContext = TestDataLoader.load("json/acds-responses/masterDataContext.json", MasterdataContext.class);
        List<MasterdataInventory> masterdataInventories =
                TestDataLoader.loadList("json/acds-responses/wiremock/master-data-inventories.json", new TypeReference<>() {
                });
        List<MovementdataInventory> movementdataInventories =
                TestDataLoader.loadList("json/acds-responses/wiremock/movement-data-inventories-2021.json", new TypeReference<>() {
                });
        when(movementDataClient.getMovementDataInventories(any())).thenReturn(Flux.fromIterable(movementdataInventories));
        when(masterDataClient.getInventories(any())).thenReturn(Mono.just(masterdataInventories));
        StateDoc stateDoc = createStateDoc(StateDocState.INIT, TEST_FISCAL_YEAR_2021_START, TEST_FISCAL_YEAR_2021_END, schemaVersion);

        importService.afterMovementData(masterDataContext, new HashSet<>(), new HashMap<>(), new HashMap<>(), new HashSet<>(), stateDoc).block();

        // verify buffer by wgId (10 movementdataInventories in input and 8 remain after the filter)
        verify(importService, times(5)).filterMovementDataInventories(any(), any(), any());
        // verify buffer before saving into DB (batch in test is set to 5, so method should be called twice for 8 items)
        verify(importService, times(2)).insertMovementDataInventories(any(), any());
        List<MovementDataInventory> expectedMovementDataInventories =
                TestDataLoader.loadMongoDBList("json/collections/expected/movement-data-inventories/movementDataInventories.json",
                                               MovementDataInventory.class);
        List<MovementDataInventory> foundMovementDataInventories = mongoHelperService.findAllMovementDataInventories();
        assertThat(foundMovementDataInventories).isNotNull().hasSize(5);
        assertThat(foundMovementDataInventories).usingRecursiveComparison().ignoringCollectionOrder().isEqualTo(expectedMovementDataInventories);
        String expectedLog = ProcessingErrorMessageConstants.INVENTORY_VALID_FROM_IS_NULL.replace("{}", "3");
        assertThat(memoryAppender.search(expectedLog)).hasSize(1);
    }

    @Test
    @Order(9)
    @DisplayName("MasterData and MovementData Inventories should not be imported if no data is provided from the clients")
    void should_finish_import_when_data_has_no_inventories() {
        MasterdataContext masterDataContext = TestDataLoader.load("json/acds-responses/masterDataContext.json", MasterdataContext.class);
        List<AccountPurposeMapping> accountPurposeMappings =
                TestDataLoader.loadNdJsonList("json/acds-responses/accountPurposeMappings.ndjson", AccountPurposeMapping.class);
        when(masterDataClient.getAccountPurposeMappings(any())).thenReturn(Mono.just(accountPurposeMappings));
        StateDoc stateDoc = createStateDoc(StateDocState.INIT, TEST_FISCAL_YEAR_2021_START, TEST_FISCAL_YEAR_2021_END, schemaVersion);

        importService.afterMovementData(masterDataContext, new HashSet<>(), new HashMap<>(), new HashMap<>(), new HashSet<>(), stateDoc).block();

        // verify that the buffer by wgId wasn't called
        verify(importService, times(0)).filterMovementDataInventories(any(), any(), any());
        // verify that the buffer before saving into DB wasn't called
        verify(importService, times(0)).insertMovementDataInventories(any(), any());
        List<MovementDataInventory> foundMovementDataInventories = mongoHelperService.findAllMovementDataInventories();
        assertThat(foundMovementDataInventories).isNotNull().isEmpty();
        List<MasterDataAccount> masterDataAccounts = mongoHelperService.findAllMasterDataAccounts();
        assertThat(masterDataAccounts).isNotNull().hasSize(2);
        assertThat(masterDataAccounts.get(0).getInventories()).isNull();
        assertThat(masterDataAccounts.get(1).getInventories()).isNull();
    }

    @Test
    @Order(10)
    @DisplayName("Should not save CustomStructures when iasd flag is disabled")
    void should_not_save_custom_structures_when_iasd_flag_is_disabled() {
        // prepare
        CustomReportStructuresApi customReportStructuresApi = Mockito.mock(CustomReportStructuresApi.class);
        CustomColumnStructuresApi customColumnStructuresApi = Mockito.mock(CustomColumnStructuresApi.class);
        List<CustomColumnStructure> customColumnStructures =
                TestDataLoader.loadList("json/acds-responses/customColumnStructures.json", new TypeReference<>() {
                });
        when(customColumnStructuresApi.getCustomColumnStructures(anyInt(), anyInt(), anyInt(), anyBoolean(), anyLong(), anyLong(), anyBoolean(),
                                                                 anyString(), anyString())).thenReturn(Flux.fromIterable(customColumnStructures));
        List<CustomReportStructure> customReportStructures =
                TestDataLoader.loadList("json/acds-responses/customReportStructures.json", new TypeReference<>() {
                });
        when(customReportStructuresApi.getCustomReportStructures(anyInt(), anyInt(), anyInt(), anyBoolean(), anyLong(), anyLong(), anyBoolean(),
                                                                 anyString(), anyString())).thenReturn(Flux.fromIterable(customReportStructures));
        CustomStructuresClient iasdDisabledClient = new CustomStructuresClient(customReportStructuresApi, customColumnStructuresApi, false, RetryRegistry.custom().build());
        CustomStructuresService customStructuresService =
                new CustomStructuresServiceImpl(iasdDisabledClient, customReportStructureContentRepository,
                                                customColumnStructureContentRepository, customReportStructureContentMapper,
                                                customColumnStructureContentMapper, customReportStructureInfoMapper, customColumnStructureInfoMapper);
        Translation translation = TestDataLoader.load("json/acds-responses/translation.json", Translation.class);
        when(masterDataClient.getTranslation(any())).thenReturn(Mono.just(translation));
        List<CollectiveAccount> collectiveAccounts =
                TestDataLoader.loadNdJsonList("json/acds-responses/collectiveAccounts.ndjson", CollectiveAccount.class);
        when(masterDataClient.getCollectiveAccounts(any())).thenReturn(Mono.just(collectiveAccounts));
        ImportService iasdDisabledImportService =
                Mockito.spy(new ImportServiceImpl(stateDocRepository, masterDataRepository, movementDataDayRepository, movementDataMonthRepository,
                                                  personGroupDayRepository, personGroupMonthRepository, masterDataAccountRepository,
                                                  movementDataInventoryRepository, meterRegistry,
                                                  additionalParametersMapper, masterDataContextMapper, collectiveAccountMapper,
                                                  alternativeAccountTranslationMapper, previousYearAccountTranslationMapper,
                                                  shareholderRelationMapper, accountDbKeyFieldsMapper, accountValueMapper,
                                                  shareholderAdditionMapper, shareholderAddressMapper, shareholderTaxOfficeMapper,
                                                  masterDataClient, movementDataClient,
                                                  initialLoadConfiguration, inventoryDbKeyFieldsMapper, accountSumDayMapper,
                                                  customStructuresService));
        MasterdataContext masterDataContext = TestDataLoader.load("json/acds-responses/masterDataContext.json", MasterdataContext.class);
        StateDoc stateDoc = createStateDoc(StateDocState.INIT, TEST_FISCAL_YEAR_2021_START, TEST_FISCAL_YEAR_2021_END, schemaVersion);
        // execute
        iasdDisabledImportService.executeFullImport(new ImportData(masterDataContext, stateDoc)).block();
        // assert
        List<MasterData> masterDataList = mongoHelperService.findAllMasterData();
        assertThat(masterDataList).isNotNull().hasSize(1);
        MasterData masterData = masterDataList.get(0);
        assertThat(masterData.getContext()).isNotNull();
        assertThat(masterData.getContext().getCustomColumnStructureInfos()).isNull();
        assertThat(masterData.getContext().getCustomReportStructureInfos()).isNull();
        List<CustomColumnStructureContent> allCustomColumnStructureContents = mongoHelperService.findAllCustomColumnStructureContents();
        assertThat(allCustomColumnStructureContents).isEmpty();
        List<CustomReportStructureContent> allCustomReportStructureContents = mongoHelperService.findAllCustomReportStructureContents();
        assertThat(allCustomReportStructureContents).isEmpty();
    }

    private void mockAllAcdsEndpointsToEmptyResponses() {
        when(movementDataClient.getAccountSumDays(any())).thenReturn(Flux.fromIterable(new ArrayList<>()));
        when(masterDataClient.getMasterDataContext(any(), any(), any(), any(), any(), any())).thenReturn(Mono.just(new MasterdataContext()));
        when(masterDataClient.getCollectiveAccounts(any())).thenReturn(Mono.just(new ArrayList<>()));
        when(masterDataClient.getAccountPurposeMappings(any())).thenReturn(Mono.just(new ArrayList<>()));
        when(masterDataClient.getAccountCaptions(any())).thenReturn(Mono.just(new ArrayList<>()));
        when(masterDataClient.getShareholderData(any())).thenReturn(Mono.just(new ShareholderData()));
        when(masterDataClient.getTranslation(any())).thenReturn(Mono.just(new Translation()));
        when(masterDataClient.getInventories(any())).thenReturn(Mono.just(new ArrayList<>()));
        when(movementDataClient.getMovementDataInventories(any())).thenReturn(Flux.fromIterable(new ArrayList<>()));
    }

    static Stream<Arguments> dataForTestOne() {
        ShareholderData shareholderData = new ShareholderData()
                .shareholderAdditions(Collections.emptyList())
                .shareholderAddressees(Collections.emptyList())
                .shareholderRelations(Collections.emptyList())
                .shareholderTaxOffices(Collections.emptyList());
        Translation translation = new Translation()
                .alternativeAccountTranslations(Collections.emptyList())
                .previousYearAccountTranslations(Collections.emptyList());
        return Stream.of(
                Arguments.of(new HashSet<>(), Collections.emptyList(), shareholderData, translation),
                Arguments.of(null, Collections.emptyList(), new ShareholderData(), new Translation())
                        );
    }
}
