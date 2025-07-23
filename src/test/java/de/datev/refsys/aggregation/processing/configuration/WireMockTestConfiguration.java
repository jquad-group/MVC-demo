package de.datev.refsys.aggregation.processing.configuration;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class WireMockTestConfiguration {

    @Value("${wiremock.server.port}")
    private Integer WIREMOCK_PORT;

    @Bean(initMethod = "start", destroyMethod = "stop")
    public WireMockServer wireMockServer() {
        WireMockConfiguration wireMockConfiguration = WireMockConfiguration.wireMockConfig()
                .port(WIREMOCK_PORT);
        return new WireMockServer(wireMockConfiguration);
    }
}
