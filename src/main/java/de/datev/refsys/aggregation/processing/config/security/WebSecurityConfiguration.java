package de.datev.refsys.aggregation.processing.config.security;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.header.XFrameOptionsServerHttpHeadersWriter;
import org.springframework.security.web.server.header.XXssProtectionServerHttpHeadersWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import static de.datev.refsys.aggregation.processing.constant.ProfileConstants.CLOUD_PROFILE;
import static de.datev.refsys.aggregation.processing.constant.ProfileConstants.TEST_SECURITY_PROFILE;
import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebFluxSecurity
@Profile({ CLOUD_PROFILE, TEST_SECURITY_PROFILE })
public class WebSecurityConfiguration {

    private static final int HALF_AN_HOUR_IN_SECONDS = 1800;
    private static final int HSTS_MAX_AGE = 31_536_000;
    private static final String[] AUTH_WHITELIST = {
            // -- Actuator
            "/actuator",
            "/actuator/health",
            "/actuator/metrics",
            // -- Swagger UI
            "/api-docs/**",
            "/api-docs.yaml",
            "/v3/api-docs/**",
            "/v3/api-docs.yaml",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/swagger-resources/**",
            "/webjars/**"
            // other public endpoints of your API may be appended to this array
    };
    private static final String GRACEFUL_SWAGGER_UI_CSP = "default-src 'none'; script-src 'self' 'unsafe-inline'; " +
            "connect-src 'self'; img-src 'self' data:; style-src 'self' 'unsafe-inline'; font-src 'self'";

    @Value("${ref-sys.basic-auth-admin.username}")
    private String basicAuthAdminUsername;

    @Value("${ref-sys.basic-auth-admin.password}")
    private String basicAuthAdminPassword;

    /**
     * Replaces deprecated configuration property: <code>datev-web-security.cors</code> and its nested properties.
     *
     * @return corsConfigurationSource
     */
    @Bean("customCorsConfigurationSource")
    CorsConfigurationSource corsConfigurationSource() {
        final var configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("http://localhost:*", "*.datev.de"));
        configuration.setAllowedMethods(Arrays.asList("POST", "PUT", "PATCH", "GET", "OPTIONS", "DELETE", "HEAD"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(Duration.ofSeconds(HALF_AN_HOUR_IN_SECONDS));
        final var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /*@Order(Ordered.HIGHEST_PRECEDENCE)
    @Bean
    SecurityWebFilterChain deleteInitialLoadSecurityWebFilterChain(ServerHttpSecurity http,
                                                                   @Qualifier("customCorsConfigurationSource")
                                                                   CorsConfigurationSource corsConfigurationSource) {


        http.csrf(ServerHttpSecurity.CsrfSpec::disable)
            .cors(corsSpec -> corsSpec.configurationSource(corsConfigurationSource))
            .securityMatcher(new PathPatternParserServerWebExchangeMatcher(pattern, HttpMethod.DELETE))
            .authorizeExchange(exchanges -> exchanges.anyExchange().authenticated())
            .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
            .httpBasic(withDefaults());
        return http.build();
    }*/

    @Bean
    SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http,
                                                  @Qualifier("customCorsConfigurationSource")
                                                  CorsConfigurationSource corsConfigurationSource) {
        final String initialPattern = "/api/v1/aggregation-processing/consultants/{consultant}/clients/{client}/initial-load";
        final String updateVersionPattern = "/api/v1/aggregation-processing/consultants/{consultant}/clients/{client}/update-version";

        return http.csrf(ServerHttpSecurity.CsrfSpec::disable)
                   .headers(c -> c
                           .contentSecurityPolicy(csp -> csp.policyDirectives(GRACEFUL_SWAGGER_UI_CSP))
                           .hsts(hsts -> hsts.maxAge(Duration.ofSeconds(HSTS_MAX_AGE)).includeSubdomains(true))
                           .frameOptions(fop -> fop.mode(XFrameOptionsServerHttpHeadersWriter.Mode.DENY))
                           .xssProtection(xss -> xss.headerValue(XXssProtectionServerHttpHeadersWriter.HeaderValue.ENABLED_MODE_BLOCK)))
                   .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                   .cors(corsSpec -> corsSpec.configurationSource(corsConfigurationSource))
                   .httpBasic(withDefaults())
                   .authorizeExchange(exchanges -> exchanges
                           .pathMatchers(HttpMethod.POST, updateVersionPattern).hasRole("INTERNAL_ADMIN")
                           .pathMatchers(HttpMethod.POST, "/api/**").authenticated()
                           .pathMatchers(HttpMethod.DELETE, initialPattern).hasRole("INTERNAL_ADMIN")
                           .pathMatchers(AUTH_WHITELIST).permitAll()
                           // access control for actuator endpoints
                           .pathMatchers(HttpMethod.GET, "/actuator/info").hasAnyRole("DVZPRO_ACTUATOR")
                           // deny any other endpoint
                           .anyExchange().denyAll())
                   .oauth2ResourceServer(oAuth2ResourceServerSpec -> oAuth2ResourceServerSpec.jwt(Customizer.withDefaults()))
                   .build();
    }

    @Bean
    public MapReactiveUserDetailsService userDetailsService(PasswordEncoder encoder) {
        UserDetails user = User.withUsername(basicAuthAdminUsername)
                               .password(encoder.encode(basicAuthAdminPassword))
                               .roles("INTERNAL_ADMIN")
                               .build();
        return new MapReactiveUserDetailsService(user);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
