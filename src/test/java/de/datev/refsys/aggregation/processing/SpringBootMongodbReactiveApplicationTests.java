package de.datev.refsys.aggregation.processing;

import de.datev.refsys.aggregation.processing.config.TestResilienceConfiguration;
import de.datev.refsys.aggregation.processing.configuration.TestMongoClientConfiguration;
import de.datev.refsys.aggregation.processing.configuration.TestcontainersConfiguration;
import io.cucumber.spring.CucumberContextConfiguration;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import static de.datev.refsys.aggregation.processing.util.TestUtil.TEST_PROFILE;
import static org.assertj.core.api.Assertions.assertThat;

@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ImportAutoConfiguration(TestMongoClientConfiguration.class)
@Import(TestcontainersConfiguration.class)
@ContextConfiguration(classes = { TestResilienceConfiguration.class })
@TestPropertySource(properties = {"ref-sys.circuitbreaker-enabled=false"})
@ActiveProfiles({ TEST_PROFILE })
class SpringBootMongodbReactiveApplicationTests {

    @Autowired
    CircuitBreakerRegistry registry;

    @Test
    void contextLoads() {

    }

    @Test
    void expectAllCircutibreakersDisabled() {
        registry.getAllCircuitBreakers().forEach( cb -> {
            assertThat(cb.getState()).isEqualByComparingTo(CircuitBreaker.State.DISABLED);
        });
    }

}