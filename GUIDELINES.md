### Spring MVC Context Engineering Guide

This guide defines the standards, patterns, and AI-assisted workflow for migrating an existing **Java** application to the **Spring MVC** (Spring Boot) framework using **Context Engineering** practices.

---
## 1. Core Principles

1. **KISS (Keep It Simple, Stupid)** – Prefer the simplest working solution.
2. **YAGNI (You Aren't Gonna Need It)** – Implement what you need _now_, not what you _might_ need later.
3. **Open/Closed Principle** – New behaviour comes from extension, not modification.
4. **S.O.L.I.D.** – Adhere to clean-code object-oriented design patterns.
5. **Documentation-as-Code** – Keep docs, ADRs, and PRPs inside the repo, updated in the same PR as the code change.

---
## 2. Context Engineering Workflow

> **Context Engineering** is the practice of supplying structured context (requirements, architecture, constraints) that enables AI agents to generate accurate code.

| # | Phase | Human Task | AI-Assisted Task |
|---|-------|-----------|-----------------|
| 1 | Define Requirements | Update `THIS_PROJECT.md` with high-level business & technical goals. |  |
| 2 | Generate PRP | Run the **PRP generator** to create a Product Requirement Prompt in `PRPs/`. | Refine PRP wording, propose acceptance criteria. |
| 3 | Plan | Break PRP into atomic tasks; create / update TODO list. | Suggest task breakdown, risk flags. |
| 4 | Implement | Write / modify code & tests in a feature branch. | Generate boilerplate, mapping layers, DTOs, etc. |
| 5 | Validate | Run build, linter, unit & integration tests. | Fix compile / test errors; suggest improvements. |
| 6 | Review & Merge | Submit PR with description linking PRP & solved TODOs. | Draft PR description & migration notes. |

---
## 3. AI Behaviour Rules

1. **Never assume missing context. Ask questions if uncertain.**
2. **Do NOT hallucinate libraries or APIs.** Use only official Spring, Jakarta, Lombok, SLF4J, Maven plugins, etc.
3. **Confirm file paths & class names** exist before referencing.
4. **Do NOT delete or overwrite existing code** unless explicitly instructed.
5. **Keep files ≤500 LOC**; split into packages when near the limit.
6. **Write unit tests for every public method & every migration step.**
7. **Generate JavaDoc** for each public class, method, and enum.
8. **Use the `jakarta.*` namespace** for all enterprise specifications (e.g., `jakarta.persistence.*`, `jakarta.servlet.*`), not the legacy `javax.*` namespace, which was used in Spring Boot 2.x.
9. **Unresolvable dependencies** These dependencies will be resolved in the target platform later.

---
## 4. Project Structure (Target)

```
project-root/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/example/
│   │   │       ├── Application.java        # Spring Boot entry
│   │   │       ├── controller/             # @RestController classes
│   │   │       ├── service/                # Business logic
│   │   │       ├── repository/             # Spring Data JPA
│   │   │       ├── model/                  # Entities / DTOs
│   │   │       └── config/                 # Java-based configuration
│   │   └── resources/
│   │       ├── application.properties      # Environment-specific properties
│   │       └── db.migration/               # Flyway / Liquibase scripts
│   └── test/
│       └── java/com/example/               # JUnit tests mirroring main tree
├── pom.xml                                 # Maven build
├── README.md                               # How to run, build, deploy
└── spring-mvc-guidelines.md                # << this file
```

---
## 5. Code Structure & Modularity

* **Constructor injection** over `@Autowired` field injection.
* **One class, one responsibility.** Split controllers by REST resource, services by domain concept.
* **Layer boundaries:** Controller → Service → Repository.
* **No business logic in controllers or repositories.**
* **Configuration** lives in `config/` package, using `@Configuration` classes.

---
## 6. Build & Tooling (Maven)

```bash
# Clean & build
mvn clean install

# Run tests with coverage
mvn test jacoco:report

# Run the app locally
mvn spring-boot:run

# Spotless & Checkstyle
mvn spotless:apply checkstyle:check
```

Key plugins (declare in `pom.xml`):

* `spring-boot-starter-parent` (version 3.x)
* `spring-boot-starter-web`
* `spring-boot-starter-data-jpa`
* `spring-boot-starter-validation`
* `spring-boot-starter-test` (scope `test`)
* `lombok` (scope `provided`)
* `spotless-maven-plugin` for google-java-format
* `jacoco-maven-plugin` for coverage (≥90% line coverage target)
* `maven-compiler-plugin` to enforce Java 17+

---
## 7. Testing & Reliability

* Use **JUnit 5** (`@ExtendWith(SpringExtension.class)` for Spring tests, `@ExtendWith(MockitoExtension.class)` for plain unit tests).
* Use **AssertJ** for fluent assertions and **Mockito** for creating mocks/spies.
* Each new method requires:
  * 1 _happy path_ test.
  * 1 _edge case_ test.
  * 1 _failure / exception_ test.
* Tests mirror the package of the class they test.
* Integration tests are placed under `src/test/java` with `@SpringBootTest`.

---
## 8. Logging & Error Handling

* Use **SLF4J** (`LoggerFactory.getLogger(...)`).
* Externalise log pattern & level via `application.properties`.
* Global error handling via `@RestControllerAdvice`.
* NEVER expose stack traces or internal messages to clients; return problem-details JSON.

---
## 9. Security

* **CSRF** enabled (unless stateless API with JWT).
* **HTTPS** enforced in production; HSTS headers; no plain HTTP.
* Validate & sanitise **all** inputs (`@Valid`, `@Pattern`, etc.).
* Secure endpoints with method-level security (`@PreAuthorize`, `@RolesAllowed`).
* Keep dependencies updated (use `mvn versions:display-dependency-updates`).

Example Spring Security Config:

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity // Enables method-level security like @PreAuthorize
public class SecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // For stateless APIs (e.g., using JWTs), CSRF can be disabled.
            // For stateful, session-based applications, it should be enabled.
            .csrf(AbstractHttpConfigurer::disable) 
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/public/**", "/actuator/health").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            // Example for basic auth, replace with JWT filter for real apps
            .httpBasic(Customizer.withDefaults()); 
        return http.build();
    }
}
```

---
## 10. Migration Patterns

| Legacy | Spring MVC Equivalent |
|--------|-----------------------|
| `HttpServlet#doGet` | `@GetMapping` method in `@RestController` |
| `web.xml` filters | `@Configuration` class with `FilterRegistrationBean` |
| JSTL JSP views | Thymeleaf templates or React SPA |
| Direct JDBC | Spring Data JPA / JdbcTemplate |
| `web.xml` servlet mapping | `@RequestMapping` annotations |
| ServletContext init params | `@ConfigurationProperties` classes |
| HttpSession attributes | `@SessionAttributes` or Redis sessions |

### Detailed Migration Examples

#### Servlet to Controller Migration

**Before (Legacy Servlet):**

```java
@WebServlet("/users/*")
public class UserServlet extends HttpServlet {
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        String pathInfo = request.getPathInfo();
        String userId = pathInfo.substring(1); // Remove leading slash
        
        // Manual parameter extraction
        String format = request.getParameter("format");
        
        // Business logic
        User user = userService.findById(Long.parseLong(userId));
        
        // Manual response handling
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        out.print(objectMapper.writeValueAsString(user));
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        // Read JSON from request body manually
        StringBuilder sb = new StringBuilder();
        String line;
        try (BufferedReader reader = request.getReader()) {
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        
        User user = objectMapper.readValue(sb.toString(), User.class);
        User saved = userService.save(user);
        
        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_CREATED);
        PrintWriter out = response.getWriter();
        out.print(objectMapper.writeValueAsString(saved));
    }
}
```

**After (Spring MVC Controller):**

```java
@RestController
@RequestMapping("/users")
@Validated
public class UserController {
    
    private final UserService userService;
    
    public UserController(UserService userService) {
        this.userService = userService;
    }
    
    @GetMapping("/{userId}")
    public ResponseEntity<User> getUser(
            @PathVariable @Positive Long userId,
            @RequestParam(defaultValue = "json") String format) {
        
        User user = userService.findById(userId);
        return ResponseEntity.ok(user);
    }
    
    @PostMapping
    public ResponseEntity<User> createUser(@Valid @RequestBody User user) {
        User saved = userService.save(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }
    
    @PutMapping("/{userId}")
    public ResponseEntity<User> updateUser(
            @PathVariable @Positive Long userId,
            @Valid @RequestBody User user) {
        
        User updated = userService.update(userId, user);
        return ResponseEntity.ok(updated);
    }
    
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable @Positive Long userId) {
        userService.delete(userId);
        return ResponseEntity.noContent().build();
    }
}
```

#### Configuration Migration

**Before (web.xml):**

```xml
<web-app>
    <context-param>
        <param-name>contextConfigLocation</param-name>
        <param-value>/WEB-INF/spring/root-context.xml</param-value>
    </context-param>
    
    <servlet>
        <servlet-name>appServlet</servlet-name>
        <servlet-class>org.springframework.web.servlet.DispatcherServlet</servlet-class>
        <init-param>
            <param-name>contextConfigLocation</param-name>
            <param-value>/WEB-INF/spring/servlet-context.xml</param-value>
        </init-param>
        <load-on-startup>1</load-on-startup>
    </servlet>
    
    <servlet-mapping>
        <servlet-name>appServlet</servlet-name>
        <url-pattern>/</url-pattern>
    </servlet-mapping>
    
    <filter>
        <filter-name>characterEncodingFilter</filter-name>
        <filter-class>org.springframework.web.filter.CharacterEncodingFilter</filter-class>
        <init-param>
            <param-name>encoding</param-name>
            <param-value>UTF-8</param-value>
        </init-param>
    </filter>
</web-app>
```

**After (Java Config):**

```java
public class WebAppInitializer extends AbstractAnnotationConfigDispatcherServletInitializer {
    
    @Override
    protected Class<?>[] getRootConfigClasses() {
        return new Class<?>[] { RootConfig.class };
    }
    
    @Override
    protected Class<?>[] getServletConfigClasses() {
        return new Class<?>[] { WebConfig.class };
    }
    
    @Override
    protected String[] getServletMappings() {
        return new String[] { "/" };
    }
    
    @Override
    protected Filter[] getServletFilters() {
        CharacterEncodingFilter characterEncodingFilter = new CharacterEncodingFilter();
        characterEncodingFilter.setEncoding("UTF-8");
        characterEncodingFilter.setForceEncoding(true);
        
        return new Filter[] { characterEncodingFilter };
    }
}

@Configuration
@EnableWebMvc
@ComponentScan("com.example.web")
public class WebConfig implements WebMvcConfigurer {
    
    @Override
    public void configureViewResolvers(ViewResolverRegistry registry) {
        registry.jsp("/WEB-INF/views/", ".jsp");
    }
    
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/resources/**")
                .addResourceLocations("/resources/")
                .setCachePeriod(31556926);
    }
}
```

### Annotation Migration Patterns

#### Request Mapping Migration

**Before (URL Pattern Matching):**

```java
// Multiple servlets for different HTTP methods
@WebServlet("/api/products")
public class ProductGetServlet extends HttpServlet { /* ... */ }

