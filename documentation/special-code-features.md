# Besonderheiten im Code

## Fire-And-Forget

um einen Teil des Codes ausführen zu lassen und nicht auf die Verarbeitung zu warten wurde die __doOnNext__ und die __subscribe__ Methode benutzt.
In diesem Fall wird win neuer WebFlux Chain gestartet und es wird nichts aus dem bisherigen Chain übernommen. Daher muss der Reactive-Context
und auch manuell an den neuen Chain übergeben werden mit der __contextWrite__ Methode.

Ein Beispiel wie ein Fire-And-Forget benutzt wird befindet sich in der __de.datev.refsys.aggregation.processing.service.CommonImportServiceImpl__
Klasse

## ImportExecutionService

Der ImportExecutionService ist dafür gedacht einen kompletten Import (InitialLoad) oder Teilimports (schemaUpdate, movementdata,
accountCaptions, ...) zu initialisieren, ausführen, abschließen und Fehler zu behandeln.

Die __initializeImport__ und __initializePartialImport__ Methoden prüfen das StateDoc und setzen den __State__ auf __INIT__

Die __executeImport__ Methode führt das übergebene Mono __importAction__ aus (das kann irgendein WebFlux Chain sein). Wenn der Chain der
__importAction__ erfolgreich war, wird in StateDoc der __State__ auf __DONE__ gesetzt. Wenn es aber Fehler gibt, werden diese mit der
__handleExceptionAndUpdateStateDoc__ Methode behandelt, wo abhängig vom Use-Case Error oder Warn geloggt wird, verschiedene Responses
geliefert werden oder das StateDoc __State__ auf __BAD__ oder __INVALID_DATA__ gesetzt wird.

Ein Beispiel für einen kompletten Import (InitialLoad) befindet sich in der
__de.datev.refsys.aggregation.processing.service.CommonImportServiceImpl__ Klasse

Ein Beispiel für einen Teilimport, wo verschiedene __importAction__ an die __executeImport__ Methode übergeben werden befindet sich in der
__de.datev.refsys.aggregation.processing.service.UpdateSchemaServiceImpl__ Klasse

## AccountSumDays Streaming von ACDS

Da die AccountSumDays bei sehr großen Beständen mehrere millionen Elemente enthalten können, wurden diese mit der __buffer__ Methode gestreamt.
Vorher wurde die nach Kontonummer sortiert geliefert, gesammelt bis sich die Kontonummer geändert hat und mit einem bulkInsert in die mongoDB
gespeichert. Da aber ACDS OutOfMemory Probleme hatte, werden die AccountSumDays nicht länger in ACDS sortiert und es können auch für denselben Tag
und mit demselben fachlichen Schlüssel mehrere AccountSumDay Elemente im Response vorhanden sein.

Daher wurde die Implementierung geändert das ein bulkUpdate aufgerufen wird mit dem __UpdateOptions.upsert = true__, somit wird ein Upsert in der
mongoDB ausgeführt (auch als bulkUpsert bekannt).

Es werden eine bestimmte Anzahl von AccountSumDays gesammelt (konfigurierbar in der application.yml mit der
ref-sys.initial-load.account-sum-days-buffer-size Property) und mit einem bulkUpsert in die MovementDataDay Collection gespeichert. Da die Daten
aber Nebenläufig geholt und hintereinander mit der bulkUpsert Methode an die mongoDB versendet werden, wartet ein bulkUpsert nicht bis der vorherige
fertig ist. Das kann dann sporadisch zu DuplicateKeyExceptions in der mongoDB führen, falls von ein vorheriger bulkUpsert langsamer ist und dann
mindestens zwei bulkUpsert einen Insert auf dasselbe Document tätigen.

Um die DuplicateKeyException zu vermeiden wurden die preFetch und concurrency Parameter in der __flatMap__ Methode auf den Wert 1 gesetzt.
Der preFetch Parameter entscheidet wie viele Elemente im Voraus geholt werden und der concurrency Parameter entscheidet wie viele parallel
verarbeitet werden können. Mit dieser Konfiguration ist es garantiert, das die bulkUpsert Aufrufe hintereinander passieren.

Die Erkenntnisse sind das die mongoDB keine Garantie gibt, das die Aufrufe genau in derselben Reihenfolge verarbeitet wie im Quellcode aufgerufen.
Wenn ein Aufruf vom selben Client mehrere bulkUpsert tätigt, dann ist die Reihenfolge nicht garantiert.

## MovementDataDay Streaming aus der mongoDB

Nachdem die AccountSumDays mit einem bulkUpsert in die MovementDataDay Collection gespeichert wurden, werden die MovementDataDays mit einem
Flux-Stream gelesen und in Batches mit der __buffer__ Methode verarbeitet. Anders als wie beim AccountSumDay Streaming, wo diese anhand einer
festgelegten Anzahl gesammelt werden, werden die MovementDataDays in jedem Batch gesammelt bis sich die Kontonummer ändert. Die MovementDataDays
werden zurzeit auch in AccountSumDays gemappt, da die Personenkontenberechnung Method mit AccountSumDays funktioniert (das sollte geändert werden).
Nachdem die Personenkontenberechnung für jeden Batch durchgeführt wurde, werden die MovementDataMonth Documents in die mongoDB mit einem bulkInsert
gespeichert. Die MovementDataPersonGroupDays und MovementDataPersonGroupMonths werden in Maps gesammelt und nachdem alle Batches verarbeitet wurden
einmal mit einem bulkInsert gespeichert.

