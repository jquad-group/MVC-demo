package de.datev.refsys.aggregation.processing.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.result.InsertManyResult;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoCollection;
import com.mongodb.reactivestreams.client.MongoDatabase;
import de.datev.refsys.aggregation.document.model.CustomColumnStructureContent;
import de.datev.refsys.aggregation.document.model.CustomReportStructureContent;
import de.datev.refsys.aggregation.document.model.MasterData;
import de.datev.refsys.aggregation.document.model.MasterDataAccount;
import de.datev.refsys.aggregation.document.model.MovementDataDay;
import de.datev.refsys.aggregation.document.model.MovementDataInventory;
import de.datev.refsys.aggregation.document.model.MovementDataMonth;
import de.datev.refsys.aggregation.document.model.MovementDataPersonGroupDay;
import de.datev.refsys.aggregation.document.model.MovementDataPersonGroupMonth;
import de.datev.refsys.aggregation.document.model.StateDoc;
import de.datev.refsys.aggregation.processing.model.MongoIndex;
import lombok.SneakyThrows;
import org.bson.Document;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

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
import static de.datev.refsys.aggregation.document.model.constants.FieldConstants.ID;
import static de.datev.refsys.aggregation.document.model.constants.FieldConstants.INDIV_LEVEL;
import static de.datev.refsys.aggregation.document.model.constants.FieldConstants.INDIV_NO;
import static de.datev.refsys.aggregation.document.model.constants.FieldConstants.INDUSTRY_NO;
import static de.datev.refsys.aggregation.document.model.constants.FieldConstants.NATIONAL_RIGHT;
import static de.datev.refsys.aggregation.document.model.constants.FieldConstants.REPORT_STRUCTURE_ID;
import static de.datev.refsys.aggregation.document.model.constants.FieldConstants.SECTION_NO;
import static de.datev.refsys.aggregation.document.model.constants.FieldConstants.YEAR_BEGIN;
import static de.datev.refsys.aggregation.document.model.constants.FieldConstants.YEAR_END;
import static org.assertj.core.api.Assertions.assertThat;

@Component
public class MongoHelperService {
    private final MongoDatabase mongoDatabase;
    private final MongoCollection<StateDoc> stateDocCollection;
    private final MongoCollection<MasterData> masterDataCollection;
    private final MongoCollection<MasterDataAccount> masterDataAccountCollection;
    private final MongoCollection<MovementDataDay> movementDataDayCollection;
    private final MongoCollection<MovementDataMonth> movementDataMonthCollection;
    private final MongoCollection<MovementDataPersonGroupDay> personGroupDayCollection;
    private final MongoCollection<MovementDataPersonGroupMonth> personGroupMonthCollection;
    private final MongoCollection<MovementDataInventory> movementDataInventoryCollection;
    private final MongoCollection<CustomReportStructureContent> customReportStructureContentCollection;
    private final MongoCollection<CustomColumnStructureContent> customColumnStructureContentCollection;
    
    public MongoHelperService(MongoClient insertMongoClient, @Value("${spring.data.mongodb.database}") String databaseName) {
        this.mongoDatabase = insertMongoClient.getDatabase(databaseName);
        this.stateDocCollection = mongoDatabase.getCollection(STATE_DOC, StateDoc.class);
        this.masterDataCollection = mongoDatabase.getCollection(MASTER_DATA, MasterData.class);
        this.masterDataAccountCollection = mongoDatabase.getCollection(MASTER_DATA_ACCOUNTS, MasterDataAccount.class);
        this.movementDataDayCollection = mongoDatabase.getCollection(MOVEMENT_DATA_DAYS, MovementDataDay.class);
        this.movementDataMonthCollection = mongoDatabase.getCollection(MOVEMENT_DATA_MONTHS, MovementDataMonth.class);
        this.personGroupDayCollection = mongoDatabase.getCollection(MOVEMENT_DATA_PERSON_GROUP_DAYS, MovementDataPersonGroupDay.class);
        this.personGroupMonthCollection = mongoDatabase.getCollection(MOVEMENT_DATA_PERSON_GROUP_MONTHS, MovementDataPersonGroupMonth.class);
        this.movementDataInventoryCollection = mongoDatabase.getCollection(MOVEMENT_DATA_INVENTORIES, MovementDataInventory.class);
        this.customReportStructureContentCollection = mongoDatabase.getCollection(CUSTOM_REPORT_STRUCTURE_CONTENTS, CustomReportStructureContent.class);
        this.customColumnStructureContentCollection = mongoDatabase.getCollection(CUSTOM_COLUMN_STRUCTURE_CONTENTS, CustomColumnStructureContent.class);
    }

