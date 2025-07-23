package de.datev.refsys.generated.acds.api;

import de.datev.refsys.generated.acds.ApiClient;

import de.datev.refsys.generated.acds.api.model.CollectiveAccount;
import de.datev.refsys.generated.acds.api.model.ProblemDetails;

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
public class CollectiveAccountsApi {
    private ApiClient apiClient;

    public CollectiveAccountsApi() {
        this(new ApiClient());
    }

    @Autowired
    public CollectiveAccountsApi(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public ApiClient getApiClient() {
        return apiClient;
    }

    public void setApiClient(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    /**
     * Liefert die Sammelkonto-Stammdaten für das ReferenzSystem
     * Die Sammelkonto-Stammdaten für das ReferenzSystem werden für das angegebene Geschäftsjahr mit WJ-Beginn &#39;fiscal-year&#39; zurückgegeben. 
     * <p><b>200</b> - Objekt mit Sammelkonto-Stammdaten
     * <p><b>204</b> - No Collective-Accounts available
     * <p><b>555</b> - Technischer Fehler im Kontofunktionsmischer
     * <p><b>400</b> - Problem JSON
     * <p><b>404</b> - Problem JSON
     * <p><b>500</b> - Problem JSON
     * @param consultant (Beraternummer) Consultant-Number
     * @param client (Mandantennummer) Client-Number
     * @param fiscalYear Geschäftsjahr
     * @param baseVersion Daten werden Basisversions bezogen abgefragt
     * @param deltaVersion Daten werden Deltaversions bezogen abgefragt
     * @param accountLength (Sachkontenlänge) General ledger account length, permissible general ledger account length of 4–8 digits, subledger accounts have general ledger account length + 1 (v1)
     * @param accountSystem (DATEV-Standardkontenrahmen) DATEV standard chart of accounts (SKR)
     * @param industryId Branchenlösungs-Id
     * @param useConsultantAccountingFunctions Nutzung Kontofunktionen Kanzlei
     * @param useClientAccountingFunctions Nutzung Kontofunktionen individuell
     * @param useSkrFollowingYear Nutzung SKR Folgejahr
     * @param nearTimeData Sollen auch aktuelle Daten aus OnPremise mitgeliefert werden? In diesen Fällen sind aktuelle Stammdaten und aktuelle Kontenmonatsverkehrszahlen enthalten. 
     * @param xCorrelationId Id zum Anwendungsübergreifenden Logging
     * @param requestId Id zum Anwendungsverfolgen Logging.
     * @return List&lt;CollectiveAccount&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getCollectiveAccountsRequestCreation(Integer consultant, Integer client, Integer fiscalYear, Long baseVersion, Long deltaVersion, Integer accountLength, Integer accountSystem, Integer industryId, Boolean useConsultantAccountingFunctions, Boolean useClientAccountingFunctions, Boolean useSkrFollowingYear, Boolean nearTimeData, String xCorrelationId, String requestId) throws WebClientResponseException {
        Object postBody = null;
        // verify the required parameter 'consultant' is set
        if (consultant == null) {
            throw new WebClientResponseException("Missing the required parameter 'consultant' when calling getCollectiveAccounts", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'client' is set
        if (client == null) {
            throw new WebClientResponseException("Missing the required parameter 'client' when calling getCollectiveAccounts", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'fiscalYear' is set
        if (fiscalYear == null) {
            throw new WebClientResponseException("Missing the required parameter 'fiscalYear' when calling getCollectiveAccounts", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'baseVersion' is set
        if (baseVersion == null) {
            throw new WebClientResponseException("Missing the required parameter 'baseVersion' when calling getCollectiveAccounts", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'deltaVersion' is set
        if (deltaVersion == null) {
            throw new WebClientResponseException("Missing the required parameter 'deltaVersion' when calling getCollectiveAccounts", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'accountLength' is set
        if (accountLength == null) {
            throw new WebClientResponseException("Missing the required parameter 'accountLength' when calling getCollectiveAccounts", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'accountSystem' is set
        if (accountSystem == null) {
            throw new WebClientResponseException("Missing the required parameter 'accountSystem' when calling getCollectiveAccounts", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'industryId' is set
        if (industryId == null) {
            throw new WebClientResponseException("Missing the required parameter 'industryId' when calling getCollectiveAccounts", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'useConsultantAccountingFunctions' is set
        if (useConsultantAccountingFunctions == null) {
            throw new WebClientResponseException("Missing the required parameter 'useConsultantAccountingFunctions' when calling getCollectiveAccounts", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'useClientAccountingFunctions' is set
        if (useClientAccountingFunctions == null) {
            throw new WebClientResponseException("Missing the required parameter 'useClientAccountingFunctions' when calling getCollectiveAccounts", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'useSkrFollowingYear' is set
        if (useSkrFollowingYear == null) {
            throw new WebClientResponseException("Missing the required parameter 'useSkrFollowingYear' when calling getCollectiveAccounts", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
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
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "account-length", accountLength));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "account-system", accountSystem));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "industry-id", industryId));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "use-consultant-accounting-functions", useConsultantAccountingFunctions));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "use-client-accounting-functions", useClientAccountingFunctions));
        queryParams.putAll(apiClient.parameterToMultiValueMap(null, "use-skr-following-year", useSkrFollowingYear));

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

        ParameterizedTypeReference<CollectiveAccount> localVarReturnType = new ParameterizedTypeReference<CollectiveAccount>() {};
        return apiClient.invokeAPI("/api/v1/accounting-dataservices/consultants/{consultant}/clients/{client}/collective-accounts", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Liefert die Sammelkonto-Stammdaten für das ReferenzSystem
     * Die Sammelkonto-Stammdaten für das ReferenzSystem werden für das angegebene Geschäftsjahr mit WJ-Beginn &#39;fiscal-year&#39; zurückgegeben. 
     * <p><b>200</b> - Objekt mit Sammelkonto-Stammdaten
     * <p><b>204</b> - No Collective-Accounts available
     * <p><b>555</b> - Technischer Fehler im Kontofunktionsmischer
     * <p><b>400</b> - Problem JSON
     * <p><b>404</b> - Problem JSON
     * <p><b>500</b> - Problem JSON
     * @param consultant (Beraternummer) Consultant-Number
     * @param client (Mandantennummer) Client-Number
     * @param fiscalYear Geschäftsjahr
     * @param baseVersion Daten werden Basisversions bezogen abgefragt
     * @param deltaVersion Daten werden Deltaversions bezogen abgefragt
     * @param accountLength (Sachkontenlänge) General ledger account length, permissible general ledger account length of 4–8 digits, subledger accounts have general ledger account length + 1 (v1)
     * @param accountSystem (DATEV-Standardkontenrahmen) DATEV standard chart of accounts (SKR)
     * @param industryId Branchenlösungs-Id
     * @param useConsultantAccountingFunctions Nutzung Kontofunktionen Kanzlei
     * @param useClientAccountingFunctions Nutzung Kontofunktionen individuell
     * @param useSkrFollowingYear Nutzung SKR Folgejahr
     * @param nearTimeData Sollen auch aktuelle Daten aus OnPremise mitgeliefert werden? In diesen Fällen sind aktuelle Stammdaten und aktuelle Kontenmonatsverkehrszahlen enthalten. 
     * @param xCorrelationId Id zum Anwendungsübergreifenden Logging
     * @param requestId Id zum Anwendungsverfolgen Logging.
     * @return List&lt;CollectiveAccount&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Flux<CollectiveAccount> getCollectiveAccounts(Integer consultant, Integer client, Integer fiscalYear, Long baseVersion, Long deltaVersion, Integer accountLength, Integer accountSystem, Integer industryId, Boolean useConsultantAccountingFunctions, Boolean useClientAccountingFunctions, Boolean useSkrFollowingYear, Boolean nearTimeData, String xCorrelationId, String requestId) throws WebClientResponseException {
        ParameterizedTypeReference<CollectiveAccount> localVarReturnType = new ParameterizedTypeReference<CollectiveAccount>() {};
        return getCollectiveAccountsRequestCreation(consultant, client, fiscalYear, baseVersion, deltaVersion, accountLength, accountSystem, industryId, useConsultantAccountingFunctions, useClientAccountingFunctions, useSkrFollowingYear, nearTimeData, xCorrelationId, requestId).bodyToFlux(localVarReturnType);
    }

    /**
     * Liefert die Sammelkonto-Stammdaten für das ReferenzSystem
     * Die Sammelkonto-Stammdaten für das ReferenzSystem werden für das angegebene Geschäftsjahr mit WJ-Beginn &#39;fiscal-year&#39; zurückgegeben. 
     * <p><b>200</b> - Objekt mit Sammelkonto-Stammdaten
     * <p><b>204</b> - No Collective-Accounts available
     * <p><b>555</b> - Technischer Fehler im Kontofunktionsmischer
     * <p><b>400</b> - Problem JSON
     * <p><b>404</b> - Problem JSON
     * <p><b>500</b> - Problem JSON
     * @param consultant (Beraternummer) Consultant-Number
     * @param client (Mandantennummer) Client-Number
     * @param fiscalYear Geschäftsjahr
     * @param baseVersion Daten werden Basisversions bezogen abgefragt
     * @param deltaVersion Daten werden Deltaversions bezogen abgefragt
     * @param accountLength (Sachkontenlänge) General ledger account length, permissible general ledger account length of 4–8 digits, subledger accounts have general ledger account length + 1 (v1)
     * @param accountSystem (DATEV-Standardkontenrahmen) DATEV standard chart of accounts (SKR)
     * @param industryId Branchenlösungs-Id
     * @param useConsultantAccountingFunctions Nutzung Kontofunktionen Kanzlei
     * @param useClientAccountingFunctions Nutzung Kontofunktionen individuell
     * @param useSkrFollowingYear Nutzung SKR Folgejahr
     * @param nearTimeData Sollen auch aktuelle Daten aus OnPremise mitgeliefert werden? In diesen Fällen sind aktuelle Stammdaten und aktuelle Kontenmonatsverkehrszahlen enthalten. 
     * @param xCorrelationId Id zum Anwendungsübergreifenden Logging
     * @param requestId Id zum Anwendungsverfolgen Logging.
     * @return ResponseEntity&lt;List&lt;CollectiveAccount&gt;&gt;
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public Mono<ResponseEntity<List<CollectiveAccount>>> getCollectiveAccountsWithHttpInfo(Integer consultant, Integer client, Integer fiscalYear, Long baseVersion, Long deltaVersion, Integer accountLength, Integer accountSystem, Integer industryId, Boolean useConsultantAccountingFunctions, Boolean useClientAccountingFunctions, Boolean useSkrFollowingYear, Boolean nearTimeData, String xCorrelationId, String requestId) throws WebClientResponseException {
        ParameterizedTypeReference<CollectiveAccount> localVarReturnType = new ParameterizedTypeReference<CollectiveAccount>() {};
        return getCollectiveAccountsRequestCreation(consultant, client, fiscalYear, baseVersion, deltaVersion, accountLength, accountSystem, industryId, useConsultantAccountingFunctions, useClientAccountingFunctions, useSkrFollowingYear, nearTimeData, xCorrelationId, requestId).toEntityList(localVarReturnType);
    }

    /**
     * Liefert die Sammelkonto-Stammdaten für das ReferenzSystem
     * Die Sammelkonto-Stammdaten für das ReferenzSystem werden für das angegebene Geschäftsjahr mit WJ-Beginn &#39;fiscal-year&#39; zurückgegeben. 
     * <p><b>200</b> - Objekt mit Sammelkonto-Stammdaten
     * <p><b>204</b> - No Collective-Accounts available
     * <p><b>555</b> - Technischer Fehler im Kontofunktionsmischer
     * <p><b>400</b> - Problem JSON
     * <p><b>404</b> - Problem JSON
     * <p><b>500</b> - Problem JSON
     * @param consultant (Beraternummer) Consultant-Number
     * @param client (Mandantennummer) Client-Number
     * @param fiscalYear Geschäftsjahr
     * @param baseVersion Daten werden Basisversions bezogen abgefragt
     * @param deltaVersion Daten werden Deltaversions bezogen abgefragt
     * @param accountLength (Sachkontenlänge) General ledger account length, permissible general ledger account length of 4–8 digits, subledger accounts have general ledger account length + 1 (v1)
     * @param accountSystem (DATEV-Standardkontenrahmen) DATEV standard chart of accounts (SKR)
     * @param industryId Branchenlösungs-Id
     * @param useConsultantAccountingFunctions Nutzung Kontofunktionen Kanzlei
     * @param useClientAccountingFunctions Nutzung Kontofunktionen individuell
     * @param useSkrFollowingYear Nutzung SKR Folgejahr
     * @param nearTimeData Sollen auch aktuelle Daten aus OnPremise mitgeliefert werden? In diesen Fällen sind aktuelle Stammdaten und aktuelle Kontenmonatsverkehrszahlen enthalten. 
     * @param xCorrelationId Id zum Anwendungsübergreifenden Logging
     * @param requestId Id zum Anwendungsverfolgen Logging.
     * @return ResponseSpec
     * @throws WebClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getCollectiveAccountsWithResponseSpec(Integer consultant, Integer client, Integer fiscalYear, Long baseVersion, Long deltaVersion, Integer accountLength, Integer accountSystem, Integer industryId, Boolean useConsultantAccountingFunctions, Boolean useClientAccountingFunctions, Boolean useSkrFollowingYear, Boolean nearTimeData, String xCorrelationId, String requestId) throws WebClientResponseException {
        return getCollectiveAccountsRequestCreation(consultant, client, fiscalYear, baseVersion, deltaVersion, accountLength, accountSystem, industryId, useConsultantAccountingFunctions, useClientAccountingFunctions, useSkrFollowingYear, nearTimeData, xCorrelationId, requestId);
    }
}
