package de.datev.refsys.aggregation.processing.featuretest;

import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoCollection;
import de.datev.refsys.aggregation.document.model.MasterData;
import de.datev.refsys.aggregation.document.model.MovementDataDay;
import de.datev.refsys.aggregation.document.model.MovementDataMonth;
import de.datev.refsys.aggregation.document.model.MovementDataPersonGroupDay;
import de.datev.refsys.aggregation.document.model.MovementDataPersonGroupMonth;
import de.datev.refsys.aggregation.document.model.StateDoc;
import de.datev.refsys.aggregation.document.model.constants.CollectionConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MongoDBTestConfiguration {

    @Qualifier("insertMongoClient")
    @Autowired
    private MongoClient mongoClient;

    @Value("${spring.data.mongodb.database}")
    private String databaseName;

    @Bean
    public MongoCollection<MovementDataDay> movementDataDayMongoCollection() {
        return mongoClient.getDatabase(databaseName).getCollection(CollectionConstants.MOVEMENT_DATA_DAYS, MovementDataDay.class);
    }

    @Bean
    public MongoCollection<MovementDataMonth> movementDataMonthMongoCollection() {
        return mongoClient.getDatabase(databaseName).getCollection(CollectionConstants.MOVEMENT_DATA_MONTHS, MovementDataMonth.class);
    }

    @Bean
    public MongoCollection<MasterData> masterDataMongoCollection() {
        return mongoClient.getDatabase(databaseName).getCollection(CollectionConstants.MASTER_DATA, MasterData.class);
    }

    @Bean
    public MongoCollection<MovementDataPersonGroupDay> movementDataPersonenGroupDayDayMongoCollection() {
        return mongoClient.getDatabase(databaseName).getCollection(CollectionConstants.MOVEMENT_DATA_PERSON_GROUP_DAYS, MovementDataPersonGroupDay.class);
    }

    @Bean
    public MongoCollection<MovementDataPersonGroupMonth> movementDataPersonenGroupDayMonthMongoCollection() {
        return mongoClient.getDatabase(databaseName).getCollection(CollectionConstants.MOVEMENT_DATA_PERSON_GROUP_MONTHS, MovementDataPersonGroupMonth.class);
    }

    @Bean
    public MongoCollection<StateDoc> stateDocMongoCollection() {
        return mongoClient.getDatabase(databaseName).getCollection(CollectionConstants.STATE_DOC, StateDoc.class);
    }
}
