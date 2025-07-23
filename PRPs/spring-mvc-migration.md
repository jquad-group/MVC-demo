name: "Spring WebFlux to MVC Migration - RefSys Aggregation Processing Service"
description: |

## Purpose
Comprehensive PRP for migrating the RefSys Aggregation Processing Service from Spring WebFlux to Spring MVC with sufficient context and self-validation capabilities to achieve working code through iterative refinement.

## Core Principles
1. **Context is King**: Include ALL necessary documentation, examples, and caveats
2. **Validation Loops**: Provide executable tests/lints the AI can run and fix
3. **Information Dense**: Use keywords and patterns from the codebase
4. **Progressive Success**: Start simple, validate, then enhance
5. **Global rules**: Be sure to follow all rules in GUIDELINES.md

---

## Goal
Migrate the entire RefSys Aggregation Processing Service from Spring WebFlux reactive architecture to Spring MVC synchronous architecture while maintaining all existing functionality, performance characteristics, and integration points.

## Why
- **Team Knowledge Gap**: Reduce complexity by moving from reactive to more familiar synchronous patterns
- **Simplified Debugging**: Traditional stack traces and debugging tools work better with synchronous code
- **Maintenance Efficiency**: Easier onboarding for new developers and reduced cognitive overhead
- **Integration Compatibility**: Better compatibility with existing synchronous downstream services
- **Proven Architecture**: Spring MVC is battle-tested for high-volume enterprise applications

## What
Complete migration from WebFlux reactive programming model to Spring MVC synchronous model including:
- **Controllers**: Convert from reactive return types (Mono/Flux) to traditional ResponseEntity patterns
- **Repository Layer**: Replace reactive MongoDB operations with synchronous MongoTemplate/MongoRepository
- **Service Layer**: Convert reactive service methods to synchronous operations
- **Configuration**: Replace reactive MongoDB configuration with synchronous drivers
- **Testing**: Migrate from WebTestClient to MockMvc testing patterns
- **Dependencies**: Replace reactive dependencies with MVC equivalents
- **Error Handling**: Maintain existing error handling patterns in synchronous context

### Success Criteria
- [ ] All 5 controllers migrated from WebFlux to MVC patterns
- [ ] All 8 repository classes converted to synchronous MongoDB operations
- [ ] All 84 service methods with Mono/Flux return types converted to synchronous
- [ ] Application starts successfully with MVC configuration
- [ ] All existing endpoints respond correctly with same HTTP contracts
- [ ] All 200+ unit tests pass with MVC test patterns
- [ ] All integration tests pass with MockMvc instead of WebTestClient
- [ ] Kafka integration continues to work without changes
- [ ] Circuit breaker and resilience patterns preserved
- [ ] Performance meets or exceeds current WebFlux performance benchmarks

## All Needed Context

### Documentation & References
```yaml
# MUST READ - Include these in your context window

# Project Documentation
- file: GUIDELINES.md
  why: Technical implementation standards for Spring MVC patterns
  critical: Constructor injection, testing patterns, error handling rules

- file: THIS_PROJECT.md  
  why: Complete migration requirements with examples and gotchas
  critical: WebFlux to MVC controller examples, dependency changes, pitfalls

- file: README.md
  why: Project setup, local development, and operational procedures
  critical: MongoDB setup, Kafka configuration, testing approaches

# External Documentation - URLs to reference during implementation
- url: https://docs.spring.io/spring-framework/reference/web/webmvc.html
  why: Official Spring MVC reference documentation
  critical: Controller patterns, request mapping, validation

- url: https://docs.spring.io/spring-data/mongodb/docs/current/reference/html/
  why: Spring Data MongoDB (non-reactive) reference
  critical: Repository patterns, MongoTemplate usage, transaction support

- url: https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/
  why: Spring Boot 3.x reference for configuration patterns
  critical: Auto-configuration, dependency management, testing support

- url: https://medium.com/@sunda.nitsri/step-into-the-future-a-comprehensive-guide-to-migrating-from-spring-mvc-to-webflux-ea2f7fa498e6
  why: WebFlux migration patterns (reverse apply for MVC migration)
  critical: Common pitfalls when moving between reactive and synchronous patterns
```

