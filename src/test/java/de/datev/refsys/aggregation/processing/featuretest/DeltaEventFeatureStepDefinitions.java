package de.datev.refsys.aggregation.processing.featuretest;

import com.mongodb.client.model.Updates;
import com.mongodb.reactivestreams.client.MongoCollection;
import de.datev.refsys.aggregation.document.model.AccountGroupValue;
import de.datev.refsys.aggregation.document.model.AccountValue;
import de.datev.refsys.aggregation.document.model.MasterData;
import de.datev.refsys.aggregation.document.model.MasterDataContext;
import de.datev.refsys.aggregation.document.model.MovementDataDay;
import de.datev.refsys.aggregation.document.model.MovementDataMonth;
import de.datev.refsys.aggregation.document.model.MovementDataPersonGroupDay;
import de.datev.refsys.aggregation.document.model.MovementDataPersonGroupMonth;
import de.datev.refsys.aggregation.document.model.StateDoc;
import de.datev.refsys.aggregation.document.model.constants.FieldConstants;
import de.datev.refsys.aggregation.document.model.enums.StateDocState;
import de.datev.refsys.aggregation.processing.api.model.AccountSumDayDelta;
import de.datev.refsys.aggregation.processing.api.model.DeltaInfo;
import de.datev.refsys.aggregation.processing.api.model.DeltaRequest;
import de.datev.refsys.aggregation.processing.constant.ProcessingErrorMessageConstants;
import de.datev.refsys.aggregation.processing.constant.ProcessingServiceConstants;
import de.datev.refsys.aggregation.processing.exception.RestWarnException;
import de.datev.refsys.aggregation.processing.featuretest.model.BookingKmvz;
import de.datev.refsys.aggregation.processing.featuretest.model.DayBooking;
import de.datev.refsys.aggregation.processing.featuretest.model.DeltaRequestContent;
import de.datev.refsys.aggregation.processing.featuretest.model.FullGroupDayBooking;
import de.datev.refsys.aggregation.processing.featuretest.model.FullGroupMonthBooking;
import de.datev.refsys.aggregation.processing.featuretest.model.GroupDayBooking;
import de.datev.refsys.aggregation.processing.featuretest.model.GroupMonthBooking;
import de.datev.refsys.aggregation.processing.featuretest.model.MonthBooking;
import de.datev.refsys.aggregation.processing.model.AccountDbKeyFields;
import de.datev.refsys.aggregation.processing.repository.MasterDataRepository;
import de.datev.refsys.aggregation.processing.repository.MovementDataDayRepository;
import de.datev.refsys.aggregation.processing.repository.MovementDataMonthRepository;
import de.datev.refsys.aggregation.processing.repository.MovementDataPersonGroupDayRepository;
import de.datev.refsys.aggregation.processing.repository.MovementDataPersonGroupMonthRepository;
import de.datev.refsys.aggregation.processing.repository.StateDocRepository;
import de.datev.refsys.aggregation.processing.service.DeltaEventProcessingService;
import de.datev.refsys.aggregation.processing.service.ImportService;
import de.datev.refsys.aggregation.processing.util.DateFunctions;
import de.datev.refsys.aggregation.processing.util.QueryUtil;
import de.datev.refsys.aggregation.processing.util.TestDataLoader;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.After;
import io.cucumber.java.de.Angenommen;
import io.cucumber.java.de.Dann;
import io.cucumber.java.de.Wenn;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Condition;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static de.datev.refsys.aggregation.processing.util.QueryUtil.getByMasterDataBusinessKey;
import static de.datev.refsys.aggregation.processing.util.QueryUtil.getOneMovementDataDocument;
import static de.datev.refsys.aggregation.processing.util.QueryUtil.getOneMovementDataPersonGroupDocument;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.assertj.core.api.BDDAssertions.then;

@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") // Autowiring via Cucumber Context not recognized by IntelliJ
@Slf4j
public class DeltaEventFeatureStepDefinitions {
    public static final int DEFAULT_RECORD_TYPE = 1;

    @Autowired
    private DeltaEventFeatureContext deltaEventFeatureContext;

    @Autowired
    private DeltaEventProcessingService deltaEventProcessingService;

    @Autowired
    private StateDocRepository stateDocRepository;

    @Autowired
    private MovementDataDayRepository movementDataDayRepository;

    @Autowired
    private MovementDataMonthRepository movementDataMonthRepository;

    @Autowired
    private MongoCollection<MovementDataDay> movementDataDayCollection;

    @Autowired
    private MongoCollection<MovementDataMonth> movementDataMonthCollection;

    @Autowired
    private MongoCollection<MasterData> masterDataCollection;

    @Autowired
    private MovementDataPersonGroupDayRepository movementDataPersonGroupDayRepository;

    @Autowired
    private MovementDataPersonGroupMonthRepository movementDataPersonGroupMonthRepository;

    @Autowired
    private MongoCollection<MovementDataPersonGroupDay> movementDataPersonGroupDayCollection;

    @Autowired
    private MongoCollection<MovementDataPersonGroupMonth> movementDataPersonGroupMonthCollection;

    @Autowired
    private MongoCollection<StateDoc> stateDocCollection;

    @Autowired
    private MasterDataRepository masterDataRepository;

    @Autowired
    private ImportService importService;

    @Value("${ref-sys.update-schema.schema-version}")
    private int schemaVersion;

    @Angenommen("Berater {int} und Mandant {int}")
    public void beraterMandantUndWirtschaftsjahr(int consultant, int client) {
        this.deltaEventFeatureContext.setConsultant(consultant);
        this.deltaEventFeatureContext.setClient(client);
    }

    @Angenommen("das Wirtschaftsjahr beginnt am {int} und endet am {int}.")
    public void dasWirtschaftsjahrBeginntAmUndEndetAm(int fiscalYearBegin, int fiscalYearEnd) {
        this.deltaEventFeatureContext.setFiscalYearBegin(fiscalYearBegin);
        this.deltaEventFeatureContext.setFiscalYearEnd(fiscalYearEnd);
    }

    @Angenommen("es existiert ein StateDoc")
    @Angenommen("die Initialisierung wurde erfolgreich abgeschlossen")
    public void dasStateDocStehtImStatusDoneFurDieBaseVersionUndDieDeltaVersion() {
        this.deltaEventFeatureContext.setBaseVersion(1L);
        this.deltaEventFeatureContext.setDeltaVersion(0L);
        StateDoc stateDoc = StateDoc.builder()
                                    .consultant(this.deltaEventFeatureContext.getConsultant())
                                    .client(this.deltaEventFeatureContext.getClient())
                                    .yearBegin(this.deltaEventFeatureContext.getFiscalYearBegin())
                                    .yearEnd(this.deltaEventFeatureContext.getFiscalYearEnd())
                                    .baseVersion(this.deltaEventFeatureContext.getBaseVersion())
                                    .deltaVersion(this.deltaEventFeatureContext.getDeltaVersion())
                                    .state(StateDocState.DONE)
                                    .stateTimestamp(OffsetDateTime.now())
                                    .createdTimestamp(OffsetDateTime.now())
                                    .schemaVersion(schemaVersion)
                                    .build();
        this.stateDocRepository.insertOne(stateDoc).block();

        StateDoc insertedStateDoc =
                this.stateDocRepository.findOneByBusinessKey(this.deltaEventFeatureContext.getConsultant(),
                                                             this.deltaEventFeatureContext.getClient(),
                                                             this.deltaEventFeatureContext.getFiscalYearBegin()).block();
        Condition<StateDoc> stateDocIsDone = new Condition<>(sd -> sd.getState() == StateDocState.DONE, "statedoc done");
        assertThat(insertedStateDoc).isNotNull().has(stateDocIsDone);
        deltaEventFeatureContext.setStateDocBaseVersion(insertedStateDoc.getBaseVersion());
        deltaEventFeatureContext.setStateDocDeltaVersion(insertedStateDoc.getDeltaVersion());

    }