    //###################################################
    //insertOne methods
    //###################################################

    public InsertOneResult insertOneStateDoc(StateDoc dataToInsert) {
        return Mono.from(stateDocCollection.insertOne(dataToInsert)).block();
    }

    public InsertOneResult insertOneMasterData(MasterData dataToInsert) {
        return Mono.from(masterDataCollection.insertOne(dataToInsert)).block();
    }

    public <T> InsertOneResult insertOneGeneric(Object dataToInsert, String collectionName, Class<T> clazz) {
        T castedData = (T) dataToInsert;
        return Mono.from(mongoDatabase.getCollection(collectionName, clazz).insertOne(castedData)).block();
    }

    //###################################################
    //insertManyObjects methods
    //###################################################

    public InsertManyResult insertManyMasterDataAccounts(List<MasterDataAccount> dataToInsert) {
        return Mono.from(masterDataAccountCollection.insertMany(dataToInsert)).block();
    }

    public InsertManyResult insertManyMasterData(List<MasterData> dataToInsert) {
        return Mono.from(masterDataCollection.insertMany(dataToInsert)).block();
    }

    public InsertManyResult insertManyMovementDataDays(List<MovementDataDay> dataToInsert) {
        return Mono.from(movementDataDayCollection.insertMany(dataToInsert)).block();
    }

    public InsertManyResult insertManyMovementDataInventories(List<MovementDataInventory> dataToInsert) {
        return Mono.from(movementDataInventoryCollection.insertMany(dataToInsert)).block();
    }

    public InsertManyResult insertManyMovementDataMonths(List<MovementDataMonth> dataToInsert) {
        return Mono.from(movementDataMonthCollection.insertMany(dataToInsert)).block();
    }

    public InsertManyResult insertManyMovementDataPersonGroupDays(List<MovementDataPersonGroupDay> dataToInsert) {
        return Mono.from(personGroupDayCollection.insertMany(dataToInsert)).block();
    }

    public InsertManyResult insertManyMovementDataPersonGroupMonths(List<MovementDataPersonGroupMonth> dataToInsert) {
        return Mono.from(personGroupMonthCollection.insertMany(dataToInsert)).block();
    }

    public InsertManyResult insertManyStateDocs(List<StateDoc> dataToInsert) {
        return Mono.from(stateDocCollection.insertMany(dataToInsert)).block();
    }

    public InsertManyResult insertManyCustomColumnStructureContents(List<CustomColumnStructureContent> dataToInsert) {
        return Mono.from(customColumnStructureContentCollection.insertMany(dataToInsert)).block();
    }

    public InsertManyResult insertManyCustomReportStructureContents(List<CustomReportStructureContent> dataToInsert) {
        return Mono.from(customReportStructureContentCollection.insertMany(dataToInsert)).block();
    }

    //###################################################
    //insertManyDocuments methods
    //###################################################

    public Publisher<InsertManyResult> insertManyMasterDataAccountDocuments(List<Document> dataToInsert) {
        return Mono.from(mongoDatabase.getCollection(MASTER_DATA_ACCOUNTS).insertMany(dataToInsert));
    }

