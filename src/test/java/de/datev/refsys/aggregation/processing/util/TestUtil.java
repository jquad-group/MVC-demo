package de.datev.refsys.aggregation.processing.util;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import de.datev.refsys.aggregation.document.model.AccountDescription;
import de.datev.refsys.aggregation.document.model.AccountGroupValue;
import de.datev.refsys.aggregation.document.model.AccountValue;
import de.datev.refsys.aggregation.document.model.Description;
import de.datev.refsys.aggregation.document.model.MovementDataDay;
import de.datev.refsys.aggregation.document.model.MovementDataMonth;
import de.datev.refsys.aggregation.document.model.MovementDataPersonGroupDay;
import de.datev.refsys.aggregation.document.model.MovementDataPersonGroupMonth;
import de.datev.refsys.aggregation.document.model.ProblemInfo;
import de.datev.refsys.aggregation.document.model.StateDoc;
import de.datev.refsys.aggregation.document.model.enums.StateDocState;
import de.datev.refsys.aggregation.processing.client.test_model.StatusCodeErrorInfo;
import de.datev.refsys.aggregation.processing.constant.ProcessingErrorMessageConstants;
import de.datev.refsys.aggregation.processing.exception.HttpCallBusinessException;
import de.datev.refsys.aggregation.processing.exception.HttpCallTechnicalException;
import de.datev.refsys.aggregation.processing.model.AccountDbKeyFields;
import de.datev.refsys.aggregation.processing.model.enums.SourceError;
import de.datev.refsys.generated.acds.api.model.MasterdataContext;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import lombok.Builder;
import lombok.experimental.UtilityClass;
import org.slf4j.LoggerFactory;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import reactor.util.context.Context;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static de.datev.refsys.aggregation.document.model.constants.CollectionConstants.DAY_PREFIX;
import static de.datev.refsys.aggregation.document.model.constants.CollectionConstants.MONTH_PREFIX;
import static de.datev.refsys.aggregation.processing.util.CircuitBreakerUtil.CB_LOG;
import static org.assertj.core.api.Assertions.assertThat;

@UtilityClass
public class TestUtil {
    public static final String TEST_PROFILE = "test";
    public static final String LOG_TEST_PROFILE = "log-test";
    public static final String KAFKA_TEST_PROFILE = "kafka-test";
    public static final Integer TEST_CONSULTANT = 29098;
    public static final Integer TEST_CLIENT = 55003;
    public static final Integer TEST_FISCAL_YEAR_2020_START = 20200101;
    public static final Integer TEST_FISCAL_YEAR_2021_START = 20210101;
    public static final Integer TEST_FISCAL_YEAR_2021_END = 20211231;
    public static final Integer TEST_FISCAL_YEAR_2022_START = 20220101;
    public static final Integer TEST_FISCAL_YEAR_2022_END = 20221231;
    public static final Integer TEST_ACCOUNT_NUMBER_2 = 620000000;
    public static final Integer TEST_ACCOUNT_NUMBER_3 = 200000;
    public static final Integer TEST_ACCOUNT_NUMBER_4 = 255555;
    public static final Integer TEST_ACCOUNT_NUMBER_5 = 300000;
    public static final Integer TEST_ACCOUNT_NUMBER_6 = 355555;
    public static final Integer TEST_ACCOUNT_GROUP_NUMBER_2 = 2;
    public static final Long TEST_BASE_VERSION = 0L;
    public static final Long TEST_BASE_VERSION_6 = 6L;
    public static final Long TEST_DELTA_VERSION = 0L;
    public static final Long TEST_DELTA_VERSION_3 = 3L;
    public static final Long TEST_BASE_VERSION_UPDATE = 1L;
    public static final Long TEST_DELTA_VERSION_UPDATE = 1L;
    public static final Set<Integer> TEST_ACCOUNT_NUMBERS_SET = Set.of(14000000, 14000001);
    public static final Integer TEST_FALSE_CONSULTANT = 0;
    public static final Integer TEST_FALSE_CLIENT = 0;
    public static final Integer TEST_FALSE_FISCAL_YEAR = 00000000;
    public static final Long TEST_AMOUNT_CREDIT = 100L;
    public static final Long TEST_AMOUNT_DEBIT = 200L;
    public static final Long TEST_AMOUNT_CREDIT_UNUSUAL = 50L;
    public static final Long TEST_AMOUNT_DEBIT_UNUSUAL = 50L;
    public static final Long TEST_WEIGHT_CREDIT = 200L;
    public static final Long TEST_WEIGHT_DEBIT = 200L;
    public static final Integer TEST_QUANTITY_CREDIT = 200;
    public static final Integer TEST_QUANTITY_DEBIT = 200;
    public static final Integer TEST_ACCOUNT_VALUE_DAY_1 = 20200201;
    public static final Integer TEST_ACCOUNT_VALUE_DAY_2 = 20200202;
    public static final Integer TEST_ACCOUNT_VALUE_PERSON_GROUP_DAY_1 = 20210501;
    public static final Integer TEST_ACCOUNT_VALUE_PERSON_GROUP_DAY_2 = 20220202;
    public static final Integer TEST_ACCOUNT_VALUE_MONTH_1 = 1;
    public static final Integer TEST_ACCOUNT_VALUE_MONTH_2 = 2;
    public static final Integer TEST_ACCOUNT_VALUE_PERSON_GROUP_MONTH_1 = 1;
    public static final Integer TEST_ACCOUNT_VALUE_PERSON_GROUP_MONTH_2 = 2;
    public static final String TEST_CULTURE_CODE = "en-GB";
    public static final String TEST_ACCOUNT_CAPTION = "Account Caption";
    public static final String EXCEPTION_MESSAGE = "test exception";
    public static final String TEST_CORRELATION_ID = "9d2dee33-7803-485a-a2b1-2c7538e597ee";
    public static final Integer TEST_ACCOUNT_SYSTEM = 3;
    public static final Integer TEST_INDUSTRY_NO = 0;
    public static final Integer TEST_INDUSTRY_ID = 0;
    public static final Integer TEST_ACCOUNT_LENGTH = 4;
    public static final String TEST_DATEV_CLIENT_ID = "8d2dee34-7805-485a-a2b1-2c7838e597ee";

