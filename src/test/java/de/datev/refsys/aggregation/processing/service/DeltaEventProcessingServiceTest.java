package de.datev.refsys.aggregation.processing.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.mongodb.ClientSessionOptions;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.result.UpdateResult;
import com.mongodb.reactivestreams.client.ClientSession;
import com.mongodb.reactivestreams.client.MongoClient;
import de.datev.refsys.aggregation.document.model.StateDoc;
import de.datev.refsys.aggregation.document.model.enums.StateDocState;
import de.datev.refsys.aggregation.processing.api.model.DeltaInfo;
import de.datev.refsys.aggregation.processing.api.model.DeltaRequest;
import de.datev.refsys.aggregation.processing.constant.ProcessingErrorMessageConstants;
import de.datev.refsys.aggregation.processing.constant.ProcessingServiceConstants;
import de.datev.refsys.aggregation.processing.exception.InitialLoadFailedException;
import de.datev.refsys.aggregation.processing.exception.RestWarnException;
import de.datev.refsys.aggregation.processing.mapper.AccountDbKeyFieldsMapper;
import de.datev.refsys.aggregation.processing.mapper.AccountDbKeyFieldsMapperImpl;
import de.datev.refsys.aggregation.processing.mapper.AccountSumDayMapper;
import de.datev.refsys.aggregation.processing.mapper.AccountSumDayMapperImpl;
import de.datev.refsys.aggregation.processing.mapper.AccountValueMapper;
import de.datev.refsys.aggregation.processing.mapper.AccountValueMapperImpl;
import de.datev.refsys.aggregation.processing.mapper.AdditionalParametersMapper;
import de.datev.refsys.aggregation.processing.mapper.AdditionalParametersMapperImpl_;
import de.datev.refsys.aggregation.processing.repository.MasterDataAccountRepository;
import de.datev.refsys.aggregation.processing.repository.MasterDataRepository;
import de.datev.refsys.aggregation.processing.repository.MovementDataDayRepository;
import de.datev.refsys.aggregation.processing.repository.MovementDataMonthRepository;
import de.datev.refsys.aggregation.processing.repository.MovementDataPersonGroupDayRepository;
import de.datev.refsys.aggregation.processing.repository.MovementDataPersonGroupMonthRepository;
import de.datev.refsys.aggregation.processing.repository.StateDocRepository;
import de.datev.refsys.aggregation.processing.util.LoggingUtil;
import de.datev.refsys.aggregation.processing.util.MemoryAppender;
import de.datev.refsys.aggregation.processing.util.TestDataLoader;
import de.datev.refsys.aggregation.processing.util.TestUtil;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static de.datev.refsys.aggregation.processing.util.LoggingUtil.DELTA_EVENT_BATCH_START_LOG;
import static de.datev.refsys.aggregation.processing.util.LoggingUtil.DELTA_EVENT_BATCH_SUCCESS_LOG;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_BASE_VERSION_UPDATE;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_CLIENT;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_CONSULTANT;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_DELTA_VERSION_3;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_DELTA_VERSION_UPDATE;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_FISCAL_YEAR_2021_END;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_FISCAL_YEAR_2021_START;
import static de.datev.refsys.aggregation.processing.util.TestUtil.createStateDoc;
import static de.datev.refsys.aggregation.processing.util.TestUtil.setupMemoryAppender;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles(TestUtil.TEST_PROFILE)
class DeltaEventProcessingServiceTest {

    @Mock
    private MongoClient mongoClientMock;

    @Mock
    private StateDocRepository stateDocRepositoryMock;

    @Mock
    private MasterDataRepository masterDataRepositoryMock;

    @Mock
    private MasterDataAccountRepository masterDataAccountRepositoryMock;

    @Mock
    private MovementDataDayRepository movementDataDayRepositoryMock;

    @Mock
    private MovementDataMonthRepository movementDataMonthRepositoryMock;

    @Mock
    private MovementDataPersonGroupDayRepository movementDataPersonGroupDayRepositoryMock;

    @Mock
    private MovementDataPersonGroupMonthRepository movementDataPersonGroupMonthRepositoryMock;

    @Mock
    private ClientSessionOptions clientSessionOptionsMock;

    @Mock
    private ClientSession clientSessionMock;

    private MemoryAppender memoryAppender;
    private MemoryAppender importServiceMemoryAppender;
    private DeltaEventProcessingService deltaEventProcessingService;
    private Mono<DeltaRequest> deltaRequest;
    private Integer schemaVersion;