    @Angenommen("die Datenbasis enthält Masterdaten für den Berater, Mandant und Wirtschaftsjahr ohne KMVZ")
    public void dieDatenbasisEnthaltMasterdatenFurDenBeraterMandantUndWirtschaftsjahrOhneKMVZ() {

        MasterDataContext masterDataContext = MasterDataContext.builder()
                                                               .consultant(this.deltaEventFeatureContext.getConsultant())
                                                               .client(this.deltaEventFeatureContext.getClient())
                                                               .yearBegin(this.deltaEventFeatureContext.getFiscalYearBegin())
                                                               .yearEnd(this.deltaEventFeatureContext.getFiscalYearEnd())
                                                               .containsNearTimeData(false)
                                                               .build();
        masterDataRepository.upsertOne(masterDataContext, null, null, null, null, null, null, null, null)
                            .block();
    }

    @Angenommen("die Base Version im State Doc ist {int} und die Delta Version ist {int}")
    public void dieBaseVersionImStateDocIst(long baseVersion, long deltaVersion) {
        // TODO: Query by existing state Doc
        Mono.from(stateDocCollection.updateOne(
                    QueryUtil.getByMasterDataBusinessKey(this.deltaEventFeatureContext.getConsultant(), this.deltaEventFeatureContext.getClient(),
                                                         this.deltaEventFeatureContext.getFiscalYearBegin()),
                    Updates.combine(Updates.set(FieldConstants.BASE_VERSION, baseVersion), Updates.set(FieldConstants.DELTA_VERSION, deltaVersion))))
            .block();

        StateDoc insertedStateDoc =
                this.stateDocRepository.findOneByBusinessKey(this.deltaEventFeatureContext.getConsultant(),
                                                             this.deltaEventFeatureContext.getClient(),
                                                             this.deltaEventFeatureContext.getFiscalYearBegin()).block();
        Condition<StateDoc> stateDocHasCorrectVersion =
                new Condition<>(sd -> sd.getBaseVersion() == baseVersion && sd.getDeltaVersion() == deltaVersion, "statedoc version set");
        assertThat(insertedStateDoc).isNotNull().has(stateDocHasCorrectVersion);
        deltaEventFeatureContext.setStateDocBaseVersion(baseVersion);
        deltaEventFeatureContext.setStateDocDeltaVersion(deltaVersion);
    }

    @Angenommen("^das StateDoc ist im Zustand '([^\\']*)'$")
    public void dasStateDocIstImZustandINIT(StateDocState state) {
        Mono.from(stateDocCollection.updateOne(
                QueryUtil.getByMasterDataBusinessKey(this.deltaEventFeatureContext.getConsultant(), this.deltaEventFeatureContext.getClient(),
                                                     this.deltaEventFeatureContext.getFiscalYearBegin()),
                Updates.combine(
                        Updates.set(FieldConstants.STATE, state)
                               ))).block();
    }

    @Angenommen("es gibt folgende Buchungen für das Sachkonto {int}")
    @Angenommen("es gibt folgende Buchungen für das Konto {int}")
    @Angenommen("es gibt folgende Buchungen für das Personenkonto {int}")
    public void esGibtFolgendeBuchungenFuerDasKonto(int accountNumber, List<DayBooking> bookings) {
        Map<AccountDbKeyFields, MovementDataDay> dayDocuments = new HashMap<>();
        Map<AccountDbKeyFields, MovementDataMonth> monthDocuments = new HashMap<>();
        //TODO: die keyfields werden nur gebraucht, da dies an der Schnittstelle des Repos aktuell so designed ist -> Werte sind doppelt
        AccountDbKeyFields dbKey = AccountDbKeyFields.builder()
                                                     .accountNumber(accountNumber)
                                                     .accountingReasonId(0) //TODO: define default constant
                                                     .recordType(DEFAULT_RECORD_TYPE)
                                                     .build();
        MovementDataDay dayDocument = MovementDataDay.builder()
                                                     .consultant(this.deltaEventFeatureContext.getConsultant())
                                                     .client(this.deltaEventFeatureContext.getClient())
                                                     .accountNumber(accountNumber)
                                                     .fiscalYear(this.deltaEventFeatureContext.getFiscalYearBegin())
                                                     .accountingReasonId(0)
                                                     .values(new HashMap<>())
                                                     .build();
        MovementDataMonth monthDocument = MovementDataMonth.builder()
                                                           .consultant(this.deltaEventFeatureContext.getConsultant())
                                                           .client(this.deltaEventFeatureContext.getClient())
                                                           .accountNumber(accountNumber)
                                                           .accountingReasonId(0)
                                                           .fiscalYear(this.deltaEventFeatureContext.getFiscalYearBegin())
                                                           .values(new HashMap<>())
                                                           .build();
        dayDocuments.put(dbKey, dayDocument);
        monthDocuments.put(dbKey, monthDocument);
        for (DayBooking booking : bookings) {
            //TODO: Database Know how should be inside the Repository
            String dayKey = "d" + booking.day();
            long amountCredit = Utility.convertAmountToLongIncludingTwoFractionalDigits(booking.amountCredit());
            long amountDebit = Utility.convertAmountToLongIncludingTwoFractionalDigits(booking.amountDebit());
            AccountValue dayValue = AccountValue.builder()
                                                //TODO: clarify - do we need to know the difference beetween zero and null here?
                                                .amountCredit(amountCredit)
                                                .amountDebit(amountDebit)
                                                .build();
            dayDocument.getValues().put(dayKey, dayValue);
            String monthKey = "m" + Utility.getMonthFromDate(booking.day());
            if (monthDocument.getValues().containsKey(monthKey)) {
                AccountValue monthValue = monthDocument.getValues().get(monthKey);
                monthValue.addAmountCredit(amountCredit);
                monthValue.addAmountDebit(amountDebit);
            } else {
                AccountValue monthValue = AccountValue.builder()
                                                      .amountCredit(amountCredit)
                                                      .amountDebit(amountDebit)
                                                      .build();
                monthDocument.getValues().put(monthKey, monthValue);
            }
        }
        this.movementDataDayRepository.bulkInsert(dayDocuments).block();
        this.deltaEventFeatureContext.addExistingDayValues(dayDocuments.values().stream().toList());
        this.movementDataMonthRepository.bulkInsert(monthDocuments).block();
        this.deltaEventFeatureContext.addExistingMonthValues(monthDocuments.values().stream().toList());
    }

