# Metriken im Processing Service

Eigene Kafka Metriken für die Verarbeitung der ACDS Changed Events: 

__Initial Import getriggert von einem Event__   
Reactor spezifische Metrik / Observability   
.name("process_event.import")   
.tag("changed_type", changedTypesAsString)  
.tag("is_delta", "false")   

Erzeugt Metriken: 
- Flow Duration (count, seconds, bucket, max)
- Subscribed

__Delta Events__  
.name("process_event.delta")  
.tag("changed_type", changedTypesAsString)  
.tag("is_delta", "true")  

Erzeugt Metriken:
- Flow Duration (count, seconds, bucket, max)
- Subscribed

__Zähler über alle Events__  
`process_event.count` - tags changed_type  
`process_event.changed_types_count` - Changed types in one event (GAUGE)

__Reactor Metriken für die mongo DB Operationen__ 
- Flow Duration (count, seconds, bucket, max)
- Subscribed