## MovementDataInventories Streaming von ACDS

Ähnlich wie bei dem AccountSumDays kann es Bestände geben, welche eine große Anzahl von MovementDataInventories enthalten, daher werden die
MovementDataInventories mit der __buffer__ Methode gestreamt. Die MasterDataInventories werden nicht gestreamt. Wenn der Stream der
MovementDataInventories von ACDS verarbeitet wird, werden die Batches zuerst nach dem wgId Feld gesammelt, fachlich geprüft und auf die
Datenbank Modelle gemappt. Da es aber nicht viele MovementDataInventories mit derselben wgId gibt (meistens zwei), wurden diese nochmals in einen
Flux gepackt und wieder mit der __buffer__ Methode gestreamt. In diesen zweiten Stream werden dann eine bestimmte Anzahl (konfigurierbar in der
application.yml mit der ref-sys.initial-load.inventories-buffer-size Property) von gemappten MasterDataInventories in Batches mit einem bulkInsert
in die mongoDB gespeichert.

## VK3 Daten Maskieren

Das Maskieren der Daten funktioniert nur, wenn diese das Java Lombok @ToString Format haben oder ein JSON Format ohne PrettyPrint. In dem
refsys-aggregation-service-model Projekt existieren Test die sicherstellen, das die Modellklassen ein ToString Methode enthalten und das jede Klasse
alle Felder im korrekten Format ausloggt. Alle Log-Ausgaben in der Produktion werden automatisch maskiert. Da Exceptions aber auch Log-Ausgaben mit
VK3 Daten enthalten können, müssen die Entwickler dafür sorgen, dass Exception-Messages auch maskiert werden. Zurzeit werden Exception-Messages im
RestExceptionHandler im Problem.detail Feld geschrieben, sowie auch im StateDoc wenn, Fehler passieren im ProcessingError.ProblemInfo.detail Feld.

Die __de.datev.refsys.aggregation.processing.logging.MaskingPatternLayout__ wird in der src/main/resources/logback-spring.xml in den prod und
log-test Profilen benutzt für die automatische Maskierung beim Loggen.

Die Implementation der Maskierung befindet sich in der __de.datev.refsys.aggregation.processing.util.Vk3MaskingUtil__ Klasse in der __maskMessage__
Methode, welche überall verwendet werden kann.

## Paralleles Ausführen von Mono und Flux

### Mono

Um mehrere Mono-Befehle parallel auszuführen wurde __Mono.zip__ oder __Flux.merge/Flux.concat__ benutzt.

Wenn die Return Typen nicht bei mindestens zwei Mono-Befehlen nicht gleich sind, wurde ein __Mono.zip__ verwendet. Dabei muss beachtet werden das
keines der Mono-Befehle ein __Mono.empty__ zurückliefert. Falls eins der Mono-Befehle ein __Mono.empty__zurückliefert wird der__Mono.zip__ sofort
unterbrochen, ohne auf die restlichen Mono-Befehle zu warten.

Wenn die Return Typen von jedem Mono-Befehl gleich sind, wurde __Flux.merge/Flux.concat__ Methoden benutzt. Falls die Return Typen nicht gleich sind
und die __Flux.merge/Flux.concat__ Methoden trotzdem benutzt werden, wird ein Flux<Object> von den __Flux.merge/Flux.concat__ Methoden
zurückgeliefert, bei welchem dann jedes Element auf mit __instanceof__ geprüft werden muss

### Flux

Ähnlich wie beim Mono wurde auch für einen Parallelaufruf von mehreren Flux-Befehlen __Mono.zip__ oder __Flux.merge/Flux.concat__ benutzt.

Wenn der Flux-Inhalt nicht zu groß ist und die Return Typen nicht bei mindestens zwei Flux-Befehlen nicht gleich sind, wurde ein __Mono.zip__ verwendet.
Die Flux-Befehle wurden hierbei mit der __collectList__ Methode zu Mono-Befehlen umgewandelt. Die Flux-Befehle können auch ein __Mono.empty__
zurückliefern da die __collectList__ Methode aufgerufen wird und dies in einem __Mono<List<Void>>__ Return Typen endet (leere Liste).

Wenn die Return Typen von jedem Flux-Befehl gleich sind, wurde __Flux.merge/Flux.concat__ Methoden benutzt. Falls die Return Typen nicht gleich sind
und die __Flux.merge/Flux.concat__ Methoden trotzdem benutzt werden, wird ein Flux<Object> von den __Flux.merge/Flux.concat__ Methoden
zurückgeliefert, bei welchem dann jedes Element auf mit __instanceof__ geprüft werden muss

## Konfigurationen

### Applikation

Die Applikationskonfigurationen befinden sich in den application.yml Dateien sowie auch in der
__de.datev.refsys.aggregation.processing.config.AggregationProcessingConfiguration__ Klasse