    @Angenommen("es gibt folgende übliche aggregierte Tageswerte für die Personengruppe {int}")
    public void esGibtFolgendeUeblicheAggregierteTagesWerteFuerDiePersonengruppe(int personGroup, List<GroupDayBooking> bookings) {
        Map<String, AccountGroupValue> values = new HashMap<>();
        for (GroupDayBooking booking : bookings) {
            values.put("d" + booking.day(),
                       AccountGroupValue.builder()
                                        .amountDebitUsual(
                                                Utility.convertAmountToLongIncludingTwoFractionalDigits(
                                                        booking.amountDebit()))
                                        .amountCreditUsual(Utility.convertAmountToLongIncludingTwoFractionalDigits(
                                                booking.amountCredit()))
                                        .build());
        }
        MovementDataPersonGroupDay personGroupDayDocument = MovementDataPersonGroupDay.builder()
                                                                                      .consultant(
                                                                                              this.deltaEventFeatureContext.getConsultant())
                                                                                      .client(this.deltaEventFeatureContext.getClient())
                                                                                      .fiscalYear(
                                                                                              this.deltaEventFeatureContext.getFiscalYearBegin())
                                                                                      .accountingReasonId(0)
                                                                                      .accountGroupNumber(personGroup)
                                                                                      .values(values)
                                                                                      .build();
        Mono.from(this.movementDataPersonGroupDayCollection.insertOne(personGroupDayDocument)).block();
        this.deltaEventFeatureContext.addExistingPersonGroupDayValues(List.of(personGroupDayDocument));
    }

    @Angenommen("es gibt folgende übliche aggregierte Monatswerte für die Personengruppe {int}")
    public void esGibtFolgendeUeblicheAggregierteMonatsWerteFuerDiePersonengruppe(int personGroup, List<GroupMonthBooking> bookings) {
        Map<String, AccountGroupValue> values = new HashMap<>();
        for (GroupMonthBooking booking : bookings) {
            values.put("m" + booking.month(),
                       AccountGroupValue.builder()
                                        .amountDebitUsual(
                                                Utility.convertAmountToLongIncludingTwoFractionalDigits(
                                                        booking.amountDebit()))
                                        .amountCreditUsual(Utility.convertAmountToLongIncludingTwoFractionalDigits(
                                                booking.amountCredit()))
                                        .build());
        }
        MovementDataPersonGroupMonth personGroupMonthDocument = MovementDataPersonGroupMonth.builder()
                                                                                            .consultant(
                                                                                                    this.deltaEventFeatureContext.getConsultant())
                                                                                            .client(this.deltaEventFeatureContext.getClient())
                                                                                            .fiscalYear(
                                                                                                    this.deltaEventFeatureContext.getFiscalYearBegin())
                                                                                            .accountingReasonId(0)
                                                                                            .accountGroupNumber(personGroup)
                                                                                            .values(values)
                                                                                            .build();
        Mono.from(this.movementDataPersonGroupMonthCollection.insertOne(personGroupMonthDocument)).block();
        this.deltaEventFeatureContext.addExistingPersonGroupMonthValues(List.of(personGroupMonthDocument));
    }

    @Angenommen("es gibt ein Delta Event mit Monatsverkehrszahlen für das Sachkonto {int}")
    @Angenommen("es gibt ein Delta Event mit Monatsverkehrszahlen für das Personenkonto {int}")
    @Angenommen("es gibt ein Delta Event mit Monatsverkehrszahlen und üblichen Buchungen für das Personenkonto {int}")
    @Angenommen("es gibt ein Delta Event mit Monatsverkehrszahlen und einer Generalumkehr für das Personenkonto {int}")
    public void esGibtEinEventMitEinerGeneralumkehrFuerDasKonto(int accountNumber, List<BookingKmvz> kmvzInEvent) {
        setData(accountNumber, kmvzInEvent, 1, 1);
    }

    @Angenommen("das Personenkonto {int} ist in der Liste der gesondert zu betrachtenden Personenkonten aufgenommen")
    public void dasPersonenkontoIstInDerListeDerGesondertBetrachtenPersonenkontenAufgenommen(int personGroupAccount) {
        Mono.from(masterDataCollection.updateOne(
                QueryUtil.getByMasterDataBusinessKey(this.deltaEventFeatureContext.getConsultant(), this.deltaEventFeatureContext.getClient(),
                                                     this.deltaEventFeatureContext.getFiscalYearBegin()),
                Updates.addEachToSet(FieldConstants.INDIVIDUAL_PERSON_ACCOUNT_NUMBERS,
                                     List.of(personGroupAccount)))).block();
    }

    @Angenommen("es gibt ein Delta Event für das Personenkonto {int} mit einer Base Version {int}")
    @Angenommen("es gibt ein Delta Event für das Sachkonto {int} mit einer Base Version {int}")
    public void esGibtEinDeltaEventFurDasSachkontoMitEinerBaseVersion(int account, long baseVersion)  {
        setData(account, Collections.singletonList(new BookingKmvz(8, 100, 0)), baseVersion, 1L);
    }

    @Angenommen("es gibt ein Delta Event für das Personenkonto {int} mit einer Base Version {int} und einer Delta Version {int}")
    @Angenommen("es gibt ein Delta Event für das Sachkonto {int} mit einer Base Version {int} und einer Delta Version {int}")
    public void esGibtEinDeltaEventFurDasKontoMitEinerBaseVersionUndEinerDeltaVersion(int account, long baseVersion, int deltaVersion) {
        setData(account, Collections.singletonList(new BookingKmvz(8, 100, 0)), baseVersion, deltaVersion);
    }

    @Angenommen("es gibt die folgende Events")
    public void esGibtDieFolgendenEvents(DataTable dataTable) {
        Map<String, String> firstRow = dataTable.asMaps().get(0);
        deltaEventFeatureContext.setConsultant(Integer.parseInt(firstRow.get("Berater")));
        deltaEventFeatureContext.setClient(Integer.parseInt(firstRow.get("Mandant")));
        deltaEventFeatureContext.setFiscalYearBegin(Integer.parseInt(firstRow.get("WJ")));
        deltaEventFeatureContext.setBaseVersion(Long.parseLong(firstRow.get("Base-Version")));
        List<DeltaRequestContent> deltaRequestContents =
                dataTable.asMaps()
                         .stream()
                         .map(row -> new DeltaRequestContent(Long.parseLong(row.get("Delta-Version")), row.get("Delta-Request")))
                         .toList();
        deltaEventFeatureContext.setDeltaRequestContents(deltaRequestContents);
    }

    @Wenn("das Delta Event verarbeitet wird")
    public void dasDeltaEventVerarbeitetWird() {
        try {
            DeltaInfo deltaInfo = deltaEventProcessingService.processDeltaEvent(deltaEventFeatureContext.getConsultant(),
                                                                                deltaEventFeatureContext.getClient(),
                                                                                deltaEventFeatureContext.getFiscalYearBegin(),
                                                                                deltaEventFeatureContext.getBaseVersion(),
                                                                                deltaEventFeatureContext.getDeltaVersion(),
                                                                                Mono.just(deltaEventFeatureContext.getDeltaRequest()))
                                                             .block();
            assertThat(deltaInfo).isNotNull();
            deltaEventFeatureContext.setDeltaInfos(Collections.singletonList(deltaInfo));
        } catch (RestWarnException e) {
            deltaEventFeatureContext.setRestWarnException(e);
        } catch (Exception e) {
            log.error("An unexpected error occurred", e);
        }
    }

