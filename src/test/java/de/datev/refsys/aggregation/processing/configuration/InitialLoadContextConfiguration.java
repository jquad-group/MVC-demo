package de.datev.refsys.aggregation.processing.configuration;

import de.datev.refsys.aggregation.processing.config.InitialLoadConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class InitialLoadContextConfiguration {

    @Value("${ref-sys.initial-load.max-import-duration-in-ms}")
    private Integer maxImportDurationInMs;

    @Value("${ref-sys.initial-load.parking-time-in-ms}")
    private Integer parkingTimeInMs;

    @Value("${ref-sys.initial-load.inventories-buffer-size}")
    private Integer inventoriesBufferSize;

    @Value("${ref-sys.initial-load.account-sum-days-buffer-size}")
    private Integer accountSumDaysBufferSize;

    @Bean
    public InitialLoadConfiguration initialLoadConfiguration() {
        InitialLoadConfiguration initialLoadConfiguration = new InitialLoadConfiguration();
        initialLoadConfiguration.setMaxImportDurationInMs(maxImportDurationInMs);
        initialLoadConfiguration.setParkingTimeInMs(parkingTimeInMs);
        initialLoadConfiguration.setInventoriesBufferSize(inventoriesBufferSize);
        initialLoadConfiguration.setAccountSumDaysBufferSize(accountSumDaysBufferSize);
        return initialLoadConfiguration;
    }
}