    public Publisher<InsertManyResult> insertManyMasterDataDocuments(List<Document> dataToInsert) {
        return Mono.from(mongoDatabase.getCollection(MASTER_DATA).insertMany(dataToInsert));
    }

    public Publisher<InsertManyResult> insertManyMovementDataDayDocuments(List<Document> dataToInsert) {
        return Mono.from(mongoDatabase.getCollection(MOVEMENT_DATA_DAYS).insertMany(dataToInsert));
    }

    public Publisher<InsertManyResult> insertManyMovementDataInventoryDocuments(List<Document> dataToInsert) {
        return Mono.from(mongoDatabase.getCollection(MOVEMENT_DATA_INVENTORIES).insertMany(dataToInsert));
    }

    public Publisher<InsertManyResult> insertManyMovementDataMonthsDocuments(List<Document> dataToInsert) {
        return Mono.from(mongoDatabase.getCollection(MOVEMENT_DATA_MONTHS).insertMany(dataToInsert));
    }

    public Publisher<InsertManyResult> insertManyMovementDataPersonGroupDayDocuments(List<Document> dataToInsert) {
        return Mono.from(mongoDatabase.getCollection(MOVEMENT_DATA_PERSON_GROUP_DAYS).insertMany(dataToInsert));
    }

    public Publisher<InsertManyResult> insertManyMovementDataPersonGroupMonthDocuments(List<Document> dataToInsert) {
        return Mono.from(mongoDatabase.getCollection(MOVEMENT_DATA_PERSON_GROUP_MONTHS).insertMany(dataToInsert));
    }

    public Publisher<InsertManyResult> insertManyStateDocDocuments(List<Document> dataToInsert) {
        return Mono.from(mongoDatabase.getCollection(STATE_DOC).insertMany(dataToInsert));
    }

    public Publisher<InsertManyResult> insertManyCustomColumnStructureContentDocuments(List<Document> dataToInsert) {
        return Mono.from(mongoDatabase.getCollection(CUSTOM_COLUMN_STRUCTURE_CONTENTS).insertMany(dataToInsert));
    }

    public Publisher<InsertManyResult> insertManyCustomReportStructureContentDocuments(List<Document> dataToInsert) {
        return Mono.from(mongoDatabase.getCollection(CUSTOM_REPORT_STRUCTURE_CONTENTS).insertMany(dataToInsert));
    }

    //###################################################
    //findAll methods
    //###################################################

    public List<MasterDataAccount> findAllMasterDataAccounts() {
        return Flux.from(masterDataAccountCollection.find()).collectList().block();
    }

    public List<MasterData> findAllMasterData() {
        return Flux.from(masterDataCollection.find()).collectList().block();
    }

    public List<MovementDataDay> findAllMovementDataDays() {
        return Flux.from(movementDataDayCollection.find()).collectList().block();
    }

    public List<MovementDataInventory> findAllMovementDataInventories() {
        return Flux.from(movementDataInventoryCollection.find()).collectList().block();
    }

    public List<MovementDataMonth> findAllMovementDataMonths() {
        return Flux.from(movementDataMonthCollection.find()).collectList().block();
    }

    public List<MovementDataPersonGroupDay> findAllMovementDataPersonGroupDays() {
        return Flux.from(personGroupDayCollection.find()).collectList().block();
    }

    public List<MovementDataPersonGroupMonth> findAllMovementDataPersonGroupMonths() {
        return Flux.from(personGroupMonthCollection.find()).collectList().block();
    }

    public List<StateDoc> findAllStateDocs() {
        return Flux.from(stateDocCollection.find()).collectList().block();
    }

    public List<CustomColumnStructureContent> findAllCustomColumnStructureContents() {
        return Flux.from(customColumnStructureContentCollection.find()).collectList().block();
    }

    public List<CustomReportStructureContent> findAllCustomReportStructureContents() {
        return Flux.from(customReportStructureContentCollection.find()).collectList().block();
    }

