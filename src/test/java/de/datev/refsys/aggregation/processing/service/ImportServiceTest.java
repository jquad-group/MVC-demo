package de.datev.refsys.aggregation.processing.service;

import ch.qos.logback.classic.Level;
import com.fasterxml.jackson.core.type.TypeReference;
import de.datev.refsys.aggregation.document.model.StateDoc;
import de.datev.refsys.aggregation.document.model.enums.StateDocState;
import de.datev.refsys.aggregation.processing.client.CustomStructuresClient;
import de.datev.refsys.aggregation.processing.client.MasterDataClient;
import de.datev.refsys.aggregation.processing.client.MovementDataClient;
import de.datev.refsys.aggregation.processing.config.InitialLoadConfiguration;
import de.datev.refsys.aggregation.processing.configuration.ImportServiceConfiguration;
import de.datev.refsys.aggregation.processing.configuration.InitialLoadContextConfiguration;
import de.datev.refsys.aggregation.processing.exception.AggregationProcessingBusinessException;
import de.datev.refsys.aggregation.processing.exception.HttpCallTechnicalException;
import de.datev.refsys.aggregation.processing.mapper.AccountDbKeyFieldsMapper;
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
import de.datev.refsys.aggregation.processing.model.enums.SourceEndpoint;
import de.datev.refsys.aggregation.processing.model.enums.SourceError;
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
import de.datev.refsys.aggregation.processing.util.MemoryAppender;
import de.datev.refsys.aggregation.processing.util.TestDataLoader;
import de.datev.refsys.aggregation.processing.util.TestUtil;
import de.datev.refsys.aggregation.processing.util.Util;
import de.datev.refsys.generated.acds.api.model.AccountingReasonProps;
import de.datev.refsys.generated.acds.api.model.CollectiveAccount;
import de.datev.refsys.generated.acds.api.model.CustomColumnStructure;
import de.datev.refsys.generated.acds.api.model.CustomReportStructure;
import de.datev.refsys.generated.acds.api.model.MasterdataContext;
import de.datev.refsys.generated.acds.api.model.ShareholderData;
import de.datev.refsys.generated.acds.api.model.ShareholderRelation;
import de.datev.refsys.generated.acds.api.model.Translation;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static de.datev.refsys.aggregation.processing.constant.ProcessingErrorMessageConstants.COLUMN_STRUCTURE_REQUIRED_FIELDS_ARE_NULL_ERROR;
import static de.datev.refsys.aggregation.processing.constant.ProcessingErrorMessageConstants.MASTER_DATA_MAPPING_ERROR;
import static de.datev.refsys.aggregation.processing.constant.ProcessingErrorMessageConstants.REPORT_STRUCTURE_REQUIRED_FIELDS_ARE_NULL_ERROR;
import static de.datev.refsys.aggregation.processing.util.TestUtil.EXCEPTION_MESSAGE;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_FISCAL_YEAR_2021_END;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_FISCAL_YEAR_2021_START;
import static de.datev.refsys.aggregation.processing.util.TestUtil.createProblemInfo;
import static de.datev.refsys.aggregation.processing.util.TestUtil.createStateDoc;
import static de.datev.refsys.aggregation.processing.util.TestUtil.setupMemoryAppender;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(initializers = ConfigDataApplicationContextInitializer.class, classes = { ImportServiceConfiguration.class,
        InitialLoadContextConfiguration.class })
@ActiveProfiles(TestUtil.TEST_PROFILE)
class ImportServiceTest {

    @Autowired
    private InitialLoadConfiguration initialLoadConfiguration;

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
    private AccountDbKeyFieldsMapper accountDbKeyFieldsMapper;

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

    @MockitoBean
    private StateDocRepository stateDocRepository;

    @MockitoBean
    private MasterDataRepository masterDataRepository;

    @MockitoBean
    private MasterDataAccountRepository masterDataAccountRepository;

    @MockitoBean
    private MovementDataDayRepository movementDataDayRepository;

    @MockitoBean
    private MovementDataMonthRepository movementDataMonthRepository;

    @MockitoBean
    private MovementDataPersonGroupDayRepository personGroupDayRepository;

    @MockitoBean
    private MovementDataPersonGroupMonthRepository personGroupMonthRepository;

