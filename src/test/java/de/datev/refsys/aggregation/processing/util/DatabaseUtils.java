package de.datev.refsys.aggregation.processing.util;

import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoDatabase;
import org.springframework.context.ApplicationContext;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static de.datev.refsys.aggregation.document.model.constants.CollectionConstants.CUSTOM_COLUMN_STRUCTURE_CONTENTS;
import static de.datev.refsys.aggregation.document.model.constants.CollectionConstants.CUSTOM_REPORT_STRUCTURE_CONTENTS;
import static de.datev.refsys.aggregation.document.model.constants.CollectionConstants.MASTER_DATA;
import static de.datev.refsys.aggregation.document.model.constants.CollectionConstants.MASTER_DATA_ACCOUNTS;
import static de.datev.refsys.aggregation.document.model.constants.CollectionConstants.MOVEMENT_DATA_DAYS;
import static de.datev.refsys.aggregation.document.model.constants.CollectionConstants.MOVEMENT_DATA_INVENTORIES;
import static de.datev.refsys.aggregation.document.model.constants.CollectionConstants.MOVEMENT_DATA_MONTHS;
import static de.datev.refsys.aggregation.document.model.constants.CollectionConstants.MOVEMENT_DATA_PERSON_GROUP_DAYS;
import static de.datev.refsys.aggregation.document.model.constants.CollectionConstants.MOVEMENT_DATA_PERSON_GROUP_MONTHS;
import static de.datev.refsys.aggregation.document.model.constants.CollectionConstants.STATE_DOC;
import static de.datev.refsys.aggregation.document.model.constants.FieldConstants.ACCOUNTING_REASON_ID;
import static de.datev.refsys.aggregation.document.model.constants.FieldConstants.ACCOUNT_GROUP_NUMBER;
import static de.datev.refsys.aggregation.document.model.constants.FieldConstants.ACCOUNT_NUMBER;
import static de.datev.refsys.aggregation.document.model.constants.FieldConstants.ACCOUNT_NUMBER_FROM;
import static de.datev.refsys.aggregation.document.model.constants.FieldConstants.ACCOUNT_NUMBER_TO;
import static de.datev.refsys.aggregation.document.model.constants.FieldConstants.ADDITIONAL_PARAMS;
import static de.datev.refsys.aggregation.document.model.constants.FieldConstants.ANLAG_ACCOUNTING_REASON;
import static de.datev.refsys.aggregation.document.model.constants.FieldConstants.CLIENT;
import static de.datev.refsys.aggregation.document.model.constants.FieldConstants.COLUMN_STRUCTURE_ID;
import static de.datev.refsys.aggregation.document.model.constants.FieldConstants.CONSULTANT;
import static de.datev.refsys.aggregation.document.model.constants.FieldConstants.FISCAL_YEAR;
import static de.datev.refsys.aggregation.document.model.constants.FieldConstants.INDIV_LEVEL;
import static de.datev.refsys.aggregation.document.model.constants.FieldConstants.INDIV_NO;
import static de.datev.refsys.aggregation.document.model.constants.FieldConstants.INDUSTRY_NO;
import static de.datev.refsys.aggregation.document.model.constants.FieldConstants.NATIONAL_RIGHT;
import static de.datev.refsys.aggregation.document.model.constants.FieldConstants.REPORT_STRUCTURE_ID;
import static de.datev.refsys.aggregation.document.model.constants.FieldConstants.SECTION_NO;
import static de.datev.refsys.aggregation.document.model.constants.FieldConstants.YEAR_BEGIN;
import static de.datev.refsys.aggregation.document.model.constants.FieldConstants.YEAR_END;

public class DatabaseUtils {

    public static void clearDatabase(MongoDatabase mongoDatabase) {
        Flux.merge(Mono.from(mongoDatabase.getCollection(STATE_DOC).drop()),
                   Mono.from(mongoDatabase.getCollection(MASTER_DATA).drop()),
                   Mono.from(mongoDatabase.getCollection(MASTER_DATA_ACCOUNTS).drop()),
                   Mono.from(mongoDatabase.getCollection(MOVEMENT_DATA_DAYS).drop()),
                   Mono.from(mongoDatabase.getCollection(MOVEMENT_DATA_MONTHS).drop()),
                   Mono.from(mongoDatabase.getCollection(MOVEMENT_DATA_PERSON_GROUP_DAYS).drop()),
                   Mono.from(mongoDatabase.getCollection(MOVEMENT_DATA_PERSON_GROUP_MONTHS).drop()),
                   Mono.from(mongoDatabase.getCollection(MOVEMENT_DATA_INVENTORIES).drop()),
                   Mono.from(mongoDatabase.getCollection(CUSTOM_COLUMN_STRUCTURE_CONTENTS).drop()),
                   Mono.from(mongoDatabase.getCollection(CUSTOM_REPORT_STRUCTURE_CONTENTS).drop()))
            .collectList().block();
    }

