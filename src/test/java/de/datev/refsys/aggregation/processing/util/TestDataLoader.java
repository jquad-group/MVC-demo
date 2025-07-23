package de.datev.refsys.aggregation.processing.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mongodb.MongoClientSettings;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import de.datev.refsys.aggregation.processing.config.mongo.MongoConfigUtil;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.bson.BsonArray;
import org.bson.BsonBinaryWriter;
import org.bson.BsonDocument;
import org.bson.BsonDocumentReader;
import org.bson.BsonDocumentWrapper;
import org.bson.BsonDocumentWriter;
import org.bson.BsonInvalidOperationException;
import org.bson.BsonValue;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.UuidRepresentation;
import org.bson.codecs.BsonArrayCodec;
import org.bson.codecs.BsonDocumentWrapperCodec;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.json.JsonReader;
import org.bson.json.JsonWriter;
import wiremock.com.google.common.io.Resources;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@UtilityClass
public class TestDataLoader {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());
    private static final CodecRegistry CODEC_REGISTRY = createCodecRegistry();
    private static final DecoderContext DECODER_CONTEXT = DecoderContext.builder().build();
    public static final EncoderContext ENCODER_CONTEXT = EncoderContext.builder().build();

    /**
     * This method loads a non DBElement from a Json.
     * @param path
     * @param classType
     * @return
     * @param <T>
     */
    @SneakyThrows
    public static <T> T load(String path, Class<T> classType) {
        InputStream resourceAsStream = TestDataLoader.class.getClassLoader().getResourceAsStream(path);
        return OBJECT_MAPPER.readValue(resourceAsStream, classType);
    }

    /**
     * This method loads a List<NonDBElement> from a Json
     * @param path
     * @param typeReference
     * @return
     * @param <T>
     */
    @SneakyThrows
    public static <T> List<T> loadList(String path, TypeReference<List<T>> typeReference) {
        InputStream resourceAsStream = TestDataLoader.class.getClassLoader().getResourceAsStream(path);
        return OBJECT_MAPPER.readValue(resourceAsStream, typeReference);
    }

    /**
     * This method loads a DBElement from a Json.
     * @param path
     * @param classType
     * @return
     * @param <T>
     */
    @SneakyThrows
    public static <T> T loadDBElement(String path, Class<T> classType) {
        try (InputStream inputStream = TestDataLoader.class.getClassLoader().getResourceAsStream(path)) {
            String text = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            try {
                return CODEC_REGISTRY.get(classType).decode(Document.parse(text).toBsonDocument(classType, CODEC_REGISTRY).asBsonReader(), DECODER_CONTEXT);
            } catch (BsonInvalidOperationException e) {
                throw new RuntimeException("Input Json is a List instead of a single Object, use #loadMongoDBListWithCodec instead", e);
            }
        }
    }

    public static <T> String writeToMongoJson(Object instance, Class<T> classType) {
        Codec<T> classCodec = CODEC_REGISTRY.get(classType);
        T castedInstance = (T) instance;
        BsonDocumentWrapper<T> bsonDocumentWrapper = new BsonDocumentWrapper<T>(castedInstance, classCodec);
        BsonDocumentWriter bsonWriter = new BsonDocumentWriter(bsonDocumentWrapper.toBsonDocument());
        classCodec.encode(bsonWriter, castedInstance, ENCODER_CONTEXT);
        return bsonWriter.getDocument().toJson();
    }

    /**
     * This method loads a List<DBElement> from a Json
     * @param path
     * @param classType
     * @return
     * @param <T>
     */
    @SneakyThrows
    public static <T> List<T> loadMongoDBList(String path, Class<T> classType) {
        try (InputStream inputStream = TestDataLoader.class.getClassLoader().getResourceAsStream(path)) {
            String json = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

            JsonReader reader = new JsonReader(json);
            BsonArrayCodec arrayReader = new BsonArrayCodec(CODEC_REGISTRY);
            BsonArray docArray = arrayReader.decode(reader, DecoderContext.builder().build());

            ArrayList<T> value = new ArrayList<>();
            for (BsonValue doc : docArray.getValues()) {
                value.add(CODEC_REGISTRY.get(classType).decode(doc.asDocument().asBsonReader(), DECODER_CONTEXT));
            }
            return value;
        }
    }

    /**
     * This method loads a List<nonDBElement> from a ndJson
     * @param jsonFileLocation
     * @param classType
     * @return
     * @param <T>
     */
    public static <T> List<T> loadNdJsonList(String jsonFileLocation, Class<T> classType) {
        try (Stream<String> lines = Files.lines(Path.of(TestDataLoader.class.getClassLoader().getResource(jsonFileLocation).toURI()))) {
            return lines.map(line -> mapToGenericType(classType, line)).toList();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * This method laods a List<DBElement> from a ndJson
     * @param jsonFileLocation
     * @return
     */
    @SneakyThrows
    public static List<Document> parseDocuments(String jsonFileLocation) {
        try (Stream<String> lines = Files.lines(Path.of(TestDataLoader.class.getClassLoader().getResource(jsonFileLocation).toURI()))) {
            return lines.map(Document::parse).toList();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * This method reads the data of the File and tries to return the String within it.
     * @param path
     * @return
     */
    @SneakyThrows
    public static String load(String path) {
        return Resources.toString(TestDataLoader.class.getClassLoader().getResource(path), StandardCharsets.UTF_8);
    }

    private static <T> T mapToGenericType(Class<T> classType, String line) {
        try {
            return OBJECT_MAPPER.readValue(line, classType);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private static CodecRegistry createCodecRegistry() {
        MongoClientSettings clientSettings = MongoClientSettings.builder().uuidRepresentation(UuidRepresentation.JAVA_LEGACY).codecRegistry(MongoConfigUtil.configureCodecRegistry()).build();
        try (MongoClient mongoClient = MongoClients.create(clientSettings)) {
            return mongoClient.getDatabase("test").getCodecRegistry();
        }
    }
}