### Current Codebase Structure (WebFlux Implementation)
```bash
src/main/java/de/datev/refsys/aggregation/processing/
├── SpringBootMongodbReactiveApplication.java    # @EnableWebFlux entry point
├── boundry/controller/
│   ├── DeleteInventoryController.java            # Mono<Void> returns, ServerWebExchange
│   ├── InitialLoadController.java               # Reactive controller patterns
│   ├── ProcessDeltaController.java              # Complex reactive flows
│   ├── UpdateSchemaController.java              # ServerWebExchange usage
│   └── UpdateVersionController.java             # Mono return types
├── repository/
│   ├── MasterDataRepository.java                # Custom reactive MongoDB operations
│   ├── MovementDataDayRepository.java           # Mono<UpdateResult> patterns
│   ├── StateDocRepository.java                  # Reactive transactions
│   ├── MovementDataInventoryRepository.java     # Bulk reactive operations
│   └── [8 total repository files]               # All using reactive MongoDB client
├── service/
│   ├── ImportService.java                       # Mono<Boolean> executeFullImport
│   ├── DeltaEventProcessingService.java         # Mono<DeltaInfo> return types
│   ├── CommonImportService.java                 # Complex reactive chains
│   └── [12 total service files]                 # 84 methods with Mono/Flux returns
└── config/
    ├── mongo/MongoConfiguration.java            # Reactive MongoClient configuration
    └── AggregationProcessingConfiguration.java  # WebFlux-specific beans
```

### Target Codebase Structure (Spring MVC)
```bash
src/main/java/de/datev/refsys/aggregation/processing/
├── SpringBootMongodbMvcApplication.java         # Remove @EnableWebFlux, add MVC config
├── boundry/controller/
│   ├── DeleteInventoryController.java            # ResponseEntity<Void>, HttpServletRequest
│   ├── InitialLoadController.java               # Traditional MVC patterns
│   ├── ProcessDeltaController.java              # Synchronous flows
│   ├── UpdateSchemaController.java              # HttpServletRequest/Response
│   └── UpdateVersionController.java             # ResponseEntity return types
├── repository/
│   ├── MasterDataRepository.java                # MongoTemplate, DeleteResult returns
│   ├── MovementDataDayRepository.java           # Synchronous UpdateResult
│   ├── StateDocRepository.java                  # Traditional @Transactional
│   ├── MovementDataInventoryRepository.java     # Synchronous bulk operations
│   └── [8 total repository files]               # All using synchronous MongoDB
├── service/
│   ├── ImportService.java                       # Boolean executeFullImport
│   ├── DeltaEventProcessingService.java         # DeltaInfo return types
│   ├── CommonImportService.java                 # Synchronous method chains
│   └── [12 total service files]                 # Direct object returns
└── config/
    ├── mongo/MongoConfiguration.java            # Synchronous MongoClient configuration
    └── AggregationProcessingConfiguration.java  # MVC-specific beans
```

### Known Gotchas & Library Quirks
```java
// CRITICAL MIGRATION GOTCHAS FROM CODEBASE ANALYSIS:

// 1. DEPENDENCY CONFLICTS - Spring Boot 3.5.3
// REMOVE these reactive dependencies:
// - spring-boot-starter-webflux
// - spring-boot-starter-data-mongodb-reactive  
// - io.projectreactor.kafka:reactor-kafka
// - resilience4j-reactor
// ADD these MVC dependencies:
// - spring-boot-starter-web
// - spring-boot-starter-data-mongodb

// 2. MONGODB CLIENT CONFIGURATION
// Current: Reactive MongoClient with custom connection pools
// Target: Standard MongoClient with connection pool settings
// GOTCHA: Connection pool configurations need adjustment for synchronous patterns

// 3. CIRCUIT BREAKER PATTERNS
// Current: CircuitBreakerOperator.of(afterMovementDataCircuitBreaker)
// Target: @CircuitBreaker annotation or programmatic decoration
// GOTCHA: Resilience4j reactive operators must be replaced with synchronous decorators

// 4. REPOSITORY TRANSACTION PATTERNS  
// Current: Mono.from(transactionalMasterDataCollection.updateOne(...))
// Target: @Transactional with MongoTemplate operations
// GOTCHA: MongoDB transactions work differently in synchronous mode

// 5. LOGGING AND METRICS INTEGRATION
// Current: .elapsed().map(LoggingUtil.logDebugWithDuration(...))
// Target: Manual timing with StopWatch or AOP-based timing
// GOTCHA: Reactor context propagation doesn't exist in MVC

// 6. CONTROLLER METHOD SIGNATURES
// Current: public Mono<Void> deleteInventory(..., ServerWebExchange exchange)
// Target: public ResponseEntity<Void> deleteInventory(..., HttpServletRequest request)
// GOTCHA: ServerWebExchange methods don't exist in HttpServletRequest

// 7. KAFKA INTEGRATION PRESERVATION
// KEEP: org.springframework.kafka (non-reactive) - works with MVC
// REMOVE: reactor-kafka dependency
// GOTCHA: Kafka consumers can remain reactive or be converted to @KafkaListener

// 8. TEST PATTERNS MIGRATION
// Current: WebTestClient for integration tests
// Target: MockMvc with @WebMvcTest
// GOTCHA: StepVerifier and reactor-test patterns must be removed

// 9. APPLICATION STARTUP
// Current: @EnableWebFlux on SpringBootMongodbReactiveApplication
// Target: Remove @EnableWebFlux, Spring Boot auto-detects MVC
// GOTCHA: WebFlux and MVC cannot coexist in same application

// 10. ERROR HANDLING PRESERVATION
// Current: Global error handling works in WebFlux context
// Target: @ControllerAdvice works in MVC context  
// GOTCHA: Error response patterns should remain identical for API compatibility
```

