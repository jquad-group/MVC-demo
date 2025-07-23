# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

<a name="unreleased"></a>

## [Unreleased]

- chore(deps): update dependency io.swagger.parser.v3:swagger-parser to v2.1.31 
- chore(deps): update dependency datev.acds:acds-api-library to v1.3.6 


<a name="1.7.35"></a>

## Version [1.7.35] - 2025-07-04

<a name="1.7.34"></a>

## Version [1.7.34] - 2025-06-26
### Others
- **deps:** update minor dependencies (with automerge)
- **deps:** update minor dependencies (with automerge)

### Renovate


| datasource | package | from   | to     |
| ---------- | --------------------------------------------------- | ------ | ------ |
| maven      | io.swagger.core.v3:swagger-models                   | 2.2.33 | 2.2.34 |
| maven      | io.swagger.parser.v3:swagger-parser                 | 2.1.29 | 2.1.30 |
| maven      | datev.acds:acds-api-library                         | 1.3.0  | 1.3.5  |
| maven      | org.modelmapper.extensions:modelmapper-spring       | 3.2.3  | 3.2.4  |
| maven      | io.swagger.core.v3:swagger-annotations              | 2.2.33 | 2.2.34 |
| maven      | org.springframework.boot:spring-boot-starter-parent | 3.5.0  | 3.5.3  |

| datasource | package | from   | to     |
| ---------- | -------------------------------------- | ------ | ------ |
| maven      | io.swagger.core.v3:swagger-models      | 2.2.32 | 2.2.33 |
| maven      | org.wiremock:wiremock-standalone       | 3.13.0 | 3.13.1 |
| maven      | io.swagger.core.v3:swagger-annotations | 2.2.32 | 2.2.33 |


<a name="1.7.33"></a>

## Version [1.7.33] - 2025-06-13

<a name="1.7.32"></a>

## Version [1.7.32] - 2025-06-13
### Features
- added datev-block-dcal-bff-policy, renamed invoke policy title to "invoke paas backend", removed rate-limit


<a name="1.7.31"></a>

## Version [1.7.31] - 2025-06-02

<a name="1.7.30"></a>

## Version [1.7.30] - 2025-06-02
### Features
- fix: schemaUpdate handle acds-no-content

### Others
- **deps:** update minor dependencies (with automerge)
- **deps:** update minor dependencies (with automerge)

### Renovate


| datasource | package | from   | to     |
| ---------- | --------------------------------------------------- | ------ | ------ |
| maven      | io.swagger.parser.v3:swagger-parser                 | 2.1.28 | 2.1.29 |
| maven      | org.apache.maven.plugins:maven-surefire-plugin      | 3.5.2  | 3.5.3  |
| maven      | datev.acds:acds-api-library                         | 1.2.1  | 1.3.0  |
| maven      | org.springframework.boot:spring-boot-starter-parent | 3.4.5  | 3.5.0  |

| datasource | package | from   | to     |
| ---------- | --------------------------------------------- | ------ | ------ |
| maven      | io.swagger.core.v3:swagger-models             | 2.2.30 | 2.2.32 |
| maven      | io.swagger.parser.v3:swagger-parser           | 2.1.26 | 2.1.28 |
| maven      | org.modelmapper.extensions:modelmapper-spring | 3.2.2  | 3.2.3  |
| maven      | io.swagger.core.v3:swagger-annotations        | 2.2.30 | 2.2.32 |


<a name="1.7.29"></a>