    public static final String CALL_NOT_PERMITTED_CIRCUIT_BREAKER_ERROR =
                                                        "CircuitBreaker '%s' is OPEN and does not permit further calls";

    public static final Context TEST_CONTEXT = Context.of(LoggingUtil.CORRELATION_ID_KEY, TestUtil.TEST_CORRELATION_ID);
    public static final String TEST_INVENTORY_NUMBER = "140001";
    public static final String TEST_INVENTORY_NUMBER_2 = "140002";
    public static final String TEST_INVENTORY_NUMBER_3 = "140003";
    public static final String TEST_TITLE = "test title";
    public static final String TEST_DETAILS = "test details";
    public static final String TEST_TYPE = "test type";
    public static final String TEST_INSTANCE = "test instance";

    public static MasterdataContext createTestMasterdataContext() {
        return MasterdataContext.builder()
                                .consultant(TEST_CONSULTANT)
                                .client(TEST_CLIENT)
                                .yearBegin(TEST_FISCAL_YEAR_2022_START)
                                .baseVersion(TEST_BASE_VERSION)
                                .deltaVersion(TEST_DELTA_VERSION)
                                .accountSystem(TEST_ACCOUNT_SYSTEM)
                                .industryNo(TEST_INDUSTRY_NO)
                                .industryId(TEST_INDUSTRY_ID)
                                .accountLength(TEST_ACCOUNT_LENGTH)
                                .useOrganisationAccountCaption(false)
                                .useConsultantAccountingFunctions(false)
                                .useClientAccountingFunctions(false)
                                .useSkrFollowingYear(false)
                                .usePreviousYearAccountTranslation(false)
                                .useAlternativeAccountTranslation(false)
                                .supportedLanguages(Collections.emptyList())
                                .build();
    }

    public void stubACDSResponse(WireMockServer wireMockServer, int statusCode, String endpoint, Map<String, Object> queryParams, String body) {
        String url = "/api/v1/accounting-dataservices/consultants/" + TEST_CONSULTANT + "/clients/" + TEST_CLIENT + "/" + endpoint;
        MappingBuilder mappingBuilder = get(urlPathEqualTo(url));
        queryParams.forEach((key, value) -> mappingBuilder.withQueryParam(key, equalTo(String.valueOf(value))));
        wireMockServer.stubFor(mappingBuilder
                                       .willReturn(aResponse().withBody(body).withStatus(statusCode)));
    }