## Implementation Blueprint

### Data Models and Structure
Current reactive patterns use the same domain models, so minimal changes needed:
```java
// KEEP EXISTING: All domain models in model/ package work with both approaches
// KEEP EXISTING: MapStruct mappers work identically  
// KEEP EXISTING: Validation annotations (@Valid, @NotNull) work identically
// MODIFY: Any reactive-specific model extensions or reactive stream wrappers
```

### List of Tasks (Implementation Order)

```yaml
Task 1: Dependency Migration (Foundation)
MODIFY pom.xml:
  - REMOVE: spring-boot-starter-webflux dependency
  - REMOVE: spring-boot-starter-data-mongodb-reactive dependency  
  - REMOVE: reactor-kafka dependency
  - REMOVE: resilience4j-reactor dependency
  - ADD: spring-boot-starter-web dependency
  - ADD: spring-boot-starter-data-mongodb dependency
  - MODIFY: resilience4j-spring-boot3 (keep existing)
  - MODIFY: Test dependencies - replace reactor-test with standard spring-test

Task 2: MongoDB Configuration Migration
MODIFY src/main/java/de/datev/refsys/aggregation/processing/config/mongo/MongoConfiguration.java:
  - REPLACE: MongoClient reactive configuration
  - WITH: com.mongodb.client.MongoClient synchronous configuration
  - PRESERVE: Connection pool settings (maxIdleTimeMS, minPoolSize, maxPoolSize)
  - PRESERVE: Retry settings adaptation for synchronous context
  - MODIFY: MongoTemplate bean creation instead of ReactiveMongoTemplate

Task 3: Application Class Migration  
MODIFY src/main/java/de/datev/refsys/aggregation/processing/SpringBootMongodbReactiveApplication.java:
  - REMOVE: @EnableWebFlux annotation
  - RENAME: Class to SpringBootMongodbMvcApplication  
  - PRESERVE: All other configuration and startup logic
  - ADD: Any MVC-specific configuration if needed

Task 4: Repository Layer Migration (Critical)
MODIFY all 8 repository files in repository/ package:
  
  MODIFY src/main/java/de/datev/refsys/aggregation/processing/repository/MasterDataRepository.java:
    - REPLACE: MongoCollection<MasterData> with MongoTemplate injection
    - CONVERT: Mono<DeleteResult> deleteOne(...) to DeleteResult deleteOne(...)
    - CONVERT: Mono<UpdateResult> upsertOne(...) to UpdateResult upsertOne(...)
    - PRESERVE: Circuit breaker logic with synchronous decorators
    - PRESERVE: Logging patterns with manual timing
    - PRESERVE: Retry logic with synchronous Resilience4j

  APPLY SAME PATTERN to:
    - MovementDataDayRepository.java
    - StateDocRepository.java  
    - MovementDataInventoryRepository.java
    - MovementDataMonthRepository.java
    - MovementDataPersonGroupDayRepository.java
    - MovementDataPersonGroupMonthRepository.java
    - CustomColumnStructureContentRepository.java
    - CustomReportStructureContentRepository.java

Task 5: Service Layer Migration (Complex)
MODIFY all 12 service files with reactive patterns:

  MODIFY src/main/java/de/datev/refsys/aggregation/processing/service/ImportService.java:
    - CONVERT: Mono<Boolean> executeFullImport to Boolean executeFullImport
    - PRESERVE: All business logic, convert reactive chains to sequential calls
    
  MODIFY src/main/java/de/datev/refsys/aggregation/processing/service/ImportServiceImpl.java:
    - CONVERT: Complex reactive chains to sequential service calls
    - REPLACE: .flatMap(), .map(), .filter() with traditional if/else and method calls
    - PRESERVE: Error handling with try/catch blocks
    - PRESERVE: Transaction boundaries with @Transactional

  APPLY SAME PATTERN to:
    - DeltaEventProcessingService.java & DeltaEventProcessingServiceImpl.java
    - CommonImportService.java & CommonImportServiceImpl.java  
    - ImportExecutionService.java & ImportExecutionServiceImpl.java
    - CustomStructuresService.java & CustomStructuresServiceImpl.java
    - UpdateSchemaService.java & UpdateSchemaServiceImpl.java
    - PersonGroupAggregationService.java
    - CommonService.java

Task 6: Controller Layer Migration (User-Facing)
MODIFY all 5 controllers in boundry/controller/ package:

  MODIFY src/main/java/de/datev/refsys/aggregation/processing/boundry/controller/DeleteInventoryController.java:
    - CONVERT: public Mono<Void> deleteInventory(..., ServerWebExchange exchange)
    - TO: public ResponseEntity<Void> deleteInventory(..., HttpServletRequest request)  
    - PRESERVE: Parameter validation and business logic delegation
    - PRESERVE: HTTP response codes and headers
    - PRESERVE: Correlation ID handling and logging patterns

  APPLY SAME PATTERN to:
    - InitialLoadController.java
    - ProcessDeltaController.java
    - UpdateSchemaController.java  
    - UpdateVersionController.java

Task 7: Circuit Breaker & Resilience Migration
MODIFY src/main/java/de/datev/refsys/aggregation/processing/config/AggregationProcessingConfiguration.java:
  - REPLACE: Reactive circuit breaker operators
  - WITH: Synchronous Resilience4j decorators
  - PRESERVE: Circuit breaker configuration (thresholds, timeouts)
  - PRESERVE: Retry configuration with synchronous retry mechanisms

Task 8: Logging & Metrics Migration  
MODIFY src/main/java/de/datev/refsys/aggregation/processing/util/LoggingUtil.java:
  - REPLACE: Reactive timing patterns (.elapsed().map())
  - WITH: StopWatch-based timing or AOP timing aspects
  - PRESERVE: Log message formats and correlation ID propagation
  - PRESERVE: Micrometer metrics integration patterns

Task 9: Error Handling Verification
MODIFY src/main/java/de/datev/refsys/aggregation/processing/exception/RestExceptionHandler.java:
  - VERIFY: @ControllerAdvice works correctly with MVC
  - PRESERVE: All error response formats and HTTP status codes
  - PRESERVE: Exception mapping and logging patterns
  - TEST: Ensure client contract compatibility

Task 10: Test Migration (Comprehensive)
MODIFY all test files to use MVC patterns:
  
  Controller Tests:
    - REPLACE: WebTestClient with MockMvc
    - REPLACE: @WebFluxTest with @WebMvcTest  
    - PRESERVE: Test scenarios and assertions
    - PRESERVE: Mock configurations and test data

  Repository Tests:
    - REPLACE: StepVerifier with standard assertions
    - REPLACE: Reactive test patterns with synchronous equivalents
    - PRESERVE: Test data and verification logic

  Integration Tests:
    - REPLACE: WebTestClient integration patterns
    - WITH: @SpringBootTest with TestRestTemplate or MockMvc
    - PRESERVE: End-to-end test scenarios
```

