# RefSys Aggregation Processing Service - Spring MVC Migration Project

## FEATURE:

> **📋 Project Overview:** This document describes WHAT needs to be built. For technical implementation details see [GUIDELINES.md](./GUIDELINES.md)

**Current State:** A Spring Boot WebFlux application integrated as a microservice in a larger system. The application uses reactive programming patterns with MongoDB reactive drivers, Kafka integration, and provides RESTful APIs for aggregation processing.

**Target State:** Migrate to a traditional Spring Boot MVC application while maintaining all existing functionality. This involves replacing reactive MongoDB drivers, converting reactive controllers to MVC controllers, and updating the service layer to use synchronous operations. Kafka integration should be largely preserved.

**Core Functionalities:**
- **Data Aggregation Processing:** Processes and aggregates RefSys (Reference System) data from various sources
- **Inventory Management:** Handles creation, updating, and deletion of inventory data
- **Master Data Management:** Manages master data contexts, accounts, and business entities
- **Delta Processing:** Processes incremental data changes and updates
- **Custom Structure Management:** Handles custom column and report structures
- **Event-Driven Architecture:** Kafka-based messaging for system integration
- **RESTful API:** Provides HTTP endpoints for external system integration

For detailed business functionality, see [README.md](./README.md)

## EXAMPLES:

**Current WebFlux Controller Example:**
```java
@RestController
@RequestMapping("/api/v1")
public class DeleteInventoryController implements DeleteInventoryApi {
    @Override
    public Mono<Void> deleteInventory(Integer consultant, Integer client, Integer fiscalYear, 
                                      String xCorrelationId, String requestId,
                                      ServerWebExchange exchange) {
        return importExecutionService.deleteImportData(consultant, client, fiscalYear)
                .collect(Collectors.toSet())
                .flatMap(deleteResults -> {
                    // Process results and send Kafka message
                    return changeEventProducer.sendMessage(consultant, client, fiscalYear, null, null);
                });
    }
}
```

**Target MVC Controller Example:**
```java
@RestController
@RequestMapping("/api/v1")
public class DeleteInventoryController implements DeleteInventoryApi {
    @Override
    public ResponseEntity<Void> deleteInventory(Integer consultant, Integer client, Integer fiscalYear, 
                                               String xCorrelationId, String requestId,
                                               HttpServletRequest request) {
        Set<DeleteResult> deleteResults = importExecutionService.deleteImportData(consultant, client, fiscalYear);
        
        if (deleteResults.stream().anyMatch(result -> !result.wasAcknowledged())) {
            throw new RuntimeException("MongoDB write unacknowledged");
        }
        
        if (!deleteResults.isEmpty()) {
            changeEventProducer.sendMessage(consultant, client, fiscalYear, null, null);
        }
        
        return ResponseEntity.noContent().build();
    }
}
```

## DOCUMENTATION:

**Required Documentation for Development:**

📖 **Project Documentation:**
- **[GUIDELINES.md](./GUIDELINES.md)** - Technical implementation guidelines for this project
- **[README.md](./README.md)** - Setup instructions and operational documentation

🌐 **External Documentation:**