    public static void stubACDSMultipleYearsResponses(WireMockServer wireMockServer, Map<String, WireMockServerSettings> wireMockServerSettings) {
        wireMockServerSettings.forEach((key, serverSettings) -> serverSettings.fiscalYears.forEach(fiscalYear -> {
            MappingBuilder mappingBuilder = get(urlPathMatching(serverSettings.url))
                    .withQueryParam("fiscal-year", equalTo(String.valueOf(fiscalYear)));
            serverSettings.queryParams.forEach((name, val) ->
                                               {
                                                   if (name.equals("culture-codes")) {
                                                       Arrays.asList(val.split(",", -1))
                                                             .forEach(cultureCode -> mappingBuilder.withQueryParam(name, equalTo(cultureCode)));
                                                   } else {
                                                       mappingBuilder.withQueryParam(name, equalTo(val));
                                                   }
                                               });
            ResponseDefinitionBuilder responseDefinitionBuilder = aResponse()
                    .withStatus(serverSettings.httpStatus);
            if (serverSettings.path != null) {
                responseDefinitionBuilder.withBody(loadResourceAsString(serverSettings.path))
                                         .withTransformers("response-template")
                                         .withHeader("Content-Type", serverSettings.mediaType);
            }
            wireMockServer.stubFor(mappingBuilder.willReturn(responseDefinitionBuilder));
        }));
    }

    public static String loadResourceAsString(String fileName) {
        Scanner scanner = new Scanner(TestUtil.class.getClassLoader().getResourceAsStream(fileName));
        String contents = scanner.useDelimiter("\\A").next();
        scanner.close();
        return contents;
    }

    @Builder
    public static class WireMockServerSettings {
        public String url;
        public Map<String, String> queryParams;
        public List<Integer> fiscalYears;
        public Integer httpStatus;
        public String path;
        public String mediaType;
    }

    public static MovementDataMonth createMovementDataMonth(MovementDataMonth movementDataMonth) {
        movementDataMonth.setValues(createMonthAccountValues());
        return movementDataMonth;
    }

    public static MovementDataDay createMovementDataDay(MovementDataDay movementDataDay) {
        movementDataDay.setValues(createDayAccountValues());
        return movementDataDay;
    }

    public static MovementDataPersonGroupDay createMovementDataPersonGroupDay(MovementDataPersonGroupDay movementDataPersonGroupDay) {
        movementDataPersonGroupDay.setValues(createPersonGroupDayAccountGroupValues());
        return movementDataPersonGroupDay;
    }

    public static MovementDataPersonGroupMonth createMovementDataPersonGroupMonth(MovementDataPersonGroupMonth movementDataPersonGroupMonth) {
        movementDataPersonGroupMonth.setValues(createPersonGroupMonthAccountGroupValues());
        return movementDataPersonGroupMonth;
    }

    public static Map<String, AccountValue> createMonthAccountValues() {
        return Map.of(MONTH_PREFIX + TEST_ACCOUNT_VALUE_MONTH_1, createAccountValue(),
                      MONTH_PREFIX + TEST_ACCOUNT_VALUE_MONTH_2, createAccountValue());
    }

    public static Map<String, AccountValue> createDayAccountValues() {
        return Map.of(DAY_PREFIX + TEST_ACCOUNT_VALUE_DAY_1, createAccountValue(),
                      DAY_PREFIX + TEST_ACCOUNT_VALUE_DAY_2, createAccountValue());
    }

    public static Map<String, AccountGroupValue> createPersonGroupDayAccountGroupValues() {
        return Map.of(DAY_PREFIX + TEST_ACCOUNT_VALUE_PERSON_GROUP_DAY_1, createAccountGroupValue(),
                      DAY_PREFIX + TEST_ACCOUNT_VALUE_PERSON_GROUP_DAY_2, createAccountGroupValue());
    }

    public static Map<String, AccountGroupValue> createPersonGroupMonthAccountGroupValues() {
        return Map.of(MONTH_PREFIX + TEST_ACCOUNT_VALUE_PERSON_GROUP_MONTH_1, createAccountGroupValue(),
                      MONTH_PREFIX + TEST_ACCOUNT_VALUE_PERSON_GROUP_MONTH_2, createAccountGroupValue());
    }