Das Event-Logging für die CircuitBreaker wurde in der __de.datev.refsys.aggregation.processing.config.CircuitBreakerRegistryEventConsumer__ Klasse
implementiert.

### WebSecurity

Die Spring-Security Konfiguration befindet sich in der __de.datev.refsys.aggregation.processing.config.security.WebSecurityConfiguration__ Klasse

### MongoDB

Die MongoDB-Konfigurationen befinden in den application.yml Dateien sowie auch in dem __de.datev.refsys.aggregation.processing.config.mongo__ Package
in folgenden Klassen:
- MongoConfiguration
- MongoConfigUtil
- MongoLocalConfiguration
- MongoSharedConfiguration

Eine Codec-Konfiguration um die Daten mit dem __java.time.OffsetDateTime__ Typen zu speichern und zu lesen befindet sich in der
__de.datev.refsys.aggregation.processing.document.codec.OffsetDateTimeCodec__ Klasse

### Kafka ChangeEvent Producer

Die Kafka-Producer-Konfiguration sind in den application.yml Dateien sowie auch in der
__de.datev.refsys.aggregation.processing.boundry.event.config.AggregationServiceKafkaConfiguration__ Klasse

### OpenApiGenerator

Der OpenApiGenerator ist als maven-plugin in der pom.xml konfiguriert. Die Dokumentation ist auf der
[OpenApiGenerator Plugin](https://openapi-generator.tech/docs/plugins) Seite. zusätzliche Dokumentation gibt es auch auf der
[OpenApiGenerator Maven plug GitHub](https://github.com/OpenAPITools/openapi-generator/tree/master/modules/openapi-generator-maven-plugin) Seite

Die Interfaces für die Controller, sowie auch die Modelle werden bei jedem compile generiert und befinden sich im
__PROJECT_ROOT/target/generated-sources/openapi__ Ordner.

Die ACDS Clients, sowie auch die Modelle befinden sich im __PROJECT_ROOT/src/main/java/de/datev/refsys/generated/acds__ Ordner. Diese werden mit
dem Aufruf der __PROJECT_ROOT/scripts/generate_sources.bat__ generiert und müssen nach dem Generieren in Git-Repository commited werden.

## Applikationsschichten

### Controller

Die REST-Controller sind in dem __de.datev.refsys.aggregation.processing.boundry.controller__ Package und rufen Services oder Repositories auf.
Diese sollten Methoden aus anderen Controllern nicht aufrufen

### Kafka

Die Kafka Klassen befinden sich in dem __de.datev.refsys.aggregation.processing.boundry.event__ Package (inklusive Producer, Consumer, Services und
Modelle)

### Service

Die Services befinden sich in dem __de.datev.refsys.aggregation.processing.service__ Package und rufen die Repositories oder Clients auf um Daten
zu bekommen oder zu verarbeiten.

### Mapper

Die MapStruct Mapper befinden sich in dem __de.datev.refsys.aggregation.processing.mapper__ Package und werden von Services benutzt um die 
ACDS Modelle zu mongoDB Modellen oder umgekehrt zu mappen.

### Modelle

Die Hilfsmodelle befinden sich in dem __de.datev.refsys.aggregation.processing.model__ Package und werden von Services benutzt für die Verarbeitung
der Daten.

### Client

Die ACDS Clients befinden sich in dem __de.datev.refsys.aggregation.processing.client__ Package und rufen die ACDS Schnittstellen auf.

### Repository

Die Repositories befinden sich in dem __de.datev.refsys.aggregation.processing.repository__ Package und werden benutzt, um Daten aus der mongoDB
zu holen, speichern oder aktualisieren.

### Utility

Die Utility Klassen sind in dem __de.datev.refsys.aggregation.processing.util__ Package und enthalten statische Hilfsmethoden, die in mehreren
Klassen benutzt werden.

### ExceptionHandling

Alle Exceptions befinden sich in dem __de.datev.refsys.aggregation.processing.exception__ Package

Das ExceptionHandling für die REST Schnittstellen wurde in der __de.datev.refsys.aggregation.processing.exception.RestExceptionHandler__ Klasse 
implementiert

Das ExceptionHandling für die Fire-And-Forget Aufrufe wurde in der
__de.datev.refsys.aggregation.processing.service.ImportExecutionServiceImpl.handleExceptionAndUpdateStateDoc__ Methode implementiert. Da das
ExceptionHandling in dieser Methode auch Fehler verursachen kann, sollte diese Methode mit einem ExceptionHandling nochmals behandelt werden um
onErrorDropped Logs zu vermeiden. Ein Beispiel gibt es in der __de.datev.refsys.aggregation.processing.service.CommonImportServiceImpl__ Klasse für
den kompletten Import, sowie auch in der __de.datev.refsys.aggregation.processing.service.UpdateSchemaServiceImpl__ Klasse für einen Teilimport.

### Constants

Die Konstanten sind in dem __de.datev.refsys.aggregation.processing.constant__ Package und enthalten statische Texte für Profile, Metriken,
Namen und Fehlermeldungen.