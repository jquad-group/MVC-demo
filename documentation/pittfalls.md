# Sammlung von Fallen & unschönen Stellen 

- Wir verwenden Datenobjekte aus unterschiedlichen Namespaces die sich nur durch Groß - Kleinschreibung unterscheiden   
  Beispiel: Masterdatacontext (acds) MasterDataContext (refsys.model)
- Wir verwenden Objekte aus der Schnittstelle zu ACDS quer durch den Code. 
- Wir haben interne Model Objekte, die Teilsichten von Datenbankmodellen abbilden   
  Beispiel: PersonGroupAmountValues & AccountGroupValue (aus Model)
- Irgendwie hängt alles von allem ab -> Testaufwand (mocking, teilweise auch nicht möglich, Mapper)
- Wir mappen Objekte relativ häufig (nicht nur in Repository oder Boundary)
- Wir haben Objekte im Processing und im QueryService die fast gleich heißen, aber unterschiedliche Semantik haben   
  Beispiel: AccountSumDay vs. AccountSumDayValues - Ersterer hat Double Werte, zweiter Long Werte; Der Mapper für zweiten funktioniert aber auch für den ersten und macht dann automatisch Mist. 
- Es gibt zwei unterschiedlich konfigurierte MongoClients im Projekt (insert und update Client). Der Updateclient hat retryWrites auf false konfiguriert, um die bulkupdates paralell laufen zu lassen (Sonst kommt es zu einer Fehlermeldung der Mongo DB "TransactionTooOld") 
- Die BeanKonfiguration für den Mongoclient ist an 3 Stellen dupliziert (MongoConfiguration, MongoLocalConfiguration, EmbeddedMongoConfiguration). Änderungen sind schwierig (nur im Test geändert, erzeugt dann aber Laufzeitfehler)  