package de.datev.refsys.aggregation.processing.config.mongo;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.ReadConcern;
import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;
import de.datev.refsys.aggregation.processing.document.codec.OffsetDateTimeCodec;
import lombok.experimental.UtilityClass;
import org.bson.UuidRepresentation;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.Convention;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.codecs.pojo.PropertyModelBuilder;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.mongodb.MongoClientSettings.getDefaultCodecRegistry;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

@UtilityClass
public class MongoConfigUtil {
    private static final String CAMEL_CASE_REGEXP = "([^_A-Z])([A-Z])";
    private static final String SNAKE_CASE_REGEXP = "$1_$2";
    private static final List<Convention> conventions = Collections.singletonList(
            classModelBuilder -> {
                for (PropertyModelBuilder<?> fieldModelBuilder : classModelBuilder.getPropertyModelBuilders()) {
                    fieldModelBuilder.discriminatorEnabled(false);
                    fieldModelBuilder.readName(fieldModelBuilder.getName()
                            .replaceAll(CAMEL_CASE_REGEXP, SNAKE_CASE_REGEXP).toLowerCase());
                    fieldModelBuilder.writeName(fieldModelBuilder.getName()
                            .replaceAll(CAMEL_CASE_REGEXP, SNAKE_CASE_REGEXP).toLowerCase());
                }
                classModelBuilder.enableDiscriminator(false);
            });

    public static CodecRegistry configureCodecRegistry() {
        PojoCodecProvider.Builder pojoCodecBuilder = PojoCodecProvider.builder()
                .register("de.datev.refsys.aggregation.document.model")
                .register("de.datev.refsys.aggregation.document.model.enums")
                .conventions(conventions);
        return fromRegistries(getDefaultCodecRegistry(), fromProviders(pojoCodecBuilder.build()),
                CodecRegistries.fromCodecs(new OffsetDateTimeCodec()));
    }

    public static MongoClientSettings.Builder mongoClientSettingsBuilder(String uri, Long maxIdleTimeMS, Integer minPoolSize, Integer maxPoolSize) {
        return MongoClientSettings.builder()
                .readConcern(ReadConcern.MAJORITY)
                .readPreference(ReadPreference.primaryPreferred())
                .writeConcern(WriteConcern.MAJORITY)
                .uuidRepresentation(UuidRepresentation.JAVA_LEGACY)
                .codecRegistry(MongoConfigUtil.configureCodecRegistry())
                .applyConnectionString(new ConnectionString(uri))
                .applyToConnectionPoolSettings(connectionBuilder -> connectionBuilder
                        .maxConnectionIdleTime(maxIdleTimeMS, TimeUnit.MILLISECONDS)
                        .minSize(minPoolSize)
                        .maxSize(maxPoolSize));
    }
}
