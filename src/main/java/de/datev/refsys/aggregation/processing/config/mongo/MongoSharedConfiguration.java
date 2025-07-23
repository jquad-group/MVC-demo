package de.datev.refsys.aggregation.processing.config.mongo;

import com.mongodb.ClientSessionOptions;
import com.mongodb.ReadConcern;
import com.mongodb.ReadPreference;
import com.mongodb.TransactionOptions;
import com.mongodb.WriteConcern;
import com.mongodb.reactivestreams.client.MongoClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.ReactiveMongoDatabaseFactory;
import org.springframework.data.mongodb.ReactiveMongoTransactionManager;
import org.springframework.data.mongodb.config.EnableReactiveMongoAuditing;
import org.springframework.data.mongodb.core.SimpleReactiveMongoDatabaseFactory;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.concurrent.TimeUnit;

@Configuration
@RequiredArgsConstructor
@EnableReactiveMongoAuditing
@EnableTransactionManagement
public class MongoSharedConfiguration {

    @Value("${ref-sys.mongodb.transaction-timeout-in-ms}")
    private Long transactionTimeout;

    @Value("${spring.data.mongodb.database}")
    private String databaseName;

    @Bean
    public ReactiveMongoDatabaseFactory reactiveMongoDatabaseFactory(MongoClient updateMongoClient) {
        return new SimpleReactiveMongoDatabaseFactory(updateMongoClient, databaseName);
    }

    @Bean
    public ReactiveMongoTransactionManager transactionManager(ReactiveMongoDatabaseFactory dbFactory) {
        return new ReactiveMongoTransactionManager(dbFactory);
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
