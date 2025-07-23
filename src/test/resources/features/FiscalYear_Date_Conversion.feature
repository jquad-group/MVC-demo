#In diesen Szenarien wird die Auswertungssicht dargestellt.
#Ist bei einem Dann-Schritt "Exception-Meldung" angegeben so bedeutet das, dass die Buchung bereits während des Buchens hätte abgelehnt werden bzw. ein Hinweis hätte kommen müssen.
#Insbesondere zu besprechen: 2, 3,

#language: de
Funktionalität: Datum Wirtschaftsjahr Umwandlung

  @RunMe
  Szenario: Januar-Buchung in den Februar-Stapel gebucht
    Angenommen folgender Datumssachverhalt
      | WJStart  | WJEnde   |
      | 20220101 | 20221231 |
    Wenn folgendes Datum in der Kontobewegung enthalten ist
      | Buchungsdatum | Stapel-Monat |
      | 20220102      | 2            |
    Dann muss aus Auswertungssicht folgende Datumskorrektur vorgenommen werden
      | Datum    | Monat |
      | 20220201 | 2     |

  @RunMe
  Szenario: Februar-Buchung in den Januar-Stapel gebucht
    Angenommen folgender Datumssachverhalt
      | WJStart  | WJEnde   |
      | 20220101 | 20221231 |
    Wenn folgendes Datum in der Kontobewegung enthalten ist
      | Buchungsdatum | Stapel-Monat |
      | 20220202      | 1            |
    Dann muss aus Auswertungssicht folgende Datumskorrektur vorgenommen werden
      | Datum    | Monat |
      | 20220131 | 1     |

  @RunMe
  Szenario: EB-Buchung wird vorgenommen
    Angenommen folgender Datumssachverhalt
      | WJStart  | WJEnde   |
      | 20220101 | 20221231 |
    Wenn folgendes Datum in der Kontobewegung enthalten ist
      | Buchungsdatum | Stapel-Monat |
      | 20220102      | 0            |
    Dann muss aus Auswertungssicht folgende Datumskorrektur vorgenommen werden
      | Datum | Monat |
      | EB    | EB    |

  @RunMe
  Szenario: Buchung vor WJ-Start ("normaler" Bestand mit einem WJ=12 Mte.)
    Angenommen folgender Datumssachverhalt
      | WJStart  | WJEnde   |
      | 20220101 | 20221231 |
    Wenn folgendes Datum in der Kontobewegung enthalten ist
      | Buchungsdatum | Stapel-Monat |
      | 20211215      | 1            |
    Dann muss aus Auswertungssicht folgende Datumskorrektur vorgenommen werden
      | Datum    | Monat |
      | 20220101 | 1     |

  @RunMe
  Szenario: Buchung nach WJ-Ende ("normaler" Bestand mit einem WJ=12 Mte.)
    Angenommen folgender Datumssachverhalt
      | WJStart  | WJEnde   |
      | 20220101 | 20221231 |
    Wenn folgendes Datum in der Kontobewegung enthalten ist
      | Buchungsdatum | Stapel-Monat |
      | 20230115      | 12           |
    Dann muss aus Auswertungssicht folgende Datumskorrektur vorgenommen werden
      | Datum    | Monat |
      | 20221231 | 12    |

  @RunMe
  Szenario: Buchung vor WJ-Start (Bestand mit einem Rumpf-WJ<12 Mte.), Datum 20220115
    Angenommen folgender Datumssachverhalt
      | WJStart  | WJEnde   |
      | 20220601 | 20221231 |
    Wenn folgendes Datum in der Kontobewegung enthalten ist
      | Buchungsdatum | Stapel-Monat |
      | 20220115      | 1            |
    Dann muss aus Auswertungssicht folgende Datumskorrektur vorgenommen werden
      | Datum    | Monat |
      | 20220601 | 1     |

  @RunMe
  Szenario: Buchung vor WJ-Start (Bestand mit einem Rumpf-WJ<12 Mte.), Datum 20220614
    Angenommen folgender Datumssachverhalt
      | WJStart  | WJEnde   |
      | 20220615 | 20221231 |
    Wenn folgendes Datum in der Kontobewegung enthalten ist
      | Buchungsdatum | Stapel-Monat |
      | 20220614      | 6            |
    Dann muss aus Auswertungssicht folgende Datumskorrektur vorgenommen werden
      | Datum    | Monat |
      | 20221101 | 6     |

  @RunMe
  Szenario: Buchung und Monat außerhalb des WJ (Bestand mit einem Rumpf-WJ<12 Mte.)
    Angenommen folgender Datumssachverhalt
      | WJStart  | WJEnde   |
      | 20220101 | 20220615 |
    Wenn folgendes Datum in der Kontobewegung enthalten ist
      | Buchungsdatum | Stapel-Monat |
      | 20221201      | 12           |
    Dann Stapel-Monat außerhalb des WJ Exception-Meldung

  @RunMe
  Szenario: Buchung nach WJ-Ende (Bestand mit einem Rumpf-WJ<12 Mte.), Datum 20220616
    Angenommen folgender Datumssachverhalt
      | WJStart  | WJEnde   |
      | 20220101 | 20220615 |
    Wenn folgendes Datum in der Kontobewegung enthalten ist
      | Buchungsdatum | Stapel-Monat |
      | 20220616      | 6            |
    Dann muss aus Auswertungssicht folgende Datumskorrektur vorgenommen werden
      | Datum    | Monat |
      | 20220615 | 6     |

  @RunMe
  Szenario: Buchung außerhalb (nach) des WJ (mehrere WJ in einem Bestand wg. Liquidationsschlussbilanz; nur mit Konsolidierung möglich)
    Angenommen folgender Datumssachverhalt
      | WJStart  | WJEnde   |
      | 20220101 | 20221231 |
    Wenn folgendes Datum in der Kontobewegung enthalten ist
      | Buchungsdatum | Stapel-Monat |
      | 20230115      | 1            |
    Dann muss aus Auswertungssicht folgende Datumskorrektur vorgenommen werden
      | Datum    | Monat |
      | 20220131 | 1     |

  @RunMe
  Szenario: 12: WJ 15.1. eines Jahres bis 14.1. des Folgejahres
    Angenommen folgender Datumssachverhalt
      | WJStart  | WJEnde   |
      | 20220115 | 20230114 |
    Wenn folgendes Datum in der Kontobewegung enthalten ist
      | Buchungsdatum | Stapel-Monat |
      | 20220115      | 2            |
    Dann muss aus Auswertungssicht folgende Datumskorrektur vorgenommen werden
      | Datum    | Monat |
      | 20220201 | 2     |

  @RunMe
  Szenario: Schaltjahr-Buchung in einem Nicht-Schaltjahr
    Angenommen folgender Datumssachverhalt
      | WJStart  | WJEnde   |
      | 20230101 | 20231231 |
    Wenn folgendes Datum in der Kontobewegung enthalten ist
      | Buchungsdatum | Stapel-Monat |
      | 20230229      | 2            |
    Dann Ungültiges Datumsformat Exception-Meldung

  @RunMe
  Szenario: Dezember-Buchung 2021 in den Januar-Stapel 2022 gebucht
    Angenommen folgender Datumssachverhalt
      | WJStart  | WJEnde   |
      | 20210115 | 20220114 |
    Wenn folgendes Datum in der Kontobewegung enthalten ist
      | Buchungsdatum | Stapel-Monat |
      | 20211220      | 13           |
    Dann muss aus Auswertungssicht folgende Datumskorrektur vorgenommen werden
      | Datum    | Monat |
      | 20220101 | 13    |

  @RunMe
  Szenario: Januar-Buchung 2022 in den Dezember-Stapel 2021 gebucht
    Angenommen folgender Datumssachverhalt
      | WJStart  | WJEnde   |
      | 20210115 | 20220114 |
    Wenn folgendes Datum in der Kontobewegung enthalten ist
      | Buchungsdatum | Stapel-Monat |
      | 20220103      | 12           |
    Dann muss aus Auswertungssicht folgende Datumskorrektur vorgenommen werden
      | Datum    | Monat |
      | 20211231 | 12    |

  @RunMe
  Szenario: Januar-Buchung 2021 in den Januar-Stapel 2022 gebucht
    Angenommen folgender Datumssachverhalt
      | WJStart  | WJEnde   |
      | 20210115 | 20220114 |
    Wenn folgendes Datum in der Kontobewegung enthalten ist
      | Buchungsdatum | Stapel-Monat |
      | 20210115      | 13           |
    Dann muss aus Auswertungssicht folgende Datumskorrektur vorgenommen werden
      | Datum    | Monat |
      | 20220101 | 13    |

  @RunMe
  Szenario: Stapel-Monat außerhalb des WJ
    Angenommen folgender Datumssachverhalt
      | WJStart  | WJEnde   |
      | 20210101 | 20210531 |
    Wenn folgendes Datum in der Kontobewegung enthalten ist
      | Buchungsdatum | Stapel-Monat |
      | 20210515      | 6            |
    Dann Stapel-Monat außerhalb des WJ Exception-Meldung

  @RunMe
  Szenario: Ungültiger Stapel-Monat Werte
    Angenommen folgender Datumssachverhalt
      | WJStart  | WJEnde   |
      | 20210101 | 20210531 |
    Wenn folgendes Datum in der Kontobewegung enthalten ist
      | Buchungsdatum | Stapel-Monat |
      | 20210515      | -1           |
    Dann Ungültiger Stapel-Monat Exception-Meldung

#Hinweis: Falls vorhanden Leistungsdatum, ansonsten Belegdatum