    @BeforeAll
    public  void setUp() {
        schemaVersion = 1;
        deltaRequest = Mono.just(TestDataLoader.load("json/requests/delta/delta-request-with-account-sum-day-deltas.json", DeltaRequest.class));
        memoryAppender = setupMemoryAppender(memoryAppender, DeltaEventProcessingServiceImpl.class, Level.DEBUG);
        importServiceMemoryAppender = setupMemoryAppender(importServiceMemoryAppender, LoggingUtil.class, Level.DEBUG);
        AdditionalParametersMapper additionalParametersMapper = new AdditionalParametersMapperImpl_();
        AccountDbKeyFieldsMapper accountDbKeyFieldsMapper = new AccountDbKeyFieldsMapperImpl();
        AccountValueMapper accountValueMapper = new AccountValueMapperImpl();
        AccountSumDayMapper accountSumDayMapper = new AccountSumDayMapperImpl();
        deltaEventProcessingService = new DeltaEventProcessingServiceImpl(mongoClientMock, stateDocRepositoryMock, masterDataRepositoryMock,
                                                                          masterDataAccountRepositoryMock, movementDataDayRepositoryMock,
                                                                          movementDataMonthRepositoryMock, movementDataPersonGroupDayRepositoryMock,
                                                                          movementDataPersonGroupMonthRepositoryMock, new SimpleMeterRegistry(),
                                                                          clientSessionOptionsMock, additionalParametersMapper,
                                                                          accountDbKeyFieldsMapper, accountValueMapper, accountSumDayMapper);
        reset(clientSessionMock);
        reset(mongoClientMock);
        reset(stateDocRepositoryMock);
    }

    @Test
    @DisplayName("process a delta request with empty state doc collection")
    void should_return_nothing_when_state_doc_is_missing() {
        // prepare
        when(stateDocRepositoryMock.findOneByBusinessKey(anyInt(), anyInt(), anyInt())).thenReturn(Mono.empty());
        // execute
        // request.baseVersion = 1 and request.deltaVersion = 1
        DeltaInfo deltaInfo =
                deltaEventProcessingService.processDeltaEvent(TEST_CONSULTANT, TEST_CLIENT, TEST_FISCAL_YEAR_2021_START, TEST_BASE_VERSION_UPDATE,
                                                              TEST_DELTA_VERSION_UPDATE, deltaRequest).block();
        // assert
        assertThat(deltaInfo).isNull();
    }

