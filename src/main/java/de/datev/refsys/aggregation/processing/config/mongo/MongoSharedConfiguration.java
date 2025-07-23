package de.datev.refsys.aggregation.processing.config.mongo;

import com.mongodb.ClientSessionOptions;
import com.mongodb.ReadConcern;
import com.mongodb.ReadPreference;
import com.mongodb.TransactionOptions;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoDatabaseFactory;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.concurrent.TimeUnit;

@Configuration
@RequiredArgsConstructor
@EnableMongoAuditing
@EnableTransactionManagement
public class MongoSharedConfiguration {

    @Value("${ref-sys.mongodb.transaction-timeout-in-ms}")
    private Long transactionTimeout;

    @Value("${spring.data.mongodb.database}")
    private String databaseName;

    @Bean
    public MongoDatabaseFactory mongoDatabaseFactory(MongoClient updateMongoClient) {
        return new SimpleMongoDatabaseFactory(updateMongoClient, databaseName);
    }

    @Bean
    public MongoTransactionManager transactionManager(MongoDatabaseFactory dbFactory) {
        return new MongoTransactionManager(dbFactory);
    }

    @Bean
    public MongoTemplate mongoTemplate(MongoDatabaseFactory dbFactory) {
        return new MongoTemplate(dbFactory);
    }

    @Bean
    public ClientSessionOptions clientSessionOptions() {
        return ClientSessionOptions.builder()
                .causallyConsistent(true)
                .defaultTransactionOptions(TransactionOptions.builder()
                        .writeConcern(WriteConcern.MAJORITY)
                        .readConcern(ReadConcern.MAJORITY)
                        .readPreference(ReadPreference.primary())
                        .maxCommitTime(transactionTimeout, TimeUnit.MILLISECONDS)
                        .build())
                .build();
    }
}
