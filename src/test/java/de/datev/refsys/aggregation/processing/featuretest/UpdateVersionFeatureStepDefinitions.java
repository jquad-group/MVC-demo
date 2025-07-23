package de.datev.refsys.aggregation.processing.featuretest;

import de.datev.refsys.aggregation.document.model.StateDoc;
import de.datev.refsys.aggregation.document.model.enums.StateDocState;
import de.datev.refsys.aggregation.processing.api.model.DeltaInfo;
import de.datev.refsys.aggregation.processing.repository.StateDocRepository;
import io.cucumber.java.de.Angenommen;
import io.cucumber.java.de.Dann;
import io.cucumber.java.de.Und;
import io.cucumber.java.de.Wenn;
import io.restassured.module.webtestclient.response.WebTestClientResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") // Autowiring via Cucumber Context not recognized by IntelliJ
@Slf4j
public class UpdateVersionFeatureStepDefinitions {

    @LocalServerPort
    int randomServerPort;

    @Autowired
    DeltaEventFeatureContext deltaEventFeatureContext;

    @Autowired
    StateDocRepository stateDocRepository;

    private WebTestClientResponse res = null;
    private ResponseEntity<DeltaInfo> retrieve;

    @Dann("soll der Berater {int} und der Mandant {int} sein")
    public void test(int berater, int mandant) {
        assertThat(deltaEventFeatureContext.getConsultant()).isEqualTo(berater);
        assertThat(deltaEventFeatureContext.getClient()).isEqualTo(mandant);
    }

    @Wenn("die update-version API mit Base Version {int} und Delta Version {int} aufgerufen wird")
    public void dieUpdateVersionAPIMitBaseVersionUndDeltaVersionAufgerufenWird(int baseVersion, int deltaVersion) {

        RestClient client = RestClient.builder()
                                      .baseUrl("http://localhost:" + randomServerPort)
                                      .build();
        retrieve = client.post()
                         .uri(uriBuilder -> uriBuilder
                                 .queryParam("fiscal-year", deltaEventFeatureContext.getFiscalYearBegin()).pathSegment()
                                 .path("/api/v1/aggregation-processing/consultants/{consultant}/clients/{client}/update-version")
                                 .build(deltaEventFeatureContext.getConsultant(), deltaEventFeatureContext.getClient(),
                                        deltaEventFeatureContext.getFiscalYearBegin()))
                         .body("""
                                       {
                                       "base_version": $base_version,
                                       "delta_version": $delta_version
                                       }
                                       """.replace("$base_version", String.valueOf(baseVersion))
                                          .replace("$delta_version", String.valueOf(deltaVersion)))
                         .contentType(MediaType.APPLICATION_JSON)
                         .retrieve()
                         .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {})
                         .toEntity(DeltaInfo.class);
        deltaEventFeatureContext.setDeltaInfos(Collections.singletonList(retrieve.getBody()));
    }

    @Dann("sollte der Rückgabewert {int} sein")
    public void sollteDerRuckgabewertSein(int expectedStatusCode) {
        assertThat(retrieve.getStatusCode()).isEqualTo(HttpStatus.resolve(expectedStatusCode));
    }

    @Und("sollte das StateDoc unverändert sein")
    public void sollteDasStateDocUnverandertSein() {
        StateDoc doc =
                this.stateDocRepository.findOneByBusinessKey(this.deltaEventFeatureContext.getConsultant(),
                                                             this.deltaEventFeatureContext.getClient(),
                                                             this.deltaEventFeatureContext.getFiscalYearBegin()).block();
        assertThat(doc).isNotNull();
        assertThat(doc.getBaseVersion()).isEqualTo(deltaEventFeatureContext.getStateDocBaseVersion());
        assertThat(doc.getDeltaVersion()).isEqualTo(deltaEventFeatureContext.getStateDocDeltaVersion());
    }

    @Angenommen("es existiert kein StateDoc")
    @Dann("sollte kein StateDoc vorhanden sein")
    public void esExistiertKeinStatedoc() {
        StateDoc doc =
                this.stateDocRepository.findOneByBusinessKey(this.deltaEventFeatureContext.getConsultant(),
                                                             this.deltaEventFeatureContext.getClient(),
                                                             this.deltaEventFeatureContext.getFiscalYearBegin()).block();
        assertThat(doc).isNull();
    }

}
