package de.datev.refsys.aggregation.processing.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.mongodb.client.result.InsertOneResult;
import de.datev.refsys.aggregation.processing.configuration.TestMongoClientConfiguration;
import de.datev.refsys.aggregation.processing.configuration.TestcontainersConfiguration;
import de.datev.refsys.aggregation.processing.util.ClearDatabaseAnCreateIndexesBeforeEachTest;
import de.datev.refsys.aggregation.processing.util.MemoryAppender;
import de.datev.refsys.aggregation.processing.util.MongoHelperService;
import de.datev.refsys.aggregation.processing.util.TestDataLoader;
import lombok.extern.slf4j.Slf4j;
import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.test.context.ActiveProfiles;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static de.datev.refsys.aggregation.processing.util.TestUtil.LOG_TEST_PROFILE;
import static de.datev.refsys.aggregation.processing.util.TestUtil.setupMemoryAppender;
import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ImportAutoConfiguration(TestMongoClientConfiguration.class)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles(LOG_TEST_PROFILE)
@ClearDatabaseAnCreateIndexesBeforeEachTest
class MaskingPatternLayoutTest {
    private static final EasyRandom EASY_RANDOM = new EasyRandom();
    private static Reflections javaReflections;
    private static Reflections mongoReflections;

    @Autowired
    private MongoHelperService mongoHelperService;

    private MemoryAppender memoryAppender;

    @BeforeAll
    static void setUpAll() throws MalformedURLException {
        // by default Object class is excluded, add filterResultsBy to include it
        Scanners subTypeScannerWithObjectType = Scanners.SubTypes.filterResultsBy(c -> true);
        FilterBuilder mongoFilterBuilder = getFilterBuilder("de.datev.refsys.aggregation.document.model",
                                                            List.of("de.datev.refsys.aggregation.document.model.constants",
                                                                    "de.datev.refsys.aggregation.document.model.enum"));
        ConfigurationBuilder mongoConfigurationBuilder = new ConfigurationBuilder()
                .setScanners(subTypeScannerWithObjectType)
                .addUrls(ClasspathHelper.forJavaClassPath())
                .filterInputsBy(mongoFilterBuilder);
        mongoReflections = new Reflections(mongoConfigurationBuilder);
        // take only classes from src/main
        String mainClassesFolder = System.getProperty("user.dir") + "\\target\\classes";
        FilterBuilder javaFilterBuilder =
                getFilterBuilder("de.datev.refsys.aggregation.processing.model", List.of("de.datev.refsys.aggregation.processing.model.enums"));
        ConfigurationBuilder javaConfigurationBuilder = new ConfigurationBuilder().setScanners(subTypeScannerWithObjectType)
                                                                                  .addUrls(Path.of(mainClassesFolder).toUri().toURL())
                                                                                  .filterInputsBy(javaFilterBuilder);
        javaReflections = new Reflections(javaConfigurationBuilder);
    }

    @BeforeEach
    void setUpEach() {
        memoryAppender = setupMemoryAppender(memoryAppender, this.getClass(), Level.DEBUG);
    }

    @Test
    @DisplayName("Logs every java class from model package with masked VK3 values")
    void should_mask_vk3_values_for_each_java_model() {
        Set<Class<?>> allModels = javaReflections.getSubTypesOf(Object.class);
        for (Class<?> modelClass : allModels) {
            // skip abstract classes
            if (Modifier.isAbstract(modelClass.getModifiers())) {
                continue;
            }
            log.info(EASY_RANDOM.nextObject(modelClass).toString());
            List<ILoggingEvent> searchResult = memoryAppender.search(modelClass.getSimpleName());
            assertThat(searchResult).hasSize(1);
            // reset logs after each class to make sure only one log is present
            memoryAppender.reset();
        }
    }

    @Test
    @DisplayName("Logs every mongo document class from model package with masked VK3 values")
    void should_mask_vk3_values_for_each_mongo_model() {
        Set<Class<?>> allModels = mongoReflections.getSubTypesOf(Object.class);
        for (Class<?> modelClass : allModels) {
            // skip abstract classes
            if (Modifier.isAbstract(modelClass.getModifiers())) {
                continue;
            }
            String message = TestDataLoader.writeToMongoJson(modelClass.cast(EASY_RANDOM.nextObject(modelClass)), modelClass);
            log.info("{} {}", modelClass.getSimpleName(), message);
            List<ILoggingEvent> searchResult = memoryAppender.search(modelClass.getSimpleName());
            assertThat(searchResult).hasSize(1);
            // reset logs after each class to make sure only one log is present
            memoryAppender.reset();
        }
    }

    @Test
    @DisplayName("Logs every mongo document class from model package and the @Document annotation with masked VK3 values when an error occur2s")
    void should_mask_vk3_values_for_each_mongo_document_model_when_a_duplicate_key_error_occurs() {
        Set<Class<?>> allModels = mongoReflections.getSubTypesOf(Object.class);
        for (Class<?> modelClass : allModels) {
            Optional<Annotation> documentAnnotation = Arrays.stream(modelClass.getDeclaredAnnotations())
                                               .filter(annotation -> annotation.annotationType().equals(Document.class))
                                               .findFirst();
            // skip classes that don't have the @Document annotation and abstract classes
            if (documentAnnotation.isEmpty() || Modifier.isAbstract(modelClass.getModifiers())) {
                continue;
            }
            Document document = (Document) documentAnnotation.get();
            Object dataToInsert = EASY_RANDOM.nextObject(modelClass);
            InsertOneResult insertOneResult = mongoHelperService.insertOneGeneric(dataToInsert, document.value(), modelClass);
            assertThat(insertOneResult).isNotNull();
            assertThat(insertOneResult.getInsertedId()).isNotNull();
            try {
                mongoHelperService.insertOneGeneric(dataToInsert, document.value(), modelClass);
            } catch (Exception e) {
                log.error("Mask pattern test for DB", e);
                List<ILoggingEvent> searchResult = memoryAppender.search(Level.ERROR);
                assertThat(searchResult).hasSize(1);
                // reset logs after each class to make sure only one log is present
            }
            memoryAppender.reset();
        }
    }

    private static FilterBuilder getFilterBuilder(String includedPackage, List<String> excludedPackages) {
        // take only classes from the includedPackage, exclude classes from the excludedPackages and exclude the builder classes with the regex
        FilterBuilder filterBuilder = new FilterBuilder().includePackage(includedPackage).excludePattern(".*\\$.*");
        excludedPackages.forEach(filterBuilder::excludePackage);
        return filterBuilder;
    }

}