### Per Task Pseudocode

```java
// Task 4: Repository Migration Example (MasterDataRepository)
@Repository
public class MasterDataRepository {
    private final MongoTemplate mongoTemplate; // Inject MongoTemplate instead of MongoClient
    private final MeterRegistry meterRegistry;
    private final CircuitBreaker masterDataCircuitBreaker;
    
    public MasterDataRepository(MongoTemplate mongoTemplate, MeterRegistry meterRegistry, 
                               CircuitBreaker masterDataCircuitBreaker) {
        this.mongoTemplate = mongoTemplate;
        this.meterRegistry = meterRegistry;
        this.masterDataCircuitBreaker = masterDataCircuitBreaker;
    }
    
    // CONVERT: Mono<DeleteResult> to DeleteResult
    public DeleteResult deleteOne(Integer consultant, Integer client, Integer fiscalYear) {
        Supplier<DeleteResult> deleteOperation = () -> {
            StopWatch stopWatch = StopWatch.createStarted();
            try {
                DeleteResult result = mongoTemplate.remove(
                    QueryUtil.getByMasterDataBusinessKey(consultant, client, fiscalYear), 
                    MasterData.class);
                // Manual timing instead of .elapsed()
                LoggingUtil.logDebugWithDuration(LoggingUtil.MASTER_DATA_REPOSITORY_DELETE_ONE_LOG)
                          .accept(stopWatch.getTotalTimeMillis());
                return result;
            } finally {
                stopWatch.stop();
            }
        };
        
        // Apply circuit breaker with synchronous decorator
        return masterDataCircuitBreaker.executeSupplier(deleteOperation);
    }
}

// Task 5: Service Migration Example (ImportService)  
@Service
public class ImportServiceImpl implements ImportService {
    private final MasterDataRepository masterDataRepository;
    private final ChangeEventProducer changeEventProducer;
    
    // CONVERT: Mono<Boolean> to Boolean
    @Override
    @Transactional
    public Boolean executeFullImport(ImportData importData) {
        try {
            // Sequential calls instead of reactive chains
            DeleteResult deleteResult = masterDataRepository.deleteOne(
                importData.getConsultant(), 
                importData.getClient(), 
                importData.getFiscalYear());
                
            if (!deleteResult.wasAcknowledged()) {
                throw new RuntimeException("MongoDB write unacknowledged");
            }
            
            // Process import data synchronously
            boolean importSuccess = processImportData(importData);
            
            if (importSuccess) {
                // Send Kafka message synchronously  
                changeEventProducer.sendMessage(
                    importData.getConsultant(),
                    importData.getClient(), 
                    importData.getFiscalYear(),
                    null, null);
            }
            
            return importSuccess;
        } catch (Exception e) {
            log.error("Import failed for consultant={}, client={}, fiscalYear={}", 
                     importData.getConsultant(), importData.getClient(), 
                     importData.getFiscalYear(), e);
            throw e;
        }
    }
}

// Task 6: Controller Migration Example (DeleteInventoryController)
@RestController
@RequestMapping("/api/v1")
public class DeleteInventoryController implements DeleteInventoryApi {
    private final ImportExecutionService importExecutionService;
    private final ChangeEventProducer changeEventProducer;
    
    // CONVERT: Mono<Void> with ServerWebExchange to ResponseEntity<Void> with HttpServletRequest
    @Override
    public ResponseEntity<Void> deleteInventory(Integer consultant, Integer client, Integer fiscalYear, 
                                               String xCorrelationId, String requestId,
                                               HttpServletRequest request) {
        try {
            // Synchronous service call
            Set<DeleteResult> deleteResults = importExecutionService.deleteImportData(
                consultant, client, fiscalYear);
            
            // Validate results synchronously
            if (deleteResults.stream().anyMatch(result -> !result.wasAcknowledged())) {
                throw new RuntimeException("MongoDB write unacknowledged");
            }
            
            // Send Kafka message if deletions occurred
            if (!deleteResults.isEmpty()) {
                changeEventProducer.sendMessage(consultant, client, fiscalYear, null, null);
            }
            
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Delete inventory failed for consultant={}, client={}, fiscalYear={}", 
                     consultant, client, fiscalYear, e);
            throw e; // Let @ControllerAdvice handle exception mapping
        }
    }
}
```

