Diese App ist ein Fork von bodensee/stromzaehler von Daniel Hirscher.

Die Android-App zeigt den aktuellen Strombezug bzw. die Einspeisung, sowie andere Daten des Smartmeters als übersichtliche Graph-Darstellung auf einem Android-Gerät an. Die Messung und das Datenmanagement wird über einen RaspberryPi realisiert. Die notwendige Java-Anwendung ist ebenfalls in diesem Repository enthalten.

Folgende Andwendungen sind in diesem Repository enthalten:
1. Android-App (stromzaehler-app)
  - Anzeige der aktuellen Werte als Zahlenwerte, Live-Graph und als 7-Tage-Historie
  - Android 4
2. Java-Anwendung für RaspberryPi (stromzaehler-reader)
  - Java 1.8 Anwendung. Nutzt den IR-Schreib-/Lesekopf z.B. von Volkszaehler.org oder anderen Quellen via Built-in-UART
  - berechnet 7-Tage-Historie und dient als Netzwerkserver für Android-App und ESP8266
3. Anwendung für ESP8266 (stromzaehler-wifipvlast)
  - ESP8266-Anwendung ruft Funktion zum Berechnen eines PID-Reglers innerhalb des Java-Servers auf
  - Regler nimmt als Sollwert die gewünschte Leistung an, die in Richtung Stromnetz übertragen werden soll
  - Ausgang des Reglers steuert einen PWM-Ausgang am ESP8266 und über die sekundäre UART einen DMX512-Ausgang zum Ansteuern von Glühlampen oder Heizungen
4. Web-Anwendung (stromzaehler-phpclient)
  - über beliebige Webbrowser werden die wichtigsten Werte angezeigt
  - via Chartjs wird zudem die 7-Tage-History für 1.8.0 und 2.8.0 als Bargraph angezeigt:
![image](https://user-images.githubusercontent.com/9845353/114463354-9e8fb900-9be4-11eb-96b6-07cbc800753c.png)


Da der alte Code für Eclipse geschrieben wurde, habe ich in diesem Fork die Android-App auf Android Studio 4 und den restlichen Code auf NetBeans 11 migriert.

Weitere Informationen zur Anwendung gibt es hier: https://www.pcdimmer.de/index.php/heimautomation/smartmeter-auslesen