    @MockitoBean
    private MovementDataInventoryRepository movementDataInventoryRepository;

    @MockitoBean
    private CustomReportStructureContentRepository customReportStructureContentRepository;

    @MockitoBean
    private CustomColumnStructureContentRepository customColumnStructureContentRepository;

    @MockitoBean
    private MasterDataClient masterDataClient;

    @MockitoBean
    private MovementDataClient movementDataClient;

    @MockitoBean
    private CustomStructuresClient customStructuresClient;

    @Value("${ref-sys.update-schema.schema-version}")
    private int schemaVersion;

    private ImportServiceImpl importService;
    private MemoryAppender memoryAppender;

    @BeforeEach
    void setUp() {
        when(customStructuresClient.getCustomReportStructuresList(any())).thenReturn(Mono.just(new ArrayList<>()));
        when(customStructuresClient.getCustomColumnStructuresList(any())).thenReturn(Mono.just(new ArrayList<>()));
        CustomStructuresService customStructuresService =
                new CustomStructuresServiceImpl(customStructuresClient, customReportStructureContentRepository,
                                                customColumnStructureContentRepository, customReportStructureContentMapper,
                                                customColumnStructureContentMapper, customReportStructureInfoMapper, customColumnStructureInfoMapper);
        memoryAppender = setupMemoryAppender(memoryAppender, ImportServiceImpl.class, Level.ERROR);
        importService = new ImportServiceImpl(stateDocRepository, masterDataRepository, movementDataDayRepository, movementDataMonthRepository,
                                              personGroupDayRepository, personGroupMonthRepository, masterDataAccountRepository,
                                              movementDataInventoryRepository, new SimpleMeterRegistry(),
                                              additionalParametersMapper, masterDataContextMapper, collectiveAccountMapper,
                                              alternativeAccountTranslationMapper, previousYearAccountTranslationMapper, shareholderRelationMapper,
                                              accountDbKeyFieldsMapper, accountValueMapper,
                                              shareholderAdditionMapper, shareholderAddressMapper, shareholderTaxOfficeMapper,
                                              masterDataClient, movementDataClient, initialLoadConfiguration,
                                              inventoryDbKeyFieldsMapper, accountSumDayMapper, customStructuresService);
        mockAllServiceCallsToEmptyResponses();
    }