    public static AccountValue createAccountValue() {
        return AccountValue.builder()
                           .amountCredit(TEST_AMOUNT_CREDIT)
                           .amountDebit(TEST_AMOUNT_DEBIT)
                           .weightCredit(TEST_WEIGHT_CREDIT)
                           .weightDebit(TEST_WEIGHT_DEBIT)
                           .quantityCredit(TEST_QUANTITY_CREDIT)
                           .quantityDebit(TEST_QUANTITY_DEBIT).build();
    }

    public static AccountGroupValue createAccountGroupValue() {
        return AccountGroupValue.builder()
                                .amountCreditUsual(TEST_AMOUNT_CREDIT)
                                .amountDebitUsual(TEST_AMOUNT_DEBIT)
                                .amountCreditUnusual(TEST_AMOUNT_CREDIT_UNUSUAL)
                                .amountDebitUnusual(TEST_AMOUNT_DEBIT_UNUSUAL)
                                .weightCredit(TEST_WEIGHT_CREDIT)
                                .weightDebit(TEST_WEIGHT_DEBIT)
                                .quantityCredit(TEST_QUANTITY_CREDIT)
                                .quantityDebit(TEST_QUANTITY_DEBIT).build();
    }

    public static AccountDescription createAccountDescription(Integer accountNumber) {
        Map<String, Description> caption = new HashMap<>();
        caption.put(TEST_CULTURE_CODE, new Description(TEST_ACCOUNT_CAPTION, null));
        return new AccountDescription(accountNumber, true, caption);
    }

    public static AccountDbKeyFields createAccountDbKeyFields(MovementDataPersonGroupDay personGroupDay) {
        return AccountDbKeyFields.builder()
                                 .accountNumber(personGroupDay.getAccountGroupNumber())
                                 .accountingCommitted(personGroupDay.getAdditionalParams().getAccountingCommitted())
                                 .accountingReasonId(personGroupDay.getAccountingReasonId())
                                 .agricultureAndForestryAccountType(personGroupDay.getAdditionalParams().getAgricultureAndForestryAccountType())
                                 .businessAssetsAssignment(personGroupDay.getAdditionalParams().getBusinessAssetsAssignment())
                                 .cost1(personGroupDay.getAdditionalParams().getCost1())
                                 .cost2(personGroupDay.getAdditionalParams().getCost2())
                                 .recordType(personGroupDay.getAdditionalParams().getRecordType())
                                 .rwShareholderId(personGroupDay.getAdditionalParams().getRwShareholderId())
                                 .taxRate(personGroupDay.getAdditionalParams().getTaxRate().floatValue())
                                 .build();
    }

    public static AccountDbKeyFields createAccountDbKeyFields(MovementDataPersonGroupMonth personGroupMonth) {
        return AccountDbKeyFields.builder()
                                 .accountNumber(personGroupMonth.getAccountGroupNumber())
                                 .accountingCommitted(personGroupMonth.getAdditionalParams().getAccountingCommitted())
                                 .accountingReasonId(personGroupMonth.getAccountingReasonId())
                                 .agricultureAndForestryAccountType(personGroupMonth.getAdditionalParams().getAgricultureAndForestryAccountType())
                                 .businessAssetsAssignment(personGroupMonth.getAdditionalParams().getBusinessAssetsAssignment())
                                 .cost1(personGroupMonth.getAdditionalParams().getCost1())
                                 .cost2(personGroupMonth.getAdditionalParams().getCost2())
                                 .recordType(personGroupMonth.getAdditionalParams().getRecordType())
                                 .rwShareholderId(personGroupMonth.getAdditionalParams().getRwShareholderId())
                                 .taxRate(personGroupMonth.getAdditionalParams().getTaxRate().floatValue())
                                 .build();
    }