1. **Spring Boot Framework & MVC Migration**
   - [Spring Boot Reference Documentation](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/)
   - [Spring Web MVC](https://docs.spring.io/spring-framework/reference/web/webmvc.html)
   - [Spring WebFlux](https://docs.spring.io/spring-framework/reference/web/webflux.html)
   - [WebFlux to MVC Migration Guide](https://medium.com/@sunda.nitsri/step-into-the-future-a-comprehensive-guide-to-migrating-from-spring-mvc-to-webflux-ea2f7fa498e6)

2. **Spring Data MongoDB (Non-Reactive)**
   - [Spring Data MongoDB Reference](https://docs.spring.io/spring-data/mongodb/docs/current/reference/html/)
   - [MongoDB Java Driver](https://www.mongodb.com/docs/drivers/java/sync/current/)
   - [Spring Data MongoDB API](https://docs.spring.io/spring-data/mongodb/docs/current/api/)

3. **Apache Kafka & Spring Kafka**
   - [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
   - [Spring Kafka Reference](https://docs.spring.io/spring-kafka/docs/current/reference/html/)
   - [Spring Kafka GitHub](https://github.com/spring-projects/spring-kafka)

4. **Additional Technologies**
   - [Project Lombok](https://projectlombok.org/)
   - [OpenAPI 3 & Spring Boot](https://springdoc.org/)
   - [MapStruct](https://mapstruct.org/)
   - [Resilience4j](https://resilience4j.readme.io/)

## OTHER CONSIDERATIONS:

**Critical Migration Considerations and Common Pitfalls:**

### 1. Spring WebFlux to MVC Migration (CRITICAL):
- ❌ **Pitfall:** Using reactive types (Mono/Flux) directly in MVC controllers
- ✅ **Solution:** Replace Mono/Flux with traditional synchronous types (List, single objects)
- ❌ **Pitfall:** Using WebClient instead of RestTemplate in MVC
- ✅ **Solution:** Use RestTemplate or new RestClient for synchronous HTTP calls
- ❌ **Pitfall:** Keeping ServerWebExchange in MVC controllers
- ✅ **Solution:** Replace with HttpServletRequest/HttpServletResponse

### 2. Spring Data MongoDB Migration:
- ❌ **Pitfall:** Continuing to use ReactiveMongoRepository
- ✅ **Solution:** Use MongoRepository for synchronous database operations
- ❌ **Pitfall:** Keeping ReactiveMongoTemplate
- ✅ **Solution:** Use MongoTemplate for synchronous template operations
- ⚠️ **Note:** Remove spring-boot-starter-data-mongodb-reactive, use spring-boot-starter-data-mongodb

### 3. Dependency Management:
- ❌ **Pitfall:** Having both spring-boot-starter-webflux and spring-boot-starter-web
- ✅ **Solution:** Use only spring-boot-starter-web for MVC
- ❌ **Pitfall:** Keeping Reactor Core dependencies unnecessarily
- ✅ **Solution:** Remove reactive dependencies except those explicitly needed for Kafka

### 4. Repository Layer Migration:
**Current Reactive Repository Pattern:**
```java
public Mono<DeleteResult> deleteOne(Integer consultant, Integer client, Integer fiscalYear) {
    return Mono.from(masterDataCollection.deleteOne(
        QueryUtil.getByMasterDataBusinessKey(consultant, client, fiscalYear)))
           .elapsed().map(LoggingUtil.logDebugWithDuration(...));
}
```

**Target MVC Repository Pattern:**
```java
public DeleteResult deleteOne(Integer consultant, Integer client, Integer fiscalYear) {
    DeleteResult result = masterDataCollection.deleteOne(
        QueryUtil.getByMasterDataBusinessKey(consultant, client, fiscalYear));
    // Synchronous logging and metrics
    return result;
}
```

### 5. Service Layer Migration:
**Current Reactive Service:**
```java
public interface ImportService {
    Mono<Boolean> executeFullImport(ImportData importData);
}
```

**Target MVC Service:**
```java
public interface ImportService {
    Boolean executeFullImport(ImportData importData);
}
```

### 6. Testing Strategy:
- ✅ **MVC Testing:** Use @WebMvcTest and MockMvc for controller tests
- ✅ **MongoDB Testing:** Use @DataMongoTest for repository tests
- ❌ **Pitfall:** Using WebTestClient for MVC tests
- ✅ **Solution:** Use MockMvc for synchronous MVC tests
- ❌ **Pitfall:** Keeping reactive test patterns like StepVerifier
- ✅ **Solution:** Use standard JUnit/AssertJ patterns for synchronous testing

### 7. Kafka Integration:
- ✅ **Preserve:** Spring Kafka works with MVC without changes
- ⚠️ **Note:** Kafka consumers can remain reactive if desired
- ✅ **Best Practice:** Use @KafkaListener for simple consumers
- ✅ **Keep:** Event-driven architecture with ChangeEventProducer

### 8. Performance Considerations:
- ⚠️ **Note:** MVC is blocking - proper thread pool sizing required
- ✅ **Solution:** Configure server.tomcat.threads.max appropriately
- ❌ **Pitfall:** Blocking operations in WebFlux threads (during migration)
- ✅ **Solution:** Move all blocking operations to MVC thread pool

### 9. Error Handling Migration:
- ✅ **MVC Pattern:** Use @ControllerAdvice for global exception handling
- ✅ **MongoDB:** Utilize MongoTemplate exception translation
- ❌ **Pitfall:** Keeping reactive error handlers
- ✅ **Solution:** Implement synchronous exception handlers

### 10. Circuit Breaker and Resilience:
- ✅ **Keep:** Resilience4j works with both reactive and synchronous code
- ⚠️ **Migration:** Update circuit breaker operators from reactive to synchronous
- ✅ **Solution:** Use synchronous Resilience4j decorators instead of reactive operators

## Required Configuration for Migration:

### Maven Dependencies (pom.xml Critical Changes):
```xml
<!-- REMOVE: WebFlux Dependencies -->
<!-- <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency> -->

<!-- REMOVE: Reactive MongoDB -->
<!-- <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-mongodb-reactive</artifactId>
</dependency> -->

<!-- REMOVE: Reactor Kafka (if not needed for consumers) -->
<!-- <dependency>
    <groupId>io.projectreactor.kafka</groupId>
    <artifactId>reactor-kafka</artifactId>
</dependency> -->

<!-- ADD: Spring MVC -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<!-- ADD: Non-Reactive MongoDB -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-mongodb</artifactId>
</dependency>

<!-- KEEP: Kafka, Lombok, OpenAPI, Security, Actuator -->
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
</dependency>
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.0.4</version>
</dependency>
```

### Application Configuration (application.yml Template):
```yaml
# MongoDB Configuration (Non-Reactive)
spring:
  data:
    mongodb:
      host: localhost
      port: 27017
      database: refsys_aggregation
      # Authentication if required
      # username: ${MONGO_USERNAME:}
      # password: ${MONGO_PASSWORD:}

# Kafka Configuration (Unchanged)
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: refsys-aggregation-group
      auto-offset-reset: latest
    producer:
      retries: 3
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer

# Server Configuration for MVC
server:
  port: 8080
  tomcat:
    threads:
      max: 200  # Critical for MVC performance
      min-spare: 10
  compression:
    enabled: true

# Actuator Configuration
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: when-authorized

# Logging Configuration
logging:
  level:
    org.springframework.data.mongodb: DEBUG
    org.apache.kafka: INFO
    de.datev.refsys: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"

# Application-specific Configuration
ref-sys:
  circuitbreaker-enabled: true
```

### Main Application Class Migration:
```java
// BEFORE (WebFlux)
@EnableWebFlux
@SpringBootApplication
public class SpringBootMongodbReactiveApplication {
    // ...
}

// AFTER (MVC)
@SpringBootApplication
public class SpringBootMongodbMvcApplication {
    // Remove @EnableWebFlux
    // Keep existing circuit breaker and startup logic
}
```

### Implementation Priority and Details:
1. **Dependency Migration:** See [GUIDELINES.md - Dependency Management](./GUIDELINES.md#dependency-management)
2. **Repository Layer Migration:** See [GUIDELINES.md - MongoDB Repository Migration](./GUIDELINES.md#mongodb-repository-migration)
3. **Service Layer Migration:** See [GUIDELINES.md - Service Layer Migration](./GUIDELINES.md#service-layer-migration)
4. **Controller Layer Migration:** See [GUIDELINES.md - WebFlux to MVC Controller Migration](./GUIDELINES.md#webflux-to-mvc-controller-migration)
5. **Testing Strategy:** See [GUIDELINES.md - Testing Guidelines](./GUIDELINES.md#testing-guidelines)
6. **Kafka Integration:** See [GUIDELINES.md - Kafka Integration](./GUIDELINES.md#kafka-integration)

### Migration Validation Checklist:
- [ ] All reactive types (Mono/Flux) replaced with synchronous equivalents
- [ ] Repository methods return concrete types instead of reactive publishers
- [ ] Service methods use synchronous operations
- [ ] Controllers use ResponseEntity instead of reactive publishers
- [ ] ServerWebExchange replaced with HttpServletRequest/Response
- [ ] MongoDB operations use synchronous MongoTemplate/MongoRepository
- [ ] Circuit breakers use synchronous decorators
- [ ] Tests use MockMvc instead of WebTestClient
- [ ] Application starts successfully with MVC configuration
- [ ] All endpoints respond correctly
- [ ] Kafka integration continues to work
- [ ] Performance meets requirements with pr/oper thread pool configuration