@WebServlet("/api/products")  
public class ProductPostServlet extends HttpServlet { /* ... */ }
```

**After (Unified Controller):**

```java
@RestController
@RequestMapping("/api/products")
public class ProductController {
    
    @GetMapping
    public List<Product> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return productService.findAll(PageRequest.of(page, size));
    }
    
    @GetMapping("/{id}")
    public Product getProduct(@PathVariable Long id) {
        return productService.findById(id);
    }
    
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Product createProduct(@Valid @RequestBody Product product) {
        return productService.save(product);
    }
}
```

### Additional Migration Tips

- **Error Page Migration**: Replace `<error-page>` in web.xml with `@ControllerAdvice` global exception handlers
- **Session Management**: Migrate from `HttpSession` direct manipulation to `@SessionAttributes` or externalized session storage
- **Security Migration**: Replace security-constraint in web.xml with Spring Security Java config
- **CORS Migration**: Replace custom CORS filters with `@CrossOrigin` annotations or global CORS configuration

---
## 11. Database & Persistence

* Prefer **Spring Data JPA**; repositories extend `JpaRepository<T, ID>`.
* Use **Flyway** (or Liquibase) for schema migration; auto-run on app start.
* Entity classes are immutable where feasible; use Lombok `@Builder` + `@With`.

### JPA Configuration Example

```java
@Configuration
@EnableJpaRepositories(basePackages = "com.example.repository")
@EnableTransactionManagement
public class JpaConfig {
    