    public static void createIndexes(MongoDatabase mongoDatabase) {
        IndexOptions uniqueIndexOptions = new IndexOptions().unique(true);
        Flux.merge(Mono.from(
                           mongoDatabase.getCollection(STATE_DOC).createIndex(Indexes.ascending(CONSULTANT, CLIENT, YEAR_BEGIN, YEAR_END),
                                                                              uniqueIndexOptions)),
                   Mono.from(mongoDatabase.getCollection(MASTER_DATA)
                                          .createIndex(Indexes.ascending(CONSULTANT, CLIENT, YEAR_BEGIN, YEAR_END), uniqueIndexOptions)),
                   Mono.from(mongoDatabase.getCollection(MASTER_DATA_ACCOUNTS)
                                          .createIndex(
                                                  Indexes.ascending(CONSULTANT, CLIENT, YEAR_BEGIN, YEAR_END, ACCOUNT_NUMBER_FROM, ACCOUNT_NUMBER_TO),
                                                  uniqueIndexOptions)),
                   Mono.from(mongoDatabase.getCollection(MOVEMENT_DATA_DAYS)
                                          .createIndex(Indexes.compoundIndex(Indexes.ascending(CONSULTANT, CLIENT, FISCAL_YEAR),
                                                                             Indexes.descending(ACCOUNT_NUMBER),
                                                                             Indexes.ascending(ACCOUNTING_REASON_ID, ADDITIONAL_PARAMS)),
                                                       uniqueIndexOptions)),
                   Mono.from(mongoDatabase.getCollection(MOVEMENT_DATA_MONTHS)
                                          .createIndex(Indexes.compoundIndex(Indexes.ascending(CONSULTANT, CLIENT, FISCAL_YEAR),
                                                                             Indexes.descending(ACCOUNT_NUMBER),
                                                                             Indexes.ascending(ACCOUNTING_REASON_ID, ADDITIONAL_PARAMS)),
                                                       uniqueIndexOptions)),
                   Mono.from(mongoDatabase.getCollection(MOVEMENT_DATA_PERSON_GROUP_DAYS)
                                          .createIndex(Indexes.compoundIndex(Indexes.ascending(CONSULTANT, CLIENT, FISCAL_YEAR),
                                                                             Indexes.descending(ACCOUNT_GROUP_NUMBER),
                                                                             Indexes.ascending(ACCOUNTING_REASON_ID, ADDITIONAL_PARAMS)),
                                                       uniqueIndexOptions)),
                   Mono.from(mongoDatabase.getCollection(MOVEMENT_DATA_PERSON_GROUP_MONTHS)
                                          .createIndex(Indexes.compoundIndex(Indexes.ascending(CONSULTANT, CLIENT, FISCAL_YEAR),
                                                                             Indexes.descending(ACCOUNT_GROUP_NUMBER),
                                                                             Indexes.ascending(ACCOUNTING_REASON_ID, ADDITIONAL_PARAMS)),
                                                       uniqueIndexOptions)),
                   Mono.from(mongoDatabase.getCollection(MOVEMENT_DATA_INVENTORIES)
                                          .createIndex(Indexes.compoundIndex(Indexes.ascending(CONSULTANT, CLIENT, FISCAL_YEAR),
                                                                             Indexes.descending(ACCOUNT_NUMBER),
                                                                             Indexes.ascending(ANLAG_ACCOUNTING_REASON)), uniqueIndexOptions)),
                   Mono.from(mongoDatabase.getCollection(CUSTOM_COLUMN_STRUCTURE_CONTENTS)
                                          .createIndex(Indexes.compoundIndex(
                                                  Indexes.ascending(CONSULTANT, CLIENT, YEAR_BEGIN, YEAR_END, COLUMN_STRUCTURE_ID, INDUSTRY_NO,
                                                                    SECTION_NO, INDIV_NO, NATIONAL_RIGHT, INDIV_LEVEL)), uniqueIndexOptions)),
                   Mono.from(mongoDatabase.getCollection(CUSTOM_REPORT_STRUCTURE_CONTENTS)
                                          .createIndex(Indexes.compoundIndex(
                                                  Indexes.ascending(CONSULTANT, CLIENT, YEAR_BEGIN, YEAR_END, REPORT_STRUCTURE_ID, INDUSTRY_NO,
                                                                    SECTION_NO, INDIV_NO, NATIONAL_RIGHT, INDIV_LEVEL)), uniqueIndexOptions)))
            .collectList().block();
    }

    public static MongoDatabase getDatabase(ApplicationContext applicationContext) {
        String databaseName = applicationContext.getEnvironment().getProperty("spring.data.mongodb.database");
        assert databaseName != null;
        return applicationContext.getBean("insertMongoClient", MongoClient.class).getDatabase(databaseName);
    }
}
