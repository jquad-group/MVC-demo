package de.datev.refsys.aggregation.processing.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "refsys.initial-load")
public class InitialLoadConfiguration {
    private Integer maxImportDurationInMs;
    private Integer parkingTimeInMs;
    private Integer inventoriesBufferSize;
    private Integer accountSumDaysBufferSize;
}
