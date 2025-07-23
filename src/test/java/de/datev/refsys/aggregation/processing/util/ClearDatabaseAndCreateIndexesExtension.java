package de.datev.refsys.aggregation.processing.util;

import com.mongodb.reactivestreams.client.MongoDatabase;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

public class ClearDatabaseAndCreateIndexesExtension implements BeforeEachCallback {

    @Override
    public void beforeEach(ExtensionContext extensionContext) {
        MongoDatabase mongoDatabase = DatabaseUtils.getDatabase(SpringExtension.getApplicationContext(extensionContext));
        DatabaseUtils.clearDatabase(mongoDatabase);
        DatabaseUtils.createIndexes(mongoDatabase);
    }
}
