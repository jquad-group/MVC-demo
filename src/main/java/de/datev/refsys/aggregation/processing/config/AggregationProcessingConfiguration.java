package de.datev.refsys.aggregation.processing.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.datev.refsys.generated.acds.ApiClient;
import de.datev.refsys.generated.acds.api.AccountCaptionsApi;
import de.datev.refsys.generated.acds.api.AccountPurposeMappingsApi;
import de.datev.refsys.generated.acds.api.AccountSumDaysApi;
import de.datev.refsys.generated.acds.api.CollectiveAccountsApi;
import de.datev.refsys.generated.acds.api.CustomColumnStructuresApi;
import de.datev.refsys.generated.acds.api.CustomReportStructuresApi;
import de.datev.refsys.generated.acds.api.MasterdataContextApi;
import de.datev.refsys.generated.acds.api.MasterdataInventoriesApi;
import de.datev.refsys.generated.acds.api.MovementdataInventoriesApi;
import de.datev.refsys.generated.acds.api.ShareholderApi;
import de.datev.refsys.generated.acds.api.TranslationApi;
import de.datev.refsys.generated.acds.auth.ApiKeyAuth;
import de.datev.refsys.aggregation.processing.util.LoggingUtil;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.micrometer.context.ContextRegistry;
import jakarta.annotation.PostConstruct;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Hooks;
import reactor.netty.http.client.HttpClient;

import java.text.DecimalFormat;
import java.time.Duration;
import java.util.Locale;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_NDJSON;

@Configuration
public class AggregationProcessingConfiguration implements WebFluxConfigurer {
    private static final String CLIENT_ID_KEY = "client_id";
    private static final String CLIENT_SECRET_KEY = "client_secret";
    private static final int BYTES_IN_KB = 1024;

    @Value("${ref-sys.client.acds.base-path}")
    private String acdsBaseUrl;

    @Value("${ref-sys.client.acds.client-id}")
    private String clientId;

    @Value("${ref-sys.client.acds.client-secret}")
    private String clientSecret;

    @Override
    public void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
        WebFluxConfigurer.super.configureHttpMessageCodecs(configurer);
        var serverObjectMapper = serverObjectMapper();
        configurer.defaultCodecs().jackson2JsonEncoder(new Jackson2JsonEncoder(serverObjectMapper));
        configurer.defaultCodecs().jackson2JsonDecoder(new Jackson2JsonDecoder(serverObjectMapper));

        configureCustomContext();
    }

    @Bean
    public DecimalFormat decimalFormat() {
        return new DecimalFormat("#.00");
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
    public TranslationApi translationApi(final ApiClient apiClient) { return new TranslationApi(apiClient); }

    @Bean
    public MasterdataInventoriesApi masterdataInventoriesApi (final ApiClient apiClient) { return new MasterdataInventoriesApi(apiClient); }

    @Bean
    public MovementdataInventoriesApi movementdataInventoriesApi(final ApiClient apiClient) {
        return new MovementdataInventoriesApi(apiClient);
    }

    @Bean
    public AccountSumDaysApi accountSumDaysApi(final ApiClient sumDaysApiClient) {
        return new AccountSumDaysApi(sumDaysApiClient);
    }

    @Bean
    public CustomColumnStructuresApi customColumnStructuresApi(final ApiClient apiClient) {  return new CustomColumnStructuresApi(apiClient); }

    @Bean
    public CustomReportStructuresApi customReportStructuresApi(final ApiClient apiClient) {  return new CustomReportStructuresApi(apiClient); }

    @Bean
    public ApiClient apiClient(WebClient webClient) {
        ApiClient apiClient = new ApiClient(webClient);
        apiClient.setBasePath(acdsBaseUrl);
        ((ApiKeyAuth) apiClient.getAuthentication(CLIENT_ID_KEY)).setApiKey(clientId);
        ((ApiKeyAuth) apiClient.getAuthentication(CLIENT_SECRET_KEY)).setApiKey(clientSecret);
        return apiClient;
    }

    @Bean
    public ApiClient sumDaysApiClient(@Value("${ref-sys.client.acds.timeout-in-ms}") int timeoutInMs,
                                      @Value("${ref-sys.client.acds.response-body-size-in-kb}") int maxResponseBodySizeInKb) {
        HttpClient httpClient = HttpClient.create().responseTimeout(Duration.ofMillis(timeoutInMs));

        WebClient webClient = webClient(maxResponseBodySizeInKb).mutate().clientConnector(new ReactorClientHttpConnector(httpClient)).build();
        ApiClient apiClient = new ApiClient(webClient);
        apiClient.setBasePath(acdsBaseUrl);
        ((ApiKeyAuth) apiClient.getAuthentication(CLIENT_ID_KEY)).setApiKey(clientId);
        ((ApiKeyAuth) apiClient.getAuthentication(CLIENT_SECRET_KEY)).setApiKey(clientSecret);
        return apiClient;
    }

    @Bean
    public WebClient webClient(@Value("${ref-sys.client.acds.response-body-size-in-kb}") int maxResponseBodySizeInKb) {
        var clientObjectMapper = clientObjectMapper();
        return WebClient.builder().exchangeStrategies(ExchangeStrategies.builder().codecs(clientDefaultCodecsConfigurer -> {
                            clientDefaultCodecsConfigurer.defaultCodecs()
                                                         .jackson2JsonEncoder(new Jackson2JsonEncoder(clientObjectMapper, APPLICATION_JSON,
                                                                                                      APPLICATION_NDJSON));
                            clientDefaultCodecsConfigurer.defaultCodecs()
                                                         .jackson2JsonDecoder(new Jackson2JsonDecoder(clientObjectMapper, APPLICATION_JSON,
                                                                                                      APPLICATION_NDJSON));
                            clientDefaultCodecsConfigurer.defaultCodecs().maxInMemorySize(maxResponseBodySizeInKb * BYTES_IN_KB);
                        }).build())
                        .build();
    }

    /**
     * Registers a custom {@link RegistryEventConsumer} for circuit breakers to add custom events.
     *
     * @return {@link CircuitBreakerRegistryEventConsumer} instance
     * @see CircuitBreakerRegistryEventConsumer
     */
    @Bean
    public RegistryEventConsumer<CircuitBreaker> stateLoggingRegistryEventConsumer() {
        return new CircuitBreakerRegistryEventConsumer();
    }

    @PostConstruct
    public void setDefaultLocale() {
        Locale.setDefault(Locale.ENGLISH);
    }

    private static ObjectMapper serverObjectMapper() {
        var mapper = new ObjectMapper();
        mapper.setDateFormat(StdDateFormat.instance);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
        return mapper;
    }

    private static ObjectMapper clientObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setDateFormat(ApiClient.createDefaultDateFormat());
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }

    private static void configureCustomContext() {
        Hooks.enableAutomaticContextPropagation();
        ContextRegistry.getInstance().registerThreadLocalAccessor(LoggingUtil.LOGGING_CONTEXT_KEY,
                                                                  MDC::getCopyOfContextMap,
                                                                  value -> value.forEach(MDC::put),
                                                                  MDC::clear);
    }
}
