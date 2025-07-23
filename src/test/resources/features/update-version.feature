#language: de
Funktionalität: State-Doc Versionsaktualisierung

  Hintergrund:
    Angenommen Berater 1234567 und Mandant 4711
    Und das Wirtschaftsjahr beginnt am 20210101 und endet am 20211231.

  @RunMe
  Szenario: Erfolgreiche Aktualisierung des Statedocs
    Angenommen es existiert ein StateDoc
    Und die Base Version im State Doc ist 1 und die Delta Version ist 0
    Wenn die update-version API mit Base Version 1 und Delta Version 1 aufgerufen wird
    Dann sollte der Rückgabewert 200 sein
    Und sollte das StateDoc mit Delta Version 1 erfolgreich aktualisiert sein

  @RunMe
  Szenario: Statedoc im Status INIT
    Angenommen es existiert ein StateDoc
    Und das StateDoc ist im Zustand 'INIT'
    Wenn die update-version API mit Base Version 1 und Delta Version 1 aufgerufen wird
    Dann sollte der Rückgabewert 409 sein
    Und sollte das StateDoc unverändert sein

  @RunMe
  Szenario: Statedoc im Status BAD
    Angenommen es existiert ein StateDoc
    Und das StateDoc ist im Zustand 'BAD'
    Wenn die update-version API mit Base Version 1 und Delta Version 1 aufgerufen wird
    Dann sollte der Rückgabewert 400 sein
    Und sollte das StateDoc unverändert sein

  @RunMe
  Szenario: Statedoc im Status INVALID_DATA
    Angenommen es existiert ein StateDoc
    Und das StateDoc ist im Zustand 'INVALID_DATA'
    Wenn die update-version API mit Base Version 1 und Delta Version 1 aufgerufen wird
    Dann sollte der Rückgabewert 400 sein
    Und sollte das StateDoc unverändert sein

  @RunMe
  Szenario: Statedoc im Status REINIT
    Angenommen es existiert ein StateDoc
    Und das StateDoc ist im Zustand 'REINIT'
    Wenn die update-version API mit Base Version 1 und Delta Version 1 aufgerufen wird
    Dann sollte der Rückgabewert 400 sein
    Und sollte das StateDoc unverändert sein

  @RunMe
  Szenario: Statedoc nicht vorhanden
    Angenommen es existiert kein StateDoc
    Wenn die update-version API mit Base Version 1 und Delta Version 1 aufgerufen wird
    Dann sollte der Rückgabewert 204 sein
    Und sollte kein StateDoc vorhanden sein

  @RunMe
  Szenario: Neuere Base Version vorhanden
    Angenommen es existiert ein StateDoc
    Und die Base Version im State Doc ist 2 und die Delta Version ist 0
    Wenn die update-version API mit Base Version 1 und Delta Version 1 aufgerufen wird
    Dann sollte der Rückgabewert 200 sein
    Und sollte das StateDoc unverändert sein

  @RunMe
  Szenario: Neuere Delta Version vorhanden
    Angenommen es existiert ein StateDoc
    Und die Base Version im State Doc ist 1 und die Delta Version ist 2
    Wenn die update-version API mit Base Version 1 und Delta Version 1 aufgerufen wird
    Dann sollte der Rückgabewert 200 sein
    Und sollte das StateDoc unverändert sein

  @RunMe
  Szenario: Neuere Delta Version vorhanden
    Angenommen es existiert ein StateDoc
    Und die Base Version im State Doc ist 1 und die Delta Version ist 0
    Wenn die update-version API mit Base Version 1 und Delta Version 2 aufgerufen wird
    Dann sollte der Rückgabewert 400 sein
    Und sollte das StateDoc unverändert sein