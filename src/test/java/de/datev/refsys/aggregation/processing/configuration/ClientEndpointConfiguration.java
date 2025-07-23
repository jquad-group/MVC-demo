package de.datev.refsys.aggregation.processing.configuration;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import de.datev.refsys.generated.acds.ApiClient;
import de.datev.refsys.generated.acds.api.AccountCaptionsApi;
import de.datev.refsys.generated.acds.api.AccountPurposeMappingsApi;
import de.datev.refsys.generated.acds.api.AccountSumDaysApi;
import de.datev.refsys.generated.acds.api.CollectiveAccountsApi;
import de.datev.refsys.generated.acds.api.MasterdataContextApi;
import de.datev.refsys.generated.acds.api.MasterdataInventoriesApi;
import de.datev.refsys.generated.acds.api.MovementdataInventoriesApi;
import de.datev.refsys.generated.acds.api.ShareholderApi;
import de.datev.refsys.generated.acds.api.TranslationApi;
import de.datev.refsys.aggregation.processing.config.AggregationProcessingConfiguration;
import de.datev.refsys.aggregation.processing.util.TestUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.reactive.function.client.WebClient;

@TestConfiguration
@ContextConfiguration(initializers = ConfigDataApplicationContextInitializer.class)
@Import(AggregationProcessingConfiguration.class)
@ActiveProfiles(TestUtil.TEST_PROFILE)
public class ClientEndpointConfiguration {
    private static final String ACDS_URL_TEMPLATE = "http://localhost:%s";
    private final AggregationProcessingConfiguration aggregationProcessingConfiguration = new AggregationProcessingConfiguration();

    @Bean
    public WebClient webClient(@Value("${ref-sys.client.acds.response-body-size-in-kb}") int maxResponseBodySizeInKb) {
        return aggregationProcessingConfiguration.webClient(maxResponseBodySizeInKb);
    }

    @Bean
    public ApiClient apiClient(final WebClient webClient, WireMockServer wireMockServer) {
        ApiClient apiClient = aggregationProcessingConfiguration.apiClient(webClient);
        apiClient.setBasePath(String.format(ACDS_URL_TEMPLATE, wireMockServer.port()));
        return apiClient;
    }

    @Bean
    public ApiClient sumDaysApiClient(WireMockServer wireMockServer, @Value("${ref-sys.client.acds.timeout-in-ms}") int timeoutInMs,
                                      @Value("${ref-sys.client.acds.response-body-size-in-kb}") int maxResponseBodySizeInKb) {
        ApiClient apiClient = aggregationProcessingConfiguration.sumDaysApiClient(timeoutInMs, maxResponseBodySizeInKb);
        apiClient.setBasePath(String.format(ACDS_URL_TEMPLATE, wireMockServer.port()));

        return apiClient;
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public WireMockServer wireMockServer() {
        WireMockConfiguration wireMockConfiguration = WireMockConfiguration.wireMockConfig()
                .dynamicPort();
        return new WireMockServer(wireMockConfiguration);
    }

    @Bean
    public MasterdataContextApi masterdataContextApi (final ApiClient apiClient) {
        return new MasterdataContextApi(apiClient);
    }

    @Bean
    public AccountCaptionsApi accountCaptionsApi(final ApiClient apiClient) {
        return new AccountCaptionsApi(apiClient);
    }

    @Bean
    public ShareholderApi shareholderApi(final ApiClient apiClient) {
        return new ShareholderApi(apiClient);
    }

    @Bean
    public AccountPurposeMappingsApi accountPurposeMappingsApi(final ApiClient apiClient) {
        return new AccountPurposeMappingsApi(apiClient);
    }

    @Bean
    public CollectiveAccountsApi collectiveAccountsApi(final ApiClient apiClient) {
        return new CollectiveAccountsApi(apiClient);
    }

    @Bean
    public TranslationApi translationApi(final ApiClient apiClient) {
        return new TranslationApi(apiClient);
    }

    @Bean
    public MasterdataInventoriesApi masterdataInventoriesApi(final ApiClient apiClient) {
        return new MasterdataInventoriesApi(apiClient);
    }

    @Bean
    public MovementdataInventoriesApi movementdataInventoriesApi(final ApiClient apiClient) {
        return new MovementdataInventoriesApi(apiClient);
    }

    @Bean
    public AccountSumDaysApi accountSumDaysApi(final ApiClient apiClient) {
        return new AccountSumDaysApi(apiClient);
    }

}