## Version [1.7.29] - 2025-05-26
### Bug Fixes
- pin version for surefireplugin to 3.5.2 ([#345](https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/issues/345))

### Features
- **statistics:** add number of accounting entries (movementdata) to logcontext.


<a name="1.7.28"></a>

## Version [1.7.28] - 2025-05-21
### Bug Fixes
- set useFlag when delta-event with movementdata on new accounts

### Others
- Abkündigung und Entfernung AppDynamics Service #https://git.datev.de/refsys-online/product-team/-/issues/703
- Abkündigung und Entfernung AppDynamics Service #https://git.datev.de/refsys-online/product-team/-/issues/703
- **deps:** update minor dependencies (with automerge)

### Renovate


| datasource | package | from   | to     |
| ---------- | --------------------------------------------------- | ------ | ------ |
| maven      | io.swagger.core.v3:swagger-models                   | 2.2.29 | 2.2.30 |
| maven      | datev.acds:acds-api-library                         | 1.1.0  | 1.2.1  |
| maven      | org.wiremock:wiremock-standalone                    | 3.12.1 | 3.13.0 |
| maven      | io.swagger.core.v3:swagger-annotations              | 2.2.29 | 2.2.30 |
| maven      | org.springframework.boot:spring-boot-starter-parent | 3.4.4  | 3.4.5  |


<a name="1.7.27"></a>

## Version [1.7.27] - 2025-04-10

<a name="1.7.26"></a>

## Version [1.7.26] - 2025-04-10
### Bug Fixes
- set isOrganisationData true, if copyFromComprehensiveConsultant is not 0

### Others
- **deps:** update dependency org.jacoco:jacoco-maven-plugin to v0.8.13

### Renovate


| datasource | package | from   | to     |
| ---------- | ------------------------------ | ------ | ------ |
| maven      | org.jacoco:jacoco-maven-plugin | 0.8.12 | 0.8.13 |


<a name="1.7.25"></a>

## Version [1.7.25] - 2025-04-04
### Bug Fixes
- log update-version-StateDoc-Checks with VersionInfos
- unittestIT added
- log warn when ResponseStatusException in updateSchema

### Features
- log update-version-StateDoc-Checks with VersionInfos


<a name="1.7.24"></a>

## Version [1.7.24] - 2025-04-04

<a name="1.7.23"></a>

## Version [1.7.23] - 2025-04-03
### Bug Fixes
- log warn when conflict or bad-request


<a name="1.7.22"></a>

## Version [1.7.22] - 2025-04-02
### Bug Fixes
- Resolve "build errors" regarding splunk usage in logger tests

### Others
- **deps:** update minor dependencies (with automerge)

### Renovate


| datasource | package | from   | to     |
| ---------- | ----------------------------------- | ------ | ------ |
| maven      | io.swagger.parser.v3:swagger-parser | 2.1.25 | 2.1.26 |
| maven      | datev.acds:acds-api-library         | 1.0.15 | 1.1.0  |


<a name="1.7.21"></a>

## Version [1.7.21] - 2025-03-27
### Features
- [#331](https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/issues/331) schema version 4


<a name="1.7.20"></a>

## Version [1.7.20] - 2025-03-27
### Features
- [#330](https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/issues/330) limit exception stacktrace


<a name="1.7.19"></a>

## Version [1.7.19] - 2025-03-26
### Features
- [#125](https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/issues/125) logging mask refactoring


<a name="1.7.18"></a>

## Version [1.7.18] - 2025-03-25
### Features
- [#325](https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/issues/325) Erweiterung um Tracing-Logs

### Others
- **deps:** update minor dependencies (with automerge)

### Renovate


| datasource | package | from   | to     |
| ---------- | --------------------------------------------------- | ------ | ------ |
| maven      | datev.acds:acds-api-library                         | 1.0.12 | 1.0.15 |
| maven      | org.instancio:instancio-junit                       | 5.4.0  | 5.4.1  |
| maven      | org.springframework.boot:spring-boot-starter-parent | 3.4.3  | 3.4.4  |


<a name="1.7.17"></a>

## Version [1.7.17] - 2025-03-20

<a name="1.7.16"></a>

## Version [1.7.16] - 2025-03-19
### Features
- [#335](https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/issues/335) updated baseVersion and deltaVersion in MDC LoggingContext
- [#323](https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/issues/323) Add check if accountNumberFrom is bigger than accountNumberTo
- [#316](https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/issues/316) removed KafkaEventConsumer


<a name="1.7.15"></a>

## Version [1.7.15] - 2025-03-13
### Features
- [#290](https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/issues/290) IASD einlesen
- [#332](https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/issues/332) increased the maximum response body size to 6MB

### Others
- **deps:** update minor dependencies (with automerge) to v2.2.29
- **deps:** update minor dependencies (with automerge)

### Renovate


| datasource | package | from   | to     |
| ---------- | -------------------------------------- | ------ | ------ |
| maven      | io.swagger.core.v3:swagger-models      | 2.2.28 | 2.2.29 |
| maven      | io.swagger.core.v3:swagger-annotations | 2.2.28 | 2.2.29 |

| datasource | package | from   | to     |
| ---------- | ---------------------------------------------------- | ------ | ------ |
| maven      | org.wiremock:wiremock-standalone                     | 3.12.0 | 3.12.1 |
| maven      | datev.refsys-online:refsys-aggregation-service-model | 1.2.18 | 1.2.20 |


<a name="1.7.14"></a>

## Version [1.7.14] - 2025-03-06
### Features
- [#328](https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/issues/328) fixed missing context


<a name="1.7.13"></a>

## Version [1.7.13] - 2025-03-03
### Features
- [#327](https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/issues/327) fixed StateDoc initialization for SchemaUpdate and partial imports


<a name="1.7.12"></a>

## Version [1.7.12] - 2025-02-27
### Bug Fixes
- Upgrade DirectMemory to 1GB ([#326](https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/issues/326))


<a name="1.7.11"></a>

## Version [1.7.11] - 2025-02-26
### Features
- [#324](https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/issues/324) fixed incorrect handling of an OutOfMemoryError in the retry predicate for http calls

### Others
- **deps:** update minor dependencies (with automerge)

### Renovate


| datasource | package | from   | to     |
| ---------- | --------------------------------------------------- | ------ | ------ |
| maven      | datev.acds:acds-api-library                         | 1.0.9  | 1.0.12 |
| maven      | au.com.dius.pact.provider:maven                     | 4.6.16 | 4.6.17 |
| maven      | au.com.dius.pact.provider:junit5                    | 4.6.16 | 4.6.17 |
| maven      | au.com.dius.pact.consumer:junit5                    | 4.6.16 | 4.6.17 |
| maven      | org.instancio:instancio-junit                       | 5.3.0  | 5.4.0  |
| maven      | org.wiremock:wiremock-standalone                    | 3.11.0 | 3.12.0 |
| maven      | org.springframework.boot:spring-boot-starter-parent | 3.4.2  | 3.4.3  |


<a name="1.7.10"></a>

## Version [1.7.10] - 2025-02-20
### Features
- [#289](https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/issues/289) near time data account sum days

### Others
- **dependencies:** Rollback OTEL Instrumentation to 2.11.0 & pin in renovate


<a name="1.7.9"></a>

## Version [1.7.9] - 2025-02-18
### Features
- [#322](https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/issues/322) fixed no content response for delete method
- [#219](https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/issues/219) enhance jwt policy 2.0


<a name="1.7.8"></a>

## Version [1.7.8] - 2025-02-14
### Features
- fix: [#320](https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/issues/320) master data context no content fix


<a name="1.7.7"></a>

## Version [1.7.7] - 2025-02-12

<a name="1.7.6"></a>

## Version [1.7.6] - 2025-02-11
### Features
- [#319](https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/issues/319) added industryId and forceReftabCurrentYear to the InitalLoad, industryId to...
- [#318](https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/issues/318) fixed import duration value


<a name="1.7.5"></a>

## Version [1.7.5] - 2025-02-07

<a name="1.7.4"></a>

## Version [1.7.4] - 2025-02-07
### Features
- [#317](https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/issues/317) ACDS lib update to version 1.0.9, use industry_id for account captions and fixed error handling before fire and forget call


<a name="1.7.3"></a>

## Version [1.7.3] - 2025-02-05

<a name="1.7.2"></a>

## Version [1.7.2] - 2025-02-05
### Features
- # 299 Neuer Endpoint für Event-Verarbeitung


<a name="1.7.1"></a>

## Version [1.7.1] - 2025-02-04
### Features
- [#276](https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/issues/276) account purpose mapping selective loading and import service refactoring


<a name="1.7.0"></a>

## Version [1.7.0] - 2025-02-03
### Features
- **API:** New state version update endpoint for internal use from event-processor ([#311](https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/issues/311))

### Others
- **deps:** update dependency org.wiremock:wiremock-standalone to v3.11.0
- **version:** bump version to 1.7.0

### Reverts

- clean uup dependencies

### Renovate


| datasource | package | from   | to     |
| ---------- | -------------------------------- | ------ | ------ |
| maven      | org.wiremock:wiremock-standalone | 3.10.0 | 3.11.0 |


<a name="1.6.6"></a>

## Version [1.6.6] - 2025-01-30
### Others
- **deps:** update minor dependencies (with automerge)

### Reverts

- fix: [#309](https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/issues/309) switch to ACDS v2-API to prevent streaming problems

### Renovate


| datasource | package | from   | to     |
| ---------- | ---------------------------------------------------- | ------ | ------ |
| maven      | org.instancio:instancio-junit                        | 5.2.1  | 5.3.0  |
| maven      | datev.refsys-online:refsys-aggregation-service-model | 1.2.16 | 1.2.17 |
| maven      | org.springframework.boot:spring-boot-starter-parent  | 3.4.1  | 3.4.2  |


<a name="1.6.5"></a>

## Version [1.6.5] - 2025-01-24
### Bug Fixes
- [#309](https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/issues/309) switch to ACDS v2-API to prevent streaming problems


<a name="1.6.4"></a>

## Version [1.6.4] - 2025-01-24
### Features
- **config:** configuration to disable circuitbreaker ([#308](https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/issues/308))


<a name="1.6.3"></a>

## Version [1.6.3] - 2025-01-21
### Features
- [#307](https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/issues/307) target url for api gateway on apps-internal


<a name="1.6.2"></a>

## Version [1.6.2] - 2025-01-21
### Others
- [#307](https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/issues/307) add second route, different for test


<a name="1.6.1"></a>

## Version [1.6.1] - 2025-01-21
### Others
- [#307](https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/issues/307) add second route for internal calls, also for -testing profile


<a name="1.6.0"></a>

## Version [1.6.0] - 2025-01-20
### Others
- Bump Version to 1.6.0


<a name="1.5.10"></a>

## Version [1.5.10] - 2025-01-20
### Features
- **Events:** disable consuming of acds events in favor of new event-processor (QS , Prod)

### Others
- **deps:** update minor dependencies (with automerge)

### Renovate


| datasource | package | from   | to     |
| ---------- | ------------------------------------------------------------------ | ------ | ------ |
| maven      | io.swagger.core.v3:swagger-models                                  | 2.2.27 | 2.2.28 |
| maven      | io.swagger.parser.v3:swagger-parser                                | 2.1.24 | 2.1.25 |
| maven      | io.opentelemetry.instrumentation:opentelemetry-instrumentation-bom | 2.11.0 | 2.12.0 |
| maven      | io.swagger.core.v3:swagger-annotations                             | 2.2.27 | 2.2.28 |


<a name="1.5.9"></a>

## Version [1.5.9] - 2025-01-17
### Bug Fixes
- Set direct Memory size to 64MB to prevent OutOfMemoryErrors in load situations ([#304](https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/issues/304))

### Features
- disable event processing on DEV ([#302](https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/issues/302))


<a name="1.5.8"></a>

## Version [1.5.8] - 2025-01-14

<a name="1.5.7"></a>

## Version [1.5.7] - 2025-01-14
### Features
- switch to turn off kafka_consumer
- [#84](https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/issues/84) tests fuer duplicate key exception
- [#287](https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/issues/287) Add check if event is consistent, add tests
- [#284](https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/issues/284) replace deprecated annotations
- [#285](https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/issues/285) generate acds sources manually
- **Authorization:** Service to Service Kommunikation direkt im Space ermöglichen

### Others
- **deps:** update minor dependencies (with automerge) to v2.3.0

### Renovate


| datasource | package | from  | to    |
| ---------- | ------------------------------------------------ | ----- | ----- |
| maven      | io.github.resilience4j:resilience4j-reactor      | 2.2.0 | 2.3.0 |
| maven      | io.github.resilience4j:resilience4j-spring-boot3 | 2.2.0 | 2.3.0 |


<a name="1.5.6"></a>

## Version [1.5.6] - 2025-01-03
### Features
- **Observability:** Einfache Metriken für Kafka Events

### Others
- **deps:** update dependency io.opentelemetry.instrumentation:opentelemetry-instrumentation-bom to v2.11.0
- **deps:** update dependency org.springframework.boot:spring-boot-starter-parent to v3.4.1

### Renovate


| datasource | package | from   | to     |
| ---------- | ------------------------------------------------------------------ | ------ | ------ |
| maven      | io.opentelemetry.instrumentation:opentelemetry-instrumentation-bom | 2.10.0 | 2.11.0 |

| datasource | package | from  | to    |
| ---------- | --------------------------------------------------- | ----- | ----- |
| maven      | org.springframework.boot:spring-boot-starter-parent | 3.4.0 | 3.4.1 |


<a name="1.5.5"></a>

## Version [1.5.5] - 2024-12-18
### Features
- [#286](https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/issues/286) Disable delta-event in dev


<a name="1.5.4"></a>

## Version [1.5.4] - 2024-12-16
### Features
- [#294](https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/issues/294) set the max.poll.interval.ms for kafka to 15 minutes

### Others
- **deps:** update minor dependencies (with automerge)

### Renovate


| datasource | package | from   | to     |
| ---------- | -------------------------------------- | ------ | ------ |
| maven      | io.swagger.core.v3:swagger-models      | 2.2.26 | 2.2.27 |
| maven      | org.instancio:instancio-junit          | 5.2.0  | 5.2.1  |
| maven      | io.swagger.core.v3:swagger-annotations | 2.2.26 | 2.2.27 |


<a name="1.5.3"></a>

## Version [1.5.3] - 2024-12-11
### Features
- [#293](https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/issues/293) fixed an error in the date correction logic

### Others
- **deps:** update minor dependencies (with automerge)

### Renovate


| datasource | package | from   | to     |
| ---------- | --------------------------------------------- | ------ | ------ |
| maven      | datev.acds:acds-api-library                   | 0.0.96 | 0.0.98 |
| maven      | au.com.dius.pact.provider:maven               | 4.6.15 | 4.6.16 |
| maven      | au.com.dius.pact.provider:junit5              | 4.6.15 | 4.6.16 |
| maven      | au.com.dius.pact.consumer:junit5              | 4.6.15 | 4.6.16 |
| maven      | org.instancio:instancio-junit                 | 5.0.2  | 5.2.0  |
| maven      | org.wiremock:wiremock-standalone              | 3.9.2  | 3.10.0 |
| maven      | org.modelmapper.extensions:modelmapper-spring | 3.2.1  | 3.2.2  |


<a name="1.5.2"></a>

## Version [1.5.2] - 2024-11-28
### Features
- [#283](https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/issues/283) inventories filtering


<a name="1.5.1"></a>

## Version [1.5.1] - 2024-11-27
### Features
- [#281](https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/issues/281) grossbestaende nacharbeiten
- [#267](https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/issues/267) refactored tests that used InsertMongoClient
- [#216](https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/issues/216) addedKafkaLoggingContext, and base für JunitKafka Tests

### Others
- **deps:** update minor dependencies (with automerge)

### Renovate


| datasource | package | from   | to     |
| ---------- | --------------------------------------------------- | ------ | ------ |
| maven      | io.swagger.core.v3:swagger-models                   | 2.2.25 | 2.2.26 |
| maven      | io.swagger.parser.v3:swagger-parser                 | 2.1.23 | 2.1.24 |
| maven      | datev.acds:acds-api-library                         | 0.0.91 | 0.0.96 |
| maven      | io.swagger.core.v3:swagger-annotations              | 2.2.25 | 2.2.26 |
| maven      | org.springframework.boot:spring-boot-starter-parent | 3.3.5  | 3.4.0  |


<a name="1.5.0"></a>

## Version [1.5.0] - 2024-11-19
### Features
- [#270](https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/issues/270) movement data adjustment for large data sizes


<a name="1.4.10"></a>

## Version [1.4.10] - 2024-11-19
### Features
- [#274](https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/issues/274) Add tests for maxResponseBodySize
- [#271](https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/issues/271) Add error log when duplicate inventoryNumbers appear
- [#233](https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/issues/233) circuitbreaker tests schneller machen

### Others
- **deps:** update dependency io.opentelemetry.instrumentation:opentelemetry-instrumentation-bom to v2.10.0
- **deps:** update minor dependencies (with automerge)
- **deps:** update minor dependencies (with automerge)
- **deps:** update minor dependencies (with automerge)

### Renovate


| datasource | package | from  | to     |
| ---------- | ------------------------------------------------------------------ | ----- | ------ |
| maven      | io.opentelemetry.instrumentation:opentelemetry-instrumentation-bom | 2.9.0 | 2.10.0 |

| datasource | package | from   | to     |
| ---------- | ---------------------------------------------------- | ------ | ------ |
| maven      | org.mapstruct:mapstruct-processor                    | 1.6.2  | 1.6.3  |
| maven      | org.mapstruct:mapstruct                              | 1.6.2  | 1.6.3  |
| maven      | datev.refsys-online:refsys-aggregation-service-model | 1.2.15 | 1.2.16 |

| datasource | package | from   | to     |
| ---------- | -------------------------------- | ------ | ------ |
| maven      | datev.acds:acds-api-library      | 0.0.90 | 0.0.91 |
| maven      | au.com.dius.pact.provider:maven  | 4.6.14 | 4.6.15 |
| maven      | au.com.dius.pact.provider:junit5 | 4.6.14 | 4.6.15 |
| maven      | au.com.dius.pact.consumer:junit5 | 4.6.14 | 4.6.15 |

| datasource | package | from   | to     |
| ---------- | --------------------------------------------------- | ------ | ------ |
| maven      | org.apache.maven.plugins:maven-dependency-plugin    | 3.8.0  | 3.8.1  |
| maven      | io.swagger.parser.v3:swagger-parser                 | 2.1.22 | 2.1.23 |
| maven      | datev.acds:acds-api-library                         | 0.0.89 | 0.0.90 |
| maven      | org.wiremock:wiremock-standalone                    | 3.9.1  | 3.9.2  |
| maven      | org.springframework.boot:spring-boot-starter-parent | 3.3.4  | 3.3.5  |


<a name="1.4.9"></a>

## Version [1.4.9] - 2024-10-23
### Features
- [#273](https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/issues/273) increased response body size limit


<a name="1.4.8"></a>

## Version [1.4.8] - 2024-10-21
### Features
- **KMVZ:** activate Delta Event Processing on DEV

### Others
- **acds-api:** additional parameter for accountSumdays api (aggregated-per-day - always true)
- **deps:** update minor dependencies (with automerge)

### Renovate


| datasource | package | from   | to     |
| ---------- | ------------------------------------------------------------------ | ------ | ------ |
| maven      | io.swagger.core.v3:swagger-models                                  | 2.2.24 | 2.2.25 |
| maven      | io.opentelemetry.instrumentation:opentelemetry-instrumentation-bom | 2.8.0  | 2.9.0  |
| maven      | datev.acds:acds-api-library                                        | 0.0.86 | 0.0.89 |
| maven      | datev.refsys-online:refsys-aggregation-service-model               | 1.2.14 | 1.2.15 |
| maven      | io.swagger.core.v3:swagger-annotations                             | 2.2.24 | 2.2.25 |


<a name="1.4.7"></a>

## Version [1.4.7] - 2024-10-11

<a name="1.4.6"></a>

## Version [1.4.6] - 2024-10-10
### Features
- schema-version Update - Anlag Release
- schema-version Update - Anlag Release
- schema-version Update - Anlag Release
- [#210](https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/issues/210) problem info in state doc


<a name="1.4.5"></a>

## Version [1.4.5] - 2024-10-04
### Bug Fixes
- importService.deleteImportData soll auch AInhalt von Anlag Collection löschen

### Features
- Resolve "Aggregation-Service um Kumulierte Abschreibung (Anlag) erweitern"
- [#255](https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/issues/255) increased timeout to 10 minutes


<a name="1.4.4"></a>

## Version [1.4.4] - 2024-10-01
### Features
- [#248](https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/issues/248) Move errorhandling to importAccountingData
- [#250](https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/issues/250) Changed timeout from seconds to milliseconds
- [#249](https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/issues/249) Add logic to mapper from Enums fromValue method
- **KMVZ:** add a feature toggle for delta event processing

### Others
- **deps:** update minor dependencies (with automerge)
- **deps:** update minor dependencies (with automerge)

### Renovate


| datasource | package | from   | to     |
| ---------- | -------------------------------------- | ------ | ------ |
| maven      | io.swagger.core.v3:swagger-models      | 2.2.23 | 2.2.24 |
| maven      | datev.acds:acds-api-library            | 0.0.77 | 0.0.83 |
| maven      | io.swagger.core.v3:swagger-annotations | 2.2.23 | 2.2.24 |

| datasource | package | from   | to     |
| ---------- | --------------------------------------------------- | ------ | ------ |
| maven      | datev.acds:acds-api-library                         | 0.0.73 | 0.0.77 |
| maven      | org.mapstruct:mapstruct-processor                   | 1.6.0  | 1.6.2  |
| maven      | org.mapstruct:mapstruct                             | 1.6.0  | 1.6.2  |
| maven      | org.springframework.boot:spring-boot-starter-parent | 3.3.3  | 3.3.4  |


<a name="1.4.3"></a>

## Version [1.4.3] - 2024-09-19

<a name="1.4.2"></a>

## Version [1.4.2] - 2024-09-18

<a name="1.4.1"></a>

## Version [1.4.1] - 2024-09-18
### Features
- **observability:** Anbindung an grafana.cloud per push (metrics, traces)

### Others
- **deps:** update minor dependencies (with automerge)
- **deps:** update minor dependencies (with automerge)
- **deps:** update minor dependencies (with automerge)

### Renovate


| datasource | package | from   | to     |
| ---------- | ----------------------------- | ------ | ------ |
| maven      | datev.acds:acds-api-library   | 0.0.72 | 0.0.73 |
| maven      | org.instancio:instancio-junit | 5.0.1  | 5.0.2  |

| datasource | package | from   | to     |
| ---------- | ----------------------------------------- | ------ | ------ |
| maven      | datev.acds:acds-api-library               | 0.0.71 | 0.0.72 |
| maven      | io.projectreactor:reactor-core-micrometer | 1.1.9  | 1.1.10 |

| datasource | package | from   | to     |
| ---------- | ------------------------------------------------ | ------ | ------ |
| maven      | org.apache.maven.plugins:maven-dependency-plugin | 3.1.1  | 3.8.0  |
| maven      | io.swagger.core.v3:swagger-models                | 2.2.22 | 2.2.23 |


<a name="1.4.0"></a>

## Version [1.4.0] - 2024-09-05

<a name="1.3.15"></a>

## Version [1.3.15] - 2024-09-04
### Features
- [#217](https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/issues/217) feature/217_LoggingContextFilterChange
- [#231](https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/issues/231) Add circuit breaker state event logger
- [#221](https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/issues/221) Generate acds client yaml from acds-api-library
- [#244](https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/issues/244) refactor ErrorMessageConstants

### Others
- **deps:** update minor dependencies (with automerge)

### Renovate


| datasource | package | from   | to     |
| ---------- | -------------------------------------- | ------ | ------ |
| maven      | au.com.dius.pact.provider:maven        | 4.6.13 | 4.6.14 |
| maven      | au.com.dius.pact.provider:junit5       | 4.6.13 | 4.6.14 |
| maven      | au.com.dius.pact.consumer:junit5       | 4.6.13 | 4.6.14 |
| maven      | io.swagger.core.v3:swagger-annotations | 2.2.22 | 2.2.23 |


<a name="1.3.14"></a>

## Version [1.3.14] - 2024-08-27
### Features
- [#242](https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/issues/242) moved db circuitbreaker and retry implementations to repositories
- [#237](https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/issues/237) enum erweiterungen

### Others
- **Create Changelog:** clean workspace and only commit changelog
- **deps:** update dependency org.springframework.boot:spring-boot-starter-parent to v3.3.3
- **deps:** update minor dependencies (with automerge)

### Renovate


| datasource | package | from  | to    |
| ---------- | --------------------------------------------------- | ----- | ----- |
| maven      | org.springframework.boot:spring-boot-starter-parent | 3.3.2 | 3.3.3 |

| datasource | package | from        | to    |
| ---------- | ---------------------------------------------------- | ----------- | ----- |
| maven      | org.mapstruct:mapstruct-processor                    | 1.5.5.Final | 1.6.0 |
| maven      | org.mapstruct:mapstruct                              | 1.5.5.Final | 1.6.0 |
| maven      | io.projectreactor:reactor-core-micrometer            | 1.1.8       | 1.1.9 |
| maven      | datev.refsys-online:refsys-aggregation-service-model | 1.2.6       | 1.2.7 |


<a name="1.3.13"></a>

## Version [1.3.13] - 2024-08-13
### Code Refactoring
- correct naming of persongroup account ranges

### Features
- feat: nach dem Löschen wird Wertermittlungs-Service via Kafka informiert


<a name="1.3.12"></a>

## Version [1.3.12] - 2024-08-12
### Features
- feature: Neuer Endpunkt zum Löschen von Beständen ([#239](https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/issues/239))


<a name="1.3.11"></a>

## Version [1.3.11] - 2024-08-12
### Features
- Feature/227 kafka producer

### Others
- **deps:** update minor dependencies (with automerge)
- **deps:** update dependency org.instancio:instancio-junit to v5

### Renovate


| datasource | package | from   | to     |
| ---------- | -------------------------------- | ------ | ------ |
| maven      | org.awaitility:awaitility        | 4.2.1  | 4.2.2  |
| maven      | au.com.dius.pact.provider:maven  | 4.6.11 | 4.6.13 |
| maven      | au.com.dius.pact.provider:junit5 | 4.6.11 | 4.6.13 |
| maven      | au.com.dius.pact.consumer:junit5 | 4.6.11 | 4.6.13 |

| datasource | package | from  | to    |
| ---------- | ----------------------------- | ----- | ----- |
| maven      | org.instancio:instancio-junit | 4.8.1 | 5.0.1 |


<a name="1.3.10"></a>

## Version [1.3.10] - 2024-08-08
### Features
- [#236](https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/issues/236) feature/236_Exception_Handling_Mapper
- [#236](https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/issues/236) feature/236_Exception_Handling_Mapper
- [#234](https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/issues/234) Add check if clientId is zero on changedEvents, add Test

### Others
- **Jenkins:** set pact broker url via pipelinesettings
- **deps:** update minor dependencies (with automerge)

### Reverts

- feat: [#236](https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/issues/236) feature/236_Exception_Handling_Mapper first commit

### Renovate


| datasource | package | from  | to    |
| ---------- | --------------------------------------------- | ----- | ----- |
| maven      | org.wiremock:wiremock-standalone              | 3.9.0 | 3.9.1 |
| maven      | org.modelmapper.extensions:modelmapper-spring | 3.2.0 | 3.2.1 |


<a name="1.3.9"></a>

## Version [1.3.9] - 2024-07-26
### Features
- [#213](https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/issues/213) retry configuration for clients
- [#229](https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/issues/229) removed the upsert when an error happens before a state doc is inserted into the db
- [#230](https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/issues/230) added new Enum value for the PermittedAccountReasonEnum and added missing...
- [#207](https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/issues/207) Timeout für ACDS-Requests auf 5 min setzen
- [#153](https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/issues/153) MongoDB Resilienz

### Others
- **deps:** update minor dependencies (with automerge)

### Renovate


| datasource | package | from  | to    |
| ---------- | --------------------------------------------------- | ----- | ----- |
| maven      | io.projectreactor:reactor-test                      | 3.6.7 | 3.6.8 |
| maven      | org.wiremock:wiremock-standalone                    | 3.8.0 | 3.9.0 |
| maven      | io.projectreactor:reactor-core-micrometer           | 1.1.7 | 1.1.8 |
| maven      | org.springframework.boot:spring-boot-starter-parent | 3.3.1 | 3.3.2 |


<a name="1.3.8"></a>

## Version [1.3.8] - 2024-07-18
### Features
- [#226](https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/issues/226) changed the response status to 409 when an import is already running or an import is interrupted


<a name="1.3.7"></a>

## Version [1.3.7] - 2024-07-17
### Features
- **KMVZ:** Basic Delta Event processing - triggers an Initial Load


<a name="1.3.6"></a>

## Version [1.3.6] - 2024-07-12
### Bug Fixes
- **EventProcessing:** Trigger Initial Load on Events without type

### Features
- [#149](https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/issues/149) Serviceresilienz http-Calls
- [#208](https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/issues/208) db schema versionierung


<a name="1.3.5"></a>

## Version [1.3.5] - 2024-07-10

<a name="1.3.4"></a>

## Version [1.3.4] - 2024-07-10
### Features
- [#187](https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/issues/187) forbid parallel imports execution

### Others
- **deps:** update minor dependencies (with automerge)

### Renovate


| datasource | package | from  | to     |
| ---------- | ---------------------------------------------------- | ----- | ------ |
| maven      | au.com.dius.pact.provider:maven                      | 4.6.9 | 4.6.11 |
| maven      | au.com.dius.pact.provider:junit5                     | 4.6.9 | 4.6.11 |
| maven      | au.com.dius.pact.consumer:junit5                     | 4.6.9 | 4.6.11 |
| maven      | datev.refsys-online:refsys-aggregation-service-model | 1.2.4 | 1.2.5  |


<a name="1.3.3"></a>

## Version [1.3.3] - 2024-07-04
### Bug Fixes
- **pact:** pact test fKafka

### Features
- [#215](https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/issues/215) temporary fixes for kafka bugs


<a name="1.3.2"></a>

## Version [1.3.2] - 2024-07-02
### Features
- JSON Kafka Consumer Test

### Others
- **deps:** update service model


<a name="1.3.1"></a>

## Version [1.3.1] - 2024-07-02
### Features
- [#209](https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/issues/209) updated openapi for SchemaUpdate

### Others
- **deployment:** [#214](https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/issues/214) -  change scaling settings; use three instances in prod
- **deps:** update minor dependencies (with automerge)
- **jenkins:** [#212](https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/issues/212) - remove ci-skip evaluator from changelog generation

### Renovate


| datasource | package | from   | to     |
| ---------- | --------------------------------------- | ------ | ------ |
| maven      | org.junit.platform:junit-platform-suite | 1.10.2 | 1.10.3 |
| maven      | org.instancio:instancio-junit           | 4.8.0  | 4.8.1  |
| maven      | org.wiremock:wiremock-standalone        | 3.7.0  | 3.8.0  |


<a name="1.3.0"></a>

## Version [1.3.0] - 2024-06-27
### Features
- [#205](https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/issues/205) - yaml anpassungen fur acds contract first
- [#196](https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/issues/196) parkuhr implementierung

### Others
- **jenkins:** [#212](https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/issues/212) - generate changelog always on develop
- **jenkins:** [#212](https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/issues/212) move changelog generation to end of pipeline for release

### Performance Improvements
- **mapping:** [#211](https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/issues/211) - Change Amount Conversion from string to Math.Round


<a name="1.2.2"></a>

## Version [1.2.2] - 2024-06-24
### Features
- [#192](https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/issues/192) ProblemInfo in StateDoc schreiben für unseren Fehler

### Others
- **deps:** update minor dependencies (with automerge)
- **deps:** update dependency org.instancio:instancio-junit to v4.8.0
- **deps:** Update to Spring Boot 3.3 and MongoDriver 5.0.1
- **deps:** update minor dependencies (with automerge)

### Renovate


| datasource | package | from   | to     |
| ---------- | --------------------------------------------------- | ------ | ------ |
| maven      | io.jsonwebtoken:jjwt                                | 0.12.5 | 0.12.6 |
| maven      | org.wiremock:wiremock-standalone                    | 3.6.0  | 3.7.0  |
| maven      | org.springframework.boot:spring-boot-starter-parent | 3.3.0  | 3.3.1  |

| datasource | package | from  | to    |
| ---------- | ----------------------------- | ----- | ----- |
| maven      | org.instancio:instancio-junit | 4.7.0 | 4.8.0 |

| datasource | package | from          | to     |
| ---------- | --------------------------------------------------- | ------------- | ------ |
| maven      | org.junit.platform:junit-platform-suite             | 1.10.1        | 1.10.2 |
| maven      | io.projectreactor:reactor-test                      | 3.2.3.RELEASE | 3.6.7  |
| maven      | org.instancio:instancio-junit                       | 4.6.0         | 4.7.0  |
| maven      | org.wiremock:wiremock-standalone                    | 3.5.4         | 3.6.0  |
| maven      | io.projectreactor:reactor-core-micrometer           | 1.1.6         | 1.1.7  |
| maven      | org.springframework.boot:spring-boot-starter-parent | 3.2.5         | 3.3.0  |


<a name="1.2.1"></a>

## Version [1.2.1] - 2024-06-11
### Bug Fixes
- [#198](https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/issues/198) base and delta version werden zurueckgesetzt

### Features
- feat: [#200](https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/issues/200) kafka topics nach jedem test initialisieren
- [#177](https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/issues/177) tests for repositories

### Others
- **jenkins:** [#138](https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/issues/138) - automtisches Erstellen eines Changelogs


<a name="1.2.0"></a>

## Version [1.2.0] - 2024-05-29
### Features
- [#168](https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/issues/168) context propagation
- [#141](https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/issues/141) exception handling webclient
- **KMVZ:** [#186](https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/issues/186) - NeartimeData von ACDS holen über account-sum-months (feature flag)


<a name="1.1.2"></a>

## Version [1.1.2] - 2024-05-15

<a name="1.1.1"></a>

## Version [1.1.1] - 2024-05-07

<a name="1.1.0"></a>

## Version [1.1.0] - 2024-05-06
### Others
- **deps:** update minor dependencies (with automerge)

### Renovate


| datasource | package | from          | to     |
| ---------- | ------------------------------------------------------ | ------------- | ------ |
| maven      | io.jsonwebtoken:jjwt                                   | 0.12.3        | 0.12.5 |
| maven      | org.instancio:instancio-junit                          | 4.3.2         | 4.5.1  |
| maven      | org.junit.platform:junit-platform-suite                | 1.10.1        | 1.10.2 |
| maven      | io.projectreactor:reactor-test                         | 3.2.3.RELEASE | 3.6.5  |
| maven      | org.springframework.kafka:spring-kafka-test            | 2.9.5         | 2.9.13 |
| maven      | de.flapdoodle.embed:de.flapdoodle.embed.mongo.spring3x | 4.12.2        | 4.13.0 |
| maven      | io.cucumber:cucumber-junit-platform-engine             | 7.5.0         | 7.17.0 |
| maven      | io.cucumber:cucumber-java                              | 7.5.0         | 7.17.0 |
| maven      | io.cucumber:cucumber-spring                            | 7.5.0         | 7.17.0 |
| maven      | org.jacoco:jacoco-maven-plugin                         | 0.8.11        | 0.8.12 |
| maven      | org.modelmapper.extensions:modelmapper-spring          | 3.0.0         | 3.2.0  |
| maven      | io.projectreactor:reactor-core-micrometer              | 1.0.11        | 1.1.5  |
| maven      | io.projectreactor.kafka:reactor-kafka                  | 1.3.21        | 1.3.22 |
| maven      | org.openapitools:openapi-generator-maven-plugin        | 7.0.1         | 7.5.0  |
| maven      | datev.refsys-online:refsys-aggregation-service-model   | 1.0.11        | 1.1.0  |
| maven      | io.swagger.core.v3:swagger-annotations                 | 2.2.16        | 2.2.21 |
| maven      | org.springframework.boot:spring-boot-starter-parent    | 3.1.5         | 3.2.5  |


<a name="1.0.4"></a>

## Version [1.0.4] - 2024-05-02
### Others
- **deps:** update flapdoodle.embed.mongo


<a name="1.0.3"></a>

## Version [1.0.3] - 2024-04-22
### Bug Fixes
- **jenkins:** publishApiSpec ist kein multibranch

### Features
- renovatebot schedule

### Others
- **deps:** update dependency org.awaitility:awaitility to v4

### Renovate


| datasource | package | from  | to    |
| ---------- | ------------------------- | ----- | ----- |
| maven      | org.awaitility:awaitility | 3.0.0 | 4.2.1 |


<a name="1.0.2"></a>

## Version [1.0.2] - 2024-04-17
### Features
- Jenkins CnRZ (jenkins.datev.de/refsys-online)
- **renovate:** init


<a name="1.0.1"></a>

## Version [1.0.1] - 2024-04-16

[Unreleased]: https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/compare/1.7.35...HEAD
[1.7.35]: https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/compare/1.7.34...1.7.35
[1.7.34]: https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/compare/1.7.33...1.7.34
[1.7.33]: https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/compare/1.7.32...1.7.33
[1.7.32]: https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/compare/1.7.31...1.7.32
[1.7.31]: https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/compare/1.7.30...1.7.31
[1.7.30]: https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/compare/1.7.29...1.7.30
[1.7.29]: https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/compare/1.7.28...1.7.29
[1.7.28]: https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/compare/1.7.27...1.7.28
[1.7.27]: https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/compare/1.7.26...1.7.27
[1.7.26]: https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/compare/1.7.25...1.7.26
[1.7.25]: https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/compare/1.7.24...1.7.25
[1.7.24]: https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/compare/1.7.23...1.7.24
[1.7.23]: https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/compare/1.7.22...1.7.23
[1.7.22]: https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/compare/1.7.21...1.7.22
[1.7.21]: https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/compare/1.7.20...1.7.21
[1.7.20]: https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/compare/1.7.19...1.7.20
[1.7.19]: https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/compare/1.7.18...1.7.19
[1.7.18]: https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/compare/1.7.17...1.7.18
[1.7.17]: https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/compare/1.7.16...1.7.17
[1.7.16]: https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/compare/1.7.15...1.7.16
[1.7.15]: https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/compare/1.7.14...1.7.15
[1.7.14]: https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/compare/1.7.13...1.7.14
[1.7.13]: https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/compare/1.7.12...1.7.13
[1.7.12]: https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/compare/1.7.11...1.7.12
[1.7.11]: https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/compare/1.7.10...1.7.11
[1.7.10]: https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/compare/1.7.9...1.7.10
[1.7.9]: https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/compare/1.7.8...1.7.9
[1.7.8]: https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/compare/1.7.7...1.7.8
[1.7.7]: https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/compare/1.7.6...1.7.7
[1.7.6]: https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/compare/1.7.5...1.7.6
[1.7.5]: https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/compare/1.7.4...1.7.5
[1.7.4]: https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/compare/1.7.3...1.7.4
[1.7.3]: https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/compare/1.7.2...1.7.3
[1.7.2]: https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/compare/1.7.1...1.7.2
[1.7.1]: https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/compare/1.7.0...1.7.1
[1.7.0]: https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/compare/1.6.6...1.7.0
[1.6.6]: https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/compare/1.6.5...1.6.6
[1.6.5]: https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/compare/1.6.4...1.6.5
[1.6.4]: https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/compare/1.6.3...1.6.4
[1.6.3]: https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/compare/1.6.2...1.6.3
[1.6.2]: https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/compare/1.6.1...1.6.2
[1.6.1]: https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/compare/1.6.0...1.6.1
[1.6.0]: https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/compare/1.5.10...1.6.0
[1.5.10]: https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/compare/1.5.9...1.5.10
[1.5.9]: https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/compare/1.5.8...1.5.9
[1.5.8]: https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/compare/1.5.7...1.5.8
[1.5.7]: https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/compare/1.5.6...1.5.7
[1.5.6]: https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/compare/1.5.5...1.5.6
[1.5.5]: https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/compare/1.5.4...1.5.5
[1.5.4]: https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/compare/1.5.3...1.5.4
[1.5.3]: https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/compare/1.5.2...1.5.3
[1.5.2]: https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/compare/1.5.1...1.5.2
[1.5.1]: https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/compare/1.5.0...1.5.1
[1.5.0]: https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/compare/1.4.10...1.5.0
[1.4.10]: https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/compare/1.4.9...1.4.10
[1.4.9]: https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/compare/1.4.8...1.4.9
[1.4.8]: https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/compare/1.4.7...1.4.8
[1.4.7]: https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/compare/1.4.6...1.4.7
[1.4.6]: https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/compare/1.4.5...1.4.6
[1.4.5]: https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/compare/1.4.4...1.4.5
[1.4.4]: https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/compare/1.4.3...1.4.4
[1.4.3]: https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/compare/1.4.2...1.4.3
[1.4.2]: https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/compare/1.4.1...1.4.2
[1.4.1]: https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/compare/1.4.0...1.4.1
[1.4.0]: https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/compare/1.3.15...1.4.0
[1.3.15]: https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/compare/1.3.14...1.3.15
[1.3.14]: https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/compare/1.3.13...1.3.14
[1.3.13]: https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/compare/1.3.12...1.3.13
[1.3.12]: https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/compare/1.3.11...1.3.12
[1.3.11]: https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/compare/1.3.10...1.3.11
[1.3.10]: https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/compare/1.3.9...1.3.10
[1.3.9]: https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/compare/1.3.8...1.3.9
[1.3.8]: https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/compare/1.3.7...1.3.8
[1.3.7]: https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/compare/1.3.6...1.3.7
[1.3.6]: https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/compare/1.3.5...1.3.6
[1.3.5]: https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/compare/1.3.4...1.3.5
[1.3.4]: https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/compare/1.3.3...1.3.4
[1.3.3]: https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/compare/1.3.2...1.3.3
[1.3.2]: https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/compare/1.3.1...1.3.2
[1.3.1]: https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/compare/1.3.0...1.3.1
[1.3.0]: https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/compare/1.2.2...1.3.0
[1.2.2]: https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/compare/1.2.1...1.2.2
[1.2.1]: https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/compare/1.2.0...1.2.1
[1.2.0]: https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/compare/1.1.2...1.2.0
[1.1.2]: https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/compare/1.1.1...1.1.2
[1.1.1]: https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/compare/1.1.0...1.1.1
[1.1.0]: https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/compare/1.0.4...1.1.0
[1.0.4]: https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/compare/1.0.3...1.0.4
[1.0.3]: https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/compare/1.0.2...1.0.3
[1.0.2]: https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/compare/1.0.1...1.0.2
[1.0.1]: https://git.datev.de/refsys-online/aggregation-service/refsys-aggregation-processing-service/compare/0.0.22...1.0.1
