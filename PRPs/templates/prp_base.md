name: "Spring MVC PRP Template (Context-Rich with Validation Loops)"
description: |

## Purpose
Template optimized for AI agents to implement Spring MVC migration features with sufficient context and self-validation capabilities to achieve working code through iterative refinement.

## Core Principles
1. **Context is King**: Include ALL necessary documentation, examples, and caveats
2. **Validation Loops**: Provide executable tests/lints the AI can run and fix
3. **Information Dense**: Use keywords and patterns from the codebase
4. **Progressive Success**: Start simple, validate, then enhance
5. **Global rules**: Be sure to follow all rules in spring-mvc-guidelines.md

---

## Goal
[What needs to be built - be specific about the end state and desires, e.g., 'Migrate UserServlet to Spring MVC controller with full CRUD operations']

## Why
- [Business value and user impact, e.g., 'Improved maintainability and scalability']
- [Integration with existing features]
- [Problems this solves and for whom, e.g., 'Eliminates manual servlet handling']

## What
[User-visible behavior and technical requirements, e.g., 'RESTful endpoints for users with validation']

### Success Criteria
- [ ] [Specific measurable outcomes, e.g., 'All endpoints pass integration tests']

## All Needed Context

### Documentation & References (list all context needed to implement the feature)
```yaml
# MUST READ - Include these in your context window

# Project Documentation
- file: GUIDELINES.md
  why: Technical implementation standards for this project
  critical: Type safety rules, security patterns, code style

- file: THIS_PROJECT.md  
  why: Project overview, gotchas, and setup requirements
  critical: Spring-specific considerations and migration patterns

# Documentation
- Relevant Documentation for this project is in THIS_PROJECT.md file in the "Documentation" section
```

### Current Codebase tree (run `tree` in the root of the project) to get an overview of the codebase
```bash

```

### Desired Codebase tree with files to be added and responsibility of file
```bash

```

### Known Gotchas of our codebase & Library Quirks
```java
// CRITICAL: [Library name] requires [specific setup]
// Example: Spring Security requires SecurityFilterChain bean
// Example: Spring Data JPA needs @EnableJpaRepositories
// Example: Use jakarta.* namespace, not legacy javax.*

// SPRING-SPECIFIC GOTCHAS:
// CRITICAL: Never use field injection; prefer constructor injection
// CRITICAL: Always annotate controllers with @RestController or @Controller
// CRITICAL: Use @Valid for input validation with BindingResult
// CRITICAL: Configure global exception handling with @ControllerAdvice
// GOTCHA: Spring Boot auto-configuration may conflict with manual config
// GOTCHA: Migration must preserve existing behavior - add tests first
// GOTCHA: Use Spring Profiles for environment-specific config
```

## Implementation Blueprint

### Data models and structure

Create the core data models, we ensure type safety and consistency.
```java
Examples: 
 - JPA entities with @Entity
 - DTO classes with Lombok annotations
 - Validation with @Valid, @NotNull, etc.
 - Mapper classes for entity-DTO conversion

```

### list of tasks to be completed to fullfill the PRP in the order they should be completed

```yaml
Task 1:
MODIFY src/main/java/com/example/controller/ExistingController.java:
  - FIND pattern: "@GetMapping"
  - INJECT after method: new endpoint
  - PRESERVE existing annotations

CREATE src/main/java/com/example/service/NewService.java:
  - MIRROR pattern from: src/main/java/com/example/service/SimilarService.java
  - MODIFY class name and core logic
  - KEEP dependency injection pattern

...(...)

Task N:
...

```


### Per task pseudocode as needed added to each task
```java
// Task 1
// Pseudocode with CRITICAL details dont write entire code
@RestController
public class UserController {
    private final UserService service; // Constructor injection
    
    public UserController(UserService service) { // CRITICAL: No @Autowired
        this.service = service;
    }
    
    @GetMapping("/users/{id}")
    public ResponseEntity<UserDTO> getUser(@PathVariable Long id) {
        // PATTERN: Use Optional for not found
        return service.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    // GOTCHA: Use @Valid for validation
    @PostMapping("/users")
    public ResponseEntity<UserDTO> create(@Valid @RequestBody UserDTO dto) {
        // CRITICAL: Use service layer for business logic
        UserDTO created = service.create(dto);
        return ResponseEntity.created(URI.create("/users/" + created.getId())).body(created);
    }
}
```

### Integration Points
```yaml
DATABASE:
  - migration: "Add column 'feature_enabled' to users table"
  - index: "CREATE INDEX idx_feature_lookup ON users(feature_id)"
  
CONFIG:
  - add to: application.properties
  - pattern: "feature.timeout=${FEATURE_TIMEOUT:30}"
  
ROUTES:
  - add to: com/example/controller/FeatureController.java  
  - pattern: "@GetMapping('/features')"
```

## Validation Loop

### Level 1: Syntax & Style
```bash
# Run these FIRST - fix any errors before proceeding
mvn spotless:apply  # Auto-fix formatting
mvn checkstyle:check  # Style checking
mvn clean compile  # Syntax checking

# Expected: No errors. If errors, READ the error and fix.
```

### Level 2: Unit Tests each new feature/file/function use existing test patterns
```java
// CREATE UserControllerTest.java with these test cases:
@WebMvcTest(UserController.class)
class UserControllerTest {
    @MockBean
    UserService service;
    
    @Test
    void testHappyPath() {
        // Mock service
        when(service.findById(1L)).thenReturn(Optional.of(new User()));
        
        mockMvc.perform(get("/users/1"))
            .andExpect(status().isOk());
    }
    
    @Test
    void testNotFound() {
        when(service.findById(1L)).thenReturn(Optional.empty());
        
        mockMvc.perform(get("/users/1"))
            .andExpect(status().isNotFound());
    }
    
    @Test
    void testValidationError() {
        mockMvc.perform(post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))  // Invalid payload
            .andExpect(status().isBadRequest());
    }
}
```

```bash
# Run and iterate until passing:
mvn test -Dtest=UserControllerTest
# If failing: Read error, understand root cause, fix code, re-run
```

### Level 3: Integration Test
```bash
# Spring Boot Testing
mvn spring-boot:run  # Start server

# Test with curl or Postman
curl -v http://localhost:8080/users/1  # Should return 200
curl -v -X POST http://localhost:8080/users -d '{"name":"test"}' -H 'Content-Type: application/json'  # Should create

# Expected: Endpoints respond correctly, no 500 errors
# If error: Check server logs for exceptions
```

### Level 4: Full Validation
```bash
# Run full build
mvn clean verify  # Includes tests, coverage, etc.

# Expected: All checks pass, coverage >90%
# If error: Fix and re-run
```

## Final validation Checklist
- [ ] All tests pass: `mvn test`
- [ ] No linting errors: `mvn checkstyle:check`
- [ ] No type/compile errors: `mvn clean compile`
- [ ] Server starts: `mvn spring-boot:run`
- [ ] All endpoints tested with curl/Postman
- [ ] Security checks: Unauthorized access denied
- [ ] Validation works: Invalid inputs rejected
- [ ] No legacy javax.* imports (use jakarta.*)
- [ ] Documentation updated if needed

---

## Anti-Patterns to Avoid
- ❌ Don't create new patterns when existing ones work
- ❌ Don't skip validation because "it should work"  
- ❌ Don't ignore failing tests - fix them
- ❌ Don't use field injection (@Autowired on fields)
- ❌ Don't hardcode values that should be config
- ❌ Don't catch all exceptions - be specific
- ❌ Don't use legacy javax.* - always jakarta.* 