    @Wenn("die Delta Events verarbeitet werden")
    public void dieDeltaEventsVerarbeitetWerden() {
        List<DeltaInfo> deltaInfos = new ArrayList<>();
        deltaEventFeatureContext.getDeltaRequestContents().forEach(deltaRequestContent -> {
            DeltaRequest deltaRequest = TestDataLoader.load(deltaRequestContent.content(), DeltaRequest.class);
            try {
                DeltaInfo deltaInfo = deltaEventProcessingService.processDeltaEvent(deltaEventFeatureContext.getConsultant(),
                                                                                    deltaEventFeatureContext.getClient(),
                                                                                    deltaEventFeatureContext.getFiscalYearBegin(),
                                                                                    deltaEventFeatureContext.getBaseVersion(),
                                                                                    deltaRequestContent.deltaVersion(),
                                                                                    Mono.just(deltaRequest))
                                                                 .block();
                assertThat(deltaInfo).isNotNull();
                deltaInfos.add(deltaInfo);
            } catch (RestWarnException e) {
                deltaEventFeatureContext.setRestWarnException(e);
            } catch (Exception e) {
                log.error("An unexpected error occurred", e);
            }
        });
        deltaEventFeatureContext.setDeltaInfos(deltaInfos);
    }

    @Dann("sollte das Flag für KMVZ im Masterdatacontext auf true gesetzt sein")
    public void sollteDerDasFlagFurKMVZImMasterdatacontextFurDenBeraterUndDasWJAufTrueGesetztSein() {
        MasterData masterData = Mono.from(this.masterDataCollection.find(getByMasterDataBusinessKey(this.deltaEventFeatureContext.getConsultant(),
                                                                                                    this.deltaEventFeatureContext.getClient(),
                                                                                                    this.deltaEventFeatureContext.getFiscalYearBegin())))
                                    .block();
        assertThat(masterData).as("masterdata should be present").isNotNull();
        MasterDataContext masterDataContext = masterData.getContext();
        assertThat(masterDataContext.getContainsNearTimeData()).as("neartimedataFlag should be set").isTrue();
    }