    public static AccountDbKeyFields createAccountDbKeyFields(MovementDataMonth movementDataMonth) {
        return AccountDbKeyFields.builder()
                                 .accountNumber(movementDataMonth.getAccountNumber())
                                 .accountingCommitted(movementDataMonth.getAdditionalParams().getAccountingCommitted())
                                 .accountingReasonId(movementDataMonth.getAccountingReasonId())
                                 .agricultureAndForestryAccountType(movementDataMonth.getAdditionalParams().getAgricultureAndForestryAccountType())
                                 .businessAssetsAssignment(movementDataMonth.getAdditionalParams().getBusinessAssetsAssignment())
                                 .cost1(movementDataMonth.getAdditionalParams().getCost1())
                                 .cost2(movementDataMonth.getAdditionalParams().getCost2())
                                 .recordType(movementDataMonth.getAdditionalParams().getRecordType())
                                 .rwShareholderId(movementDataMonth.getAdditionalParams().getRwShareholderId())
                                 .taxRate(movementDataMonth.getAdditionalParams().getTaxRate().floatValue())
                                 .build();
    }

    public static AccountDbKeyFields createAccountDbKeyFields(MovementDataDay movementDataDay) {
        return AccountDbKeyFields.builder()
                                 .accountNumber(movementDataDay.getAccountNumber())
                                 .accountingCommitted(movementDataDay.getAdditionalParams().getAccountingCommitted())
                                 .accountingReasonId(movementDataDay.getAccountingReasonId())
                                 .agricultureAndForestryAccountType(movementDataDay.getAdditionalParams().getAgricultureAndForestryAccountType())
                                 .businessAssetsAssignment(movementDataDay.getAdditionalParams().getBusinessAssetsAssignment())
                                 .cost1(movementDataDay.getAdditionalParams().getCost1())
                                 .cost2(movementDataDay.getAdditionalParams().getCost2())
                                 .recordType(movementDataDay.getAdditionalParams().getRecordType())
                                 .rwShareholderId(movementDataDay.getAdditionalParams().getRwShareholderId())
                                 .taxRate(movementDataDay.getAdditionalParams().getTaxRate().floatValue())
                                 .build();
    }

    public static Properties getTestProperties() throws IOException {
        YamlPropertySourceLoader ypsl = new YamlPropertySourceLoader();
        PropertySource<?> ps = ypsl.load("application-test", new ClassPathResource("/application-test.yml")).get(0);
        Properties props = new Properties();
        props.putAll((Map<?, ?>) ps.getSource());
        return props;
    }

    public static StateDoc createStateDoc(StateDocState state, Integer yearBegin, Integer yearEnd, Integer schemaVersion) {
        return createStateDoc(state, yearBegin, yearEnd, schemaVersion, TEST_BASE_VERSION, TEST_DELTA_VERSION);
    }

    public static StateDoc createStateDoc(StateDocState state, Integer yearBegin, Integer yearEnd, Integer schemaVersion, Long baseVersion,
                                          Long deltaVersion) {
        OffsetDateTime currentTime = OffsetDateTime.now();
        return StateDoc.builder()
                       .consultant(TEST_CONSULTANT)
                       .client(TEST_CLIENT)
                       .baseVersion(baseVersion)
                       .deltaVersion(deltaVersion)
                       .state(state)
                       .yearBegin(yearBegin)
                       .yearEnd(yearEnd)
                       .stateTimestamp(currentTime)
                       .createdTimestamp(currentTime)
                       .schemaVersion(schemaVersion)
                       .forceReftabCurrentYear(false)
                       .build();
    }

    public static void stubAcdsClientApi(String endpoint, int client, String bodyUrl, int status, WireMockServer wireMockServer) {
        String url = "/api/v1/accounting-dataservices/consultants/" + TEST_CONSULTANT + "/clients/" + client + "/" + endpoint;
        ResponseDefinitionBuilder responseDefinitionBuilder = aResponse().withHeader("x-correlation-id", String.valueOf(equalTo(TEST_CORRELATION_ID)))
                                                                         .withHeader("Content-Type", "application/json")
                                                                         .withStatus(status);
        if (bodyUrl != null) {
            responseDefinitionBuilder.withBody(TestDataLoader.load(bodyUrl));
        }
        wireMockServer.stubFor(get(urlPathEqualTo(url)).willReturn(responseDefinitionBuilder));
    }