    @Test
    @DisplayName("process a delta request with state doc bad")
    void should_return_rest_warn_error_when_state_doc_is_in_bad_state() {
        // prepare
        StateDoc stateDoc = createStateDoc(StateDocState.BAD, TEST_FISCAL_YEAR_2021_START, TEST_FISCAL_YEAR_2021_END, schemaVersion);
        when(stateDocRepositoryMock.findOneByBusinessKey(anyInt(), anyInt(), anyInt())).thenReturn(Mono.just(stateDoc));
        // execute
        // request.baseVersion = 1 and request.deltaVersion = 1
        Throwable throwable = catchThrowable(
                () -> deltaEventProcessingService.processDeltaEvent(TEST_CONSULTANT, TEST_CLIENT, TEST_FISCAL_YEAR_2021_START,
                                                                    TEST_BASE_VERSION_UPDATE, TEST_DELTA_VERSION_UPDATE, deltaRequest).block());
        // assert
        assertThat(throwable).isNotNull().isInstanceOf(RestWarnException.class);
        RestWarnException restWarnException = (RestWarnException) throwable;
        assertThat(restWarnException.getMessage()).isEqualTo(String.format(ProcessingErrorMessageConstants.STATE_DOC_STATE_ERROR, stateDoc.getState()));
        assertThat(restWarnException.getHttpStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(restWarnException.getType()).isEqualTo(ProcessingServiceConstants.STATE_DOC_STATE_ERROR_TYPE);
    }

    @Test
    @DisplayName("process a delta request with state doc invalid data")
    void should_return_rest_warn_error_when_state_doc_is_in_invalid_data_state() {
        // prepare
        StateDoc stateDoc = createStateDoc(StateDocState.INVALID_DATA, TEST_FISCAL_YEAR_2021_START, TEST_FISCAL_YEAR_2021_END, schemaVersion);
        when(stateDocRepositoryMock.findOneByBusinessKey(anyInt(), anyInt(), anyInt())).thenReturn(Mono.just(stateDoc));
        // execute
        // request.baseVersion = 1 and request.deltaVersion = 1
        Throwable throwable = catchThrowable(
                () -> deltaEventProcessingService.processDeltaEvent(TEST_CONSULTANT, TEST_CLIENT, TEST_FISCAL_YEAR_2021_START,
                                                                    TEST_BASE_VERSION_UPDATE, TEST_DELTA_VERSION_UPDATE, deltaRequest).block());
        // assert
        assertThat(throwable).isNotNull().isInstanceOf(RestWarnException.class);
        RestWarnException restWarnException = (RestWarnException) throwable;
        assertThat(restWarnException.getMessage()).isEqualTo(String.format(ProcessingErrorMessageConstants.STATE_DOC_STATE_ERROR, stateDoc.getState()));
        assertThat(restWarnException.getHttpStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(restWarnException.getType()).isEqualTo(ProcessingServiceConstants.STATE_DOC_STATE_ERROR_TYPE);
    }

    @ParameterizedTest
    @CsvSource({"0, 0", "1, 1"})
    @DisplayName("process a delta request with state doc done and base/delta version error use cases")
    void should_return_rest_warn_error_when_state_doc_is_in_done_state_and_base_delta_version_do_not_match(Long stateDocBaseVersion,
                                                                                                           Long stateDocDeltaVersion) {
        // prepare
        StateDoc stateDoc =
                createStateDoc(StateDocState.DONE, TEST_FISCAL_YEAR_2021_START, TEST_FISCAL_YEAR_2021_END, schemaVersion, stateDocBaseVersion,
                               stateDocDeltaVersion);
        when(stateDocRepositoryMock.findOneByBusinessKey(anyInt(), anyInt(), anyInt())).thenReturn(Mono.just(stateDoc));
        // execute
        // request.baseVersion = 1 and request.deltaVersion = 3
        Throwable throwable = catchThrowable(
                () -> deltaEventProcessingService.processDeltaEvent(TEST_CONSULTANT, TEST_CLIENT, TEST_FISCAL_YEAR_2021_START,
                                                                    TEST_BASE_VERSION_UPDATE, TEST_DELTA_VERSION_3, deltaRequest).block());
        // assert
        assertThat(throwable).isNotNull().isInstanceOf(RestWarnException.class);
        RestWarnException restWarnException = (RestWarnException) throwable;
        String errorMessage =
                String.format(ProcessingErrorMessageConstants.BASE_OR_DELTA_VERSION_GAP_ERROR, stateDoc.getBaseVersion(), stateDoc.getDeltaVersion());
        assertThat(restWarnException.getMessage()).isEqualTo(errorMessage);
        assertThat(restWarnException.getHttpStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(restWarnException.getType()).isEqualTo(ProcessingServiceConstants.STATE_DOC_BASE_OR_DELTA_VERSION_GAP_TYPE);
        List<ILoggingEvent> searchResult = memoryAppender.search(errorMessage, Level.WARN);
        assertThat(searchResult).hasSize(1);
    }

    @ParameterizedTest
    @CsvSource({"2, 0", "1, 3", "1, 4"})
    @DisplayName("process a delta request with state doc done and base/delta version already up to date use cases")
    void should_return_delta_info_when_state_doc_is_in_done_state_and_base_delta_version_do_not_match(Long stateDocBaseVersion,
                                                                                                      Long stateDocDeltaVersion) {
        // prepare
        StateDoc stateDoc =
                createStateDoc(StateDocState.DONE, TEST_FISCAL_YEAR_2021_START, TEST_FISCAL_YEAR_2021_END, schemaVersion, stateDocBaseVersion,
                               stateDocDeltaVersion);
        when(stateDocRepositoryMock.findOneByBusinessKey(anyInt(), anyInt(), anyInt())).thenReturn(Mono.just(stateDoc));
        // execute
        // request.baseVersion = 1 and request.deltaVersion = 3
        DeltaInfo deltaInfo =
                deltaEventProcessingService.processDeltaEvent(TEST_CONSULTANT, TEST_CLIENT, TEST_FISCAL_YEAR_2021_START, TEST_BASE_VERSION_UPDATE,
                                                              TEST_DELTA_VERSION_3, deltaRequest).block();
        // assert
        assertThat(deltaInfo).isNotNull();
        assertThat(deltaInfo.getBaseVersion()).isEqualTo(stateDoc.getBaseVersion());
        assertThat(deltaInfo.getDeltaVersion()).isEqualTo(stateDoc.getDeltaVersion());
        // replaces the first {} brackets with the first stream element (baseVersion) and the second with the second element (deltaVersion)
        String warnMessage = Stream.of(stateDocBaseVersion.toString(), stateDocDeltaVersion.toString())
                                   .reduce(ProcessingErrorMessageConstants.BASE_OR_DELTA_VERSION_UP_TO_DATE,
                                           (inputString, streamElement) -> inputString.replaceFirst(Pattern.quote("{}"), streamElement));
        List<ILoggingEvent> searchResult = memoryAppender.search(warnMessage, Level.WARN);
        assertThat(searchResult).hasSize(1);
    }

    @Test
    @DisplayName("process a delta request with state doc init")
    void should_return_rest_warn_error_when_state_doc_is_in_init_state() {
        // prepare
        StateDoc stateDoc = createStateDoc(StateDocState.INIT, TEST_FISCAL_YEAR_2021_START, TEST_FISCAL_YEAR_2021_END, schemaVersion);
        when(stateDocRepositoryMock.findOneByBusinessKey(anyInt(), anyInt(), anyInt())).thenReturn(Mono.just(stateDoc));
        // execute
        // request.baseVersion = 1 and request.deltaVersion = 1
        Throwable throwable = catchThrowable(
                () -> deltaEventProcessingService.processDeltaEvent(TEST_CONSULTANT, TEST_CLIENT, TEST_FISCAL_YEAR_2021_START,
                                                                    TEST_BASE_VERSION_UPDATE, TEST_DELTA_VERSION_UPDATE, deltaRequest).block());
        // assert
        assertThat(throwable).isNotNull().isInstanceOf(InitialLoadFailedException.class);
        InitialLoadFailedException initialLoadFailedException = (InitialLoadFailedException) throwable;
        String[] split = ProcessingErrorMessageConstants.ANOTHER_IMPORT_IN_PROGRESS.split("%s");
        assertThat(split).hasSize(2);
        assertThat(initialLoadFailedException.getMessage()).contains(split[0]);
        assertThat(initialLoadFailedException.getMessage()).contains(split[1]);
        assertThat(initialLoadFailedException.getHttpStatusCode()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(initialLoadFailedException.getType()).isEqualTo(ProcessingServiceConstants.IMPORT_IN_PROGRESS_TYPE);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "json/requests/delta/delta-request-with-missing-account-sum-day-deltas.json",
            "json/requests/delta/delta-request-with-empty-account-sum-day-deltas.json"})
    @DisplayName("process a delta request with state doc done and account sum days delta are null and empty")
    void should_update_base_and_delta_version_when_account_sum_days_delta_is_null(String inputFileLocation) {
        // prepare
        clearInvocations(mongoClientMock);
        clearInvocations(stateDocRepositoryMock);
        memoryAppender.reset();
        StateDoc stateDoc = createStateDoc(StateDocState.DONE, TEST_FISCAL_YEAR_2021_START, TEST_FISCAL_YEAR_2021_END, schemaVersion, 1L, 0L);
        when(stateDocRepositoryMock.findOneByBusinessKey(anyInt(), anyInt(), anyInt())).thenReturn(Mono.just(stateDoc));
        when(mongoClientMock.startSession(any(ClientSessionOptions.class))).thenReturn(Mono.just(clientSessionMock));
        var updateInfoMock = mock(UpdateResult.class);
        when(stateDocRepositoryMock.updateVersionInfo(anyInt(), anyInt(), anyInt(), any(ClientSession.class), any(), anyLong(),anyLong()))
                .thenReturn(Mono.just(updateInfoMock));
        deltaRequest = Mono.just(TestDataLoader.load(inputFileLocation, DeltaRequest.class));
        // execute
        // request.baseVersion = 1 and request.deltaVersion = 1
        DeltaInfo deltaInfo =
                deltaEventProcessingService.processDeltaEvent(TEST_CONSULTANT, TEST_CLIENT, TEST_FISCAL_YEAR_2021_START, TEST_BASE_VERSION_UPDATE,
                                                              TEST_DELTA_VERSION_UPDATE, deltaRequest).block();
        // assert
        assertThat(deltaInfo).isNotNull();
        assertThat(deltaInfo.getBaseVersion()).isEqualTo(TEST_BASE_VERSION_UPDATE);
        assertThat(deltaInfo.getDeltaVersion()).isEqualTo(TEST_DELTA_VERSION_UPDATE);
        verify(mongoClientMock, times(1)).startSession(any(ClientSessionOptions.class));
        verify(stateDocRepositoryMock, times(1)).updateVersionInfo(anyInt(), anyInt(), anyInt(), any(), any(), anyLong(), anyLong());
        List<ILoggingEvent> searchResult = memoryAppender.search(ProcessingErrorMessageConstants.DELTA_REQUEST_HAS_NO_ACCOUNT_SUM_DAY_DELTAS, Level.WARN);
        assertThat(searchResult).hasSize(1);
    }

    @Test
    @DisplayName("should call processDeltaEventInTransaction if stateDoc done and versions match")
    void should_update_movement_data_collections_when_state_doc_is_in_done_state_and_versions_match() {
        // prepare
        clearInvocations(mongoClientMock);
        clearInvocations(stateDocRepositoryMock);
        StateDoc stateDoc = createStateDoc(StateDocState.DONE, TEST_FISCAL_YEAR_2021_START, TEST_FISCAL_YEAR_2021_END, schemaVersion, 1L, 0L);
        when(stateDocRepositoryMock.findOneByBusinessKey(anyInt(), anyInt(), anyInt())).thenReturn(Mono.just(stateDoc));
        when(clientSessionMock.commitTransaction()).thenReturn(Mono.empty());
        when(mongoClientMock.startSession(any(ClientSessionOptions.class))).thenReturn(Mono.just(clientSessionMock));
        var bulkWriteResult = mock(BulkWriteResult.class);
        when(movementDataMonthRepositoryMock.bulkUpsert(anyInt(),anyInt(),anyInt(),anyMap(), any(ClientSession.class)))
                .thenReturn(Mono.just(new ArrayList<>()));
        when(movementDataDayRepositoryMock.bulkUpsert(anyInt(),anyInt(),anyInt(),anyMap(), any(ClientSession.class)))
                .thenReturn(Mono.just(bulkWriteResult));
        when(movementDataPersonGroupDayRepositoryMock.bulkUpdate(anyInt(),anyInt(),anyInt(),anyMap(), any(ClientSession.class)))
                .thenReturn(Mono.just(bulkWriteResult));
        when(movementDataPersonGroupMonthRepositoryMock.bulkUpdate(anyInt(),anyInt(),anyInt(),anyMap(), any(ClientSession.class)))
                .thenReturn(Mono.just(bulkWriteResult));
        when(movementDataDayRepositoryMock.findAllMovementDataDayForAccount(any(), any(), any(), any())).thenReturn(Mono.just(new ArrayList<>()));
        var updateInfoMock = mock(UpdateResult.class);
        when(masterDataRepositoryMock.addIndividualPersonAccounts(any(), anyInt(), anyInt(), anyInt(), anySet()))
                .thenReturn(Mono.just(updateInfoMock));
        when(masterDataRepositoryMock.writeNearTimeDataFlag(anyInt(), anyInt(), anyInt(), any(ClientSession.class), any(), anyBoolean()))
                .thenReturn(Mono.just(updateInfoMock));
        when(stateDocRepositoryMock.updateVersionInfo(anyInt(), anyInt(), anyInt(), any(ClientSession.class), any(), anyLong(),anyLong()))
                .thenReturn(Mono.just(updateInfoMock));
        // execute
        // request.baseVersion = 1 and request.deltaVersion = 3
        deltaEventProcessingService.processDeltaEvent(TEST_CONSULTANT, TEST_CLIENT, TEST_FISCAL_YEAR_2021_START, TEST_BASE_VERSION_UPDATE,
                                                      TEST_DELTA_VERSION_UPDATE, deltaRequest).block();
        // assert4
        verify(mongoClientMock, times(1)).startSession(any(ClientSessionOptions.class));
        verify(movementDataDayRepositoryMock, times(1)).bulkUpsert(anyInt(), anyInt(), anyInt(), anyMap(), any());
        verify(movementDataDayRepositoryMock, times(1)).bulkUpsert(anyInt(), anyInt(), anyInt(), anyMap(), any());
        verify(movementDataDayRepositoryMock, times(1)).bulkUpsert(anyInt(), anyInt(), anyInt(), anyMap(), any());
        verify(movementDataDayRepositoryMock, times(1)).bulkUpsert(anyInt(), anyInt(), anyInt(), anyMap(), any());
        verify(masterDataRepositoryMock, times(1)).addIndividualPersonAccounts(any(), anyInt(), anyInt(), anyInt(), any());
        verify(masterDataRepositoryMock, times(1)).writeNearTimeDataFlag(anyInt(), anyInt(), anyInt(), any(), any(), anyBoolean());
        verify(stateDocRepositoryMock, times(1)).updateVersionInfo(anyInt(), anyInt(), anyInt(), any(), any(), anyLong(), anyLong());

        assertThat(memoryAppender.search(DELTA_EVENT_BATCH_START_LOG, Level.DEBUG)).hasSize(1);
        assertThat(importServiceMemoryAppender.search(DELTA_EVENT_BATCH_SUCCESS_LOG.replace("{}ms", ""), Level.DEBUG)).hasSize(1);
    }
}