    //###################################################
    //findallObjectsDocument methods
    //###################################################

    public List<Document> findAllMasterDataAccountsDocument() {
        return Flux.from(mongoDatabase.getCollection(MASTER_DATA_ACCOUNTS).find()).collectList().block();
    }

    public List<Document> findAllMasterDataDocument() {
        return Flux.from(mongoDatabase.getCollection(MASTER_DATA).find()).collectList().block();
    }

    public List<Document> findAllMovementDataDaysDocument() {
        return Flux.from(mongoDatabase.getCollection(MOVEMENT_DATA_DAYS).find()).collectList().block();
    }

    public List<Document> findAllMovementDataInventoriesDocument() {
        return Flux.from(mongoDatabase.getCollection(MOVEMENT_DATA_INVENTORIES).find()).collectList().block();
    }

    public List<Document> findAllMovementDataMonthsDocument() {
        return Flux.from(mongoDatabase.getCollection(MOVEMENT_DATA_MONTHS).find()).collectList().block();
    }

    public List<Document> findAllMovementDataPersonGroupDaysDocument() {
        return Flux.from(mongoDatabase.getCollection(MOVEMENT_DATA_PERSON_GROUP_DAYS).find()).collectList().block();
    }

    public List<Document> findAllMovementDataPersonGroupMonthsDocument() {
        return Flux.from(mongoDatabase.getCollection(MOVEMENT_DATA_PERSON_GROUP_MONTHS).find()).collectList().block();
    }

    public List<Document> findAllStateDocsDocument() {
        return Flux.from(mongoDatabase.getCollection(STATE_DOC).find()).collectList().block();
    }

    public List<Document> findAllCustomColumnStructureContentsDocument() {
        return Flux.from(mongoDatabase.getCollection(CUSTOM_COLUMN_STRUCTURE_CONTENTS).find()).collectList().block();
    }

    public List<Document> findAllCustomReportStructureContentsDocument() {
        return Flux.from(mongoDatabase.getCollection(CUSTOM_REPORT_STRUCTURE_CONTENTS).find()).collectList().block();
    }

    //###################################################
    //deleteAll methods
    //###################################################

    public void deleteAllMasterDataAccounts() {
        Mono.from(masterDataAccountCollection.deleteMany(new Document()))
            .block();
    }

    public void deleteAllMasterData() {
        Mono.from(masterDataCollection.deleteMany(new Document())).block();
    }

    public void deleteAllMovementDataDays() {
        Mono.from(movementDataDayCollection.deleteMany(new Document()))
            .block();
    }

    public void deleteAllMovementDataInventories() {
        Mono.from(movementDataInventoryCollection.deleteMany(new Document())).block();
    }

    public void deleteAllMovementDataMonths() {
        Mono.from(movementDataMonthCollection.deleteMany(new Document())).block();
    }

    public void deleteAllMovementDataPersonGroupDays() {
        Mono.from(personGroupDayCollection.deleteMany(new Document())).block();
    }

    public void deleteAllDataPersonGroupMonths() {
        Mono.from(personGroupMonthCollection.deleteMany(new Document())).block();
    }

    public void deleteAllStateDocs() {
        Mono.from(stateDocCollection.deleteMany(new Document())).block();
    }

    public void deleteAllCustomColumnStructureContents() {
        Mono.from(customColumnStructureContentCollection.deleteMany(new Document())).block();
    }

    public void deleteAllCustomReportStructureContents() {
        Mono.from(customReportStructureContentCollection.deleteMany(new Document())).block();
    }

    //###################################################
    //List indexes methods
    //###################################################

    public List<Document> listStateDocIndexes() {
        return Flux.from(stateDocCollection.listIndexes()).collectList().block();
    }

    public List<Document> listMasterDataIndexes() {
        return Flux.from(masterDataCollection.listIndexes()).collectList().block();
    }

    public List<Document> listMasterDataAccountsIndexes() {
        return Flux.from(masterDataAccountCollection.listIndexes()).collectList().block();
    }