    @Test
    @DisplayName("MasterData wrong enum value")
    void should_return_a_business_error_when_accounting_reason_has_invalid_value() {
        // prepare
        Set<Integer> individualPersonAccounts = new HashSet<>();
        List<CollectiveAccount> collectiveAccounts = Collections.emptyList();
        ShareholderData shareholderData =
                new ShareholderData().shareholderAdditions(Collections.emptyList()).shareholderAddressees(Collections.emptyList())
                                     .shareholderRelations(Collections.emptyList()).shareholderTaxOffices(Collections.emptyList());
        Translation translation =
                new Translation().alternativeAccountTranslations(Collections.emptyList()).previousYearAccountTranslations(Collections.emptyList());

        when(masterDataClient.getShareholderData(any())).thenReturn(Mono.just(shareholderData));
        MasterdataContext masterDataContext = TestDataLoader.load("json/acds-responses/masterDataContext.json", MasterdataContext.class);
        List<AccountingReasonProps> accountingReasons = masterDataContext.getAccountingReasons();
        int wrongEnumValue = 999;
        assertThat(accountingReasons).isNotNull();
        accountingReasons.add(AccountingReasonProps.builder().accountingReason(wrongEnumValue).build());
        masterDataContext.setAccountingReasons(accountingReasons);
        // execute
        Throwable throwable = catchThrowable(
                () -> importService.importMasterData(masterDataContext, individualPersonAccounts, collectiveAccounts, translation).block());
        // assert
        assertThat(throwable).isInstanceOf(AggregationProcessingBusinessException.class).hasMessageStartingWith(MASTER_DATA_MAPPING_ERROR, "");
        AggregationProcessingBusinessException aggregationProcessingBusinessException = (AggregationProcessingBusinessException) throwable;
        assertThat(aggregationProcessingBusinessException.getHttpStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(aggregationProcessingBusinessException.getCause()).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("ShareholderRelation wrong number value")
    void should_return_a_business_error_when_a_number_has_the_wrong_format() {
        // prepare
        Set<Integer> individualPersonAccounts = new HashSet<>();
        List<CollectiveAccount> collectiveAccounts = Collections.emptyList();
        ShareholderRelation shareholderRelation = new ShareholderRelation();
        shareholderRelation.setEarningsSharePercent(2.5d);
        List<ShareholderRelation> shareholderRelations = new ArrayList<>();
        shareholderRelations.add(shareholderRelation);
        ShareholderData shareholderData =
                new ShareholderData().shareholderAdditions(Collections.emptyList()).shareholderAddressees(Collections.emptyList())
                                     .shareholderRelations(shareholderRelations).shareholderTaxOffices(Collections.emptyList());
        Translation translation =
                new Translation().alternativeAccountTranslations(Collections.emptyList()).previousYearAccountTranslations(Collections.emptyList());
        when(masterDataClient.getShareholderData(any())).thenReturn(Mono.just(shareholderData));
        MasterdataContext masterDataContext = TestDataLoader.load("json/acds-responses/masterDataContext.json", MasterdataContext.class);
        try (MockedStatic<Util> utilities = Mockito.mockStatic(Util.class)) {
            utilities.when(() -> Util.doubleToDecimal128(any())).thenThrow(new NumberFormatException());
            // execute
            Throwable throwable = catchThrowable(
                    () -> importService.importMasterData(masterDataContext, individualPersonAccounts, collectiveAccounts, translation).block());
            // assert
            assertThat(throwable).isInstanceOf(AggregationProcessingBusinessException.class).hasMessageStartingWith(MASTER_DATA_MAPPING_ERROR, "");
            AggregationProcessingBusinessException httpCallFailedException = (AggregationProcessingBusinessException) throwable;
            assertThat(httpCallFailedException.getHttpStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
            assertThat(httpCallFailedException.getCause()).isInstanceOf(NumberFormatException.class);
        }
    }

    @Test
    @DisplayName("Returns a http call error when accountCaptions api returns an error")
    void should_return_a_http_call_error_when_account_captions_returns_an_error() {
        // prepare
        MasterdataContext masterDataContext = TestDataLoader.load("json/acds-responses/masterDataContext.json", MasterdataContext.class);
        when(movementDataClient.getAccountSumDays(any())).thenReturn(Flux.empty());
        when(masterDataClient.getMasterDataContext(any(), any(), any(), any(), any(), any())).thenReturn(Mono.just(masterDataContext));
        HttpCallTechnicalException httpCallTechnicalException =
                new HttpCallTechnicalException(EXCEPTION_MESSAGE, SourceError.ACDS, SourceEndpoint.ACCOUNT_CAPTIONS, HttpStatus.BAD_REQUEST.value(),
                                               createProblemInfo());
        when(masterDataClient.getAccountCaptions(any()))
                .thenReturn(Mono.error(httpCallTechnicalException));
        StateDoc stateDoc = createStateDoc(StateDocState.DONE, TEST_FISCAL_YEAR_2021_START, TEST_FISCAL_YEAR_2021_END, schemaVersion);
        ImportData importData = new ImportData(masterDataContext, stateDoc);
        // execute
        Throwable throwable = catchThrowable(() -> importService.executeFullImport(importData).block());
        // assert
        assertThat(throwable).isInstanceOf(HttpCallTechnicalException.class);
        HttpCallTechnicalException httpCallException = (HttpCallTechnicalException) throwable;
        assertThat(httpCallException).isNotNull().usingRecursiveAssertion().isEqualTo(httpCallTechnicalException);
    }

    @Test
    @DisplayName("Returns a http call error when accountPurposeMappings api returns an error")
    void should_return_a_http_call_error_when_account_purpose_mappings_returns_an_error() {
        // prepare
        MasterdataContext masterDataContext = TestDataLoader.load("json/acds-responses/masterDataContext.json", MasterdataContext.class);
        when(movementDataClient.getAccountSumDays(any())).thenReturn(Flux.empty());
        when(masterDataClient.getMasterDataContext(any(), any(), any(), any(), any(), any())).thenReturn(Mono.just(masterDataContext));
        HttpCallTechnicalException httpCallTechnicalException =
                new HttpCallTechnicalException(EXCEPTION_MESSAGE, SourceError.ACDS, SourceEndpoint.ACCOUNT_PURPOSE_MAPPINGS,
                                               HttpStatus.BAD_REQUEST.value(), createProblemInfo());
        when(masterDataClient.getAccountPurposeMappings(any())).thenReturn(Mono.error(
                httpCallTechnicalException));
        StateDoc stateDoc = createStateDoc(StateDocState.DONE, TEST_FISCAL_YEAR_2021_START, TEST_FISCAL_YEAR_2021_END, schemaVersion);
        ImportData importData = new ImportData(masterDataContext, stateDoc);
        // execute
        Throwable throwable = catchThrowable(() -> importService.executeFullImport(importData).block());
        // assert
        assertThat(throwable).isInstanceOf(HttpCallTechnicalException.class);
        HttpCallTechnicalException httpCallException = (HttpCallTechnicalException) throwable;
        assertThat(httpCallException).isNotNull().usingRecursiveAssertion().isEqualTo(httpCallTechnicalException);
    }

    @Test
    @DisplayName("Returns a http call error when collectiveAccounts api returns an error")
    void should_return_a_http_call_error_when_collective_accounts_returns_an_error() {
        // prepare
        MasterdataContext masterDataContext = TestDataLoader.load("json/acds-responses/masterDataContext.json", MasterdataContext.class);
        when(movementDataClient.getAccountSumDays(any())).thenReturn(Flux.empty());
        when(masterDataClient.getMasterDataContext(any(), any(), any(), any(), any(), any())).thenReturn(Mono.just(masterDataContext));
        HttpCallTechnicalException httpCallTechnicalException =
                new HttpCallTechnicalException(EXCEPTION_MESSAGE, SourceError.ACDS, SourceEndpoint.COLLECTIVE_ACCOUNTS,
                                               HttpStatus.BAD_REQUEST.value(), createProblemInfo());
        when(masterDataClient.getCollectiveAccounts(any())).thenReturn(Mono.error(httpCallTechnicalException));
        StateDoc stateDoc = createStateDoc(StateDocState.DONE, TEST_FISCAL_YEAR_2021_START, TEST_FISCAL_YEAR_2021_END, schemaVersion);
        ImportData importData = new ImportData(masterDataContext, stateDoc);
        // execute
        Throwable throwable = catchThrowable(() -> importService.executeFullImport(importData).block());
        // assert
        assertThat(throwable).isInstanceOf(HttpCallTechnicalException.class);
        HttpCallTechnicalException httpCallException = (HttpCallTechnicalException) throwable;
        assertThat(httpCallException).isNotNull().usingRecursiveAssertion().isEqualTo(httpCallTechnicalException);
    }

    @Test
    @DisplayName("Returns a http call error when shareholderData api returns an error")
    void should_return_a_http_call_error_when_shareholder_data_returns_an_error() {
        // prepare
        MasterdataContext masterDataContext = TestDataLoader.load("json/acds-responses/masterDataContext.json", MasterdataContext.class);
        when(movementDataClient.getAccountSumDays(any())).thenReturn(Flux.empty());
        when(masterDataClient.getMasterDataContext(any(), any(), any(), any(), any(), any())).thenReturn(Mono.just(masterDataContext));
        HttpCallTechnicalException httpCallTechnicalException =
                new HttpCallTechnicalException(EXCEPTION_MESSAGE, SourceError.ACDS, SourceEndpoint.SHAREHOLDER, HttpStatus.BAD_REQUEST.value(),
                                               createProblemInfo());
        when(masterDataClient.getShareholderData(any())).thenReturn(Mono.error(httpCallTechnicalException));
        StateDoc stateDoc = createStateDoc(StateDocState.DONE, TEST_FISCAL_YEAR_2021_START, TEST_FISCAL_YEAR_2021_END, schemaVersion);
        ImportData importData = new ImportData(masterDataContext, stateDoc);
        // execute
        Throwable throwable = catchThrowable(() -> importService.executeFullImport(importData).block());
        // assert
        assertThat(throwable).isInstanceOf(HttpCallTechnicalException.class);
        HttpCallTechnicalException httpCallException = (HttpCallTechnicalException) throwable;
        assertThat(httpCallException).isNotNull().usingRecursiveAssertion().isEqualTo(httpCallTechnicalException);
    }

    @Test
    @DisplayName("Returns a http call error when translation api returns an error")
    void should_return_a_http_call_error_when_translation_returns_an_error() {
        // prepare
        MasterdataContext masterDataContext = TestDataLoader.load("json/acds-responses/masterDataContext.json", MasterdataContext.class);
        when(movementDataClient.getAccountSumDays(any())).thenReturn(Flux.empty());
        when(masterDataClient.getMasterDataContext(any(), any(), any(), any(), any(), any())).thenReturn(Mono.just(masterDataContext));
        HttpCallTechnicalException httpCallTechnicalException =
                new HttpCallTechnicalException(EXCEPTION_MESSAGE, SourceError.ACDS, SourceEndpoint.TRANSLATION, HttpStatus.BAD_REQUEST.value(),
                                               createProblemInfo());
        when(masterDataClient.getTranslation(any())).thenReturn(Mono.error(httpCallTechnicalException));
        StateDoc stateDoc = createStateDoc(StateDocState.DONE, TEST_FISCAL_YEAR_2021_START, TEST_FISCAL_YEAR_2021_END, schemaVersion);
        ImportData importData = new ImportData(masterDataContext, stateDoc);
        // execute
        Throwable throwable = catchThrowable(() -> importService.executeFullImport(importData).block());
        // assert
        assertThat(throwable).isInstanceOf(HttpCallTechnicalException.class);
        HttpCallTechnicalException httpCallException = (HttpCallTechnicalException) throwable;
        assertThat(httpCallException).isNotNull().usingRecursiveAssertion().isEqualTo(httpCallTechnicalException);
    }

    @Test
    @DisplayName("Returns a http call error when masterDataInventories api returns an error")
    void should_return_a_http_call_error_when_master_data_inventories_returns_an_error() {
        // prepare
        MasterdataContext masterDataContext = TestDataLoader.load("json/acds-responses/masterDataContext.json", MasterdataContext.class);
        when(movementDataClient.getAccountSumDays(any())).thenReturn(Flux.empty());
        when(masterDataClient.getMasterDataContext(any(), any(), any(), any(), any(), any())).thenReturn(Mono.just(masterDataContext));
        HttpCallTechnicalException httpCallTechnicalException =
                new HttpCallTechnicalException(EXCEPTION_MESSAGE, SourceError.ACDS, SourceEndpoint.MASTER_DATA_INVENTORIES,
                                               HttpStatus.BAD_REQUEST.value(), createProblemInfo());
        when(masterDataClient.getInventories(any())).thenReturn(Mono.error(
                httpCallTechnicalException));
        StateDoc stateDoc = createStateDoc(StateDocState.DONE, TEST_FISCAL_YEAR_2021_START, TEST_FISCAL_YEAR_2021_END, schemaVersion);
        ImportData importData = new ImportData(masterDataContext, stateDoc);
        // execute
        Throwable throwable = catchThrowable(() -> importService.executeFullImport(importData).block());
        // assert
        assertThat(throwable).isInstanceOf(HttpCallTechnicalException.class);
        HttpCallTechnicalException httpCallException = (HttpCallTechnicalException) throwable;
        assertThat(httpCallException).isNotNull().usingRecursiveAssertion().isEqualTo(httpCallTechnicalException);
    }

    @Test
    @DisplayName("Returns a http call error when movementDataInventories api returns an error")
    void should_return_a_http_call_error_when_movement_data_inventories_returns_an_error() {
        // prepare
        MasterdataContext masterDataContext = TestDataLoader.load("json/acds-responses/masterDataContext.json", MasterdataContext.class);
        when(movementDataClient.getAccountSumDays(any())).thenReturn(Flux.empty());
        when(masterDataClient.getMasterDataContext(any(), any(), any(), any(), any(), any())).thenReturn(Mono.just(masterDataContext));
        HttpCallTechnicalException httpCallTechnicalException =
                new HttpCallTechnicalException(EXCEPTION_MESSAGE, SourceError.ACDS, SourceEndpoint.MOVEMENT_DATA_INVENTORIES,
                                               HttpStatus.BAD_REQUEST.value(), createProblemInfo());
        when(movementDataClient.getMovementDataInventories(any())).thenReturn(Flux.error(httpCallTechnicalException));
        StateDoc stateDoc = createStateDoc(StateDocState.DONE, TEST_FISCAL_YEAR_2021_START, TEST_FISCAL_YEAR_2021_END, schemaVersion);
        ImportData importData = new ImportData(masterDataContext, stateDoc);
        // execute
        Throwable throwable = catchThrowable(() -> importService.executeFullImport(importData).block());
        // assert
        assertThat(throwable).isInstanceOf(HttpCallTechnicalException.class);
        HttpCallTechnicalException httpCallException = (HttpCallTechnicalException) throwable;
        assertThat(httpCallException).isNotNull().usingRecursiveAssertion().isEqualTo(httpCallTechnicalException);
    }

    @Test
    @DisplayName("Returns a http call error when accountSumDays api returns a bad response")
    void should_return_a_http_call_error_when_account_sum_days_returns_an_error() {
        // prepare
        MasterdataContext masterDataContext = TestDataLoader.load("json/acds-responses/masterDataContext.json", MasterdataContext.class);
        when(masterDataClient.getMasterDataContext(any(), any(), any(), any(), any(), any())).thenReturn(Mono.just(masterDataContext));
        HttpCallTechnicalException httpCallTechnicalException =
                new HttpCallTechnicalException(EXCEPTION_MESSAGE, SourceError.ACDS, SourceEndpoint.ACCOUNT_SUM_DAYS, HttpStatus.BAD_REQUEST.value(),
                                               createProblemInfo());
        when(movementDataClient.getAccountSumDays(any())).thenReturn(Flux.error(
                httpCallTechnicalException));
        StateDoc stateDoc = createStateDoc(StateDocState.DONE, TEST_FISCAL_YEAR_2021_START, TEST_FISCAL_YEAR_2021_END, schemaVersion);
        ImportData importData = new ImportData(masterDataContext, stateDoc);
        // execute
        Throwable throwable = catchThrowable(() -> importService.executeFullImport(importData).block());
        // assert
        assertThat(throwable).isInstanceOf(HttpCallTechnicalException.class);
        HttpCallTechnicalException httpCallException = (HttpCallTechnicalException) throwable;
        assertThat(httpCallException).isNotNull().usingRecursiveAssertion().isEqualTo(httpCallTechnicalException);
    }

    private void mockAllServiceCallsToEmptyResponses() {
        when(movementDataClient.getAccountSumDays(any())).thenReturn(Flux.fromIterable(new ArrayList<>()));
        when(masterDataClient.getMasterDataContext(any(), any(), any(), any(), any(), any())).thenReturn(Mono.just(new MasterdataContext()));
        when(masterDataClient.getCollectiveAccounts(any())).thenReturn(Mono.just(new ArrayList<>()));
        when(masterDataClient.getAccountPurposeMappings(any())).thenReturn(Mono.just(new ArrayList<>()));
        when(masterDataClient.getAccountCaptions(any())).thenReturn(Mono.just(new ArrayList<>()));
        when(masterDataClient.getShareholderData(any())).thenReturn(Mono.just(new ShareholderData()));
        when(masterDataClient.getTranslation(any())).thenReturn(Mono.just(new Translation()));
        when(masterDataClient.getInventories(any())).thenReturn(Mono.just(new ArrayList<>()));
        when(movementDataClient.getMovementDataInventories(any())).thenReturn(Flux.fromIterable(new ArrayList<>()));
        when(movementDataDayRepository.findByBusinessKey(any(), any(), any())).thenReturn(Flux.empty());
        when(masterDataRepository.upsertOne(any(), any(), any(), any(), any(), any(), any(), any(),any())).thenReturn(Mono.empty());
        when(customColumnStructureContentRepository.bulkInsert(any())).thenReturn(Mono.empty());
        when(customReportStructureContentRepository.bulkInsert(any())).thenReturn(Mono.empty());
    }
}