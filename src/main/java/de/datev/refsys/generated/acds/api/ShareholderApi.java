package de.datev.refsys.generated.acds.api;

import de.datev.refsys.generated.acds.ApiClient;

import de.datev.refsys.generated.acds.api.model.ProblemDetails;
import de.datev.refsys.generated.acds.api.model.ShareholderData;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

@jakarta.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2025-02-06T15:00:38.213911500+01:00[Europe/Berlin]")
public class ShareholderApi {
    private ApiClient apiClient;

    public ShareholderApi() {
        this(new ApiClient());
    }

    @Autowired
    public ShareholderApi(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public ApiClient getApiClient() {
        return apiClient;
    }

    public void setApiClient(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    /**
     * Liefert die Gesellschafter-Stammdaten für das ReferenzSystem
     * Die GesellschafterStammdaten für das ReferenzSystem werden für das angegebene Geschäftsjahr mit WJ-Beginn &#39;fiscal-year&#39; zurückgegeben. 
     * <p><b>200</b> - Objekt aller Stammdaten mit Gesellschafterrelevanz
     * <p><b>204</b> - Consultant/Client not in RWARS
     * <p><b>400</b> - Problem JSON
     * <p><b>404</b> - Problem JSON
     * <p><b>500</b> - Problem JSON
     * @param consultant (Beraternummer) Consultant-Number
     * @param client (Mandantennummer) Client-Number
     * @param fiscalYear Geschäftsjahr
     * @param baseVersion Daten werden Basisversions bezogen abgefragt
     * @param deltaVersion Daten werden Deltaversions bezogen abgefragt
     * @param nearTimeData Sollen auch aktuelle Daten aus OnPremise mitgeliefert werden? In diesen Fällen sind aktuelle Stammdaten und aktuelle Kontenmonatsverkehrszahlen enthalten. 
     * @param xCorrelationId Id zum Anwendungsübergreifenden Logging
     * @param requestId Id zum Anwendungsverfolgen Logging.
     * @return ShareholderData
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getShareholdersRequestCreation(Integer consultant, Integer client, Integer fiscalYear, Long baseVersion, Long deltaVersion, Boolean nearTimeData, String xCorrelationId, String requestId) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'consultant' is set
        if (consultant == null) {
            throw new WebClientResponseException("Missing the required parameter 'consultant' when calling getShareholders", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'client' is set
        if (client == null) {
            throw new WebClientResponseException("Missing the required parameter 'client' when calling getShareholders", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'fiscalYear' is set
        if (fiscalYear == null) {
            throw new WebClientResponseException("Missing the required parameter 'fiscalYear' when calling getShareholders", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'baseVersion' is set
        if (baseVersion == null) {
            throw new WebClientResponseException("Missing the required parameter 'baseVersion' when calling getShareholders", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'deltaVersion' is set
        if (deltaVersion == null) {
            throw new WebClientResponseException("Missing the required parameter 'deltaVersion' when calling getShareholders", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<String, Object>();

        pathParams.put("consultant", consultant);
        pathParams.put("client", client);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<String, String>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<String, String>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<String, Object>();

        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "fiscal-year", fiscalYear));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "near-time-data", nearTimeData));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "base-version", baseVersion));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "delta-version", deltaVersion));

        if (xCorrelationId != null)
        headerParams.add("x-correlation-id", apiClient.parameterToString(xCorrelationId));
        if (requestId != null)
        headerParams.add("Request-Id", apiClient.parameterToString(requestId));
        final String[] localVarAccepts = { 
            "application/x-ndjson", "application/json", "application/problem+json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] { "client_secret", "client_id" };

        ParameterizedTypeReference<ShareholderData> localVarReturnType = new ParameterizedTypeReference<ShareholderData>() {};
        return apiClient.invokeAPI("/api/v1/accounting-dataservices/consultants/{consultant}/clients/{client}/shareholder", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Liefert die Gesellschafter-Stammdaten für das ReferenzSystem
     * Die GesellschafterStammdaten für das ReferenzSystem werden für das angegebene Geschäftsjahr mit WJ-Beginn &#39;fiscal-year&#39; zurückgegeben. 
     * <p><b>200</b> - Objekt aller Stammdaten mit Gesellschafterrelevanz
     * <p><b>204</b> - Consultant/Client not in RWARS
     * <p><b>400</b> - Problem JSON
     * <p><b>404</b> - Problem JSON
     * <p><b>500</b> - Problem JSON
     * @param consultant (Beraternummer) Consultant-Number
     * @param client (Mandantennummer) Client-Number
     * @param fiscalYear Geschäftsjahr
     * @param baseVersion Daten werden Basisversions bezogen abgefragt
     * @param deltaVersion Daten werden Deltaversions bezogen abgefragt
     * @param nearTimeData Sollen auch aktuelle Daten aus OnPremise mitgeliefert werden? In diesen Fällen sind aktuelle Stammdaten und aktuelle Kontenmonatsverkehrszahlen enthalten. 
     * @param xCorrelationId Id zum Anwendungsübergreifenden Logging
     * @param requestId Id zum Anwendungsverfolgen Logging.
     * @return ShareholderData
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ShareholderData> getShareholders(Integer consultant, Integer client, Integer fiscalYear, Long baseVersion, Long deltaVersion, Boolean nearTimeData, String xCorrelationId, String requestId) throws WebClientResponseException {
        ParameterizedTypeReference<ShareholderData> localVarReturnType = new ParameterizedTypeReference<ShareholderData>() {};
        return getShareholdersRequestCreation(consultant, client, fiscalYear, baseVersion, deltaVersion, nearTimeData, xCorrelationId, requestId).bodyToMono(localVarReturnType);
    }

    /**
     * Liefert die Gesellschafter-Stammdaten für das ReferenzSystem
     * Die GesellschafterStammdaten für das ReferenzSystem werden für das angegebene Geschäftsjahr mit WJ-Beginn &#39;fiscal-year&#39; zurückgegeben. 
     * <p><b>200</b> - Objekt aller Stammdaten mit Gesellschafterrelevanz
     * <p><b>204</b> - Consultant/Client not in RWARS
     * <p><b>400</b> - Problem JSON
     * <p><b>404</b> - Problem JSON
     * <p><b>500</b> - Problem JSON
     * @param consultant (Beraternummer) Consultant-Number
     * @param client (Mandantennummer) Client-Number
     * @param fiscalYear Geschäftsjahr
     * @param baseVersion Daten werden Basisversions bezogen abgefragt
     * @param deltaVersion Daten werden Deltaversions bezogen abgefragt
     * @param nearTimeData Sollen auch aktuelle Daten aus OnPremise mitgeliefert werden? In diesen Fällen sind aktuelle Stammdaten und aktuelle Kontenmonatsverkehrszahlen enthalten. 
     * @param xCorrelationId Id zum Anwendungsübergreifenden Logging
     * @param requestId Id zum Anwendungsverfolgen Logging.
     * @return ResponseEntity&lt;ShareholderData&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<ShareholderData>> getShareholdersWithHttpInfo(Integer consultant, Integer client, Integer fiscalYear, Long baseVersion, Long deltaVersion, Boolean nearTimeData, String xCorrelationId, String requestId) throws WebClientResponseException {
        ParameterizedTypeReference<ShareholderData> localVarReturnType = new ParameterizedTypeReference<ShareholderData>() {};
        return getShareholdersRequestCreation(consultant, client, fiscalYear, baseVersion, deltaVersion, nearTimeData, xCorrelationId, requestId).toEntity(localVarReturnType);
    }

    /**
     * Liefert die Gesellschafter-Stammdaten für das ReferenzSystem
     * Die GesellschafterStammdaten für das ReferenzSystem werden für das angegebene Geschäftsjahr mit WJ-Beginn &#39;fiscal-year&#39; zurückgegeben. 
     * <p><b>200</b> - Objekt aller Stammdaten mit Gesellschafterrelevanz
     * <p><b>204</b> - Consultant/Client not in RWARS
     * <p><b>400</b> - Problem JSON
     * <p><b>404</b> - Problem JSON
     * <p><b>500</b> - Problem JSON
     * @param consultant (Beraternummer) Consultant-Number
     * @param client (Mandantennummer) Client-Number
     * @param fiscalYear Geschäftsjahr
     * @param baseVersion Daten werden Basisversions bezogen abgefragt
     * @param deltaVersion Daten werden Deltaversions bezogen abgefragt
     * @param nearTimeData Sollen auch aktuelle Daten aus OnPremise mitgeliefert werden? In diesen Fällen sind aktuelle Stammdaten und aktuelle Kontenmonatsverkehrszahlen enthalten. 
     * @param xCorrelationId Id zum Anwendungsübergreifenden Logging
     * @param requestId Id zum Anwendungsverfolgen Logging.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getShareholdersWithResponseSpec(Integer consultant, Integer client, Integer fiscalYear, Long baseVersion, Long deltaVersion, Boolean nearTimeData, String xCorrelationId, String requestId) throws WebClientResponseException {
        return getShareholdersRequestCreation(consultant, client, fiscalYear, baseVersion, deltaVersion, nearTimeData, xCorrelationId, requestId);
    }
}