    public List<Document> listMovementDataDaysIndexes() {
        return Flux.from(movementDataDayCollection.listIndexes()).collectList().block();
    }

    public List<Document> listMovementDataMonthsIndexes() {
        return Flux.from(movementDataMonthCollection.listIndexes()).collectList().block();
    }

    public List<Document> listMovementDataPersonGroupDaysIndexes() {
        return Flux.from(personGroupDayCollection.listIndexes()).collectList().block();
    }

    public List<Document> listMovementDataPersonGroupMonthsIndexes() {
        return Flux.from(personGroupMonthCollection.listIndexes()).collectList().block();
    }

    public List<Document> listMovementDataInventoriesIndexes() {
        return Flux.from(movementDataInventoryCollection.listIndexes()).collectList().block();
    }

    public List<Document> listCustomColumnContentIndexes() {
        return Flux.from(customColumnStructureContentCollection.listIndexes()).collectList().block();
    }

    public List<Document> listCustomReportContentIndexes() {
        return Flux.from(customReportStructureContentCollection.listIndexes()).collectList().block();
    }

    //###################################################
    //Verify indexes methods
    //###################################################

    @SneakyThrows
    public MongoIndex deserializeMongoIndex(String json) {
        return new ObjectMapper().readValue(json, MongoIndex.class);
    }

    public void verifyIdIndex(MongoIndex mongoIndex) {
        assertThat(mongoIndex).isNotNull();
        assertThat(mongoIndex.getKey()).hasSize(1);
        assertThat(mongoIndex.getKey().get(ID)).isNotNull();
        assertThat(mongoIndex.getKey()).containsEntry(ID, 1);
    }

    public void verifyIndexForStateDocAndMasterData(MongoIndex mongoIndex) {
        assertThat(mongoIndex).isNotNull();
        assertThat(mongoIndex.getKey()).hasSize(4);
        assertThat(mongoIndex.getKey()).containsEntry(CONSULTANT, 1);
        assertThat(mongoIndex.getKey()).containsEntry(CLIENT, 1);
        assertThat(mongoIndex.getKey()).containsEntry(YEAR_BEGIN, 1);
        assertThat(mongoIndex.getKey()).containsEntry(YEAR_END, 1);
    }

    public void verifyIndexForMasterDataAccount(MongoIndex mongoIndex) {
        assertThat(mongoIndex).isNotNull();
        assertThat(mongoIndex.getKey()).hasSize(6);
        assertThat(mongoIndex.getKey()).containsEntry(CONSULTANT, 1);
        assertThat(mongoIndex.getKey()).containsEntry(CLIENT, 1);
        assertThat(mongoIndex.getKey()).containsEntry(YEAR_BEGIN, 1);
        assertThat(mongoIndex.getKey()).containsEntry(YEAR_END, 1);
        assertThat(mongoIndex.getKey()).containsEntry(ACCOUNT_NUMBER_FROM, 1);
        assertThat(mongoIndex.getKey()).containsEntry(ACCOUNT_NUMBER_TO, 1);
    }

    public void verifyIndexForMovementDataDayAndMonth(MongoIndex mongoIndex) {
        assertThat(mongoIndex).isNotNull();
        assertThat(mongoIndex.getKey()).hasSize(6);
        assertThat(mongoIndex.getKey()).containsEntry(CONSULTANT, 1);
        assertThat(mongoIndex.getKey()).containsEntry(CLIENT, 1);
        assertThat(mongoIndex.getKey()).containsEntry(FISCAL_YEAR, 1);
        assertThat(mongoIndex.getKey()).containsEntry(ACCOUNT_NUMBER, -1);
        assertThat(mongoIndex.getKey()).containsEntry(ACCOUNTING_REASON_ID, 1);
        assertThat(mongoIndex.getKey()).containsEntry(ADDITIONAL_PARAMS, 1);
    }

