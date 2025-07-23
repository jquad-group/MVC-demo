package de.datev.refsys.aggregation.processing.configuration;

import com.mongodb.MongoClientSettings;
import com.mongodb.ReadConcern;
import com.mongodb.ReadPreference;
import com.mongodb.reactivestreams.client.MongoClient;
import de.datev.refsys.aggregation.processing.config.mongo.MongoConfigUtil;
import org.bson.UuidRepresentation;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.mongo.MongoClientSettingsBuilderCustomizer;
import org.springframework.boot.autoconfigure.mongo.ReactiveMongoClientFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.TimeUnit;

import static de.datev.refsys.aggregation.processing.config.mongo.MongoConfigUtil.mongoClientSettingsBuilder;

@TestConfiguration
public class TestMongoClientConfiguration {

    @Value("${ref-sys.mongodb.max-idle-time-in-ms}")
    private Long maxIdleTimeMS;

    @Value("${ref-sys.mongodb.min-pool-size}")
    private int minPoolSize;

    @Value("${ref-sys.mongodb.max-pool-size}")
    private int maxPoolSize;

    @Bean(name = "insertMongoClient")
    public MongoClient insertMongoClient(ObjectProvider<MongoClientSettingsBuilderCustomizer> builderCustomizers, MongoClientSettings settings) {
        ReactiveMongoClientFactory factory = new ReactiveMongoClientFactory(builderCustomizers.orderedStream().toList());
        return (MongoClient) factory.createMongoClient(
                mongoClientSettingsBuilder(settings, maxIdleTimeMS, minPoolSize, maxPoolSize).retryWrites(true).build());
    }

    @Bean(name = "updateMongoClient")
    public MongoClient updateMongoClient(ObjectProvider<MongoClientSettingsBuilderCustomizer> builderCustomizers, MongoClientSettings settings) {
        ReactiveMongoClientFactory factory = new ReactiveMongoClientFactory(builderCustomizers.orderedStream().toList());
        return (MongoClient) factory.createMongoClient(
                mongoClientSettingsBuilder(settings, maxIdleTimeMS, minPoolSize, maxPoolSize).retryWrites(false).build());
    }

    public static MongoClientSettings.Builder mongoClientSettingsBuilder(MongoClientSettings settings, Long maxIdleTimeMS, Integer minPoolSize,
                                                                         Integer maxPoolSize) {
        return MongoClientSettings.builder(settings)
                                  .readConcern(ReadConcern.MAJORITY)
                                  .readPreference(ReadPreference.primaryPreferred())
                                  .uuidRepresentation(UuidRepresentation.JAVA_LEGACY)
                                  .codecRegistry(MongoConfigUtil.configureCodecRegistry())
                                  .applyToConnectionPoolSettings(connectionBuilder -> connectionBuilder
                                          .maxConnectionIdleTime(maxIdleTimeMS, TimeUnit.MILLISECONDS)
                                          .minSize(minPoolSize)
                                          .maxSize(maxPoolSize));
    }
}
