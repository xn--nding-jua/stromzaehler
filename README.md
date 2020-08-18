Diese App ist ein Fork von bodensee/stromzaehler von Daniel Hirscher.

Die App zeigt den aktuellen Strombezug bzw. die Einspeisung auf einem Android-Telefon an. Die Messung und das Datenmanagement wird über einen RaspberryPi mit Software von https://www.volkszaehler.org/ realisiert.

Da der alte Code für Eclipse geschrieben wurde, habe ich in diesem Fork die Android-App auf Android Studio 4 und den restlichen Code auf NetBeans 11 migriert.

Neben der Android-App (stromzaehler-app) gibt es zwei Java-Module (stromzaehler-converter und stromzaehler-model), sowie eine Java-Anwendung (stromzaehler-reader). Die beiden Java-Module werden sowohl innerhalb der Java-Anwendung, als auch der Android App verwendet und müssen zuerst kompiliert werden.
