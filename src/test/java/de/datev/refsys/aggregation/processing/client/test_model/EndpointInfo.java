package de.datev.refsys.aggregation.processing.client.test_model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class EndpointInfo {
    private final String bodyUrl;
    private final String circuitBreakerName;
    private final String serviceName;
}