### Integration Points
```yaml
DATABASE:
  - preservation: All MongoDB indexes and collections remain unchanged
  - configuration: Replace reactive connection strings with synchronous equivalents
  - transactions: Convert reactive transactions to @Transactional annotations
  
CONFIG:
  - modify: application.yml MongoDB configuration
  - pattern: Remove reactive-specific properties, add MVC thread pool config
  - example: "server.tomcat.threads.max=200"
  
KAFKA:
  - preservation: Spring Kafka works identically with MVC
  - configuration: Keep existing Kafka producer/consumer configurations  
  - pattern: @KafkaListener annotations work without changes
  
SECURITY:
  - preservation: Spring Security configuration works with MVC
  - modification: Replace ServerWebExchange security context with HttpServletRequest
  - pattern: Keep existing JWT and OAuth2 resource server configuration

MONITORING:
  - preservation: Micrometer metrics work with MVC
  - modification: Replace reactive metrics operators with manual timing
  - pattern: Keep existing Prometheus and OpenTelemetry integrations
```

## Validation Loop

### Level 1: Syntax & Style
```bash
# Run these FIRST - fix any errors before proceeding
mvn clean compile  # Must pass - validates syntax and dependency resolution

# Expected: No compilation errors
# If errors: READ the Maven output carefully - likely missing MVC dependencies or incorrect imports
# Common issues: 
# - Missing spring-boot-starter-web dependency
# - Remaining WebFlux imports (ServerWebExchange, Mono, Flux)
# - Incorrect MongoDB client imports (should be com.mongodb.client.MongoClient)

mvn spotless:apply  # Auto-fix formatting - should work without issues

mvn checkstyle:check  # Style checking - should pass with existing checkstyle config
```