    @Bean
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:h2:mem:testdb");
        config.setUsername("sa");
        config.setPassword("");
        return new HikariDataSource(config);
    }
    
    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource());
        em.setPackagesToScan("com.example.model");
        em.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        
        Properties props = new Properties();
        props.setProperty("hibernate.hbm2ddl.auto", "validate");
        props.setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
        em.setJpaProperties(props);
        
        return em;
    }
    
    @Bean
    public PlatformTransactionManager transactionManager(EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }
}
```

---
## 12. Code Style

* Follow **Google Java Style** (`spotless:apply` enforces).
* **Lombok** annotations (`@Getter`, `@Builder`) for boilerplate only; avoid overuse (`@Data`).
* **No wildcard imports.**
* Line length 120 chars.

---
## 13. Documentation

* Add JavaDoc to every public class/method.
* Keep **README.md** up-to-date with build & run instructions.
* Architectural decisions captured as `docs/adr/NNN-title.md` (use [ADR template](https://adr.github.io/)).

---
## 14. Pull Request Checklist

- [ ] Linked PRP & ticket numbers
- [ ] All unit & integration tests pass (`mvn test`)
- [ ] Coverage ≥ 90%
- [ ] Spotless & Checkstyle pass
- [ ] No new TODO/FIXME left in code
- [ ] PR description follows template & includes migration notes

---
## 15. Quick Commands

```bash
# Generate a new Spring Boot project (if needed)
curl https://start.spring.io/starter.tgz \
  -d dependencies=web,data-jpa,lombok,validation \
  -d javaVersion=17 | tar -xzvf -

