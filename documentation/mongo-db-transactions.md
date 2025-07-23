# Mongo DB , Reactive & Transaktionen 
- Wir verwenden in den Projekten im Aggregationsservice die MongoDB direkt und nicht über das Spring MongoTemplate. Daher sind viele Anleitungen im Internet nicht gültig. 
- Der TransactionalOperator funktioniert nur dann korrekt, wenn die ClientSession in dem ReactiveContext verankert ist. Das hat manuell nicht funktioniert, daher verwenden wir rein manuelle Transaktionen (siehe )  
- Damit die Transaktion funktioniert muss folgendes gegeben sein: 
   - Alle Operationen laufen mit der gleichen Clientsession und mit dem gleichen MongoClient (wir haben zwei im Code)
   - Der MongoClient darf keine Retries machen (zu prüfen)
   - Alle Operationen innerhalb der Transaktion laufen sequentiell ab (evtl. auch im Zussamenhang mit der Clientsession Option `causallyConsistent(true)`) 
   - Die Clientsession benötigt den readConcern majority -> Andere Prozesse sollten zwingend auch mit dem passenden WriteConcern `majority` schreiben, sonst sind die Daten evtl. nicht lesbar
   - Zu prüfen (oder Frage an den Mongo Support) -> kann es hier auch zu Problemen mit unserer Kollisionserkennung am Statedoc geben? 
     Änderung innerhalb der Tranaktion mit den majority Werten; bestätigtes Schreiben parallel außerhalb ohne; Welcher Wert ist in der Datenbank? 