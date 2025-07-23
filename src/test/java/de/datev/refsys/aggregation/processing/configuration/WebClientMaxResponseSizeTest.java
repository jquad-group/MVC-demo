package de.datev.refsys.aggregation.processing.configuration;

import com.github.tomakehurst.wiremock.WireMockServer;
import de.datev.refsys.generated.acds.ApiClient;
import de.datev.refsys.generated.acds.api.TranslationApi;
import de.datev.refsys.generated.acds.api.model.Translation;
import de.datev.refsys.aggregation.processing.util.TestDataLoader;
import de.datev.refsys.aggregation.processing.util.TestUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_BASE_VERSION;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_CLIENT;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_CONSULTANT;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_CORRELATION_ID;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_DELTA_VERSION;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_FISCAL_YEAR_2021_START;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(initializers = ConfigDataApplicationContextInitializer.class, classes = {ClientEndpointConfiguration.class})
@ActiveProfiles(TestUtil.TEST_PROFILE)
class WebClientMaxResponseSizeTest {

    public static final String SIZE_OF_3KB_TRANSLATION_FILE_PATH = "json/acds-responses/3kb-size-translation-file.json";
    @Autowired
    private WireMockServer wireMockServer;

    private static final String TRANSLATION_ENDPOINT= "translation";

    @Test
    @DisplayName("Test that an error is thrown when max response size is smaller than received file")
    void should_throw_error_when_max_response_size_is_smaller_than_file_size() {
        ClientEndpointConfiguration clientEndpointConfiguration = new ClientEndpointConfiguration();
        WebClient webClient = clientEndpointConfiguration.webClient(1);
        ApiClient apiClient = clientEndpointConfiguration.apiClient(webClient, wireMockServer);
        TranslationApi customTranslationApi = new TranslationApi(apiClient);

        TestUtil.stubAcdsClientApi(TRANSLATION_ENDPOINT, TEST_CLIENT, SIZE_OF_3KB_TRANSLATION_FILE_PATH, 200, wireMockServer);
        Throwable throwable = catchThrowable(
                () -> customTranslationApi.getTranslation(TEST_CONSULTANT, TEST_CLIENT, TEST_FISCAL_YEAR_2021_START, TEST_BASE_VERSION,
                                                          TEST_DELTA_VERSION, false, false, true, TEST_CORRELATION_ID, "").block());
        assertThat(throwable).isInstanceOf(WebClientResponseException.class);
        assertThat(throwable.getCause()).isInstanceOf(DataBufferLimitException.class);
        assertThat(throwable.getMessage()).contains("Exceeded limit on max bytes to buffer");
    }

    @Test
    @DisplayName("Test that no error is thrown when max response size is bigger than received file")
    void should_not_throw_error_when_max_response_size_is_bigger_than_file_size() {
        ClientEndpointConfiguration clientEndpointConfiguration = new ClientEndpointConfiguration();
        WebClient webClient = clientEndpointConfiguration.webClient(5);
        ApiClient apiClient = clientEndpointConfiguration.apiClient(webClient, wireMockServer);
        TranslationApi customTranslationApi = new TranslationApi(apiClient);

        Translation expectedTranslation = TestDataLoader.load(SIZE_OF_3KB_TRANSLATION_FILE_PATH, Translation.class);

        TestUtil.stubAcdsClientApi(TRANSLATION_ENDPOINT, TEST_CLIENT, SIZE_OF_3KB_TRANSLATION_FILE_PATH, 200, wireMockServer);

        Translation actualTranslation =
                customTranslationApi.getTranslation(TEST_CONSULTANT, TEST_CLIENT, TEST_FISCAL_YEAR_2021_START, TEST_BASE_VERSION, TEST_DELTA_VERSION,
                                                    false, false, true, TEST_CORRELATION_ID, "").block();
        assertThat(actualTranslation).isEqualTo(expectedTranslation);
    }
}