    public void verifyIndexForMovementDataPersonGroupDayAndMonth(MongoIndex mongoIndex) {
        assertThat(mongoIndex).isNotNull();
        assertThat(mongoIndex.getKey()).hasSize(6);
        assertThat(mongoIndex.getKey()).containsEntry(CONSULTANT, 1);
        assertThat(mongoIndex.getKey()).containsEntry(CLIENT, 1);
        assertThat(mongoIndex.getKey()).containsEntry(FISCAL_YEAR, 1);
        assertThat(mongoIndex.getKey()).containsEntry(ACCOUNT_GROUP_NUMBER, -1);
        assertThat(mongoIndex.getKey()).containsEntry(ACCOUNTING_REASON_ID, 1);
        assertThat(mongoIndex.getKey()).containsEntry(ADDITIONAL_PARAMS, 1);
    }

    public void verifyIndexForMovementDataInventories(MongoIndex mongoIndex) {
        assertThat(mongoIndex).isNotNull();
        assertThat(mongoIndex.getKey()).hasSize(5);
        assertThat(mongoIndex.getKey()).containsEntry(CONSULTANT, 1);
        assertThat(mongoIndex.getKey()).containsEntry(CLIENT, 1);
        assertThat(mongoIndex.getKey()).containsEntry(FISCAL_YEAR, 1);
        assertThat(mongoIndex.getKey()).containsEntry(ACCOUNT_NUMBER, -1);
        assertThat(mongoIndex.getKey()).containsEntry(ANLAG_ACCOUNTING_REASON, 1);
    }

    public void verifyIndexForCustomColumnStructureContent(MongoIndex mongoIndex) {
        assertThat(mongoIndex).isNotNull();
        assertThat(mongoIndex.getKey()).hasSize(10);
        assertThat(mongoIndex.getKey()).containsEntry(CONSULTANT, 1);
        assertThat(mongoIndex.getKey()).containsEntry(CLIENT, 1);
        assertThat(mongoIndex.getKey()).containsEntry(YEAR_BEGIN, 1);
        assertThat(mongoIndex.getKey()).containsEntry(YEAR_END, 1);
        assertThat(mongoIndex.getKey()).containsEntry(COLUMN_STRUCTURE_ID, 1);
        assertThat(mongoIndex.getKey()).containsEntry(INDUSTRY_NO, 1);
        assertThat(mongoIndex.getKey()).containsEntry(SECTION_NO, 1);
        assertThat(mongoIndex.getKey()).containsEntry(INDIV_NO, 1);
        assertThat(mongoIndex.getKey()).containsEntry(NATIONAL_RIGHT, 1);
        assertThat(mongoIndex.getKey()).containsEntry(INDIV_LEVEL, 1);
    }

    public void verifyIndexForCustomReportStructureContent(MongoIndex mongoIndex) {
        assertThat(mongoIndex).isNotNull();
        assertThat(mongoIndex.getKey()).hasSize(10);
        assertThat(mongoIndex.getKey()).containsEntry(CONSULTANT, 1);
        assertThat(mongoIndex.getKey()).containsEntry(CLIENT, 1);
        assertThat(mongoIndex.getKey()).containsEntry(YEAR_BEGIN, 1);
        assertThat(mongoIndex.getKey()).containsEntry(YEAR_END, 1);
        assertThat(mongoIndex.getKey()).containsEntry(REPORT_STRUCTURE_ID, 1);
        assertThat(mongoIndex.getKey()).containsEntry(INDUSTRY_NO, 1);
        assertThat(mongoIndex.getKey()).containsEntry(SECTION_NO, 1);
        assertThat(mongoIndex.getKey()).containsEntry(INDIV_NO, 1);
        assertThat(mongoIndex.getKey()).containsEntry(NATIONAL_RIGHT, 1);
        assertThat(mongoIndex.getKey()).containsEntry(INDIV_LEVEL, 1);
    }
}
