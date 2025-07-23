#language: de
Funktionalität: Delta Event Verarbeitung

  Hintergrund:
    Angenommen Berater 1234567 und Mandant 4711
    Und das Wirtschaftsjahr beginnt am 20210101 und endet am 20211231.
    Und es gibt folgende Buchungen für das Sachkonto 10100000
      | Tag      | Soll    | Haben    |
      | 20210601 | 9619,59 |          |
      | 20210701 |         | 16465,60 |
    Und die Initialisierung wurde erfolgreich abgeschlossen
    Und die Datenbasis enthält Masterdaten für den Berater, Mandant und Wirtschaftsjahr ohne KMVZ

  @RunMe
  Szenario: Delta Event für ein Sachkonto mit Sollbuchung in neuem Monat
    Angenommen es gibt ein Delta Event mit Monatsverkehrszahlen für das Sachkonto 10100000
      | Monat | Soll     | Haben |
      | 8     | 10000,00 |       |
    Wenn das Delta Event verarbeitet wird
    Dann warte bis das Event verarbeitet wurde
    Dann sollte der Tageswert für das Sachkonto 10100000 um den Betrag der Sollbuchung erhöht sein
      | Tag      | Soll     | Haben |
      | 20210801 | 10000,00 |       |
    Dann sollte der Monatswert für das Sachkonto 10100000 um den Betrag der Sollbuchung erhöht sein
      | Monat | Soll     | Haben |
      | 8     | 10000,00 |       |
    Dann sollte das Flag für KMVZ im Masterdatacontext auf true gesetzt sein
    Und  sollte im StateDoc die Base und Delta Version aktualisiert sein

  @RunMe
  Szenario: Delta Event für ein Sachkonto mit Sollbuchung in bestehendem Monat mit existierender Sollbuchung
    Angenommen es gibt ein Delta Event mit Monatsverkehrszahlen für das Sachkonto 10100000
      | Monat | Soll    | Haben |
      | 6     | 3650,00 |       |
    Wenn das Delta Event verarbeitet wird
    Dann warte bis das Event verarbeitet wurde
    Dann sollte der Tageswert für das Sachkonto 10100000 um den Betrag der Sollbuchung erhöht sein
      | Tag      | Soll      | Haben |
      | 20210601 | 13.269,59 |       |
    Dann sollte der Monatswert für das Sachkonto 10100000 um den Betrag der Sollbuchung erhöht sein
      | Monat | Soll      | Haben |
      | 6     | 13.269,59 |       |
    Dann sollte das Flag für KMVZ im Masterdatacontext auf true gesetzt sein
    Und  sollte im StateDoc die Base und Delta Version aktualisiert sein

  @RunMe
  Szenario: Delta Event für ein Sachkonto mit Sollbuchung in bestehendem Monat ohne existierende Sollbuchung
    Angenommen es gibt ein Delta Event mit Monatsverkehrszahlen für das Sachkonto 10100000
      | Monat | Soll     | Haben |
      | 7     | 25160,70 |       |
    Wenn das Delta Event verarbeitet wird
    Dann warte bis das Event verarbeitet wurde
    Dann sollte der Tageswert für das Sachkonto 10100000 um den Betrag der Sollbuchung erhöht sein
      | Tag      | Soll     | Haben    |
      | 20210701 | 25160,70 | 16465,60 |
    Dann sollte der Monatswert für das Sachkonto 10100000 um den Betrag der Sollbuchung erhöht sein
      | Monat | Soll     | Haben    |
      | 7     | 25160,70 | 16465,60 |
    Dann sollte das Flag für KMVZ im Masterdatacontext auf true gesetzt sein
    Und  sollte im StateDoc die Base und Delta Version aktualisiert sein

  @RunMe
  Szenario: Delta Event für ein Personenkonto in einem neuen Monat
    Angenommen es gibt ein Delta Event mit Monatsverkehrszahlen für das Personenkonto 101000000
      | Monat | Soll     | Haben |
      | 8     | 10000,00 |       |
    Wenn das Delta Event verarbeitet wird
    Dann warte bis das Event verarbeitet wurde
    Dann sollte der Tageswert für das Personenkonto 101000000 um den Betrag der Sollbuchung erhöht sein
      | Tag      | Soll     | Haben |
      | 20210801 | 10000,00 |       |
    Dann sollte der Monatswert für das Personenkonto 101000000 um den Betrag der Sollbuchung erhöht sein
      | Monat | Soll     | Haben |
      | 8     | 10000,00 |       |
    Dann sollte der typische Tageswert für die Personengruppe 1 um den Betrag der Sollbuchung erhöht sein
      | Tag      | Soll     | Haben |
      | 20210801 | 10000,00 |       |
    Dann sollte das Flag für KMVZ im Masterdatacontext auf true gesetzt sein
    Und  sollte im StateDoc die Base und Delta Version aktualisiert sein

  @RunMe
  Szenario: Delta Event für ein Personenkonto in einem bestehenden Monat
    Angenommen es gibt folgende Buchungen für das Personenkonto 101000000
      | Tag      | Soll    | Haben |
      | 20210701 | 1000,00 |       |
    Angenommen es gibt folgende übliche aggregierte Tageswerte für die Personengruppe 1
      | Tag      | Soll    | Haben |
      | 20210701 | 1000,00 |       |
    Angenommen es gibt ein Delta Event mit Monatsverkehrszahlen für das Personenkonto 101000000
      | Monat | Soll     | Haben |
      | 7     | 10000,00 |       |
    Wenn das Delta Event verarbeitet wird
    Dann warte bis das Event verarbeitet wurde
    Dann sollte der Tageswert für das Personenkonto 101000000 um den Betrag der Sollbuchung erhöht sein
      | Tag      | Soll     | Haben |
      | 20210701 | 11000,00 |       |
    Dann sollte der Monatswert für das Personenkonto 101000000 um den Betrag der Sollbuchung erhöht sein
      | Monat | Soll     | Haben |
      | 7     | 11000,00 |       |
    Dann sollte der typische Tageswert für die Personengruppe 1 um den Betrag der Sollbuchung erhöht sein
      | Tag      | Soll     | Haben |
      | 20210701 | 11000,00 |       |
    Dann sollte das Flag für KMVZ im Masterdatacontext auf true gesetzt sein
    Und  sollte im StateDoc die Base und Delta Version aktualisiert sein

  # Personengruppen: 1 - 6 debitors, 7 - 9 creditors
  @RunMe
  Szenario: Delta Event für ein Debitor Personenkonto in einem neuen Monat untypisch
    Angenommen es gibt ein Delta Event mit Monatsverkehrszahlen für das Personenkonto 101000000
      | Monat | Soll | Haben    |
      | 8     |      | 10000,00 |
    Wenn das Delta Event verarbeitet wird
    Dann warte bis das Event verarbeitet wurde
    Dann sollte der Tageswert für das Personenkonto 101000000 um den Betrag der Habenbuchung erhöht sein
      | Tag      | Soll | Haben    |
      | 20210801 |      | 10000,00 |
    Dann sollte der Monatswert für das Personenkonto 101000000 um den Betrag der Habenbuchung erhöht sein
      | Monat | Soll | Haben    |
      | 8     |      | 10000,00 |
    Dann sollte der untypische Tageswert für die Personengruppe 1 um den Betrag der Habenbuchung erhöht sein - kreditorischer Debitor
      | Tag      | Soll | Haben    |
      | 20210801 |      | 10000,00 |
    Dann sollte das Flag für KMVZ im Masterdatacontext auf true gesetzt sein
    Und  sollte im StateDoc die Base und Delta Version aktualisiert sein

  @RunMe
  Szenario: Delta Event für ein Debitor Personenkonto in einem bestehenden Monat - Wechsel auf untypisch
    Angenommen es gibt folgende Buchungen für das Personenkonto 101000000
      | Tag      | Soll     | Haben |
      | 20210630 | 42000,00 |       |
    Angenommen es gibt folgende übliche aggregierte Tageswerte für die Personengruppe 1
      | Tag      | Soll     | Haben |
      | 20210630 | 42000,00 |       |
    Angenommen es gibt folgende übliche aggregierte Monatswerte für die Personengruppe 1
      | Monat | Soll     | Haben |
      | 06    | 42000,00 |       |
    Angenommen es gibt ein Delta Event mit Monatsverkehrszahlen für das Personenkonto 101000000
      | Monat | Soll | Haben     |
      | 7     |      | 100000,00 |
    Wenn das Delta Event verarbeitet wird
    Dann warte bis das Event verarbeitet wurde
    Dann sollte der Tageswert für das Personenkonto 101000000 um den Betrag der Habenbuchung erhöht sein
      | Tag      | Soll | Haben     |
      | 20210701 |      | 100000,00 |
    Dann sollte der Monatswert für das Personenkonto 101000000 um den Betrag der Habenbuchung erhöht sein
      | Monat | Soll | Haben     |
      | 7     |      | 100000,00 |
    Dann sollte der Tageswert in der Personengruppe 1 als untypisch ausgewiesen werden - kreditorischer Debitor
      | Tag      | Soll typisch | Haben typisch | Soll untypisch | Haben untypisch |
      | 20210701 | -42000,00    |                | 42000,00       | 100000,00       |
    Dann sollte der Monatswert in der Personengruppe 1 als untypisch ausgewiesen werden - kreditorischer Debitor
      | Monat | Soll typisch | Haben typisch | Soll untypisch | Haben untypisch |
      | 7     | -42000,00    |                | 42000,00       | 100000,00       |
    Dann sollte das Flag für KMVZ im Masterdatacontext auf true gesetzt sein
    Und  sollte im StateDoc die Base und Delta Version aktualisiert sein


  @RunMe
  Szenario: Delta Event für ein Kreditor Personenkonto in einem neuen Monat untypisch
    Angenommen es gibt ein Delta Event mit Monatsverkehrszahlen für das Personenkonto 701000000
      | Monat | Soll   | Haben |
      | 12    | 800,00 |       |
    Wenn das Delta Event verarbeitet wird
    Dann warte bis das Event verarbeitet wurde
    Dann sollte der Tageswert für das Personenkonto 701000000 um den Betrag der Sollbuchung erhöht sein
      | Tag      | Soll   | Haben |
      | 20211201 | 800,00 |       |
    Dann sollte der Monatswert für das Personenkonto 701000000 um den Betrag der Sollbuchung erhöht sein
      | Monat | Soll   | Haben |
      | 12    | 800,00 |       |
    Dann sollte der untypische Tageswert für die Personengruppe 7 um den Betrag der Sollbuchung erhöht sein - debitorischer Kreditor
      | Tag      | Soll   | Haben |
      | 20211201 | 800,00 |       |
    Dann sollte das Flag für KMVZ im Masterdatacontext auf true gesetzt sein
    Und  sollte im StateDoc die Base und Delta Version aktualisiert sein

  @RunMe
  Szenario: Delta Event für ein Kreditot Personenkonto in einem bestehenden Monat - Wechsel auf untypisch
    Angenommen es gibt folgende Buchungen für das Personenkonto 701000000
      | Tag      | Soll | Haben    |
      | 20210101 |      | 38000,00 |
    Angenommen es gibt folgende übliche aggregierte Tageswerte für die Personengruppe 7
      | Tag      | Soll | Haben    |
      | 20210101 |      | 38000,00 |
    Angenommen es gibt folgende übliche aggregierte Monatswerte für die Personengruppe 7
      | Monat | Soll | Haben    |
      | 1     |      | 38000,00 |
    Angenommen es gibt ein Delta Event mit Monatsverkehrszahlen für das Personenkonto 701000000
      | Monat | Soll      | Haben |
      | 1     | 100000,00 |       |
    Wenn das Delta Event verarbeitet wird
    Dann warte bis das Event verarbeitet wurde
    Dann sollte der Tageswert für das Personenkonto 701000000 um den Betrag der Sollbuchung erhöht sein
      | Tag      | Soll      | Haben    |
      | 20210101 | 100000,00 | 38000,00 |
    Dann sollte der Monatswert für das Personenkonto 701000000 um den Betrag der Sollbuchung erhöht sein
      | Monat | Soll      | Haben    |
      | 1     | 100000,00 | 38000,00 |
    Dann sollte der Tageswert in der Personengruppe 7 als untypisch ausgewiesen werden - debitorischer Kreditor
      | Tag      | Soll typisch | Haben typisch | Soll untypisch | Haben untypisch |
      | 20210101 |              | 0,00           | 100000,00      | 38000,00        |
    Dann sollte der Monatswert in der Personengruppe 7 als untypisch ausgewiesen werden - debitorischer Kreditor
      | Monat | Soll typisch | Haben typisch | Soll untypisch | Haben untypisch |
      | 1     |    | 0,00                | 100000,00        | 38000,00       |
    Dann sollte das Flag für KMVZ im Masterdatacontext auf true gesetzt sein
    Und  sollte im StateDoc die Base und Delta Version aktualisiert sein

  @RunMe
  Szenario: Delta Event mit fehlerhafter Base Version
    Angenommen die Base Version im State Doc ist 1 und die Delta Version ist 0
    Angenommen es gibt ein Delta Event für das Sachkonto 10100000 mit einer Base Version 2
    Wenn das Delta Event verarbeitet wird
    Dann sollte ein Fehler auftreten

  @RunMe
  Szenario: Delta Event mit fehlerhafter Delta Version
    Angenommen die Base Version im State Doc ist 1 und die Delta Version ist 0
    Angenommen es gibt ein Delta Event für das Sachkonto 10100000 mit einer Base Version 1 und einer Delta Version 3
    Wenn das Delta Event verarbeitet wird
    Dann sollte ein Fehler auftreten

  @RunMe
  Szenario: Mehrere Delta Events in korrekter Reihenfolge
    Angenommen die Base Version im State Doc ist 47 und die Delta Version ist 0
    Angenommen es gibt die folgende Events
      | Berater | Mandant | WJ       | Base-Version | Delta-Version | Delta-Request                             |
      | 1234567 | 4711    | 20210101 | 47           | 1             | json/requests/delta/delta-request-01.json |
      | 1234567 | 4711    | 20210101 | 47           | 2             | json/requests/delta/delta-request-02.json |
      | 1234567 | 4711    | 20210101 | 47           | 3             | json/requests/delta/delta-request-03.json |
      | 1234567 | 4711    | 20210101 | 47           | 4             | json/requests/delta/delta-request-04.json |
    Wenn die Delta Events verarbeitet werden
    Dann warte bis die Events verarbeitet wurden
    Dann sollte das StateDoc mit Delta Version 4 erfolgreich aktualisiert sein

  @RunMe
  Szenario: Ein großes Delta Events mit Personenkonten
    # if stateDoc.baseVersion == request.baseVersion and stateDoc.deltaVersion == request.deltaVersion + 1 then the delta request will be processed
    # in the previous input the request.baseVersion = 47 and the stateDoc.baseVersion = 45. in this case a gap will be recognized and an exception will be thrown
    # input data is the same, but the DeltaEventProcessingService.processDeltaEvent method changed, so the input had the change as well
    Angenommen die Base Version im State Doc ist 47 und die Delta Version ist 0
    Angenommen es gibt die folgende Events
      | Berater | Mandant | WJ       | Base-Version | Delta-Version | Delta-Request                                        |
      | 1234567 | 4711    | 20210101 | 47           | 1             | json/requests/delta/delta-request-personaccount.json |
    Wenn die Delta Events verarbeitet werden
    Dann warte bis das Event verarbeitet wurde
    Dann sollte das StateDoc mit Delta Version 1 erfolgreich aktualisiert sein

  @RunMe
  Szenario: Generalumkehr in einem neuen Personen Konto
    Angenommen es gibt folgende Buchungen für das Personenkonto 100000001
      | Tag      | Soll    | Haben |
      | 20210101 | 1000,00 |       |
      | 20210115 | 2000,00 |       |
      | 20210201 | 1000,00 |       |
      | 20210215 | 1000,00 |       |
    Und es gibt folgende übliche aggregierte Tageswerte für die Personengruppe 1
      | Tag      | Soll    | Haben |
      | 20210101 | 1000,00 |       |
      | 20210115 | 2000,00 |       |
      | 20210201 | 1000,00 |       |
      | 20210215 | 1000,00 |       |
    Angenommen es gibt folgende übliche aggregierte Monatswerte für die Personengruppe 1
      | Monat | Soll    | Haben |
      | 01    | 3000,00 |       |
      | 02    | 2000,00 |       |
    Angenommen es gibt ein Delta Event mit Monatsverkehrszahlen und einer Generalumkehr für das Personenkonto 100000002
      | Monat | Soll     | Haben |
      | 01    | 2000,00  |       |
      | 02    | -2000,00 |       |
    Wenn das Delta Event verarbeitet wird
    Dann warte bis das Event verarbeitet wurde
    Dann sollten die Tageswerte für das Konto 100000002 gespeichert sein
      | Tag      | Soll     | Haben |
      | 20210101 | 2000,00  |       |
      | 20210201 | -2000,00 |       |
    Und sollten die Monatswerte für das Konto 100000002 gespeichert sein
      | Monat | Soll     | Haben |
      | 01    | 2000,00  |       |
      | 02    | -2000,00 |       |
    Dann sollten die Werte für die Personengruppe 1 unverändert sein
    Dann sollte das Konto 100000002 in der Liste der gesondert zu betrachtenden Personenkonten aufgenommen sein
    Dann sollte das StateDoc mit Delta Version 1 erfolgreich aktualisiert sein

  @RunMe
  Szenario: Event mit Generalumkehr in einem bestehenden Personenkonto führt nicht zur individuellen Betrachtung dieses Kontos wenn der aggregierte Tageswert nicht negativ wird
    Angenommen es gibt folgende Buchungen für das Personenkonto 100000000
      | Tag      | Soll    | Haben |
      | 20210101 | 1000,00 |       |
      | 20210115 | 2000,00 |       |
      | 20210201 | 1000,00 |       |
      | 20210215 | 1000,00 |       |
    Angenommen es gibt folgende Buchungen für das Personenkonto 100000001
      | Tag      | Soll    | Haben |
      | 20210101 | 9000,00 |       |
      | 20210115 | 1000,00 |       |
      | 20210201 | 5000,00 |       |
      | 20210215 | 1000,00 |       |
    Angenommen es gibt folgende übliche aggregierte Tageswerte für die Personengruppe 1
      | Tag      | Soll     | Haben |
      | 20210101 | 10000,00 |       |
      | 20210115 | 3000,00  |       |
      | 20210201 | 6000,00  |       |
      | 20210215 | 2000,00  |       |
    Angenommen es gibt folgende übliche aggregierte Monatswerte für die Personengruppe 1
      | Monat | Soll     | Haben |
      | 01    | 13000,00 |       |
      | 02    | 8000,00  |       |
    Angenommen es gibt ein Delta Event mit Monatsverkehrszahlen und einer Generalumkehr für das Personenkonto 100000001
      | Monat | Soll     | Haben |
      | 01    | 2000,00  |       |
      | 02    | -2000,00 |       |
    Wenn das Delta Event verarbeitet wird
    Dann warte bis das Event verarbeitet wurde
    Dann sollten die Tageswerte für das Konto 100000001 gespeichert sein
      | Tag      | Soll     | Haben |
      | 20210101 | 11000,00 |       |
      | 20210115 | 1000,00  |       |
      | 20210201 | 3000,00  |       |
      | 20210215 | 1000,00  |       |
    Und sollten die Monatswerte für das Konto 100000001 gespeichert sein
      | Monat | Soll     | Haben |
      | 01    | 12000,00 |       |
      | 02    | 4000,00  |       |
    Dann sollten die üblichen aggregierten Tageswerte für die Personengruppe 1 um die Delta Werte verändert sein
      | Tag      | Soll     | Haben |
      | 20210101 | 12000,00 |       |
      | 20210115 | 3000,00  |       |
      | 20210201 | 4000,00  |       |
      | 20210215 | 2000,00  |       |
    Und sollten die üblichen aggregierten Monatswerte für die Personengruppe 1 um die Delta Werte verändert sein
      | Monat | Soll     | Haben |
      | 01    | 15000,00 |       |
      | 02    | 6000,00  |       |
    Und das Konto 100000001 sollte nicht in der Liste der gesondert zu betrachtenden Personenkonten aufgenommen sein
    Und sollte das StateDoc mit Delta Version 1 erfolgreich aktualisiert sein

  @RunMe
  Szenario: Event mit Generalumkehr in einem bestehenden Personenkonto führt zur individuellen Betrachtung dieses Kontos wenn der aggregierte Tageswert negativ wird
    Angenommen es gibt folgende Buchungen für das Personenkonto 100000000
      | Tag      | Soll    | Haben |
      | 20210101 | 1000,00 |       |
      | 20210115 | 2000,00 |       |
      | 20210201 | 1000,00 |       |
      | 20210215 | 1000,00 |       |
    Angenommen es gibt folgende Buchungen für das Personenkonto 100000001
      | Tag      | Soll    | Haben |
      | 20210101 | 9000,00 |       |
      | 20210115 | 1000,00 |       |
      | 20210201 | 5000,00 |       |
      | 20210215 | 1000,00 |       |
    Angenommen es gibt folgende übliche aggregierte Tageswerte für die Personengruppe 1
      | Tag      | Soll     | Haben |
      | 20210101 | 10000,00 |       |
      | 20210115 | 3000,00  |       |
      | 20210201 | 6000,00  |       |
      | 20210215 | 2000,00  |       |
    Angenommen es gibt folgende übliche aggregierte Monatswerte für die Personengruppe 1
      | Monat | Soll     | Haben |
      | 01    | 13000,00 |       |
      | 02    | 8000,00  |       |
    Angenommen es gibt ein Delta Event mit Monatsverkehrszahlen und einer Generalumkehr für das Personenkonto 100000001
      | Monat | Soll      | Haben |
      | 01    | 2000,00   |       |
      | 02    | -10000,00 |       |
    Wenn das Delta Event verarbeitet wird
    Dann warte bis das Event verarbeitet wurde
    Dann sollten die Tageswerte für das Konto 100000001 gespeichert sein
      | Tag      | Soll     | Haben |
      | 20210101 | 11000,00 |       |
      | 20210115 | 1000,00  |       |
      | 20210201 | -5000,00 |       |
      | 20210215 | 1000,00  |       |
    Und sollten die Monatswerte für das Konto 100000001 gespeichert sein
      | Monat | Soll     | Haben |
      | 01    | 12000,00 |       |
      | 02    | -4000,00 |       |
    Dann sollten die üblichen aggregierten Tageswerte für die Personengruppe 1 ohne Personenkonto 100000001 berechnet sein
      | Tag      | Soll    | Haben |
      | 20210101 | 1000,00 |       |
      | 20210115 | 2000,00 |       |
      | 20210201 | 1000,00 |       |
      | 20210215 | 1000,00 |       |
    Und sollten die üblichen aggregierten Monatswerte für die Personengruppe 1 ohne Personenkonto 100000001 berechnet sein
      | Monat | Soll    | Haben |
      | 01    | 3000,00 |       |
      | 02    | 2000,00 |       |
    Dann sollte das Konto 100000001 in der Liste der gesondert zu betrachtenden Personenkonten aufgenommen sein
    Und sollte das StateDoc mit Delta Version 1 erfolgreich aktualisiert sein

  @RunMe
  Szenario: Event mit bestehender Generalumkehr in einem bestehenden Personenkonto führt nach weiteren Buchungen in einem Delta Event nicht mehr zur individuellen Betrachtung dieses Kontos
    Angenommen es gibt folgende Buchungen für das Personenkonto 100000000
      | Tag      | Soll    | Haben |
      | 20210101 | 1000,00 |       |
      | 20210115 | 2000,00 |       |
      | 20210201 | 1000,00 |       |
      | 20210215 | 1000,00 |       |
    Angenommen es gibt folgende Buchungen für das Personenkonto 100000001
      | Tag      | Soll     | Haben |
      | 20210101 | -2000,00 |       |
      | 20210115 | 1000,00  |       |
      | 20210201 | 5000,00  |       |
      | 20210215 | 1000,00  |       |
    Angenommen es gibt folgende übliche aggregierte Tageswerte für die Personengruppe 1
      | Tag      | Soll    | Haben |
      | 20210101 | 1000,00 |       |
      | 20210115 | 2000,00 |       |
      | 20210201 | 1000,00 |       |
      | 20210215 | 1000,00 |       |
    Angenommen es gibt folgende übliche aggregierte Monatswerte für die Personengruppe 1
      | Monat | Soll    | Haben |
      | 01    | 3000,00 |       |
      | 02    | 2000,00 |       |
    Angenommen das Personenkonto 100000001 ist in der Liste der gesondert zu betrachtenden Personenkonten aufgenommen
    Angenommen es gibt ein Delta Event mit Monatsverkehrszahlen und üblichen Buchungen für das Personenkonto 100000001
      | Monat | Soll    | Haben |
      | 01    | 4000,00 |       |
      | 02    | 1000,00 |       |
    Wenn das Delta Event verarbeitet wird
    Dann warte bis das Event verarbeitet wurde
    Dann sollten die Tageswerte für das Konto 100000001 gespeichert sein
      | Tag      | Soll    | Haben |
      | 20210101 | 2000,00 |       |
      | 20210115 | 1000,00 |       |
      | 20210201 | 6000,00 |       |
      | 20210215 | 1000,00 |       |
    Und sollten die Monatswerte für das Konto 100000001 gespeichert sein
      | Monat | Soll    | Haben |
      | 01    | 3000,00 |       |
      | 02    | 7000,00 |       |
    Dann sollten die üblichen aggregierten Tageswerte für die Personengruppe 1 inklusive Personenkonto 100000001 berechnet sein
      | Tag      | Soll    | Haben |
      | 20210101 | 3000,00 |       |
      | 20210115 | 3000,00 |       |
      | 20210201 | 7000,00 |       |
      | 20210215 | 2000,00 |       |
    Und sollten die üblichen aggregierten Monatswerte für die Personengruppe 1 inklusive Personenkonto 100000001 berechnet sein
      | Monat | Soll    | Haben |
      | 01    | 6000,00 |       |
      | 02    | 9000,00 |       |
    Dann sollte das Konto 100000001 aus der Liste der gesondert zu betrachtenden Personenkonten entfernt sein
    Und sollte das StateDoc mit Delta Version 1 erfolgreich aktualisiert sein

  @RunMe
  Szenario: Event mit Generalumkehr mehrerer Personenkonten führt zum Wegfall der Personengruppe und zur individuellen Betrachtung dieser Konten
    Angenommen es gibt folgende Buchungen für das Personenkonto 100000000
      | Tag      | Soll    | Haben |
      | 20210101 | 1000,00 |       |
      | 20210115 | 2000,00 |       |
      | 20210201 | 1000,00 |       |
      | 20210215 | 1000,00 |       |
    Angenommen es gibt folgende Buchungen für das Personenkonto 100000001
      | Tag      | Soll    | Haben |
      | 20210101 | 9000,00 |       |
      | 20210115 | 1000,00 |       |
      | 20210201 | 5000,00 |       |
      | 20210215 | 1000,00 |       |
    Angenommen es gibt folgende übliche aggregierte Tageswerte für die Personengruppe 1
      | Tag      | Soll     | Haben |
      | 20210101 | 10000,00 |       |
      | 20210115 | 3000,00  |       |
      | 20210201 | 6000,00  |       |
      | 20210215 | 2000,00  |       |
    Angenommen es gibt folgende übliche aggregierte Monatswerte für die Personengruppe 1
      | Monat | Soll     | Haben |
      | 01    | 13000,00 |       |
      | 02    | 8000,00  |       |
    Angenommen es gibt ein Delta Event mit Monatsverkehrszahlen und einer Generalumkehr für das Personenkonto 100000000
      | Monat | Soll     | Haben |
      | 01    | -4000,00 |       |
      | 02    | -3000,00 |       |
    Angenommen es gibt ein Delta Event mit Monatsverkehrszahlen und einer Generalumkehr für das Personenkonto 100000001
      | Monat | Soll      | Haben |
      | 01    | -11000,00 |       |
      | 02    | -7000,00  |       |
    Wenn das Delta Event verarbeitet wird
    Dann warte bis das Event verarbeitet wurde
    Dann sollten die Tageswerte für das Konto 100000000 gespeichert sein
      | Tag      | Soll     | Haben |
      | 20210101 | -3000,00 |       |
      | 20210115 | 2000,00  |       |
      | 20210201 | -2000,00 |       |
      | 20210215 | 1000,00  |       |
    Und sollten die Monatswerte für das Konto 100000000 gespeichert sein
      | Monat | Soll     | Haben |
      | 01    | -1000,00 |       |
      | 02    | -1000,00 |       |
    Dann sollten die Tageswerte für das Konto 100000001 gespeichert sein
      | Tag      | Soll     | Haben |
      | 20210101 | -2000,00 |       |
      | 20210115 | 1000,00  |       |
      | 20210201 | -2000,00 |       |
      | 20210215 | 1000,00  |       |
    Und sollten die Monatswerte für das Konto 100000001 gespeichert sein
      | Monat | Soll     | Haben |
      | 01    | -1000,00 |       |
      | 02    | -1000,00 |       |
    Dann sollte die Personengruppe 1 weggefallen sein
    Dann sollte das Konto 100000000 in der Liste der gesondert zu betrachtenden Personenkonten aufgenommen sein
    Dann sollte das Konto 100000001 in der Liste der gesondert zu betrachtenden Personenkonten aufgenommen sein
    Und sollte das StateDoc mit Delta Version 1 erfolgreich aktualisiert sein

  @RunMe
  Szenario: Event mit Generalumkehr mehrerer Personenkonten führt zu untypischen Tageswerten jedoch zu einem typischen Monatswert und damit nicht zur individuellen Betrachtung dieser Konten
    Angenommen es gibt folgende Buchungen für das Personenkonto 100000000
      | Tag      | Soll    | Haben |
      | 20210101 | 1000,00 |       |
      | 20210115 | 2000,00 |       |
      | 20210201 | 1000,00 |       |
      | 20210215 | 1000,00 |       |
    Angenommen es gibt folgende Buchungen für das Personenkonto 100000001
      | Tag      | Soll    | Haben |
      | 20210101 | 9000,00 |       |
      | 20210115 | 1000,00 |       |
      | 20210201 | 5000,00 |       |
      | 20210215 | 1000,00 |       |
    Angenommen es gibt folgende übliche aggregierte Tageswerte für die Personengruppe 1
      | Tag      | Soll     | Haben |
      | 20210101 | 10000,00 |       |
      | 20210115 | 3000,00  |       |
      | 20210201 | 6000,00  |       |
      | 20210215 | 2000,00  |       |
    Angenommen es gibt folgende übliche aggregierte Monatswerte für die Personengruppe 1
      | Monat | Soll     | Haben |
      | 01    | 13000,00 |       |
      | 02    | 8000,00  |       |
    Angenommen es gibt ein Delta Event mit Monatsverkehrszahlen und einer Generalumkehr für das Personenkonto 100000000
      | Monat | Soll     | Haben |
      | 01    | -3000,00 |       |
      | 02    | -2000,00 |       |
    Angenommen es gibt ein Delta Event mit Monatsverkehrszahlen und einer Generalumkehr für das Personenkonto 100000001
      | Monat | Soll      | Haben |
      | 01    | -10000,00 |       |
      | 02    | -6000,00  |       |
    Wenn das Delta Event verarbeitet wird
    Dann warte bis das Event verarbeitet wurde
    Dann sollten die Tageswerte für das Konto 100000000 gespeichert sein
      | Tag      | Soll     | Haben |
      | 20210101 | -2000,00 |       |
      | 20210115 | 2000,00  |       |
      | 20210201 | -1000,00 |       |
      | 20210215 | 1000,00  |       |
    Und sollten die Monatswerte für das Konto 100000000 gespeichert sein
      | Monat | Soll | Haben |
      | 01    | 0,00 |       |
      | 02    | 0,00 |       |
    Dann sollten die Tageswerte für das Konto 100000001 gespeichert sein
      | Tag      | Soll     | Haben |
      | 20210101 | -1000,00 |       |
      | 20210115 | 1000,00  |       |
      | 20210201 | -1000,00 |       |
      | 20210215 | 1000,00  |       |
    Und sollten die Monatswerte für das Konto 100000001 gespeichert sein
      | Monat | Soll | Haben |
      | 01    | 0,00 |       |
      | 02    | 0,00 |       |
    Dann sollte die Personengruppe 1 nur noch null Werte beinhalten
    Dann sollte das Konto 100000000 in der Liste der gesondert zu betrachtenden Personenkonten aufgenommen sein
    Dann sollte das Konto 100000001 in der Liste der gesondert zu betrachtenden Personenkonten aufgenommen sein
    Und sollte das StateDoc mit Delta Version 1 erfolgreich aktualisiert sein