### Level 2: Unit Tests - Each Converted Component
```java
// CREATE MasterDataRepositoryTest.java with MVC patterns:
@ExtendWith(SpringExtension.class)
@DataMongoTest  // Use @DataMongoTest for repository testing
class MasterDataRepositoryTest {
    
    @Autowired
    private MongoTemplate mongoTemplate;  // Inject MongoTemplate for testing
    
    private MasterDataRepository repository;
    
    @BeforeEach
    void setUp() {
        repository = new MasterDataRepository(mongoTemplate, 
                                            mock(MeterRegistry.class),
                                            mock(CircuitBreaker.class));
    }
    
    @Test
    void deleteOne_ShouldReturnDeleteResult_WhenDocumentExists() {
        // Given: Insert test data
        MasterData testData = createTestMasterData(1, 2, 2023);
        mongoTemplate.insert(testData);
        
        // When: Delete the document
        DeleteResult result = repository.deleteOne(1, 2, 2023);
        
        // Then: Verify deletion
        assertThat(result.wasAcknowledged()).isTrue();
        assertThat(result.getDeletedCount()).isEqualTo(1);
        
        // Verify document is actually deleted
        Query query = QueryUtil.getByMasterDataBusinessKey(1, 2, 2023);
        assertThat(mongoTemplate.exists(query, MasterData.class)).isFalse();
    }
    
    @Test
    void deleteOne_ShouldHandleNonExistentDocument() {
        // When: Try to delete non-existent document
        DeleteResult result = repository.deleteOne(999, 999, 2023);
        
        // Then: Should return acknowledged result with 0 deletions
        assertThat(result.wasAcknowledged()).isTrue();
        assertThat(result.getDeletedCount()).isEqualTo(0);
    }
}

// CREATE ImportServiceTest.java with synchronous patterns:
@ExtendWith(MockitoExtension.class)
class ImportServiceTest {
    
    @Mock
    private MasterDataRepository masterDataRepository;
    
    @Mock  
    private ChangeEventProducer changeEventProducer;
    
    @InjectMocks
    private ImportServiceImpl importService;
    
    @Test
    void executeFullImport_ShouldReturnTrue_WhenImportSuccessful() {
        // Given
        ImportData importData = createTestImportData();
        DeleteResult deleteResult = mock(DeleteResult.class);
        when(deleteResult.wasAcknowledged()).thenReturn(true);
        when(masterDataRepository.deleteOne(1, 2, 2023)).thenReturn(deleteResult);
        
        // When
        Boolean result = importService.executeFullImport(importData);
        
        // Then
        assertThat(result).isTrue();
        verify(changeEventProducer).sendMessage(1, 2, 2023, null, null);
    }
    
    @Test
    void executeFullImport_ShouldThrowException_WhenDeleteUnacknowledged() {
        // Given
        ImportData importData = createTestImportData();
        DeleteResult deleteResult = mock(DeleteResult.class);
        when(deleteResult.wasAcknowledged()).thenReturn(false);
        when(masterDataRepository.deleteOne(1, 2, 2023)).thenReturn(deleteResult);
        
        // When/Then
        assertThatThrownBy(() -> importService.executeFullImport(importData))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("MongoDB write unacknowledged");
    }
}
```