    @Dann("sollte ein Fehler auftreten")
    public void sollteEinFehlerAuftreten() {
        RestWarnException restWarnException = deltaEventFeatureContext.getRestWarnException();
        String gapMessage =
                String.format(ProcessingErrorMessageConstants.BASE_OR_DELTA_VERSION_GAP_ERROR, deltaEventFeatureContext.getStateDocBaseVersion(),
                              deltaEventFeatureContext.getStateDocDeltaVersion());
        assertThat(restWarnException.getMessage()).isEqualTo(gapMessage);
        assertThat(restWarnException.getHttpStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(restWarnException.getType()).isEqualTo(ProcessingServiceConstants.STATE_DOC_BASE_OR_DELTA_VERSION_GAP_TYPE);
        assertThat(deltaEventFeatureContext.getDeltaInfos()).isNull();
    }

    @Dann("sollte im StateDoc die Base und Delta Version aktualisiert sein")
    public void sollteImStateDocFurDenBeraterUndDasWJBaseUndDeltaVersionAktualisiertSein() {
        StateDoc doc =
                this.stateDocRepository.findOneByBusinessKey(this.deltaEventFeatureContext.getConsultant(), this.deltaEventFeatureContext.getClient(),
                                                             this.deltaEventFeatureContext.getFiscalYearBegin()).block();
        assertThat(doc).isNotNull();
        Long baseVersion = doc.getBaseVersion();
        Long deltaVersion = doc.getDeltaVersion();
        assertThat(baseVersion).as("base Version shlould not be changed").isEqualTo(1L);
        assertThat(deltaVersion).as("delta Version should be higher").isEqualTo(1L);
    }

    @Dann("sollte das StateDoc mit Delta Version {int} erfolgreich aktualisiert sein")
    public void sollteDasStateDocMitDeltaVersionAktualisiertSein(long deltaVersion) {
        StateDoc doc =
                this.stateDocRepository.findOneByBusinessKey(this.deltaEventFeatureContext.getConsultant(),
                                                             this.deltaEventFeatureContext.getClient(),
                                                             this.deltaEventFeatureContext.getFiscalYearBegin()).block();
        assertThat(doc).isNotNull();
        assertThat(doc.getState()).isEqualTo(StateDocState.DONE);
        assertThat(doc.getDeltaVersion()).isEqualTo(deltaVersion);
    }

    @After
    public void cleanUp() {
        int consultant = this.deltaEventFeatureContext.getConsultant();
        int client = this.deltaEventFeatureContext.getClient();
        int fiscalYear = this.deltaEventFeatureContext.getFiscalYearBegin();

        Flux.concat(
                    this.movementDataDayRepository.deleteManyByBusinessKey(consultant, client, fiscalYear),
                    this.movementDataMonthRepository.deleteManyByBusinessKey(consultant, client, fiscalYear),
                    this.movementDataPersonGroupDayRepository.deleteManyByBusinessKey(consultant, client, fiscalYear),
                    this.movementDataPersonGroupMonthRepository.deleteManyByBusinessKey(consultant, client, fiscalYear),
                    this.stateDocRepository.deleteOne(consultant, client, fiscalYear),
                    this.masterDataRepository.deleteOne(consultant, client, fiscalYear))
            .collectList().block();
        Mockito.reset(importService);
    }

    @Dann("sollten die Werte für die Personengruppe {int} unverändert sein")
    public void solltenDieWerteFuerDiePersonengruppeUnveraendertSein(int personGroupNumber) {
        List<MovementDataPersonGroupDay> actualPersonGroupDayValues =
                Flux.from(this.movementDataPersonGroupDayCollection.find(
                            and(eq(FieldConstants.CONSULTANT, this.deltaEventFeatureContext.getConsultant()),
                                eq(FieldConstants.CLIENT, this.deltaEventFeatureContext.getClient()),
                                eq(FieldConstants.FISCAL_YEAR, this.deltaEventFeatureContext.getFiscalYearBegin()),
                                eq(FieldConstants.ACCOUNT_GROUP_NUMBER, personGroupNumber)))).collectList()
                    .block();
        List<MovementDataPersonGroupMonth> actualPersonGroupMonthValues =
                Flux.from(this.movementDataPersonGroupMonthCollection.find(
                            and(eq(FieldConstants.CONSULTANT, this.deltaEventFeatureContext.getConsultant()),
                                eq(FieldConstants.CLIENT, this.deltaEventFeatureContext.getClient()),
                                eq(FieldConstants.FISCAL_YEAR, this.deltaEventFeatureContext.getFiscalYearBegin()),
                                eq(FieldConstants.ACCOUNT_GROUP_NUMBER, personGroupNumber)))).collectList()
                    .block();

        List<MovementDataPersonGroupDay> expectedGroupDayValues =
                this.deltaEventFeatureContext.getExistingPersonGroupDayValues().stream().filter(x -> x.getAccountGroupNumber() == personGroupNumber)
                                             .toList();
        assertThat(actualPersonGroupDayValues).usingRecursiveFieldByFieldElementComparator()
                                              .as("die Tageswerte für die Personengruppe %d solten gleich sein", personGroupNumber).isEqualTo(
                                                      expectedGroupDayValues);
        List<MovementDataPersonGroupMonth> expectedGroupMonthValues = this.deltaEventFeatureContext.getExistingPersonGroupMonthValues().stream()
                                                                                                   .filter(x -> x.getAccountGroupNumber()
                                                                                                           == personGroupNumber).toList();
        assertThat(actualPersonGroupMonthValues).usingRecursiveFieldByFieldElementComparator()
                                                .as("die Monatswerte für die Personengruppe %d solten gleich sein", personGroupNumber).isEqualTo(
                                                        expectedGroupMonthValues);
    }

    @Dann("sollte das Konto {int} in der Liste der gesondert zu betrachtenden Personenkonten aufgenommen sein")
    public void sollteDasKontoInDerListeDerGesondertZuBetrachtendenPersonenkontenAufgenommenSein(int accountNumber) {
        MasterData masterdata =
                Mono.from(this.masterDataCollection.find(and(eq(FieldConstants.CONSULTANT, this.deltaEventFeatureContext.getConsultant()),
                                                             eq(FieldConstants.CLIENT, this.deltaEventFeatureContext.getClient()),
                                                             eq(FieldConstants.YEAR_BEGIN,
                                                                this.deltaEventFeatureContext.getFiscalYearBegin())
                                                            ))).block();
        assertThat(masterdata).isNotNull();
        assertThat(masterdata.getIndividualPersonAccountNumbers()).contains(accountNumber);
    }

    @Dann("warte bis das Event verarbeitet wurde")
    public void warteBisEventVerarbeitetWurde() {
        StateDoc doc =
                this.stateDocRepository.findOneByBusinessKey(this.deltaEventFeatureContext.getConsultant(),
                                                             this.deltaEventFeatureContext.getClient(),
                                                             this.deltaEventFeatureContext.getFiscalYearBegin()).block();
        assertThat(doc).isNotNull();
        //
        log.info("Waiting for event finish {} -> stateDoc {}", OffsetDateTime.now(), doc.getStateTimestamp());
        assertThat(doc.getStateTimestamp()).isCloseTo(OffsetDateTime.now(), within(1000, ChronoUnit.MILLIS));
        assertThat(deltaEventFeatureContext.getRestWarnException()).isNull();
        assertThat(deltaEventFeatureContext.getDeltaInfos()).hasSize(1);
        assertThat(deltaEventFeatureContext.getDeltaInfos().get(0).getBaseVersion()).isEqualTo(doc.getBaseVersion());
        assertThat(deltaEventFeatureContext.getDeltaInfos().get(0).getDeltaVersion()).isEqualTo(doc.getDeltaVersion());
    }

    @Dann("warte bis die Events verarbeitet wurden")
    public void warteBisDieEventsVerarbeitetWurden() {
        StateDoc doc =
                this.stateDocRepository.findOneByBusinessKey(this.deltaEventFeatureContext.getConsultant(),
                                                             this.deltaEventFeatureContext.getClient(),
                                                             this.deltaEventFeatureContext.getFiscalYearBegin()).block();
        assertThat(doc).isNotNull();
        //
        log.info("Waiting for events finish {} -> stateDoc {}", OffsetDateTime.now(), doc.getStateTimestamp());
        assertThat(doc.getStateTimestamp()).isCloseTo(OffsetDateTime.now(), within(600, ChronoUnit.MILLIS));
        assertThat(deltaEventFeatureContext.getDeltaInfos()).hasSameSizeAs(deltaEventFeatureContext.getDeltaRequestContents());
        IntStream.range(0, deltaEventFeatureContext.getDeltaInfos().size()).forEach(index -> {
            assertThat(deltaEventFeatureContext.getDeltaInfos().get(index).getBaseVersion()).isEqualTo(doc.getBaseVersion());
            assertThat(deltaEventFeatureContext.getDeltaInfos().get(index).getDeltaVersion()).isEqualTo(
                    deltaEventFeatureContext.getDeltaRequestContents().get(index).deltaVersion());
        });

    }

    @Dann("sollten die Tageswerte für die Personengruppe {int} unverändert sein")
    public void solltenDieTageswerteFuerDiePersonengruppeUnveraendertSein(int personGroup) {
        Map<String, AccountGroupValue> dayAmountsBefore = this.deltaEventFeatureContext.getExistingPersonGroupDayValues()
                                                                                       .stream()
                                                                                       .filter(dayDoc -> dayDoc.getAccountGroupNumber()
                                                                                               == personGroup)
                                                                                       .findFirst()
                                                                                       .orElse(new MovementDataPersonGroupDay())
                                                                                       .getValues();

        assertGroupDayValuesInMongoDBForPersongroup(personGroup, dayAmountsBefore, "Values should be unchanged");
    }

    @Dann("sollten die Monatswerte für die Personengruppe {int} unverändert sein")
    public void solltenDieMonatswerteFuerDiePersonengruppeUnveraendertSein(int personGroup) {
        Map<String, AccountGroupValue> monthAmountsBefore = this.deltaEventFeatureContext.getExistingPersonGroupMonthValues()
                                                                                         .stream()
                                                                                         .filter(monthDoc -> monthDoc.getAccountGroupNumber()
                                                                                                 == personGroup)
                                                                                         .findFirst()
                                                                                         .orElse(new MovementDataPersonGroupMonth())
                                                                                         .getValues();

        MovementDataPersonGroupMonth actualGroupMonthAmount = Mono.from(movementDataPersonGroupMonthCollection
                                                                                .find(getOneMovementDataPersonGroupDocument(
                                                                                        this.deltaEventFeatureContext.getConsultant(),
                                                                                        this.deltaEventFeatureContext.getClient(),
                                                                                        this.deltaEventFeatureContext.getFiscalYearBegin(), 0,
                                                                                        personGroup,
                                                                                        null)))
                                                                  .block();
        assertThat(actualGroupMonthAmount).isNotNull();
        assertThat(actualGroupMonthAmount.getValues()).as("Values should be unchanged for personGroup %d", personGroup)
                                                      .usingRecursiveComparison()
                                                      .ignoringCollectionOrder()
                                                      .isEqualTo(monthAmountsBefore);
    }

    @Dann("sollten die Tageswerte für das Personenkonto {int} unverändert sein")
    @Dann("sollten die Tageswerte für das Sachkonto {int} unverändert sein")
    @Dann("sollten die Tageswerte für das Konto {int} unverändert sein")
    public void solltenDieTageswerteUnveraendertSein(int account) {
        Map<String, AccountValue> dayAmountsBefore = this.deltaEventFeatureContext.getExistingDayValues()
                                                                                  .stream()
                                                                                  .filter(dayDoc -> dayDoc.getAccountNumber() == account)
                                                                                  .findFirst()
                                                                                  .orElse(new MovementDataDay())
                                                                                  .getValues();

        assertDayValuesInMongoDBForAccount(account, dayAmountsBefore, "Values should be unchanged");
    }

    @Dann("sollten die Monatswerte für das Personenkonto {int} unverändert sein")
    @Dann("sollten die Monatswerte für das Sachkonto {int} unverändert sein")
    @Dann("sollten die Monatswerte für das Konto {int} unverändert sein")
    public void solltenDieMonatswerteUnveraendertSein(int account) {
        Map<String, AccountValue> monthAmountsBefore = this.deltaEventFeatureContext.getExistingMonthValues()
                                                                                    .stream()
                                                                                    .filter(dayDoc -> dayDoc.getAccountNumber() == account)
                                                                                    .findFirst()
                                                                                    .orElse(new MovementDataMonth())
                                                                                    .getValues();

        assertMonthValuesAreInMongoDbForAccount(account, monthAmountsBefore, "Values should be unchanged");
    }

    @Dann("sollte der Tageswert für das Personenkonto {int} um den Betrag der Sollbuchung erhöht sein")
    @Dann("sollte der Tageswert für das Sachkonto {int} um den Betrag der Sollbuchung erhöht sein")
    @Dann("sollte der Tageswert für das Personenkonto {int} um den Betrag der Habenbuchung erhöht sein")
    @Dann("sollte der Tageswert für das Sachkonto {int} um den Betrag der Habenbuchung erhöht sein")
    @Dann("sollten die Tageswerte für das Personenkonto {} gespeichert sein")
    @Dann("sollten die Tageswerte für das Sachkonto {} gespeichert sein")
    @Dann("sollten die Tageswerte für das Konto {} gespeichert sein")
    public void solltenDieGeandertenTageswerteGespeichertSein(int account, List<DayBooking> expectedValues) {
        Map<String, AccountValue> expectedDayAmounts = new HashMap<>();
        for (DayBooking expectedValue : expectedValues) {
            expectedDayAmounts.put("d" + expectedValue.day(),
                                   new AccountValue(Utility.convertAmountToLongIncludingTwoFractionalDigits(expectedValue.amountDebit()),
                                                    Utility.convertAmountToLongIncludingTwoFractionalDigits(expectedValue.amountCredit()),
                                                    null, null, null, null));
        }

        assertDayValuesInMongoDBForAccount(account, expectedDayAmounts, "Values should be changed");
    }

    @Dann("sollte der Monatswert für das Personenkonto {int} um den Betrag der Sollbuchung erhöht sein")
    @Dann("sollte der Monatswert für das Sachkonto {int} um den Betrag der Sollbuchung erhöht sein")
    @Dann("sollte der Monatswert für das Personenkonto {int} um den Betrag der Habenbuchung erhöht sein")
    @Dann("sollte der Monatswert für das Sachkonto {int} um den Betrag der Habenbuchung erhöht sein")
    @Dann("sollten die Monatswerte für das Personenkonto {} gespeichert sein")
    @Dann("sollten die Monatswerte für das Sachkonto {} gespeichert sein")
    @Dann("sollten die Monatswerte für das Konto {} gespeichert sein")
    public void solltenDieGeandertenMonatswerteGespeichertSein(int account, List<MonthBooking> expectedValues) {
        Map<String, AccountValue> expectedMonthAmounts = new HashMap<>();
        for (MonthBooking expectedValue : expectedValues) {
            expectedMonthAmounts.put("m" + expectedValue.month(),
                                     new AccountValue(Utility.convertAmountToLongIncludingTwoFractionalDigits(expectedValue.amountDebit()),
                                                      Utility.convertAmountToLongIncludingTwoFractionalDigits(expectedValue.amountCredit()),
                                                      null, null, null, null));
        }

        assertMonthValuesAreInMongoDbForAccount(account, expectedMonthAmounts, "Values should be changed");
    }

    @Dann("sollte der typische Tageswert für die Personengruppe {int} um den Betrag der Sollbuchung erhöht sein")
    @Dann("sollten die aggregierten Tageswerte für die Personengruppe {int} um die Delta Werte verändert sein")
    @Dann("sollten die üblichen aggregierten Tageswerte für die Personengruppe {int} um die Delta Werte verändert sein")
    @Dann("sollten die üblichen aggregierten Tageswerte für die Personengruppe {int} inklusive Personenkonto 100000001 berechnet sein")
    @Dann("sollten die üblichen aggregierten Tageswerte für die Personengruppe {int} ohne Personenkonto 100000001 berechnet sein")
    @Dann("sollte der typische Tageswert für die Personengruppe {int} ausgebucht sein")
    public void solltenDieUeblichenAggregiertenTageswerteGespeichertSein(int personGroup, List<GroupDayBooking> expectedValues) {
        Map<String, AccountGroupValue> expectedUsualDayAmounts = new HashMap<>();
        for (GroupDayBooking expectedValue : expectedValues) {
            expectedUsualDayAmounts.put("d" + expectedValue.day(), AccountGroupValue.builder()
                                                                                    .amountDebitUsual(
                                                                                            Utility.convertAmountToLongIncludingTwoFractionalDigits(
                                                                                                    expectedValue.amountDebit()))
                                                                                    .amountCreditUsual(
                                                                                            Utility.convertAmountToLongIncludingTwoFractionalDigits(
                                                                                                    expectedValue.amountCredit()))
                                                                                    .build());
        }

        assertGroupDayValuesInMongoDBForPersongroup(personGroup, expectedUsualDayAmounts, "Values should be changed");
    }

    @Dann("sollte der Tageswert in der Personengruppe {int} als untypisch ausgewiesen werden - debitorischer Kreditor")
    @Dann("sollte der Tageswert in der Personengruppe {int} als untypisch ausgewiesen werden - kreditorischer Debitor")
    public void typischUntypischUebergangTag(int personGroup, List<FullGroupDayBooking> expectedValues) {
        Map<String, AccountGroupValue> expectedUsualDayAmounts = new HashMap<>();
        for (FullGroupDayBooking expectedValue : expectedValues) {
            expectedUsualDayAmounts.put("d" + expectedValue.day(), AccountGroupValue.builder()
                                                                                    .amountDebitUsual(
                                                                                            Utility.convertAmountToLongIncludingTwoFractionalDigits(
                                                                                                    expectedValue.amountDebitUsual()))
                                                                                    .amountCreditUsual(
                                                                                            Utility.convertAmountToLongIncludingTwoFractionalDigits(
                                                                                                    expectedValue.amountCreditUsual()))
                                                                                    .amountCreditUnusual(
                                                                                            Utility.convertAmountToLongIncludingTwoFractionalDigits(
                                                                                                    expectedValue.amountCreditUnusual()))
                                                                                    .amountDebitUnusual(
                                                                                            Utility.convertAmountToLongIncludingTwoFractionalDigits(
                                                                                                    expectedValue.amountDebitUnusual()))
                                                                                    .build());
        }

        assertGroupDayValuesInMongoDBForPersongroup(personGroup, expectedUsualDayAmounts, "Values should be changed");
    }

    @Dann("sollte der Monatswert in der Personengruppe {int} als untypisch ausgewiesen werden - debitorischer Kreditor")
    @Dann("sollte der Monatswert in der Personengruppe {int} als untypisch ausgewiesen werden - kreditorischer Debitor")
    public void typischUntypischUebergangMonat(int personGroup, List<FullGroupMonthBooking> expectedValues) {
        Map<String, AccountGroupValue> expectedUsualMonthAmounts = new HashMap<>();
        for (FullGroupMonthBooking expectedValue : expectedValues) {
            expectedUsualMonthAmounts.put("m" + expectedValue.month(), AccountGroupValue.builder()
                                                                                        .amountDebitUsual(
                                                                                                Utility.convertAmountToLongIncludingTwoFractionalDigits(
                                                                                                        expectedValue.amountDebitUsual()))
                                                                                        .amountCreditUsual(
                                                                                                Utility.convertAmountToLongIncludingTwoFractionalDigits(
                                                                                                        expectedValue.amountCreditUsual()))
                                                                                        .amountCreditUnusual(
                                                                                                Utility.convertAmountToLongIncludingTwoFractionalDigits(
                                                                                                        expectedValue.amountCreditUnusual()))
                                                                                        .amountDebitUnusual(
                                                                                                Utility.convertAmountToLongIncludingTwoFractionalDigits(
                                                                                                        expectedValue.amountDebitUnusual()))
                                                                                        .build());
        }

        assertGroupMonthValuesInMongoDBForPersongroup(personGroup, expectedUsualMonthAmounts, "Values should be changed");
    }

    @Dann("sollten die aggregierten Monatswerte für die Personengruppe {int} um die Delta Werte verändert sein")
    @Dann("sollten die üblichen aggregierten Monatswerte für die Personengruppe {int} inklusive Personenkonto 100000001 berechnet sein")
    @Dann("sollten die üblichen aggregierten Monatswerte für die Personengruppe {int} um die Delta Werte verändert sein")
    @Dann("sollten die üblichen aggregierten Monatswerte für die Personengruppe {int} ohne Personenkonto 100000001 berechnet sein")
    public void solltenDieUeblichenAggregiertenMonatsswerteGespeichertSein(int personGroup, List<GroupMonthBooking> expectedValues) {
        Map<String, AccountGroupValue> expectedUsualMonthAmounts = new HashMap<>();
        for (GroupMonthBooking expectedValue : expectedValues) {
            expectedUsualMonthAmounts.put("m" + expectedValue.month(), AccountGroupValue.builder()
                                                                                        .amountDebitUsual(
                                                                                                Utility.convertAmountToLongIncludingTwoFractionalDigits(
                                                                                                        expectedValue.amountDebit()))
                                                                                        .amountCreditUsual(
                                                                                                Utility.convertAmountToLongIncludingTwoFractionalDigits(
                                                                                                        expectedValue.amountCredit()))
                                                                                        .build());
        }

        assertGroupMonthValuesInMongoDBForPersongroup(personGroup, expectedUsualMonthAmounts, "Values should be changed");
    }

    @Dann("sollte der untypische Tageswert für die Personengruppe {int} um den Betrag der Sollbuchung erhöht sein - debitorischer Kreditor")
    @Dann("sollte der untypische Tageswert für die Personengruppe {int} um den Betrag der Habenbuchung erhöht sein - kreditorischer Debitor")
    public void solltenDieUnUeblichenAggregiertenTageswerteGespeichertSein(int personGroup, List<GroupDayBooking> expectedValues) {
        Map<String, AccountGroupValue> expectedUsualDayAmounts = new HashMap<>();
        for (GroupDayBooking expectedValue : expectedValues) {
            expectedUsualDayAmounts.put("d" + expectedValue.day(), AccountGroupValue.builder()
                                                                                    .amountDebitUnusual(
                                                                                            Utility.convertAmountToLongIncludingTwoFractionalDigits(
                                                                                                    expectedValue.amountDebit()))
                                                                                    .amountCreditUnusual(
                                                                                            Utility.convertAmountToLongIncludingTwoFractionalDigits(
                                                                                                    expectedValue.amountCredit()))
                                                                                    .build());
        }

        assertGroupDayValuesInMongoDBForPersongroup(personGroup, expectedUsualDayAmounts, "Values should be changed");
    }

    @Dann("sollte die Personengruppe {int} nur noch null Werte beinhalten")
    @Dann("sollte die Personengruppe {int} weggefallen sein")
    public void sollteDiePersonengruppeWeggefallenSein(int personGroup) {
        assertGroupDayValuesInMongoDBAreEmptyForPersongroup(personGroup);
        assertGroupMonthValuesInMongoDBAreEmptyForPersongroup(personGroup);
    }

    @Dann("sollte das Konto {int} aus der Liste der gesondert zu betrachtenden Personenkonten entfernt sein")
    @Dann("das Konto {int} sollte nicht in der Liste der gesondert zu betrachtenden Personenkonten aufgenommen sein")
    @Dann("sollte das Konto {int} nicht in der Liste der gesondert zu betrachtenden Personenkonten aufgenommen sein")
    public void sollteDasKontoNichtInDerListeDerGesondertZuBetrachtendenPersonenkontenAufgenommenSein(int accountNumber) {
        MasterData masterdata =
                Mono.from(this.masterDataCollection.find(and(eq(FieldConstants.CONSULTANT, this.deltaEventFeatureContext.getConsultant()),
                                                             eq(FieldConstants.CLIENT, this.deltaEventFeatureContext.getClient()),
                                                             eq(FieldConstants.YEAR_BEGIN,
                                                                this.deltaEventFeatureContext.getFiscalYearBegin())
                                                            ))).block();

        assertThat(masterdata).as("masterdata for (consultant %s, client %s, wj %s) should be present", this.deltaEventFeatureContext.getConsultant(),
                                  this.deltaEventFeatureContext.getClient(), this.deltaEventFeatureContext.getFiscalYearBegin()).isNotNull();

        if (masterdata.getIndividualPersonAccountNumbers() == null) {
            // nicht vorhandenene Liste -> kein gesondert behandeltes Personenkonto.
            return;
        }

        assertThat(masterdata.getIndividualPersonAccountNumbers()).doesNotContain(accountNumber);
    }

    // ------------------------ Consider moving to assert lib --------------------------

    private void assertMonthValuesAreInMongoDbForAccount(int account, Map<String, AccountValue> monthAmountsBefore, String description) {
        MovementDataMonth monthData = Mono.from(this.movementDataMonthCollection.find(
                getOneMovementDataDocument(this.deltaEventFeatureContext.getConsultant(),
                                           this.deltaEventFeatureContext.getClient(),
                                           this.deltaEventFeatureContext.getFiscalYearBegin(), 0, account, null))).block();
        assertThat(monthData).as("Month Document for account %d should be present", account).isNotNull();
        Map<String, AccountValue> actualMonthAmounts = monthData.getValues();
        assertThat(actualMonthAmounts).as(description + " for account %d", account).containsAllEntriesOf(monthAmountsBefore);
    }

    private void assertDayValuesInMongoDBForAccount(int account, Map<String, AccountValue> expectedDayValues, String description) {
        MovementDataDay daydata = Mono.from(this.movementDataDayCollection.find(
                getOneMovementDataDocument(this.deltaEventFeatureContext.getConsultant(),
                                           this.deltaEventFeatureContext.getClient(),
                                           this.deltaEventFeatureContext.getFiscalYearBegin(), 0, account, null))).block();
        assertThat(daydata).as("Day Document for account %d should be present", account).isNotNull();
        Map<String, AccountValue> actualDayAmounts = daydata.getValues();
        assertThat(actualDayAmounts).as(description + " for account %d", account).containsAllEntriesOf(expectedDayValues);
    }

    private void assertGroupDayValuesInMongoDBForPersongroup(int personGroup, Map<String, AccountGroupValue> expectedValues, String description) {
        MovementDataPersonGroupDay actualGroupDayAmount = Mono.from(movementDataPersonGroupDayCollection
                                                                            .find(getOneMovementDataPersonGroupDocument(
                                                                                    this.deltaEventFeatureContext.getConsultant(),
                                                                                    this.deltaEventFeatureContext.getClient(),
                                                                                    this.deltaEventFeatureContext.getFiscalYearBegin(), 0,
                                                                                    personGroup,
                                                                                    null)))
                                                              .block();
        assertThat(actualGroupDayAmount).isNotNull();
        assertThat(actualGroupDayAmount.getValues()).as(description + " for personGroup %d", personGroup).containsAllEntriesOf(expectedValues);
    }

    private void assertGroupMonthValuesInMongoDBForPersongroup(int personGroup, Map<String, AccountGroupValue> expectedValues, String description) {
        MovementDataPersonGroupMonth actualGroupMonthAmount = Mono.from(movementDataPersonGroupMonthCollection
                                                                                .find(getOneMovementDataPersonGroupDocument(
                                                                                        this.deltaEventFeatureContext.getConsultant(),
                                                                                        this.deltaEventFeatureContext.getClient(),
                                                                                        this.deltaEventFeatureContext.getFiscalYearBegin(), 0,
                                                                                        personGroup,
                                                                                        null)))
                                                                  .block();
        assertThat(actualGroupMonthAmount).isNotNull();
        assertThat(actualGroupMonthAmount.getValues()).as(description + " for personGroup %d", personGroup).containsAllEntriesOf(expectedValues);
    }

    private void assertGroupMonthValuesInMongoDBAreEmptyForPersongroup(int personGroup) {
        MovementDataPersonGroupMonth actualGroupMonthAmount = Mono.from(movementDataPersonGroupMonthCollection
                                                                                .find(getOneMovementDataPersonGroupDocument(
                                                                                        this.deltaEventFeatureContext.getConsultant(),
                                                                                        this.deltaEventFeatureContext.getClient(),
                                                                                        this.deltaEventFeatureContext.getFiscalYearBegin(), 0,
                                                                                        personGroup,
                                                                                        null)))
                                                                  .block();
        if (actualGroupMonthAmount == null) {
            return; // Not existing Group is considered equal to an existing group with only zero values
        }
        assertThat(actualGroupMonthAmount.getValues().values()).as("expect only zero values for personGroup %d", personGroup)
                                                               .allSatisfy((Consumer<AccountGroupValue>) groupValue -> {
                                                                   assertThat(groupValue.getAmountCreditUsual()).satisfiesAnyOf(x -> then(x).isZero(),
                                                                                                                                x -> then(
                                                                                                                                        x).isNull());
                                                                   assertThat(groupValue.getAmountDebitUsual()).satisfiesAnyOf(x -> then(x).isZero(),
                                                                                                                               x -> then(x).isNull());
                                                                   assertThat(groupValue.getAmountCreditUnusual()).satisfiesAnyOf(
                                                                           x -> then(x).isZero(), x -> then(x).isNull());
                                                                   assertThat(groupValue.getAmountDebitUnusual()).satisfiesAnyOf(
                                                                           x -> then(x).isZero(), x -> then(x).isNull());
                                                               });
    }

    private void assertGroupDayValuesInMongoDBAreEmptyForPersongroup(int personGroup) {
        MovementDataPersonGroupDay actualGroupDayAmount = Mono.from(movementDataPersonGroupDayCollection
                                                                            .find(getOneMovementDataPersonGroupDocument(
                                                                                    this.deltaEventFeatureContext.getConsultant(),
                                                                                    this.deltaEventFeatureContext.getClient(),
                                                                                    this.deltaEventFeatureContext.getFiscalYearBegin(), 0,
                                                                                    personGroup,
                                                                                    null)))
                                                              .block();
        if (actualGroupDayAmount == null) {
            return; // Not existing Group is considered equal to an existing group with only zero values
        }
        assertThat(actualGroupDayAmount.getValues().values()).as("expect only zero values for personGroup %d", personGroup)
                                                             .allSatisfy((Consumer<AccountGroupValue>) groupValue -> {
                                                                 assertThat(groupValue.getAmountCreditUsual()).satisfiesAnyOf(x -> then(x).isZero(),
                                                                                                                              x -> then(x).isNull());
                                                                 assertThat(groupValue.getAmountDebitUsual()).satisfiesAnyOf(x -> then(x).isZero(),
                                                                                                                             x -> then(x).isNull());
                                                                 assertThat(groupValue.getAmountCreditUnusual()).satisfiesAnyOf(x -> then(x).isZero(),
                                                                                                                                x -> then(
                                                                                                                                        x).isNull());
                                                                 assertThat(groupValue.getAmountDebitUnusual()).satisfiesAnyOf(x -> then(x).isZero(),
                                                                                                                               x -> then(x).isNull());
                                                             });
    }

    private void setData(int account, List<BookingKmvz> bookingKmvzs, long baseVersion, long deltaVersion) {
        List<AccountSumDayDelta> accountSumDayDeltas =
                bookingKmvzs.stream()
                            .map(bookingKmvz -> new AccountSumDayDelta().accountNumber(account)
                                                                        .month(bookingKmvz.month())
                                                                        .date(DateFunctions.geFirstPossibleAccountingDateForMonth(
                                                                                deltaEventFeatureContext.getFiscalYearBegin(), bookingKmvz.month()))
                                                                        .amountDebit(bookingKmvz.amountDebit())
                                                                        .amountCredit(bookingKmvz.amountCredit())
                                                                        .recordType(DEFAULT_RECORD_TYPE)
                                                                        .accountingReasonId(0)
                                                                        .accountingCommitted(null))
                            .collect(Collectors.toCollection(ArrayList::new));
        if (deltaEventFeatureContext.getDeltaRequest() == null) {
            deltaEventFeatureContext.setBookingsInEvent(bookingKmvzs);
            deltaEventFeatureContext.setDeltaRequest(new DeltaRequest(true).accountSumDayDeltas(accountSumDayDeltas));
            deltaEventFeatureContext.setBaseVersion(baseVersion);
            deltaEventFeatureContext.setDeltaVersion(deltaVersion);
        } else {
            deltaEventFeatureContext.getDeltaRequest().getAccountSumDayDeltas().addAll(accountSumDayDeltas);
        }
    }
}