# Run only tests for a single module
mvn -pl :my-module test

# Update a dependency version interactively
mvn versions:use-dep-version -Dincludes=com.fasterxml.jackson.core:jackson-databind -DdepVersion=2.17.0 -DforceVersion=true
```

---
## 16. Performance Considerations

- Use caching with @Cacheable for expensive operations.
- Optimize database queries with @Query hints or indexing.
- Monitor with Spring Boot Actuator endpoints.
- Profile with tools like VisualVM or YourKit.

Example Caching:

```java
@Service
public class CachedService {
    @Cacheable("data")
    public Data getData(String key) {
        // expensive operation
        return data;
    }
}
```

---
## 17. Error Handling & Validation

### Global Exception Handling

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> 
            errors.put(error.getField(), error.getDefaultMessage()));
        
        ErrorResponse errorResponse = new ErrorResponse("Validation failed", errors);
        return ResponseEntity.badRequest().body(errorResponse);
    }
    
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(EntityNotFoundException ex) {
        logger.warn("Entity not found: {}", ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse("Resource not found", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        logger.error("Unexpected error occurred", ex);
        ErrorResponse errorResponse = new ErrorResponse("Internal server error", "An unexpected error occurred");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}

@Data
@AllArgsConstructor
public class ErrorResponse {
    private String message;
    private Object details;
}
```

### Input Validation

```java
@RestController
@Validated
public class UserController {
    
    @PostMapping("/users")
    public ResponseEntity<User> createUser(@Valid @RequestBody CreateUserRequest request) {
        // Spring automatically validates the request body
        User user = userService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }
    
    @GetMapping("/users/{id}")
    public ResponseEntity<User> getUser(@PathVariable @Positive Long id) {
        // Path variable validation
        User user = userService.findById(id);
        return ResponseEntity.ok(user);
    }
}

@Data
public class CreateUserRequest {
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters")
    private String name;
    
    @Email(message = "Email should be valid")
    @NotBlank(message = "Email is required")
    private String email;
    
    @Min(value = 18, message = "Age must be at least 18")
    @Max(value = 120, message = "Age must be less than 120")
    private Integer age;
}
```

---
## 18. Testing Patterns

### Controller Testing

```java
@WebMvcTest(UserController.class)
class UserControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private UserService userService;
    
    @Test
    void shouldReturnUser_WhenValidIdProvided() throws Exception {
        // Given
        User user = User.builder()
                .id(1L)
                .name("John Doe")
                .email("john@example.com")
                .build();
        
        when(userService.findById(1L)).thenReturn(user);
        
        // When & Then
        mockMvc.perform(get("/users/1"))
                .andExpect(status().isOk())
                .andExpected(jsonPath("$.id").value(1))
                .andExpected(jsonPath("$.name").value("John Doe"))
                .andExpected(jsonPath("$.email").value("john@example.com"));
    }
    
    @Test
    void shouldReturnValidationError_WhenInvalidUserProvided() throws Exception {
        // Given
        String invalidUser = """
            {
                "name": "",
                "email": "invalid-email",
                "age": 15
            }
            """;
        
        // When & Then
        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidUser))
                .andExpect(status().isBadRequest())
                .andExpected(jsonPath("$.message").value("Validation failed"));
    }
}
```

### Integration Testing

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = "classpath:application-test.properties")
class UserControllerIntegrationTest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private UserRepository userRepository;
    
    @Test
    void shouldCreateAndRetrieveUser() {
        // Given
        CreateUserRequest request = new CreateUserRequest();
        request.setName("Jane Doe");
        request.setEmail("jane@example.com");
        request.setAge(25);
        
        // When - Create user
        ResponseEntity<User> createResponse = restTemplate.postForEntity(
                "/users", request, User.class);
        
        // Then
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createResponse.getBody().getName()).isEqualTo("Jane Doe");
        
        // When - Retrieve user
        Long userId = createResponse.getBody().getId();
        ResponseEntity<User> getResponse = restTemplate.getForEntity(
                "/users/" + userId, User.class);
        
        // Then
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody().getName()).isEqualTo("Jane Doe");
    }
}
```

---
### Remember

> *Perfect migrations are not one-off code dumps—they are repeatable, testable, and well-documented transformations guided by AI-assisted context.*

Keep iterations small, commit often, and let the tests—and this guide—be your safety net. 