```bash
# Run individual component tests after each migration:
mvn test -Dtest=MasterDataRepositoryTest  # Should pass
mvn test -Dtest=ImportServiceTest         # Should pass  
mvn test -Dtest=DeleteInventoryControllerTest  # Should pass

# If failing: 
# 1. Read error messages carefully - likely mock configuration issues
# 2. Verify synchronous return types in assertions
# 3. Ensure no reactive test patterns (StepVerifier, etc.) remain
# 4. Check that @DataMongoTest uses synchronous MongoTemplate
```

### Level 3: Controller Integration Tests
```java
// CREATE DeleteInventoryControllerTest.java with MockMvc:
@WebMvcTest(DeleteInventoryController.class)
class DeleteInventoryControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private ImportExecutionService importExecutionService;
    
    @MockBean  
    private ChangeEventProducer changeEventProducer;
    
    @Test
    @WithMockUser
    void deleteInventory_ShouldReturn204_WhenDeletionSuccessful() throws Exception {
        // Given
        Set<DeleteResult> successResults = Set.of(createAcknowledgedDeleteResult());
        when(importExecutionService.deleteImportData(1, 2, 2023))
            .thenReturn(successResults);
        
        // When/Then
        mockMvc.perform(delete("/api/v1/inventory")
                .param("consultant", "1")
                .param("client", "2") 
                .param("fiscalYear", "2023")
                .header("X-Correlation-Id", "test-correlation-id")
                .header("Request-Id", "test-request-id"))
                .andExpect(status().isNoContent());
        
        // Verify interactions
        verify(changeEventProducer).sendMessage(1, 2, 2023, null, null);
    }
    
    @Test
    @WithMockUser
    void deleteInventory_ShouldReturn500_WhenDeleteUnacknowledged() throws Exception {
        // Given
        Set<DeleteResult> failedResults = Set.of(createUnacknowledgedDeleteResult());
        when(importExecutionService.deleteImportData(1, 2, 2023))
            .thenReturn(failedResults);
        
        // When/Then
        mockMvc.perform(delete("/api/v1/inventory")
                .param("consultant", "1")
                .param("client", "2")
                .param("fiscalYear", "2023"))
                .andExpect(status().isInternalServerError());
        
        // Verify no Kafka message sent on failure
        verify(changeEventProducer, never()).sendMessage(any(), any(), any(), any(), any());
    }
}
```

```bash  
# Test controller endpoints individually:
mvn test -Dtest=DeleteInventoryControllerTest      # Should pass
mvn test -Dtest=InitialLoadControllerTest         # Should pass  
mvn test -Dtest=ProcessDeltaControllerTest         # Should pass

# Expected: All MockMvc tests pass, proper HTTP status codes returned
# If error: Check MockMvc setup, verify @MockBean configurations
```

### Level 4: Application Integration Test
```bash
# Start application with MVC configuration:
mvn spring-boot:run

# Expected: Application starts successfully on port 8080
# If error: Check logs for:
# - Dependency conflicts between WebFlux and MVC
# - MongoDB connection issues
# - Missing bean configurations

# Monitor startup logs for:
# ✓ "Tomcat started on port(s): 8080" (not Netty)
# ✓ "Started Spring Boot application" 
# ✓ MongoDB connection established
# ✓ Kafka producer/consumer initialization
```

### Level 5: Endpoint Verification  
```bash
# Test critical endpoints with curl:

# Test health endpoint
curl -v http://localhost:8080/actuator/health
# Expected: 200 OK with {"status":"UP"}

# Test delete inventory endpoint (requires authentication)
curl -v -X DELETE "http://localhost:8080/api/v1/inventory?consultant=1&client=2&fiscalYear=2023" \
  -H "Authorization: Bearer <test-token>" \
  -H "X-Correlation-Id: test-correlation-id" \
  -H "Request-Id: test-request-id"
# Expected: 204 No Content (or 401 if authentication required)

# Test initial load endpoint  
curl -v -X POST "http://localhost:8080/api/v1/initial-load?consultant=1&client=2&fiscalYear=2023&baseVersion=1&deltaVersion=2" \
  -H "Authorization: Bearer <test-token>"
# Expected: 202 Accepted for async processing

# If 500 errors: Check application logs for exceptions
# If 404 errors: Verify controller request mappings
# If timeout: Check for blocking operations in non-blocking contexts
```

