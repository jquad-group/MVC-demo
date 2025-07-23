package de.datev.refsys.aggregation.processing.boundry.controller;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.google.gson.Gson;
import de.datev.refsys.aggregation.processing.config.TestResilienceConfiguration;
import de.datev.refsys.aggregation.processing.config.security.WebSecurityConfiguration;
import de.datev.refsys.aggregation.processing.configuration.TestApplicationInitializer;
import de.datev.refsys.aggregation.processing.configuration.WireMockTestConfiguration;
import de.datev.refsys.aggregation.processing.service.CommonImportService;
import de.datev.refsys.aggregation.processing.util.TestDataLoader;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.beans.BeansEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.security.reactive.ReactiveManagementWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.util.UriBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static de.datev.refsys.aggregation.processing.constant.ProfileConstants.TEST_SECURITY_PROFILE;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_CLIENT;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_CONSULTANT;
import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_FISCAL_YEAR_2021_START;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@WebFluxTest(value = InitialLoadController.class)
@EnableWebFluxSecurity
@ImportAutoConfiguration({ ReactiveManagementWebSecurityAutoConfiguration.class,
        EndpointAutoConfiguration.class, BeansEndpointAutoConfiguration.class })
@Import({ WebSecurityConfiguration.class, ReactiveManagementWebSecurityAutoConfiguration.class, EndpointAutoConfiguration.class,
        BeansEndpointAutoConfiguration.class })
@ContextConfiguration(initializers = TestApplicationInitializer.class, classes = { WireMockTestConfiguration.class, TestResilienceConfiguration.class})
@ActiveProfiles(TEST_SECURITY_PROFILE)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InitialLoadControllerSecurityIT {
    @Autowired
    private WireMockServer wireMockServer;

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private CommonImportService commonImportService;

    private final Function<UriBuilder, URI> uriFunction =
            uriBuilder -> uriBuilder.path("/api/v1/aggregation-processing/consultants/{consultant}/clients/{client}/initial-load")
                                    .queryParam("fiscal-year", TEST_FISCAL_YEAR_2021_START)
                                    .build(TEST_CONSULTANT, TEST_CLIENT);

    @BeforeEach
    void setUp() throws Exception {
        wireMockServer.stubFor(get(urlPathEqualTo("/certs"))
                                       .willReturn(aResponse()
                                                           .withHeader("Content-Type", "application/json")
                                                           .withBody(CreateJwkSet("RSA", TestDataLoader.load("rsa-key/PublicKey.pem")))
                                                           .withStatus(200)));
    }

    @AfterAll
    public void cleanUp() {
        wireMockServer.resetAll();
        wireMockServer.shutdown();
    }

    @Test
    void should_return_2xx_with_valid_token() {
        when(commonImportService.doFireAndForgetFullImport(any(), any(), any(), any(), any())).thenReturn(Mono.empty());
        String jwtToken = generateToken("RSA", TestDataLoader.load("rsa-key/PrivateKey.pem"));
        webTestClient
                .post()
                .uri(uriFunction)
                .header(AUTHORIZATION, "Bearer " + jwtToken)
                .exchange()
                .expectStatus().is2xxSuccessful();
    }

    @Test
    void should_return_401_for_delete_initial_load() {
        when(commonImportService.doFireAndForgetFullImport(any(), any(), any(), any(), any())).thenReturn(Mono.empty());
        String jwtToken = generateToken("RSA", TestDataLoader.load("rsa-key/PrivateKey.pem"));
        webTestClient
                .delete()
                .uri(uriFunction)
                .header(AUTHORIZATION, "Bearer " + jwtToken)
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void should_return_401_with_invalid_token() {
        webTestClient
                .post()
                .uri(uriFunction)
                .header(AUTHORIZATION, "Bearer " + "invalid-token")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void should_return_401_without_bearer_token() {
        webTestClient
                .post()
                .uri(uriFunction)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    public static String CreateJwkSet(String signType, String signKey) throws Exception {
        RSAPublicKey rsa = (RSAPublicKey) getPublicKey(signType, signKey);
        Map<String, Object> values = new HashMap<>();
        values.put("kty", rsa.getAlgorithm());
        values.put("kid", "someuniqueid");
        values.put("n", Base64.getUrlEncoder().encodeToString(rsa.getModulus().toByteArray()));
        values.put("e", Base64.getUrlEncoder().encodeToString(rsa.getPublicExponent().toByteArray()));
        values.put("alg", "RS256");
        values.put("use", "sig");
        return new Gson().toJson(Map.of("keys", List.of(values)));
    }

    public static String generateToken(String signType, String signKey) {
        String token = null;
        try {
            Map<String, Object> claims = new HashMap<>();
            // put your information into claim
            claims.put("id", "xxx");
            claims.put("created", new Date());
            token = Jwts.builder()
                        .claims(claims)
                        .signWith(SignatureAlgorithm.RS256, getPrivateKey(signType, signKey))
                        .compact();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return token;
    }

    public static PublicKey getPublicKey(String signType, String signKey) throws Exception {
        KeyFactory keyFactory = KeyFactory.getInstance(signType);
        byte[] encodedKey = signKey.getBytes();
        encodedKey = Base64.getDecoder().decode(encodedKey);
        return keyFactory.generatePublic(new X509EncodedKeySpec(encodedKey));
    }

    public static PrivateKey getPrivateKey(String signType, String signKey) throws Exception {
        KeyFactory keyFactory = KeyFactory.getInstance(signType);
        byte[] encodedKey = signKey.getBytes();
        encodedKey = Base64.getDecoder().decode(encodedKey);
        return keyFactory.generatePrivate(new PKCS8EncodedKeySpec(encodedKey));
    }
}
