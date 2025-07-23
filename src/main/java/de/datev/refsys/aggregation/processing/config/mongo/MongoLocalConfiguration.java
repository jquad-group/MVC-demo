package de.datev.refsys.aggregation.processing.config.mongo;

import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import de.datev.refsys.aggregation.processing.constant.ProfileConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import static de.datev.refsys.aggregation.processing.config.mongo.MongoConfigUtil.mongoClientSettingsBuilder;

@Configuration
@RequiredArgsConstructor
@Profile(ProfileConstants.LOCAL_PROFILE)
@Slf4j
public class MongoLocalConfiguration {
    @Value("${spring.data.mongodb.uri}")
    private String uri;

    @Value("${ref-sys.mongodb.max-idle-time-in-ms}")
    private Long maxIdleTimeMS;

    @Value("${ref-sys.mongodb.min-pool-size}")
    private int minPoolSize;

    @Value("${ref-sys.mongodb.max-pool-size}")
    private int maxPoolSize;

    @Bean
    public MongoClient insertMongoClient() {
        return MongoClients.create(mongoClientSettingsBuilder(uri, maxIdleTimeMS, minPoolSize, maxPoolSize).retryWrites(true).build());
    }

    @Bean
    public MongoClient updateMongoClient() {
        return MongoClients.create(mongoClientSettingsBuilder(uri, maxIdleTimeMS, minPoolSize, maxPoolSize).retryWrites(false).build());
    }
}