### Level 6: Full Test Suite
```bash
# Run complete test suite:
mvn clean verify

# Expected results:
# ✓ All unit tests pass (200+ tests)
# ✓ All integration tests pass
# ✓ Code coverage > 90% (same as before migration)
# ✓ No findbugs/spotbugs violations
# ✓ Checkstyle passes

# Critical test categories that must pass:
# ✓ Repository tests (MongoDB operations)
# ✓ Service tests (business logic) 
# ✓ Controller tests (HTTP layer)
# ✓ Integration tests (end-to-end flows)
# ✓ Cucumber BDD tests (business scenarios)

# If failures:
# 1. Identify failing test category
# 2. Check for remaining reactive patterns in code
# 3. Verify mock configurations updated for synchronous patterns
# 4. Ensure proper @Transactional usage for database operations
```

### Level 7: Performance Validation
```bash  
# Run performance comparison (if benchmarks exist):
# Compare memory usage, response times, throughput

# Monitor application metrics:
curl http://localhost:8080/actuator/metrics/http.server.requests
# Expected: Similar or better performance than WebFlux version

# MongoDB operation metrics:
curl http://localhost:8080/actuator/metrics/mongodb.driver.commands
# Expected: Consistent operation times, proper connection pooling

# JVM thread usage:
curl http://localhost:8080/actuator/metrics/jvm.threads.live
# Expected: Higher thread count than WebFlux (expected for MVC)
```

## Final Validation Checklist
- [ ] Application starts successfully: `mvn spring-boot:run`
- [ ] All tests pass: `mvn clean verify` 
- [ ] No WebFlux dependencies in pom.xml
- [ ] No reactive imports (Mono, Flux, ServerWebExchange) in source code
- [ ] All controllers return ResponseEntity types
- [ ] All repositories use MongoTemplate/MongoRepository
- [ ] All services return concrete types (not reactive publishers)
- [ ] MongoDB configuration uses synchronous MongoClient
- [ ] Circuit breakers use synchronous decorators
- [ ] Tests use MockMvc instead of WebTestClient
- [ ] Performance meets expectations (benchmark if available)
- [ ] All endpoints respond with correct HTTP contracts
- [ ] Kafka integration works unchanged
- [ ] Error handling preserves API contracts
- [ ] Logging and metrics integration functional
- [ ] Security works with HttpServletRequest context

---

## Anti-Patterns to Avoid
- ❌ Don't use `.block()` on reactive types - convert to synchronous patterns entirely
- ❌ Don't mix WebFlux and MVC dependencies - they cannot coexist
- ❌ Don't keep ServerWebExchange in MVC controllers - use HttpServletRequest/Response
- ❌ Don't use ReactiveMongoTemplate in MVC - use standard MongoTemplate
- ❌ Don't keep reactive circuit breaker operators - use synchronous decorators
- ❌ Don't use WebTestClient in MVC tests - use MockMvc for web layer testing
- ❌ Don't ignore transaction boundaries - use @Transactional appropriately
- ❌ Don't forget to update thread pool configuration for MVC workload
- ❌ Don't change API contracts during migration - preserve HTTP semantics
- ❌ Don't skip integration testing - ensure end-to-end flows work correctly

---

## PRP Confidence Score: 9/10

**Strengths:**
- Comprehensive current state analysis with 84 identified reactive methods
- Complete migration path with specific file-by-file instructions
- Executable validation gates at multiple levels
- Real code examples from the actual codebase
- External research validation of migration patterns
- Preserved business logic and API contracts

**Risk Mitigation:**
- Progressive validation ensures each step works before proceeding
- Detailed error handling for common migration pitfalls
- Performance validation to ensure no regressions
- Complete test suite migration to maintain quality

**Success Probability:** Very High - The migration is well-understood, Spring MVC is mature, and the validation loops will catch issues early.