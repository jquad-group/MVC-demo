# Die Logik der Personenkonten Saldierung ist zwischen Debitoren (Konto 100.000.000 - 699.999.999) und Kreditoren (700.000.000 - 999.999.999) identisch.
# Nur die Definition was typisch/Usual und untypisch/Unusual ist unterschiedlich.
# Bei Debitoren ist ein Soll-Saldo (debit) typisch/Usual, bei Kreditoren ist ein Haben-Saldo (credit) typisch/Usual.
# Der Start des Wirtschaftsjahres ist 20210101 und das Ende ist 20211231

#language: de
Funktionalität: Kontensaldierung

  # Standardfall Sachkonto ohne Besonderheiten
  @RunMe
  Szenario: Ein Sachkonto Standard
    Angenommen folgende Tagessummen
      | Kontonummer | Buchungstag | WJ-Monat | Bereichsnummer | Buchungstyp | SummeHaben | SummeSoll |
      | 10100000    | 20210301    | 3        | 0              | 1           |            | 20522.17  |
      | 10100000    | 20210401    | 4        | 0              | 1           | 6883.85    |           |
      | 10100000    | 20210402    | 4        | 0              | 1           | 9373.43    |           |
      | 10100000    | 20210501    | 5        | 0              | 1           |            | 6637.69   |
      | 10100000    | 20210601    | 6        | 0              | 1           |            | 9619.59   |
      | 10100000    | 20210701    | 7        | 0              | 1           | 16465.6    |           |
    Wenn Tagessummen verarbeitet werden
    Dann werden folgende Tageswerte ermittelt
      | Kontonummer | Buchungstag | Bereichsnummer | Buchungstyp | SummeHaben | SummeSoll |
      | 10100000    | 20210301    | 0              |             |            | 20522.17  |
      | 10100000    | 20210401    | 0              |             | 6883.85    |           |
      | 10100000    | 20210402    | 0              |             | 9373.43    |           |
      | 10100000    | 20210501    | 0              |             |            | 6637.69   |
      | 10100000    | 20210601    | 0              |             |            | 9619.59   |
      | 10100000    | 20210701    | 0              |             | 16465.6    |           |
    Und werden folgende Monatswerte ermittelt
      | Kontonummer | WJ-Monat | Bereichsnummer | Buchungstyp | SummeHaben | SummeSoll |
      | 10100000    | 3        | 0              |             |            | 20522.17  |
      | 10100000    | 4        | 0              |             | 16257.28   |           |
      | 10100000    | 5        | 0              |             |            | 6637.69   |
      | 10100000    | 6        | 0              |             |            | 9619.59   |
      | 10100000    | 7        | 0              |             | 16465.6    |           |
    Und werden folgende Tageswerte der Personenkontengruppen ermittelt
      | Kontogruppennummer | Buchungstag | Bereichsnummer | Buchungstyp | SummeHabenTypisch | SummeHabenUntypisch | SummeSollTypisch | SummeSollUntypisch |
    Und werden folgende Monatswerte der Personenkontengruppen ermittelt
      | Kontogruppennummer | WJ-Monat | Bereichsnummer | Buchungstyp | SummeHabenTypisch | SummeHabenUntypisch | SummeSollTypisch | SummeSollUntypisch |
    Und folgende Personenkonten werden individuel betrachtet
      | Kontonummer |

  # Standardfall Sachkonto ohne Besonderheiten mit abweichenden WJ Start
  @RunMe
  Szenario: Ein Sachkonto Standard - WJ beginnt am 1.4.
    Angenommen Wirtschaftsjahr beginnt am 20210401
    Angenommen folgende Tagessummen
      | Kontonummer | Buchungstag | WJ-Monat | Bereichsnummer | Buchungstyp | SummeHaben | SummeSoll |
      | 10100000    | 20210601    | 3        | 0              | 1           |            | 20522.17  |
      | 10100000    | 20210701    | 4        | 0              | 1           | 16257.28   |           |
      | 10100000    | 20210801    | 5        | 0              | 1           |            | 6637.69   |
      | 10100000    | 20210901    | 6        | 0              | 1           |            | 9619.59   |
      | 10100000    | 20211001    | 7        | 0              | 1           | 16465.6    |           |
    Wenn Tagessummen verarbeitet werden
    Dann werden folgende Tageswerte ermittelt
      | Kontonummer | Buchungstag | Bereichsnummer | Buchungstyp | SummeHaben | SummeSoll | Festschreibung |
      | 10100000    | 20210601    | 0              |             |            | 20522.17  | false          |
      | 10100000    | 20210701    | 0              |             | 16257.28   |           | false          |
      | 10100000    | 20210801    | 0              |             |            | 6637.69   | false          |
      | 10100000    | 20210901    | 0              |             |            | 9619.59   | false          |
      | 10100000    | 20211001    | 0              |             | 16465.6    |           | false          |
    Dann werden folgende Monatswerte ermittelt
      | Kontonummer | WJ-Monat | Bereichsnummer | Buchungstyp | SummeHaben | SummeSoll |
      | 10100000    | 3        | 0              |             |            | 20522.17  |
      | 10100000    | 4        | 0              |             | 16257.28   |           |
      | 10100000    | 5        | 0              |             |            | 6637.69   |
      | 10100000    | 6        | 0              |             |            | 9619.59   |
      | 10100000    | 7        | 0              |             | 16465.6    |           |
    Und werden folgende Tageswerte der Personenkontengruppen ermittelt
      | Kontogruppennummer | Buchungstag | Bereichsnummer | Buchungstyp | SummeHabenTypisch | SummeHabenUntypisch | SummeSollTypisch | SummeSollUntypisch |
    Und werden folgende Monatswerte der Personenkontengruppen ermittelt
      | Kontogruppennummer | WJ-Monat | Bereichsnummer | Buchungstyp | SummeHabenTypisch | SummeHabenUntypisch | SummeSollTypisch | SummeSollUntypisch |
    Und folgende Personenkonten werden individuel betrachtet
      | Kontonummer |

  @RunMe
  Szenario: Ein Sachkonto Standard - Erstes WJ beginnt am 15.05.
    Angenommen Wirtschaftsjahr beginnt am 20210515
    Angenommen Wirtschaftsjahr endet am 20220514
    Angenommen folgende Tagessummen
      | Kontonummer | Buchungstag | WJ-Monat | Bereichsnummer | Buchungstyp | SummeHaben | SummeSoll |
      | 10100000    | 20210515    | 1        | 0              | 1           |            | 20522.17  |
      | 10100000    | 20210801    | 4        | 0              | 1           | 16257.28   |           |
      | 10100000    | 20210901    | 5        | 0              | 1           |            | 6637.69   |
      | 10100000    | 20211001    | 6        | 0              | 1           |            | 9619.59   |
      | 10100000    | 20211101    | 7        | 0              | 1           | 16465.6    |           |
      | 10100000    | 20220501    | 13       | 0              | 1           | 47.28      |           |
    Wenn Tagessummen verarbeitet werden
    Dann werden folgende Tageswerte ermittelt
      | Kontonummer | Buchungstag | Bereichsnummer | Buchungstyp | SummeHaben | SummeSoll | Festschreibung |
      | 10100000    | 20210515    | 0              |             |            | 20522.17  | false          |
      | 10100000    | 20210801    | 0              |             | 16257.28   |           | false          |
      | 10100000    | 20210901    | 0              |             |            | 6637.69   | false          |
      | 10100000    | 20211001    | 0              |             |            | 9619.59   | false          |
      | 10100000    | 20211101    | 0              |             | 16465.6    |           | false          |
      | 10100000    | 20220501    | 0              |             | 47.28      |           | false          |
    Dann werden folgende Monatswerte ermittelt
      | Kontonummer | WJ-Monat | Bereichsnummer | Buchungstyp | SummeHaben | SummeSoll |
      | 10100000    | 1        | 0              |             |            | 20522.17  |
      | 10100000    | 4        | 0              |             | 16257.28   |           |
      | 10100000    | 5        | 0              |             |            | 6637.69   |
      | 10100000    | 6        | 0              |             |            | 9619.59   |
      | 10100000    | 7        | 0              |             | 16465.6    |           |
      | 10100000    | 13       | 0              |             | 47.28      |           |
    Und werden folgende Tageswerte der Personenkontengruppen ermittelt
      | Kontogruppennummer | Buchungstag | Bereichsnummer | Buchungstyp | SummeHabenTypisch | SummeHabenUntypisch | SummeSollTypisch | SummeSollUntypisch |
    Und werden folgende Monatswerte der Personenkontengruppen ermittelt
      | Kontogruppennummer | WJ-Monat | Bereichsnummer | Buchungstyp | SummeHabenTypisch | SummeHabenUntypisch | SummeSollTypisch | SummeSollUntypisch |
    Und folgende Personenkonten werden individuel betrachtet
      | Kontonummer |

  # Sachkonto mit anderer Bereichsnummer
  @RunMe
  Szenario: Ein Sachkonto mit anderer Bereichsnummer
    Angenommen folgende Tagessummen
      | Kontonummer | Buchungstag | WJ-Monat | Bereichsnummer | Buchungstyp | SummeHaben | SummeSoll |
      | 10100000    | 20210301    | 3        | 0              | 1           |            | 20522.17  |
      | 10100000    | 20210401    | 4        | 0              | 1           | 6883.85    |           |
      | 10100000    | 20210402    | 4        | 0              | 1           | 9373.43    |           |
      | 10100000    | 20210501    | 5        | 1              | 1           |            | 6637.69   |
      | 10100000    | 20210601    | 6        | 0              | 1           |            | 9619.59   |
      | 10100000    | 20210701    | 7        | 0              | 1           | 16465.6    |           |
    Wenn Tagessummen verarbeitet werden
    Dann werden folgende Tageswerte ermittelt
      | Kontonummer | Buchungstag | Bereichsnummer | Buchungstyp | SummeHaben | SummeSoll |
      | 10100000    | 20210301    | 0              |             |            | 20522.17  |
      | 10100000    | 20210401    | 0              |             | 6883.85    |           |
      | 10100000    | 20210402    | 0              |             | 9373.43    |           |
      | 10100000    | 20210501    | 1              |             |            | 6637.69   |
      | 10100000    | 20210601    | 0              |             |            | 9619.59   |
      | 10100000    | 20210701    | 0              |             | 16465.6    |           |
    Und werden folgende Monatswerte ermittelt
      | Kontonummer | WJ-Monat | Bereichsnummer | Buchungstyp | SummeHaben | SummeSoll |
      | 10100000    | 3        | 0              |             |            | 20522.17  |
      | 10100000    | 4        | 0              |             | 16257.28   |           |
      | 10100000    | 5        | 1              |             |            | 6637.69   |
      | 10100000    | 6        | 0              |             |            | 9619.59   |
      | 10100000    | 7        | 0              |             | 16465.6    |           |
    Und werden folgende Tageswerte der Personenkontengruppen ermittelt
      | Kontogruppennummer | Buchungstag | Bereichsnummer | Buchungstyp | SummeHabenTypisch | SummeHabenUntypisch | SummeSollTypisch | SummeSollUntypisch |
    Und werden folgende Monatswerte der Personenkontengruppen ermittelt
      | Kontogruppennummer | WJ-Monat | Bereichsnummer | Buchungstyp | SummeHabenTypisch | SummeHabenUntypisch | SummeSollTypisch | SummeSollUntypisch |
    Und folgende Personenkonten werden individuel betrachtet
      | Kontonummer |

  # Sachkonto mit Generalumkehr
  @RunMe
  Szenario: Ein Sachkonto mit Generalumkehr
    Angenommen folgende Tagessummen
      | Kontonummer | Buchungstag | WJ-Monat | Bereichsnummer | Buchungstyp | SummeHaben | SummeSoll |
      | 10100000    | 20210301    | 3        | 0              | 1           |            | 20522.17  |
      | 10100000    | 20210401    | 4        | 0              | 1           | 6883.85    |           |
      | 10100000    | 20210402    | 4        | 0              | 1           | 9373.43    |           |
      | 10100000    | 20210501    | 5        | 0              | 1           |            | 6637.69   |
      | 10100000    | 20210601    | 6        | 0              | 1           |            | 9619.59   |
      | 10100000    | 20210701    | 7        | 0              | 1           | -16257.28  | -36779.45 |
    Wenn Tagessummen verarbeitet werden
    Dann werden folgende Tageswerte ermittelt
      | Kontonummer | Buchungstag | Bereichsnummer | Buchungstyp | SummeHaben | SummeSoll |
      | 10100000    | 20210301    | 0              |             |            | 20522.17  |
      | 10100000    | 20210401    | 0              |             | 6883.85    |           |
      | 10100000    | 20210402    | 0              |             | 9373.43    |           |
      | 10100000    | 20210501    | 0              |             |            | 6637.69   |
      | 10100000    | 20210601    | 0              |             |            | 9619.59   |
      | 10100000    | 20210701    | 0              |             | -16257.28  | -36779.45 |
    Und werden folgende Monatswerte ermittelt
      | Kontonummer | WJ-Monat | Bereichsnummer | Buchungstyp | SummeHaben | SummeSoll |
      | 10100000    | 3        | 0              |             |            | 20522.17  |
      | 10100000    | 4        | 0              |             | 16257.28   |           |
      | 10100000    | 5        | 0              |             |            | 6637.69   |
      | 10100000    | 6        | 0              |             |            | 9619.59   |
      | 10100000    | 7        | 0              |             | -16257.28  | -36779.45 |
    Und werden folgende Tageswerte der Personenkontengruppen ermittelt
      | Kontogruppennummer | Buchungstag | Bereichsnummer | Buchungstyp | SummeHabenTypisch | SummeHabenUntypisch | SummeSollTypisch | SummeSollUntypisch |
    Und werden folgende Monatswerte der Personenkontengruppen ermittelt
      | Kontogruppennummer | WJ-Monat | Bereichsnummer | Buchungstyp | SummeHabenTypisch | SummeHabenUntypisch | SummeSollTypisch | SummeSollUntypisch |
    Und folgende Personenkonten werden individuel betrachtet
      | Kontonummer |

  # Standardfall Personenkonto ohne Besonderheiten
  @RunMe
  Szenario: Ein Personenkonto Debitor Standard
    Angenommen folgende Tagessummen
      | Kontonummer | Buchungstag | WJ-Monat | Bereichsnummer | Buchungstyp | SummeHaben | SummeSoll |
      | 101000000   | 20210301    | 3        | 0              | 1           |            | 20522.17  |
      | 101000000   | 20210401    | 4        | 0              | 1           | 6883.85    |           |
      | 101000000   | 20210402    | 4        | 0              | 1           | 9373.43    |           |
      | 101000000   | 20210501    | 5        | 0              | 1           |            | 6637.69   |
      | 101000000   | 20210601    | 6        | 0              | 1           |            | 9619.59   |
      | 101000000   | 20210701    | 7        | 0              | 1           | 16465.6    |           |
    Wenn Tagessummen verarbeitet werden
    Dann werden folgende Tageswerte ermittelt
      | Kontonummer | Buchungstag | Bereichsnummer | Buchungstyp | SummeHaben | SummeSoll |
      | 101000000   | 20210301    | 0              |             |            | 20522.17  |
      | 101000000   | 20210401    | 0              |             | 6883.85    |           |
      | 101000000   | 20210402    | 0              |             | 9373.43    |           |
      | 101000000   | 20210501    | 0              |             |            | 6637.69   |
      | 101000000   | 20210601    | 0              |             |            | 9619.59   |
      | 101000000   | 20210701    | 0              |             | 16465.6    |           |
    Und werden folgende Monatswerte ermittelt
      | Kontonummer | WJ-Monat | Bereichsnummer | Buchungstyp | SummeHaben | SummeSoll |
      | 101000000   | 3        | 0              |             |            | 20522.17  |
      | 101000000   | 4        | 0              |             | 16257.28   |           |
      | 101000000   | 5        | 0              |             |            | 6637.69   |
      | 101000000   | 6        | 0              |             |            | 9619.59   |
      | 101000000   | 7        | 0              |             | 16465.6    |           |
    Und werden folgende Tageswerte der Personenkontengruppen ermittelt
      | Kontogruppennummer | Buchungstag | Bereichsnummer | Buchungstyp | SummeHabenTypisch | SummeHabenUntypisch | SummeSollTypisch | SummeSollUntypisch |
      | 1                  | 20210301    | 0              |             |                   |                     | 20522.17         |                    |
      | 1                  | 20210401    | 0              |             | 6883.85           |                     |                  |                    |
      | 1                  | 20210402    | 0              |             | 9373.43           |                     |                  |                    |
      | 1                  | 20210501    | 0              |             |                   |                     | 6637.69          |                    |
      | 1                  | 20210601    | 0              |             |                   |                     | 9619.59          |                    |
      | 1                  | 20210701    | 0              |             | 16465.6           |                     |                  |                    |
    Und werden folgende Monatswerte der Personenkontengruppen ermittelt
      | Kontogruppennummer | WJ-Monat | Bereichsnummer | Buchungstyp | SummeHabenTypisch | SummeHabenUntypisch | SummeSollTypisch | SummeSollUntypisch |
      | 1                  | 3        | 0              |             |                   |                     | 20522.17         |                    |
      | 1                  | 4        | 0              |             | 16257.28          |                     |                  |                    |
      | 1                  | 5        | 0              |             |                   |                     | 6637.69          |                    |
      | 1                  | 6        | 0              |             |                   |                     | 9619.59          |                    |
      | 1                  | 7        | 0              |             | 16465.6           |                     |                  |                    |
    Und folgende Personenkonten werden individuel betrachtet
      | Kontonummer |

  # Standardfall Personenkonto, anderer Buchungstyp führt dazu das die Monatswerte nicht aggregiert werden
  @RunMe
  Szenario: Ein Personenkonto Debitor Standard - unterschiedliche Buchungstypen
    Angenommen folgende Tagessummen
      | Kontonummer | Buchungstag | WJ-Monat | Bereichsnummer | Buchungstyp | SummeHaben | SummeSoll |
      | 101000000   | 20210301    | 3        | 0              | 1           |            | 20522.17  |
      | 101000000   | 20210401    | 4        | 0              | 1           | 6883.85    |           |
      | 101000000   | 20210401    | 4        | 0              | 2           | 9373.43    |           |
      | 101000000   | 20210501    | 5        | 0              | 1           |            | 6637.69   |
      | 101000000   | 20210601    | 6        | 0              | 1           |            | 9619.59   |
      | 101000000   | 20210701    | 7        | 0              | 1           | 16465.6    |           |
    Wenn Tagessummen verarbeitet werden
    Dann werden folgende Tageswerte ermittelt
      | Kontonummer | Buchungstag | Bereichsnummer | Buchungstyp | SummeHaben | SummeSoll |
      | 101000000   | 20210301    | 0              |             |            | 20522.17  |
      | 101000000   | 20210401    | 0              |             | 6883.85    |           |
      | 101000000   | 20210401    | 0              | 2           | 9373.43    |           |
      | 101000000   | 20210501    | 0              |             |            | 6637.69   |
      | 101000000   | 20210601    | 0              |             |            | 9619.59   |
      | 101000000   | 20210701    | 0              |             | 16465.6    |           |
    Und werden folgende Monatswerte ermittelt
      | Kontonummer | WJ-Monat | Bereichsnummer | Buchungstyp | SummeHaben | SummeSoll |
      | 101000000   | 3        | 0              |             |            | 20522.17  |
      | 101000000   | 4        | 0              |             | 6883.85    |           |
      | 101000000   | 4        | 0              | 2           | 9373.43    |           |
      | 101000000   | 5        | 0              |             |            | 6637.69   |
      | 101000000   | 6        | 0              |             |            | 9619.59   |
      | 101000000   | 7        | 0              |             | 16465.6    |           |
    Und werden folgende Tageswerte der Personenkontengruppen ermittelt
      | Kontogruppennummer | Buchungstag | Bereichsnummer | Buchungstyp | SummeHabenTypisch | SummeHabenUntypisch | SummeSollTypisch | SummeSollUntypisch |
      | 1                  | 20210301    | 0              |             |                   |                     | 20522.17         |                    |
      | 1                  | 20210401    | 0              |             | 6883.85           |                     |                  |                    |
      | 1                  | 20210401    | 0              | 2           | 9373.43           |                     |                  |                    |
      | 1                  | 20210501    | 0              |             |                   |                     | 6637.69          |                    |
      | 1                  | 20210601    | 0              |             |                   |                     | 9619.59          |                    |
      | 1                  | 20210701    | 0              |             | 16465.6           |                     |                  |                    |
    Und werden folgende Monatswerte der Personenkontengruppen ermittelt
      | Kontogruppennummer | WJ-Monat | Bereichsnummer | Buchungstyp | SummeHabenTypisch | SummeHabenUntypisch | SummeSollTypisch | SummeSollUntypisch |
      | 1                  | 3        | 0              |             |                   |                     | 20522.17         |                    |
      | 1                  | 4        | 0              |             | 6883.85           |                     |                  |                    |
      | 1                  | 4        | 0              | 2           | 9373.43           |                     |                  |                    |
      | 1                  | 5        | 0              |             |                   |                     | 6637.69          |                    |
      | 1                  | 6        | 0              |             |                   |                     | 9619.59          |                    |
      | 1                  | 7        | 0              |             | 16465.6           |                     |                  |                    |
    Und folgende Personenkonten werden individuel betrachtet
      | Kontonummer |

  # Debitor mit 0-Saldo (Gesamt Summe Soll == Gesamt Summe Haben). 0-Saldo darf nicht untypisch/Unusual werden.
  @RunMe
  Szenario: Ein Personenkonto Debitor Saldo 0
    Angenommen folgende Tagessummen
      | Kontonummer | Buchungstag | WJ-Monat | Bereichsnummer | Buchungstyp | SummeHaben | SummeSoll |
      | 101000000   | 20210101    | 0        | 0              | 1           |            | 20522.17  |
      | 101000000   | 20210401    | 4        | 0              | 1           | 6883.85    |           |
      | 101000000   | 20210402    | 4        | 0              | 1           | 9373.43    | 6637.69   |
      | 101000000   | 20210501    | 5        | 0              | 1           | 10902.58   |           |
    Wenn Tagessummen verarbeitet werden
    Dann werden folgende Tageswerte ermittelt
      | Kontonummer | Buchungstag | Bereichsnummer | Buchungstyp | SummeHaben | SummeSoll |
      | 101000000   | EB          | 0              |             |            | 20522.17  |
      | 101000000   | 20210401    | 0              |             | 6883.85    |           |
      | 101000000   | 20210402    | 0              |             | 9373.43    | 6637.69   |
      | 101000000   | 20210501    | 0              |             | 10902.58   |           |
    Und werden folgende Monatswerte ermittelt
      | Kontonummer | WJ-Monat | Bereichsnummer | Buchungstyp | SummeHaben | SummeSoll |
      | 101000000   | EB       | 0              |             |            | 20522.17  |
      | 101000000   | 4        | 0              |             | 16257.28   | 6637.69   |
      | 101000000   | 5        | 0              |             | 10902.58   |           |
    Und werden folgende Tageswerte der Personenkontengruppen ermittelt
      | Kontogruppennummer | Buchungstag | Bereichsnummer | Buchungstyp | SummeHabenTypisch | SummeHabenUntypisch | SummeSollTypisch | SummeSollUntypisch |
      | 1                  | EB          | 0              |             |                   |                     | 20522.17         |                    |
      | 1                  | 20210401    | 0              |             | 6883.85           |                     |                  |                    |
      | 1                  | 20210402    | 0              |             | 9373.43           |                     | 6637.69          |                    |
      | 1                  | 20210501    | 0              |             | 10902.58          |                     |                  |                    |
    Und werden folgende Monatswerte der Personenkontengruppen ermittelt
      | Kontogruppennummer | WJ-Monat | Bereichsnummer | Buchungstyp | SummeHabenTypisch | SummeHabenUntypisch | SummeSollTypisch | SummeSollUntypisch |
      | 1                  | EB       | 0              |             |                   |                     | 20522.17         |                    |
      | 1                  | 4        | 0              |             | 16257.28          |                     | 6637.69          |                    |
      | 1                  | 5        | 0              |             | 10902.58          |                     |                  |                    |
    Und folgende Personenkonten werden individuel betrachtet
      | Kontonummer |

  # DEbitor wird zu einem kreditorischen Debitor und damit untypisch/Unusual. (einmalig mit einheitlichem Buchungstyp)
  # Beim Wechsel wird die Summe der vorherigen Werte beim vorherigen Ausweis (Usual/Unusual) negativ ausgebucht.
  # Entscheidend ist ob die Summe Soll zum Monat x kleiner oder größer ist, als Summe Haben (bei Kleinerer Summe Soll erfolgt Wechsel zu unsual und andersrum)
  @RunMe
  Szenario: Ein Personenkonto Debitor wechselt zu kreditorischem Debitor (simpler Grundfall)
    Angenommen folgende Tagessummen
      | Kontonummer | Buchungstag | WJ-Monat | Bereichsnummer | Buchungstyp | SummeHaben | SummeSoll |
      | 101000000   | 20210301    | 3        | 0              | 1           |            | 20000.00  |
      | 101000000   | 20210401    | 4        | 0              | 1           | 20000.00   |           |
      | 101000000   | 20210402    | 4        | 0              | 1           | 20000.00   |           |
    Wenn Tagessummen verarbeitet werden
    Dann werden folgende Tageswerte ermittelt
      | Kontonummer | Buchungstag | Bereichsnummer | Buchungstyp | SummeHaben | SummeSoll |
      | 101000000   | 20210301    | 0              |             |            | 20000.00  |
      | 101000000   | 20210401    | 0              |             | 20000.00   |           |
      | 101000000   | 20210402    | 0              |             | 20000.00   |           |
    Und werden folgende Monatswerte ermittelt
      | Kontonummer | WJ-Monat | Bereichsnummer | Buchungstyp | SummeHaben | SummeSoll |
      | 101000000   | 3        | 0              |             |            | 20000.00  |
      | 101000000   | 4        | 0              |             | 40000.00   |           |
    Und werden folgende Tageswerte der Personenkontengruppen ermittelt
      | Kontogruppennummer | Buchungstag | Bereichsnummer | Buchungstyp | SummeHabenTypisch | SummeHabenUntypisch | SummeSollTypisch | SummeSollUntypisch |
      | 1                  | 20210301    | 0              |             |                   |                     | 20000.00         |                    |
      | 1                  | 20210401    | 0              |             | 20000.00          |                     |                  |                    |
      | 1                  | 20210402    | 0              |             | -20000.00         | 40000.00            | -20000.00        | 20000.00           |
    Und werden folgende Monatswerte der Personenkontengruppen ermittelt
      | Kontogruppennummer | WJ-Monat | Bereichsnummer | Buchungstyp | SummeHabenTypisch | SummeHabenUntypisch | SummeSollTypisch | SummeSollUntypisch |
      | 1                  | 3        | 0              |             |                   |                     | 20000.00         |                    |
      | 1                  | 4        | 0              |             |                   | 40000.00            | -20000.00        | 20000.00           |
    Und folgende Personenkonten werden individuel betrachtet
      | Kontonummer |

  # Debitor wird zu einem kreditorischem Debitor und damit untypisch/Unusual. (mehrfacher Wechsel mit einheitlichem Buchungstyp)
  # Dies kann wie in dem gezeigten Beispiel auch mehrfach passieren.
  # Beim Wechsel wird die Summe der vorherigen Werte beim vorherigen Ausweis (Usual/Unusual) negativ ausgebucht.
  # Entscheidend ist ob die Summe Soll zum Monat x kleiner oder größer ist, als Summe Haben (bei Kleinerer Summe Soll erfolgt Wechsel zu unsual und andersrum)
  @RunMe
  Szenario: Ein Personenkonto Debitor wechselt zu kreditorischem Debitor - Einfacher Fall
    Angenommen folgende Tagessummen
      | Kontonummer | Buchungstag | WJ-Monat | Bereichsnummer | Buchungstyp | SummeHaben | SummeSoll |
      | 101000000   | 20210301    | 3        | 0              | 1           |            | 20000.00  |
      | 101000000   | 20210401    | 4        | 0              | 1           | 20000.00   |           |
      | 101000000   | 20210402    | 4        | 0              | 1           | 20000.00   |           |
      | 101000000   | 20210501    | 5        | 0              | 1           |            | 10000.00  |
      | 101000000   | 20210601    | 6        | 0              | 1           |            | 20000.00  |
      | 101000000   | 20210701    | 7        | 0              | 1           | 20000.00   |           |
    Wenn Tagessummen verarbeitet werden
    Dann werden folgende Tageswerte ermittelt
      | Kontonummer | Buchungstag | Bereichsnummer | Buchungstyp | SummeHaben | SummeSoll |
      | 101000000   | 20210301    | 0              |             |            | 20000.00  |
      | 101000000   | 20210401    | 0              |             | 20000.00   |           |
      | 101000000   | 20210402    | 0              |             | 20000.00   |           |
      | 101000000   | 20210501    | 0              |             |            | 10000.00  |
      | 101000000   | 20210601    | 0              |             |            | 20000.00  |
      | 101000000   | 20210701    | 0              |             | 20000.00   |           |
    Und werden folgende Monatswerte ermittelt
      | Kontonummer | WJ-Monat | Bereichsnummer | Buchungstyp | SummeHaben | SummeSoll |
      | 101000000   | 3        | 0              |             |            | 20000.00  |
      | 101000000   | 4        | 0              |             | 40000.00   |           |
      | 101000000   | 5        | 0              |             |            | 10000.00  |
      | 101000000   | 6        | 0              |             |            | 20000.00  |
      | 101000000   | 7        | 0              |             | 20000.00   |           |
    Und werden folgende Tageswerte der Personenkontengruppen ermittelt
      | Kontogruppennummer | Buchungstag | Bereichsnummer | Buchungstyp | SummeHabenTypisch | SummeHabenUntypisch | SummeSollTypisch | SummeSollUntypisch |
      | 1                  | 20210301    | 0              |             |                   |                     | 20000.00         |                    |
      | 1                  | 20210401    | 0              |             | 20000.00          |                     |                  |                    |
      | 1                  | 20210402    | 0              |             | -20000.00         | 40000.00            | -20000.00        | 20000.00           |
      | 1                  | 20210501    | 0              |             |                   |                     |                  | 10000.00           |
      | 1                  | 20210601    | 0              |             | 40000.00          | -40000.00           | 50000.00         | -30000.00          |
      | 1                  | 20210701    | 0              |             | -40000.00         | 60000.00            | -50000.00        | 50000.00           |
    Und werden folgende Monatswerte der Personenkontengruppen ermittelt
      | Kontogruppennummer | WJ-Monat | Bereichsnummer | Buchungstyp | SummeHabenTypisch | SummeHabenUntypisch | SummeSollTypisch | SummeSollUntypisch |
      | 1                  | 3        | 0              |             |                   |                     | 20000.00         |                    |
      | 1                  | 4        | 0              |             |                   | 40000.00            | -20000.00        | 20000.00           |
      | 1                  | 5        | 0              |             |                   |                     |                  | 10000.00           |
      | 1                  | 6        | 0              |             | 40000.00          | -40000.00           | 50000.00         | -30000.00          |
      | 1                  | 7        | 0              |             | -40000.00         | 60000.00            | -50000.00        | 50000.00           |
    Und folgende Personenkonten werden individuel betrachtet
      | Kontonummer |

  # Debitor wird zu einem kreditorischem Debitor und damit untypisch/Unusual.
  # Dies kann wie in dem gezeigten Beispiel auch mehrfach passieren.
  # Beim Wechsel wird die Summe der vorherigen Werte beim vorherigen Ausweis (Usual/Unusual) negativ ausgebucht.
  @RunMe
  Szenario: Ein Personenkonto Debitor wechselt zu kreditorischem Debitor
    Angenommen folgende Tagessummen
      | Kontonummer | Buchungstag | WJ-Monat | Bereichsnummer | Buchungstyp | SummeHaben | SummeSoll |
      | 101000000   | 20210301    | 3        | 0              | 1           |            | 20522.17  |
      | 101000000   | 20210401    | 4        | 0              | 1           | 6883.85    |           |
      | 101000000   | 20210401    | 4        | 0              | 2           | 39373.43   |           |
      | 101000000   | 20210501    | 5        | 0              | 1           |            | 16637.69  |
      | 101000000   | 20210601    | 6        | 0              | 1           |            | 9619.59   |
      | 101000000   | 20210701    | 7        | 0              | 1           | 1465.6     |           |
    Wenn Tagessummen verarbeitet werden
    Dann werden folgende Tageswerte ermittelt
      | Kontonummer | Buchungstag | Bereichsnummer | Buchungstyp | SummeHaben | SummeSoll |
      | 101000000   | 20210301    | 0              |             |            | 20522.17  |
      | 101000000   | 20210401    | 0              |             | 6883.85    |           |
      | 101000000   | 20210401    | 0              | 2           | 39373.43   |           |
      | 101000000   | 20210501    | 0              |             |            | 16637.69  |
      | 101000000   | 20210601    | 0              |             |            | 9619.59   |
      | 101000000   | 20210701    | 0              |             | 1465.6     |           |
    Und werden folgende Monatswerte ermittelt
      | Kontonummer | WJ-Monat | Bereichsnummer | Buchungstyp | SummeHaben | SummeSoll |
      | 101000000   | 3        | 0              |             |            | 20522.17  |
      | 101000000   | 4        | 0              |             | 6883.85    |           |
      | 101000000   | 4        | 0              | 2           | 39373.43   |           |
      | 101000000   | 5        | 0              |             |            | 16637.69  |
      | 101000000   | 6        | 0              |             |            | 9619.59   |
      | 101000000   | 7        | 0              |             | 1465.6     |           |
    Und werden folgende Tageswerte der Personenkontengruppen ermittelt
      | Kontogruppennummer | Buchungstag | Bereichsnummer | Buchungstyp | SummeHabenTypisch | SummeHabenUntypisch | SummeSollTypisch | SummeSollUntypisch |
      | 1                  | 20210301    | 0              |             |                   |                     | 20522.17         |                    |
      | 1                  | 20210401    | 0              |             |                   | 6883.85             | -20522.17        | 20522.17           |
      | 1                  | 20210401    | 0              | 2           |                   | 39373.43            |                  |                    |
      | 1                  | 20210501    | 0              |             |                   |                     |                  | 16637.69           |
      | 1                  | 20210601    | 0              |             | 6883.85           | -6883.85            | 46779.45         | -37159.86          |
      | 1                  | 20210601    | 0              | 2           | 39373.43          | -39373.43           |                  |                    |
      | 1                  | 20210701    | 0              |             | -6883.85          | 8349.45             | -46779.45        | 46779.45           |
      | 1                  | 20210701    | 0              | 2           | -39373.43         | 39373.43            |                  |                    |
    Und werden folgende Monatswerte der Personenkontengruppen ermittelt
      | Kontogruppennummer | WJ-Monat | Bereichsnummer | Buchungstyp | SummeHabenTypisch | SummeHabenUntypisch | SummeSollTypisch | SummeSollUntypisch |
      | 1                  | 3        | 0              |             |                   |                     | 20522.17         |                    |
      | 1                  | 4        | 0              |             |                   | 6883.85             | -20522.17        | 20522.17           |
      | 1                  | 4        | 0              | 2           |                   | 39373.43            |                  |                    |
      | 1                  | 5        | 0              |             |                   |                     |                  | 16637.69           |
      | 1                  | 6        | 0              |             | 6883.85           | -6883.85            | 46779.45         | -37159.86          |
      | 1                  | 6        | 0              | 2           | 39373.43          | -39373.43           |                  |                    |
      | 1                  | 7        | 0              |             | -6883.85          | 8349.45             | -46779.45        | 46779.45           |
      | 1                  | 7        | 0              | 2           | -39373.43         | 39373.43            |                  |                    |
    Und folgende Personenkonten werden individuel betrachtet
      | Kontonummer |

  # Debitor wird zu einem kreditorischem Debitor und damit untypisch/Unusual. (einfacher Fall)
  # Danach wird er zu einem 0-Saldo, der wieder typisch/usual
  @RunMe
  Szenario: Ein Personenkonto Debitor wechselt zu kreditorischem Debitor und dann Saldo 0. Einfacher Fall
    Angenommen folgende Tagessummen
      | Kontonummer | Buchungstag | WJ-Monat | Bereichsnummer | Buchungstyp | SummeHaben | SummeSoll |
      | 101000000   | 20210301    | 3        | 0              | 1           |            | 20000.00  |
      | 101000000   | 20210401    | 4        | 0              | 1           | 40000.00   |           |
      | 101000000   | 20210501    | 5        | 0              | 1           |            | 20000.00  |
    Wenn Tagessummen verarbeitet werden
    Dann werden folgende Tageswerte ermittelt
      | Kontonummer | Buchungstag | Bereichsnummer | Buchungstyp | SummeHaben | SummeSoll |
      | 101000000   | 20210301    | 0              |             |            | 20000.00  |
      | 101000000   | 20210401    | 0              |             | 40000.00   |           |
      | 101000000   | 20210501    | 0              |             |            | 20000.00  |
    Und werden folgende Monatswerte ermittelt
      | Kontonummer | WJ-Monat | Bereichsnummer | Buchungstyp | SummeHaben | SummeSoll |
      | 101000000   | 3        | 0              |             |            | 20000.00  |
      | 101000000   | 4        | 0              |             | 40000.00   |           |
      | 101000000   | 5        | 0              |             |            | 20000.00  |
    Und werden folgende Tageswerte der Personenkontengruppen ermittelt
      | Kontogruppennummer | Buchungstag | Bereichsnummer | Buchungstyp | SummeHabenTypisch | SummeHabenUntypisch | SummeSollTypisch | SummeSollUntypisch |
      | 1                  | 20210301    | 0              |             |                   |                     | 20000.00         |                    |
      | 1                  | 20210401    | 0              |             |                   | 40000.00            | -20000.00        | 20000.00           |
      | 1                  | 20210501    | 0              |             | 40000.00          | -40000.00           | 40000.00         | -20000.00          |
    Und werden folgende Monatswerte der Personenkontengruppen ermittelt
      | Kontogruppennummer | WJ-Monat | Bereichsnummer | Buchungstyp | SummeHabenTypisch | SummeHabenUntypisch | SummeSollTypisch | SummeSollUntypisch |
      | 1                  | 3        | 0              |             |                   |                     | 20000.00         |                    |
      | 1                  | 4        | 0              |             |                   | 40000.00            | -20000.00        | 20000.00           |
      | 1                  | 5        | 0              |             | 40000.00          | -40000.00           | 40000.00         | -20000.00          |
    Und folgende Personenkonten werden individuel betrachtet
      | Kontonummer |

  # Debitor wird zu einem kreditorischem Debitor und damit untypisch/Unusual.
  # Danach wird er zu einem 0-Saldo, der wieder typisch/usual
  @RunMe
  Szenario: Ein Personenkonto Debitor wechselt zu kreditorischem Debitor und dann Saldo 0
    Angenommen folgende Tagessummen
      | Kontonummer | Buchungstag | WJ-Monat | Bereichsnummer | Buchungstyp | SummeHaben | SummeSoll |
      | 101000000   | 20210301    | 3        | 0              | 1           |            | 20522.17  |
      | 101000000   | 20210401    | 4        | 0              | 1           | 6883.85    |           |
      | 101000000   | 20210401    | 4        | 0              | 2           | 39373.43   |           |
      | 101000000   | 20210501    | 5        | 0              | 1           |            | 16637.69  |
      | 101000000   | 20210601    | 6        | 0              | 1           |            | 9619.59   |
      | 101000000   | 20210701    | 7        | 0              | 1           | 1465.6     |           |
      | 101000000   | 20210801    | 8        | 0              | 2           |            | 943.43    |
    Wenn Tagessummen verarbeitet werden
    Dann werden folgende Tageswerte ermittelt
      | Kontonummer | Buchungstag | Bereichsnummer | Buchungstyp | SummeHaben | SummeSoll |
      | 101000000   | 20210301    | 0              |             |            | 20522.17  |
      | 101000000   | 20210401    | 0              |             | 6883.85    |           |
      | 101000000   | 20210401    | 0              | 2           | 39373.43   |           |
      | 101000000   | 20210501    | 0              |             |            | 16637.69  |
      | 101000000   | 20210601    | 0              |             |            | 9619.59   |
      | 101000000   | 20210701    | 0              |             | 1465.6     |           |
      | 101000000   | 20210801    | 0              | 2           |            | 943.43    |
    Und werden folgende Monatswerte ermittelt
      | Kontonummer | WJ-Monat | Bereichsnummer | Buchungstyp | SummeHaben | SummeSoll |
      | 101000000   | 3        | 0              |             |            | 20522.17  |
      | 101000000   | 4        | 0              |             | 6883.85    |           |
      | 101000000   | 4        | 0              | 2           | 39373.43   |           |
      | 101000000   | 5        | 0              |             |            | 16637.69  |
      | 101000000   | 6        | 0              |             |            | 9619.59   |
      | 101000000   | 7        | 0              |             | 1465.6     |           |
      | 101000000   | 8        | 0              | 2           |            | 943.43    |
    Und werden folgende Tageswerte der Personenkontengruppen ermittelt
      | Kontogruppennummer | Buchungstag | Bereichsnummer | Buchungstyp | SummeHabenTypisch | SummeHabenUntypisch | SummeSollTypisch | SummeSollUntypisch |
      | 1                  | 20210301    | 0              |             |                   |                     | 20522.17         |                    |
      | 1                  | 20210401    | 0              |             |                   | 6883.85             | -20522.17        | 20522.17           |
      | 1                  | 20210401    | 0              | 2           |                   | 39373.43            |                  |                    |
      | 1                  | 20210501    | 0              |             |                   |                     |                  | 16637.69           |
      | 1                  | 20210601    | 0              |             | 6883.85           | -6883.85            | 46779.45         | -37159.86          |
      | 1                  | 20210601    | 0              | 2           | 39373.43          | -39373.43           |                  |                    |
      | 1                  | 20210701    | 0              |             | -6883.85          | 8349.45             | -46779.45        | 46779.45           |
      | 1                  | 20210701    | 0              | 2           | -39373.43         | 39373.43            |                  |                    |
      | 1                  | 20210801    | 0              |             | 8349.45           | -8349.45            | 46779.45         | -46779.45          |
      | 1                  | 20210801    | 0              | 2           | 39373.43          | -39373.43           | 943.43           |                    |
    Und werden folgende Monatswerte der Personenkontengruppen ermittelt
      | Kontogruppennummer | WJ-Monat | Bereichsnummer | Buchungstyp | SummeHabenTypisch | SummeHabenUntypisch | SummeSollTypisch | SummeSollUntypisch |
      | 1                  | 3        | 0              |             |                   |                     | 20522.17         |                    |
      | 1                  | 4        | 0              |             |                   | 6883.85             | -20522.17        | 20522.17           |
      | 1                  | 4        | 0              | 2           |                   | 39373.43            |                  |                    |
      | 1                  | 5        | 0              |             |                   |                     |                  | 16637.69           |
      | 1                  | 6        | 0              |             | 6883.85           | -6883.85            | 46779.45         | -37159.86          |
      | 1                  | 6        | 0              | 2           | 39373.43          | -39373.43           |                  |                    |
      | 1                  | 7        | 0              |             | -6883.85          | 8349.45             | -46779.45        | 46779.45           |
      | 1                  | 7        | 0              | 2           | -39373.43         | 39373.43            |                  |                    |
      | 1                  | 8        | 0              |             | 8349.45           | -8349.45            | 46779.45         | -46779.45          |
      | 1                  | 8        | 0              | 2           | 39373.43          | -39373.43           | 943.43           |                    |
    Und folgende Personenkonten werden individuel betrachtet
      | Kontonummer |

  # Personenkonten mit Generalumkehr lassen sich nicht fehlerfrei aufaddieren
  # Beispiel:
  #     Debitor 1 => Monat 1 400 € Monat 3 -400 € (Generalumkehr)
  #     Debitor 2 => Monat 1 300 € Monat 7 -300 € (Generalumkehr)
  # Generalumkehr wirkt nur, wenn alle Werte vollständig umgekehrt werden. Dies ist Debitor 1 in Monat 3 der Fall.
  # D.h. sobald dieser Monat mit einbezogen wird, werden die Werte des Monat 1 ungueltig.
  # Bei Debitor 2 ist dies aber erst ab Monat 7 der Fall.
  # Dies lässt sich durch eine Summe nicht abbilden.
  # Daher werden bei Personenkonten mit Generalumkehr diese einzeln zurückgegeben und nicht in die Summe mit aufgenommen.
  # GU --> immer wenn eine negative Zahl in den Spalten SummeHaben/Summesoll steht (vereinfachende Annahme)
  @RunMe
  Szenario: Ein Personenkonto Debitor mit vollständiger Generalumkehr
    Angenommen folgende Tagessummen
      | Kontonummer | Buchungstag | WJ-Monat | Bereichsnummer | Buchungstyp | SummeHaben | SummeSoll |
      | 101000000   | 20210301    | 3        | 0              | 1           |            | 20522.17  |
      | 101000000   | 20210401    | 4        | 0              | 1           | 6883.85    |           |
      | 101000000   | 20210501    | 5        | 0              | 1           | -6883.85   | -20522.17 |
    Wenn Tagessummen verarbeitet werden
    Dann werden folgende Tageswerte ermittelt
      | Kontonummer | Buchungstag | Bereichsnummer | Buchungstyp | SummeHaben | SummeSoll |
      | 101000000   | 20210301    | 0              |             |            | 20522.17  |
      | 101000000   | 20210401    | 0              |             | 6883.85    |           |
      | 101000000   | 20210501    | 0              |             | -6883.85   | -20522.17 |
    Und werden folgende Monatswerte ermittelt
      | Kontonummer | WJ-Monat | Bereichsnummer | Buchungstyp | SummeHaben | SummeSoll |
      | 101000000   | 3        | 0              |             |            | 20522.17  |
      | 101000000   | 4        | 0              |             | 6883.85    |           |
      | 101000000   | 5        | 0              |             | -6883.85   | -20522.17 |
    Und werden folgende Tageswerte der Personenkontengruppen ermittelt
      | Kontogruppennummer | Buchungstag | Bereichsnummer | Buchungstyp | SummeHabenTypisch | SummeHabenUntypisch | SummeSollTypisch | SummeSollUntypisch |
    Und werden folgende Monatswerte der Personenkontengruppen ermittelt
      | Kontogruppennummer | WJ-Monat | Bereichsnummer | Buchungstyp | SummeHabenTypisch | SummeHabenUntypisch | SummeSollTypisch | SummeSollUntypisch |
    Und folgende Personenkonten werden individuel betrachtet
      | Kontonummer |
      | 101000000   |

  # andere Bereichsnummer führt zu Rückgabe von Einzelsätzen
  @RunMe
  Szenario: Ein Personenkonto mit unterschiedlicher Bereichsnummer führt zu Einzelsätzen
    Angenommen folgende Tagessummen
      | Kontonummer | Buchungstag | WJ-Monat | Bereichsnummer | Buchungstyp | SummeHaben | SummeSoll |
      | 101000000   | 20210301    | 3        | 0              | 1           |            | 20522.17  |
      | 101000000   | 20210401    | 4        | 0              | 1           | 6883.85    |           |
      | 101000000   | 20210401    | 4        | 1              | 1           | 9373.43    |           |
      | 101000000   | 20210501    | 5        | 0              | 1           |            | 6637.69   |
      | 101000000   | 20210601    | 6        | 0              | 1           |            | 9619.59   |
      | 101000000   | 20210701    | 7        | 0              | 1           | 16465.6    |           |
    Wenn Tagessummen verarbeitet werden
    Dann werden folgende Tageswerte ermittelt
      | Kontonummer | Buchungstag | Bereichsnummer | Buchungstyp | SummeHaben | SummeSoll |
      | 101000000   | 20210301    | 0              |             |            | 20522.17  |
      | 101000000   | 20210401    | 0              |             | 6883.85    |           |
      | 101000000   | 20210401    | 1              |             | 9373.43    |           |
      | 101000000   | 20210501    | 0              |             |            | 6637.69   |
      | 101000000   | 20210601    | 0              |             |            | 9619.59   |
      | 101000000   | 20210701    | 0              |             | 16465.6    |           |
    Und werden folgende Monatswerte ermittelt
      | Kontonummer | WJ-Monat | Bereichsnummer | Buchungstyp | SummeHaben | SummeSoll |
      | 101000000   | 3        | 0              |             |            | 20522.17  |
      | 101000000   | 4        | 0              |             | 6883.85    |           |
      | 101000000   | 4        | 1              |             | 9373.43    |           |
      | 101000000   | 5        | 0              |             |            | 6637.69   |
      | 101000000   | 6        | 0              |             |            | 9619.59   |
      | 101000000   | 7        | 0              |             | 16465.6    |           |
    Und werden folgende Tageswerte der Personenkontengruppen ermittelt
      | Kontogruppennummer | Buchungstag | Bereichsnummer | Buchungstyp | SummeHabenTypisch | SummeHabenUntypisch | SummeSollTypisch | SummeSollUntypisch |
    Und werden folgende Monatswerte der Personenkontengruppen ermittelt
      | Kontogruppennummer | WJ-Monat | Bereichsnummer | Buchungstyp | SummeHabenTypisch | SummeHabenUntypisch | SummeSollTypisch | SummeSollUntypisch |
    Und folgende Personenkonten werden individuel betrachtet
      | Kontonummer |
      | 101000000   |

  # Personenkonten mit Generalumkehr und ohne Generalumkehr, von der GU betroffene Konten laufen nicht in die Gruppierung ein
  @RunMe
  Szenario: Ein Personenkonto Debitor mit vollständiger Generalumkehr und ohne Generalumkehr
    Angenommen folgende Tagessummen
      | Kontonummer | Buchungstag | WJ-Monat | Bereichsnummer | Buchungstyp | SummeHaben | SummeSoll |
      | 101000000   | 20210301    | 3        | 0              | 1           |            | 20522.17  |
      | 101000000   | 20210401    | 4        | 0              | 2           | 6883.85    |           |
      | 101000000   | 20210501    | 5        | 0              | 2           | -6883.85   | -20522.17 |
      | 102000000   | 20210301    | 3        | 0              | 1           | 3000.00    | 3000.00   |
    Wenn Tagessummen verarbeitet werden
    Dann werden folgende Tageswerte ermittelt
      | Kontonummer | Buchungstag | Bereichsnummer | Buchungstyp | SummeHaben | SummeSoll |
      | 101000000   | 20210301    | 0              |             |            | 20522.17  |
      | 101000000   | 20210401    | 0              | 2           | 6883.85    |           |
      | 101000000   | 20210501    | 0              | 2           | -6883.85   | -20522.17 |
      | 102000000   | 20210301    | 0              |             | 3000.00    | 3000.00   |
    Und werden folgende Monatswerte ermittelt
      | Kontonummer | WJ-Monat | Bereichsnummer | Buchungstyp | SummeHaben | SummeSoll |
      | 101000000   | 3        | 0              |             |            | 20522.17  |
      | 101000000   | 4        | 0              | 2           | 6883.85    |           |
      | 101000000   | 5        | 0              | 2           | -6883.85   | -20522.17 |
      | 102000000   | 3        | 0              |             | 3000.00    | 3000.00   |
    Und werden folgende Tageswerte der Personenkontengruppen ermittelt
      | Kontogruppennummer | Buchungstag | Bereichsnummer | Buchungstyp | SummeHabenTypisch | SummeHabenUntypisch | SummeSollTypisch | SummeSollUntypisch |
      | 1                  | 20210301    | 0              |             | 3000.00           |                     | 3000.00          |                    |
    Und werden folgende Monatswerte der Personenkontengruppen ermittelt
      | Kontogruppennummer | WJ-Monat | Bereichsnummer | Buchungstyp | SummeHabenTypisch | SummeHabenUntypisch | SummeSollTypisch | SummeSollUntypisch |
      | 1                  | 3        | 0              |             | 3000.00           |                     | 3000.00          |                    |
    Und folgende Personenkonten werden individuel betrachtet
      | Kontonummer |
      | 101000000   |

  # Standardfall ohne Besonderheiten
  @RunMe
  Szenario: Ein Personenkonto Kreditor Standard
    Angenommen folgende Tagessummen
      | Kontonummer | Buchungstag | WJ-Monat | Bereichsnummer | Buchungstyp | SummeHaben | SummeSoll |
      | 701000000   | 20210301    | 3        | 0              | 1           | 20522.17   |           |
      | 701000000   | 20210401    | 4        | 0              | 1           |            | 6883.85   |
      | 701000000   | 20210402    | 4        | 0              | 1           |            | 9373.43   |
      | 701000000   | 20210501    | 5        | 0              | 1           | 6637.69    |           |
      | 701000000   | 20210601    | 6        | 0              | 1           | 9619.59    |           |
      | 701000000   | 20210701    | 7        | 0              | 1           |            | 16465.6   |
    Wenn Tagessummen verarbeitet werden
    Dann werden folgende Tageswerte ermittelt
      | Kontonummer | Buchungstag | Bereichsnummer | Buchungstyp | SummeHaben | SummeSoll |
      | 701000000   | 20210301    | 0              |             | 20522.17   |           |
      | 701000000   | 20210401    | 0              |             |            | 6883.85   |
      | 701000000   | 20210402    | 0              |             |            | 9373.43   |
      | 701000000   | 20210501    | 0              |             | 6637.69    |           |
      | 701000000   | 20210601    | 0              |             | 9619.59    |           |
      | 701000000   | 20210701    | 0              |             |            | 16465.6   |
    Und werden folgende Monatswerte ermittelt
      | Kontonummer | WJ-Monat | Bereichsnummer | Buchungstyp | SummeHaben | SummeSoll |
      | 701000000   | 3        | 0              |             | 20522.17   |           |
      | 701000000   | 4        | 0              |             |            | 16257.28  |
      | 701000000   | 5        | 0              |             | 6637.69    |           |
      | 701000000   | 6        | 0              |             | 9619.59    |           |
      | 701000000   | 7        | 0              |             |            | 16465.6   |
    Und werden folgende Tageswerte der Personenkontengruppen ermittelt
      | Kontogruppennummer | Buchungstag | Bereichsnummer | Buchungstyp | SummeHabenTypisch | SummeHabenUntypisch | SummeSollTypisch | SummeSollUntypisch |
      | 7                  | 20210301    | 0              |             | 20522.17          |                     |                  |                    |
      | 7                  | 20210401    | 0              |             |                   |                     | 6883.85          |                    |
      | 7                  | 20210402    | 0              |             |                   |                     | 9373.43          |                    |
      | 7                  | 20210501    | 0              |             | 6637.69           |                     |                  |                    |
      | 7                  | 20210601    | 0              |             | 9619.59           |                     |                  |                    |
      | 7                  | 20210701    | 0              |             |                   |                     | 16465.6          |                    |
    Und werden folgende Monatswerte der Personenkontengruppen ermittelt
      | Kontogruppennummer | WJ-Monat | Bereichsnummer | Buchungstyp | SummeHabenTypisch | SummeHabenUntypisch | SummeSollTypisch | SummeSollUntypisch |
      | 7                  | 3        | 0              |             | 20522.17          |                     |                  |                    |
      | 7                  | 4        | 0              |             |                   |                     | 16257.28         |                    |
      | 7                  | 5        | 0              |             | 6637.69           |                     |                  |                    |
      | 7                  | 6        | 0              |             | 9619.59           |                     |                  |                    |
      | 7                  | 7        | 0              |             |                   |                     | 16465.6          |                    |
    Und folgende Personenkonten werden individuel betrachtet
      | Kontonummer |

  @RunMe
  Szenario: Es handelt sich um einen debitorischen Kreditor Personenkonto
    Angenommen folgende Tagessummen
      | Kontonummer | Buchungstag | WJ-Monat | Bereichsnummer | Buchungstyp | SummeHaben | SummeSoll |
      | 601000000   | 20210101    | 0        | 0              | 1           | 20522.17   |           |
    Wenn Tagessummen verarbeitet werden
    Dann werden folgende Tageswerte ermittelt
      | Kontonummer | Buchungstag | Bereichsnummer | Buchungstyp | SummeHaben | SummeSoll |
      | 601000000   | EB          | 0              |             | 20522.17   |           |
    Und werden folgende Monatswerte ermittelt
      | Kontonummer | WJ-Monat | Bereichsnummer | Buchungstyp | SummeHaben | SummeSoll |
      | 601000000   | EB       | 0              |             | 20522.17   |           |
    Und werden folgende Tageswerte der Personenkontengruppen ermittelt
      | Kontogruppennummer | Buchungstag | Bereichsnummer | Buchungstyp | SummeHabenTypisch | SummeHabenUntypisch | SummeSollTypisch | SummeSollUntypisch |
      | 6                  | EB          | 0              |             |                   | 20522.17            |                  |                    |
    Und werden folgende Monatswerte der Personenkontengruppen ermittelt
      | Kontogruppennummer | WJ-Monat | Bereichsnummer | Buchungstyp | SummeHabenTypisch | SummeHabenUntypisch | SummeSollTypisch | SummeSollUntypisch |
      | 6                  | EB       | 0              |             |                   | 20522.17            |                  |                    |
    Und folgende Personenkonten werden individuel betrachtet
      | Kontonummer |

  @RunMe
  Szenario: Es handelt sich um einen debitorischen Kreditor Personenkonto
    Angenommen folgende Tagessummen
      | Kontonummer | Buchungstag | WJ-Monat | Bereichsnummer | Buchungstyp | SummeHaben | SummeSoll |
      | 701000000   | 20210101    | 0        | 0              | 1           |            | 20522.17  |
    Wenn Tagessummen verarbeitet werden
    Dann werden folgende Tageswerte ermittelt
      | Kontonummer | Buchungstag | Bereichsnummer | Buchungstyp | SummeHaben | SummeSoll |
      | 701000000   | EB          | 0              |             |            | 20522.17  |
    Und werden folgende Monatswerte ermittelt
      | Kontonummer | WJ-Monat | Bereichsnummer | Buchungstyp | SummeHaben | SummeSoll |
      | 701000000   | EB       | 0              |             |            | 20522.17  |
    Und werden folgende Tageswerte der Personenkontengruppen ermittelt
      | Kontogruppennummer | Buchungstag | Bereichsnummer | Buchungstyp | SummeHabenTypisch | SummeHabenUntypisch | SummeSollTypisch | SummeSollUntypisch |
      | 7                  | EB          | 0              |             |                   |                     |                  | 20522.17           |
    Und werden folgende Monatswerte der Personenkontengruppen ermittelt
      | Kontogruppennummer | WJ-Monat | Bereichsnummer | Buchungstyp | SummeHabenTypisch | SummeHabenUntypisch | SummeSollTypisch | SummeSollUntypisch |
      | 7                  | EB       | 0              |             |                   |                     |                  | 20522.17           |
    Und folgende Personenkonten werden individuel betrachtet
      | Kontonummer |

  # Debitor mit 0-Saldo. 0-Saldo darf nicht untypisch/Unusual werden.
  @RunMe
  Szenario: Ein Personenkonto Kreditor Saldo 0
    Angenommen folgende Tagessummen
      | Kontonummer | Buchungstag | WJ-Monat | Bereichsnummer | Buchungstyp | SummeHaben | SummeSoll |
      | 701000000   | 20210101    | 0        | 0              | 1           | 20522.17   |           |
      | 701000000   | 20210401    | 4        | 0              | 1           |            | 6883.85   |
      | 701000000   | 20210402    | 4        | 0              | 1           | 6637.69    | 9373.43   |
      | 701000000   | 20210501    | 5        | 0              | 1           |            | 10902.58  |

    Wenn Tagessummen verarbeitet werden
    Dann werden folgende Tageswerte ermittelt
      | Kontonummer | Buchungstag | Bereichsnummer | Buchungstyp | SummeHaben | SummeSoll |
      | 701000000   | EB          | 0              |             | 20522.17   |           |
      | 701000000   | 20210401    | 0              |             |            | 6883.85   |
      | 701000000   | 20210402    | 0              |             | 6637.69    | 9373.43   |
      | 701000000   | 20210501    | 0              |             |            | 10902.58  |
    Und werden folgende Monatswerte ermittelt
      | Kontonummer | WJ-Monat | Bereichsnummer | Buchungstyp | SummeHaben | SummeSoll |
      | 701000000   | EB       | 0              |             | 20522.17   |           |
      | 701000000   | 4        | 0              |             | 6637.69    | 16257.28  |
      | 701000000   | 5        | 0              |             |            | 10902.58  |
    Und werden folgende Tageswerte der Personenkontengruppen ermittelt
      | Kontogruppennummer | Buchungstag | Bereichsnummer | Buchungstyp | SummeHabenTypisch | SummeHabenUntypisch | SummeSollTypisch | SummeSollUntypisch |
      | 7                  | EB          | 0              |             | 20522.17          |                     |                  |                    |
      | 7                  | 20210401    | 0              |             |                   |                     | 6883.85          |                    |
      | 7                  | 20210402    | 0              |             | 6637.69           |                     | 9373.43          |                    |
      | 7                  | 20210501    | 0              |             |                   |                     | 10902.58         |                    |
    Und werden folgende Monatswerte der Personenkontengruppen ermittelt
      | Kontogruppennummer | WJ-Monat | Bereichsnummer | Buchungstyp | SummeHabenTypisch | SummeHabenUntypisch | SummeSollTypisch | SummeSollUntypisch |
      | 7                  | EB       | 0              |             | 20522.17          |                     |                  |                    |
      | 7                  | 4        | 0              |             | 6637.69           |                     | 16257.28         |                    |
      | 7                  | 5        | 0              |             |                   |                     | 10902.58         |                    |
    Und folgende Personenkonten werden individuel betrachtet
      | Kontonummer |

  # Verschiedene Debitoren zusammenaddiert.
  # Basiert auf den vorherigen Tests und zeigt nur die finiale Addition verschiedenen Debitoren.
  @RunMe
  Szenario: Mehrere Personenkonto Debitoren inkl. Generalumkehr
    Angenommen folgende Tagessummen
      | Kontonummer | Buchungstag | WJ-Monat | Bereichsnummer | Buchungstyp | SummeHaben | SummeSoll |
      | 101000000   | 20210301    | 3        | 0              | 1           |            | 20522.17  |
      | 101000000   | 20210401    | 4        | 0              | 2           | 6883.85    |           |
      | 101000000   | 20210501    | 5        | 0              | 2           | -6883.85   | -20522.17 |
      | 102000000   | 20210301    | 3        | 0              | 1           |            | 20522.17  |
      | 102000000   | 20210401    | 4        | 0              | 1           | 6883.85    |           |
      | 102000000   | 20210402    | 4        | 0              | 1           | 9373.43    |           |
      | 102000000   | 20210501    | 5        | 0              | 1           |            | 6637.69   |
      | 102000000   | 20210601    | 6        | 0              | 1           |            | 9619.59   |
      | 102000000   | 20210701    | 7        | 0              | 1           | 16465.6    |           |
      | 103000000   | 20210301    | 3        | 0              | 1           |            | 20522.17  |
      | 103000000   | 20210401    | 4        | 0              | 1           | 6883.85    |           |
      | 103000000   | 20210401    | 4        | 0              | 2           | 39373.43   |           |
      | 103000000   | 20210501    | 5        | 0              | 1           |            | 16637.69  |
      | 103000000   | 20210601    | 6        | 0              | 1           |            | 9619.59   |
      | 103000000   | 20210701    | 7        | 0              | 1           | 1465.6     |           |
      | 201000000   | 20210101    | 0        | 0              | 1           |            | 20522.17  |
      | 201000000   | 20210401    | 4        | 0              | 1           | 6883.85    |           |
      | 201000000   | 20210402    | 4        | 0              | 1           | 9373.43    | 6637.69   |
      | 201000000   | 20210501    | 5        | 0              | 1           | 10902.58   |           |
    Wenn Tagessummen verarbeitet werden
    Dann werden folgende Tageswerte ermittelt
      | Kontonummer | Buchungstag | Bereichsnummer | Buchungstyp | SummeHaben | SummeSoll |
      | 101000000   | 20210301    | 0              |             |            | 20522.17  |
      | 101000000   | 20210401    | 0              | 2           | 6883.85    |           |
      | 101000000   | 20210501    | 0              | 2           | -6883.85   | -20522.17 |
      | 102000000   | 20210301    | 0              |             |            | 20522.17  |
      | 102000000   | 20210401    | 0              |             | 6883.85    |           |
      | 102000000   | 20210402    | 0              |             | 9373.43    |           |
      | 102000000   | 20210501    | 0              |             |            | 6637.69   |
      | 102000000   | 20210601    | 0              |             |            | 9619.59   |
      | 102000000   | 20210701    | 0              |             | 16465.6    |           |
      | 103000000   | 20210301    | 0              |             |            | 20522.17  |
      | 103000000   | 20210401    | 0              |             | 6883.85    |           |
      | 103000000   | 20210401    | 0              | 2           | 39373.43   |           |
      | 103000000   | 20210501    | 0              |             |            | 16637.69  |
      | 103000000   | 20210601    | 0              |             |            | 9619.59   |
      | 103000000   | 20210701    | 0              |             | 1465.6     |           |
      | 201000000   | EB          | 0              |             |            | 20522.17  |
      | 201000000   | 20210401    | 0              |             | 6883.85    |           |
      | 201000000   | 20210402    | 0              |             | 9373.43    | 6637.69   |
      | 201000000   | 20210501    | 0              |             | 10902.58   |           |
    Und werden folgende Monatswerte ermittelt
      | Kontonummer | WJ-Monat | Bereichsnummer | Buchungstyp | SummeHaben | SummeSoll |
      | 101000000   | 3        | 0              |             |            | 20522.17  |
      | 101000000   | 4        | 0              | 2           | 6883.85    |           |
      | 101000000   | 5        | 0              | 2           | -6883.85   | -20522.17 |
      | 102000000   | 3        | 0              |             |            | 20522.17  |
      | 102000000   | 4        | 0              |             | 16257.28   |           |
      | 102000000   | 5        | 0              |             |            | 6637.69   |
      | 102000000   | 6        | 0              |             |            | 9619.59   |
      | 102000000   | 7        | 0              |             | 16465.6    |           |
      | 103000000   | 3        | 0              |             |            | 20522.17  |
      | 103000000   | 4        | 0              |             | 6883.85    |           |
      | 103000000   | 4        | 0              | 2           | 39373.43   |           |
      | 103000000   | 5        | 0              |             |            | 16637.69  |
      | 103000000   | 6        | 0              |             |            | 9619.59   |
      | 103000000   | 7        | 0              |             | 1465.6     |           |
      | 201000000   | EB       | 0              |             |            | 20522.17  |
      | 201000000   | 4        | 0              |             | 16257.28   | 6637.69   |
      | 201000000   | 5        | 0              |             | 10902.58   |           |
    Und werden folgende Tageswerte der Personenkontengruppen ermittelt
      | Kontogruppennummer | Buchungstag | Bereichsnummer | Buchungstyp | SummeHabenTypisch | SummeHabenUntypisch | SummeSollTypisch | SummeSollUntypisch |
      | 1                  | 20210301    | 0              |             |                   |                     | 41044.34         |                    |
      | 1                  | 20210401    | 0              |             | 6883.85           | 6883.85             | -20522.17        | 20522.17           |
      | 1                  | 20210401    | 0              | 2           |                   | 39373.43            |                  |                    |
      | 1                  | 20210402    | 0              |             | 9373.43           |                     |                  |                    |
      | 1                  | 20210501    | 0              |             |                   |                     | 6637.69          | 16637.69           |
      | 1                  | 20210601    | 0              |             | 6883.85           | -6883.85            | 56399.04         | -37159.86          |
      | 1                  | 20210601    | 0              | 2           | 39373.43          | -39373.43           |                  |                    |
      | 1                  | 20210701    | 0              |             | 9581.75           | 8349.45             | -46779.45        | 46779.45           |
      | 1                  | 20210701    | 0              | 2           | -39373.43         | 39373.43            |                  |                    |
      | 2                  | EB          | 0              |             |                   |                     | 20522.17         |                    |
      | 2                  | 20210401    | 0              |             | 6883.85           |                     |                  |                    |
      | 2                  | 20210402    | 0              |             | 9373.43           |                     | 6637.69          |                    |
      | 2                  | 20210501    | 0              |             | 10902.58          |                     |                  |                    |
    Und werden folgende Monatswerte der Personenkontengruppen ermittelt
      # Werte aus ACDS RefSys Test
      | Kontogruppennummer | WJ-Monat | Bereichsnummer | Buchungstyp | SummeHabenTypisch | SummeHabenUntypisch | SummeSollTypisch | SummeSollUntypisch |
      | 1                  | 3        | 0              |             |                   |                     | 41044.34         |                    |
      | 1                  | 4        | 0              |             | 16257.28          | 6883.85             | -20522.17        | 20522.17           |
      | 1                  | 4        | 0              | 2           |                   | 39373.43            |                  |                    |
      | 1                  | 5        | 0              |             |                   |                     | 6637.69          | 16637.69           |
      | 1                  | 6        | 0              |             | 6883.85           | -6883.85            | 56399.04         | -37159.86          |
      | 1                  | 6        | 0              | 2           | 39373.43          | -39373.43           |                  |                    |
      | 1                  | 7        | 0              |             | 9581.75           | 8349.45             | -46779.45        | 46779.45           |
      | 1                  | 7        | 0              | 2           | -39373.43         | 39373.43            |                  |                    |
      | 2                  | EB       | 0              |             |                   |                     | 20522.17         |                    |
      | 2                  | 4        | 0              |             | 16257.28          |                     | 6637.69          |                    |
      | 2                  | 5        | 0              |             | 10902.58          |                     |                  |                    |
    Und folgende Personenkonten werden individuel betrachtet
      | Kontonummer |
      | 101000000   |

  # Verschiedene Debitoren zusammenaddiert.
  # Basiert auf den vorherigen Tests und zeigt nur die finiale Addition verschiedenen Debitoren.
  @RunMe
  Szenario: Mehrere Personenkonto Debitoren inkl. Generalumkehr mit Buchungstag nicht sortiert
    Angenommen folgende Tagessummen
      | Kontonummer | Buchungstag | WJ-Monat | Bereichsnummer | Buchungstyp | SummeHaben | SummeSoll |
      | 101000000   | 20210401    | 4        | 0              | 2           | 6883.85    |           |
      | 101000000   | 20210501    | 5        | 0              | 2           | -6883.85   | -20522.17 |
      | 101000000   | 20210301    | 3        | 0              | 1           |            | 20522.17  |
      | 102000000   | 20210301    | 3        | 0              | 1           |            | 20522.17  |
      | 102000000   | 20210601    | 6        | 0              | 1           |            | 9619.59   |
      | 102000000   | 20210701    | 7        | 0              | 1           | 16465.6    |           |
      | 102000000   | 20210401    | 4        | 0              | 1           | 6883.85    |           |
      | 102000000   | 20210402    | 4        | 0              | 1           | 9373.43    |           |
      | 102000000   | 20210501    | 5        | 0              | 1           |            | 6637.69   |
      | 103000000   | 20210501    | 5        | 0              | 1           |            | 16637.69  |
      | 103000000   | 20210301    | 3        | 0              | 1           |            | 20522.17  |
      | 103000000   | 20210401    | 4        | 0              | 1           | 6883.85    |           |
      | 103000000   | 20210401    | 4        | 0              | 2           | 39373.43   |           |
      | 103000000   | 20210601    | 6        | 0              | 1           |            | 9619.59   |
      | 103000000   | 20210701    | 7        | 0              | 1           | 1465.6     |           |
      | 201000000   | 20210101    | 0        | 0              | 1           |            | 20522.17  |
      | 201000000   | 20210401    | 4        | 0              | 1           | 6883.85    |           |
      | 201000000   | 20210402    | 4        | 0              | 1           | 9373.43    | 6637.69   |
      | 201000000   | 20210501    | 5        | 0              | 1           | 10902.58   |           |
    Wenn Tagessummen verarbeitet werden
    Dann werden folgende Tageswerte ermittelt
      | Kontonummer | Buchungstag | Bereichsnummer | Buchungstyp | SummeHaben | SummeSoll |
      | 101000000   | 20210301    | 0              |             |            | 20522.17  |
      | 101000000   | 20210401    | 0              | 2           | 6883.85    |           |
      | 101000000   | 20210501    | 0              | 2           | -6883.85   | -20522.17 |
      | 102000000   | 20210301    | 0              |             |            | 20522.17  |
      | 102000000   | 20210401    | 0              |             | 6883.85    |           |
      | 102000000   | 20210402    | 0              |             | 9373.43    |           |
      | 102000000   | 20210501    | 0              |             |            | 6637.69   |
      | 102000000   | 20210601    | 0              |             |            | 9619.59   |
      | 102000000   | 20210701    | 0              |             | 16465.6    |           |
      | 103000000   | 20210301    | 0              |             |            | 20522.17  |
      | 103000000   | 20210401    | 0              |             | 6883.85    |           |
      | 103000000   | 20210401    | 0              | 2           | 39373.43   |           |
      | 103000000   | 20210501    | 0              |             |            | 16637.69  |
      | 103000000   | 20210601    | 0              |             |            | 9619.59   |
      | 103000000   | 20210701    | 0              |             | 1465.6     |           |
      | 201000000   | EB          | 0              |             |            | 20522.17  |
      | 201000000   | 20210401    | 0              |             | 6883.85    |           |
      | 201000000   | 20210402    | 0              |             | 9373.43    | 6637.69   |
      | 201000000   | 20210501    | 0              |             | 10902.58   |           |
    Und werden folgende Monatswerte ermittelt
      | Kontonummer | WJ-Monat | Bereichsnummer | Buchungstyp | SummeHaben | SummeSoll |
      | 101000000   | 3        | 0              |             |            | 20522.17  |
      | 101000000   | 4        | 0              | 2           | 6883.85    |           |
      | 101000000   | 5        | 0              | 2           | -6883.85   | -20522.17 |
      | 102000000   | 3        | 0              |             |            | 20522.17  |
      | 102000000   | 4        | 0              |             | 16257.28   |           |
      | 102000000   | 5        | 0              |             |            | 6637.69   |
      | 102000000   | 6        | 0              |             |            | 9619.59   |
      | 102000000   | 7        | 0              |             | 16465.6    |           |
      | 103000000   | 3        | 0              |             |            | 20522.17  |
      | 103000000   | 4        | 0              |             | 6883.85    |           |
      | 103000000   | 4        | 0              | 2           | 39373.43   |           |
      | 103000000   | 5        | 0              |             |            | 16637.69  |
      | 103000000   | 6        | 0              |             |            | 9619.59   |
      | 103000000   | 7        | 0              |             | 1465.6     |           |
      | 201000000   | EB       | 0              |             |            | 20522.17  |
      | 201000000   | 4        | 0              |             | 16257.28   | 6637.69   |
      | 201000000   | 5        | 0              |             | 10902.58   |           |
    Und werden folgende Tageswerte der Personenkontengruppen ermittelt
      | Kontogruppennummer | Buchungstag | Bereichsnummer | Buchungstyp | SummeHabenTypisch | SummeHabenUntypisch | SummeSollTypisch | SummeSollUntypisch |
      | 1                  | 20210301    | 0              |             |                   |                     | 41044.34         |                    |
      | 1                  | 20210401    | 0              |             | 6883.85           | 6883.85             | -20522.17        | 20522.17           |
      | 1                  | 20210401    | 0              | 2           |                   | 39373.43            |                  |                    |
      | 1                  | 20210402    | 0              |             | 9373.43           |                     |                  |                    |
      | 1                  | 20210501    | 0              |             |                   |                     | 6637.69          | 16637.69           |
      | 1                  | 20210601    | 0              |             | 6883.85           | -6883.85            | 56399.04         | -37159.86          |
      | 1                  | 20210601    | 0              | 2           | 39373.43          | -39373.43           |                  |                    |
      | 1                  | 20210701    | 0              |             | 9581.75           | 8349.45             | -46779.45        | 46779.45           |
      | 1                  | 20210701    | 0              | 2           | -39373.43         | 39373.43            |                  |                    |
      | 2                  | EB          | 0              |             |                   |                     | 20522.17         |                    |
      | 2                  | 20210401    | 0              |             | 6883.85           |                     |                  |                    |
      | 2                  | 20210402    | 0              |             | 9373.43           |                     | 6637.69          |                    |
      | 2                  | 20210501    | 0              |             | 10902.58          |                     |                  |                    |
    Und werden folgende Monatswerte der Personenkontengruppen ermittelt
      # Werte aus ACDS RefSys Test
      | Kontogruppennummer | WJ-Monat | Bereichsnummer | Buchungstyp | SummeHabenTypisch | SummeHabenUntypisch | SummeSollTypisch | SummeSollUntypisch |
      | 1                  | 3        | 0              |             |                   |                     | 41044.34         |                    |
      | 1                  | 4        | 0              |             | 16257.28          | 6883.85             | -20522.17        | 20522.17           |
      | 1                  | 4        | 0              | 2           |                   | 39373.43            |                  |                    |
      | 1                  | 5        | 0              |             |                   |                     | 6637.69          | 16637.69           |
      | 1                  | 6        | 0              |             | 6883.85           | -6883.85            | 56399.04         | -37159.86          |
      | 1                  | 6        | 0              | 2           | 39373.43          | -39373.43           |                  |                    |
      | 1                  | 7        | 0              |             | 9581.75           | 8349.45             | -46779.45        | 46779.45           |
      | 1                  | 7        | 0              | 2           | -39373.43         | 39373.43            |                  |                    |
      | 2                  | EB       | 0              |             |                   |                     | 20522.17         |                    |
      | 2                  | 4        | 0              |             | 16257.28          |                     | 6637.69          |                    |
      | 2                  | 5        | 0              |             | 10902.58          |                     |                  |                    |
    Und folgende Personenkonten werden individuel betrachtet
      | Kontonummer |
      | 101000000   |

  # Verschiedene Debitoren zusammenaddiert.
  # Basiert auf den vorherigen Tests und zeigt nur die finiale Addition verschiedenen Debitoren.
  @RunMe
  Szenario: Mehrere Personenkonto Kreditoren inkl. Generalumkehr wo Buchungstag nicht sortiert ist und es einen zweistelligen Monat gibt
    Angenommen folgende Tagessummen
      | Kontonummer | Buchungstag | WJ-Monat | Bereichsnummer | Buchungstyp | SummeHaben | SummeSoll |
      | 101000000   | 20210401    | 4        | 0              | 2           | 6883.85    |           |
      | 101000000   | 20210501    | 5        | 0              | 2           | -6883.85   | -20522.17 |
      | 101000000   | 20210301    | 3        | 0              | 1           |            | 20522.17  |
      | 102000000   | 20210301    | 3        | 0              | 1           |            | 20522.17  |
      | 102000000   | 20210601    | 6        | 0              | 1           |            | 9619.59   |
      | 102000000   | 20211101    | 11       | 0              | 1           | 16465.6    |           |
      | 102000000   | 20210401    | 4        | 0              | 1           | 6883.85    |           |
      | 102000000   | 20210402    | 4        | 0              | 1           | 9373.43    |           |
      | 102000000   | 20210501    | 5        | 0              | 1           |            | 6637.69   |
      | 103000000   | 20210501    | 5        | 0              | 1           |            | 16637.69  |
      | 103000000   | 20210301    | 3        | 0              | 1           |            | 20522.17  |
      | 103000000   | 20210401    | 4        | 0              | 1           | 6883.85    |           |
      | 103000000   | 20210401    | 4        | 0              | 2           | 39373.43   |           |
      | 103000000   | 20210601    | 6        | 0              | 1           |            | 9619.59   |
      | 103000000   | 20211101    | 11       | 0              | 1           | 1465.6     |           |
      | 201000000   | 20210101    | 0        | 0              | 1           |            | 20522.17  |
      | 201000000   | 20210401    | 4        | 0              | 1           | 6883.85    |           |
      | 201000000   | 20210402    | 4        | 0              | 1           | 9373.43    | 6637.69   |
      | 201000000   | 20210501    | 5        | 0              | 1           | 10902.58   |           |
    Wenn Tagessummen verarbeitet werden
    Dann werden folgende Tageswerte ermittelt
      | Kontonummer | Buchungstag | Bereichsnummer | Buchungstyp | SummeHaben | SummeSoll |
      | 101000000   | 20210301    | 0              |             |            | 20522.17  |
      | 101000000   | 20210401    | 0              | 2           | 6883.85    |           |
      | 101000000   | 20210501    | 0              | 2           | -6883.85   | -20522.17 |
      | 102000000   | 20210301    | 0              |             |            | 20522.17  |
      | 102000000   | 20210401    | 0              |             | 6883.85    |           |
      | 102000000   | 20210402    | 0              |             | 9373.43    |           |
      | 102000000   | 20210501    | 0              |             |            | 6637.69   |
      | 102000000   | 20210601    | 0              |             |            | 9619.59   |
      | 102000000   | 20211101    | 0              |             | 16465.6    |           |
      | 103000000   | 20210301    | 0              |             |            | 20522.17  |
      | 103000000   | 20210401    | 0              |             | 6883.85    |           |
      | 103000000   | 20210401    | 0              | 2           | 39373.43   |           |
      | 103000000   | 20210501    | 0              |             |            | 16637.69  |
      | 103000000   | 20210601    | 0              |             |            | 9619.59   |
      | 103000000   | 20211101    | 0              |             | 1465.6     |           |
      | 201000000   | EB          | 0              |             |            | 20522.17  |
      | 201000000   | 20210401    | 0              |             | 6883.85    |           |
      | 201000000   | 20210402    | 0              |             | 9373.43    | 6637.69   |
      | 201000000   | 20210501    | 0              |             | 10902.58   |           |
    Und werden folgende Monatswerte ermittelt
      | Kontonummer | WJ-Monat | Bereichsnummer | Buchungstyp | SummeHaben | SummeSoll |
      | 101000000   | 3        | 0              |             |            | 20522.17  |
      | 101000000   | 4        | 0              | 2           | 6883.85    |           |
      | 101000000   | 5        | 0              | 2           | -6883.85   | -20522.17 |
      | 102000000   | 3        | 0              |             |            | 20522.17  |
      | 102000000   | 4        | 0              |             | 16257.28   |           |
      | 102000000   | 5        | 0              |             |            | 6637.69   |
      | 102000000   | 6        | 0              |             |            | 9619.59   |
      | 102000000   | 11       | 0              |             | 16465.6    |           |
      | 103000000   | 3        | 0              |             |            | 20522.17  |
      | 103000000   | 4        | 0              |             | 6883.85    |           |
      | 103000000   | 4        | 0              | 2           | 39373.43   |           |
      | 103000000   | 5        | 0              |             |            | 16637.69  |
      | 103000000   | 6        | 0              |             |            | 9619.59   |
      | 103000000   | 11       | 0              |             | 1465.6     |           |
      | 201000000   | EB       | 0              |             |            | 20522.17  |
      | 201000000   | 4        | 0              |             | 16257.28   | 6637.69   |
      | 201000000   | 5        | 0              |             | 10902.58   |           |
    Und werden folgende Tageswerte der Personenkontengruppen ermittelt
      | Kontogruppennummer | Buchungstag | Bereichsnummer | Buchungstyp | SummeHabenTypisch | SummeHabenUntypisch | SummeSollTypisch | SummeSollUntypisch |
      | 1                  | 20210301    | 0              |             |                   |                     | 41044.34         |                    |
      | 1                  | 20210401    | 0              |             | 6883.85           | 6883.85             | -20522.17        | 20522.17           |
      | 1                  | 20210401    | 0              | 2           |                   | 39373.43            |                  |                    |
      | 1                  | 20210402    | 0              |             | 9373.43           |                     |                  |                    |
      | 1                  | 20210501    | 0              |             |                   |                     | 6637.69          | 16637.69           |
      | 1                  | 20210601    | 0              |             | 6883.85           | -6883.85            | 56399.04         | -37159.86          |
      | 1                  | 20210601    | 0              | 2           | 39373.43          | -39373.43           |                  |                    |
      | 1                  | 20211101    | 0              |             | 9581.75           | 8349.45             | -46779.45        | 46779.45           |
      | 1                  | 20211101    | 0              | 2           | -39373.43         | 39373.43            |                  |                    |
      | 2                  | EB          | 0              |             |                   |                     | 20522.17         |                    |
      | 2                  | 20210401    | 0              |             | 6883.85           |                     |                  |                    |
      | 2                  | 20210402    | 0              |             | 9373.43           |                     | 6637.69          |                    |
      | 2                  | 20210501    | 0              |             | 10902.58          |                     |                  |                    |
    Und werden folgende Monatswerte der Personenkontengruppen ermittelt
      # Werte aus ACDS RefSys Test
      | Kontogruppennummer | WJ-Monat | Bereichsnummer | Buchungstyp | SummeHabenTypisch | SummeHabenUntypisch | SummeSollTypisch | SummeSollUntypisch |
      | 1                  | 3        | 0              |             |                   |                     | 41044.34         |                    |
      | 1                  | 4        | 0              |             | 16257.28          | 6883.85             | -20522.17        | 20522.17           |
      | 1                  | 4        | 0              | 2           |                   | 39373.43            |                  |                    |
      | 1                  | 5        | 0              |             |                   |                     | 6637.69          | 16637.69           |
      | 1                  | 6        | 0              |             | 6883.85           | -6883.85            | 56399.04         | -37159.86          |
      | 1                  | 6        | 0              | 2           | 39373.43          | -39373.43           |                  |                    |
      | 1                  | 11       | 0              |             | 9581.75           | 8349.45             | -46779.45        | 46779.45           |
      | 1                  | 11       | 0              | 2           | -39373.43         | 39373.43            |                  |                    |
      | 2                  | EB       | 0              |             |                   |                     | 20522.17         |                    |
      | 2                  | 4        | 0              |             | 16257.28          |                     | 6637.69          |                    |
      | 2                  | 5        | 0              |             | 10902.58          |                     |                  |                    |
    Und folgende Personenkonten werden individuel betrachtet
      | Kontonummer |
      | 101000000   |