    public static void stubAcdsClientApiWithDelay(String endpoint, int client, String bodyUrl, int status, WireMockServer wireMockServer,
                                                  int delayInMS) {
        String url = "/api/v1/accounting-dataservices/consultants/" + TEST_CONSULTANT + "/clients/" + client + "/" + endpoint;
        ResponseDefinitionBuilder responseDefinitionBuilder = aResponse().withHeader("x-correlation-id", String.valueOf(equalTo(TEST_CORRELATION_ID)))
                                                                         .withHeader("Content-Type", "application/json")
                                                                         .withStatus(status)
                                                                         .withFixedDelay(delayInMS);
        if (bodyUrl != null) {
            responseDefinitionBuilder.withBody(TestDataLoader.load(bodyUrl));
        }
        wireMockServer.stubFor(get(urlPathEqualTo(url)).willReturn(responseDefinitionBuilder));
    }

    public static MasterdataContext getMasterdataContext(Integer client) {
        MasterdataContext masterdataContext = createTestMasterdataContext();
        masterdataContext.setClient(client);
        masterdataContext.setYearBegin(TEST_FISCAL_YEAR_2021_START);
        return masterdataContext;
    }

    public static Map<Integer, StatusCodeErrorInfo> getStatusCodeErrorInfoMap() {
        Map<Integer,StatusCodeErrorInfo> map = new HashMap<>();
        map.put(400, new StatusCodeErrorInfo(ProcessingErrorMessageConstants.ACDS_TECHNICAL_ERROR, HttpCallTechnicalException.class, SourceError.ACDS));
        map.put(404, new StatusCodeErrorInfo(ProcessingErrorMessageConstants.ACDS_TECHNICAL_ERROR, HttpCallTechnicalException.class, SourceError.ACDS));
        map.put(555, new StatusCodeErrorInfo(ProcessingErrorMessageConstants.ACDS_BUSINESS_ERROR, HttpCallBusinessException.class, SourceError.ACDS));
        map.put(204, new StatusCodeErrorInfo(ProcessingErrorMessageConstants.NO_MASTER_DATA_CONTEXT_IN_ACDS, HttpCallTechnicalException.class, SourceError.ACDS));
        return map;
    }

    public static CircuitBreaker getCircuitBreaker(String circuitBreakerName, CircuitBreakerConfig circuitBreakerConfig) {
        CircuitBreaker circuitBreaker = CircuitBreaker.of(circuitBreakerName, circuitBreakerConfig);
        circuitBreaker.getEventPublisher().onStateTransition(CircuitBreakerUtil::logOnStateTransition);
        return circuitBreaker;
    }

    public static MemoryAppender setupMemoryAppender(MemoryAppender memoryAppender, Class<?> clazz, Level logLevel) {
        if (memoryAppender == null) {
            Logger logger = (Logger) LoggerFactory.getLogger(clazz);
            memoryAppender = new MemoryAppender();
            memoryAppender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
            logger.setLevel(logLevel);
            logger.addAppender(memoryAppender);
            memoryAppender.start();
        } else {
            memoryAppender.reset();
        }
        return memoryAppender;
    }

    public static MemoryAppender setupErrorMemoryAppender(MemoryAppender memoryAppender) {
        if (memoryAppender == null) {
            Logger rootLogger = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
            memoryAppender = new MemoryAppender();
            memoryAppender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
            rootLogger.setLevel(Level.ERROR);
            rootLogger.addAppender(memoryAppender);
            memoryAppender.start();
        } else {
            memoryAppender.reset();
        }
        return memoryAppender;
    }

    public static List<ILoggingEvent> searchCircuitBreakerLog(CircuitBreaker circuitBreaker, MemoryAppender memoryAppender, CircuitBreaker.State fromState, CircuitBreaker.State toState) {
        return memoryAppender.search(String.format(CB_LOG, circuitBreaker.getName(), fromState, toState));
    }

    public static void assertLogs(List<ILoggingEvent> loggingEvents, Level logLevel) {
        assertThat(loggingEvents).isNotEmpty();
        assertThat(loggingEvents.get(0).getLevel()).isEqualTo(logLevel);
    }

    public static ProblemInfo createProblemInfo() {
        return ProblemInfo.builder()
                          .title(TEST_TITLE)
                          .detail(TEST_DETAILS)
                          .type(TEST_TYPE)
                          .instance(TEST_INSTANCE)
                          .build();
    }
}