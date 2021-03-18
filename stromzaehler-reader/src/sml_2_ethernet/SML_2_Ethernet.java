/*
 * Copyright 2021 Dr.-Ing. Christian Nöding
 *
 * This Java-Application reads an SML-based Meter via UART-connection
 * and transmit the data via a client-server-system via standard-network.
 *
 * For more information visit http://www.pcdimmer.de
 *
 * This software is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this application.  If not, see <http://www.gnu.org/licenses/>.
 *
 * This software uses parts of the OpenMUC-software copyright 2011-2020 Fraunhofer ISE.
 * For more information visit http://www.openmuc.org
 *
 */

package sml_2_ethernet;

import gnu.io.*;
import org.openmuc.jsml.structures.SmlFile;
import org.openmuc.jsml.GenericParser;
import org.openmuc.jsml.structures.EMessageBody;
import org.openmuc.jsml.structures.SmlListEntry;
import org.openmuc.jsml.structures.SmlMessage;
import org.openmuc.jsml.structures.responses.SmlGetListRes;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Stream;
import org.json.JSONException;
import org.json.JSONObject;

class DoHourTask extends TimerTask
{
    @Override public void run()
    {
        // Executed every hour
    }
}

class DoSecondTask extends TimerTask
{
    @Override public void run()
    {
        String CurrentTime=SML_2_Ethernet.dtf.format(LocalDateTime.now());
        
        // Reset at 12 a.m.
        if (CurrentTime.equals("00:00:00")) { // 15:34:41
            // Reset the yesterday-values
            SML_2_Ethernet.value_180_yesterday=SML_2_Ethernet.SML2EthernetAppData.value_180;
            SML_2_Ethernet.value_280_yesterday=SML_2_Ethernet.SML2EthernetAppData.value_280;
        }
        
        // Run Tasks at full hour
        if (CurrentTime.substring(3,8).equals("00:00")) { // 34:41
            SML_2_Ethernet.UpdateELKSensors();
            SML_2_Ethernet.UpdateAppDataHistory();
        }
    }
}

class cConfig{
    String RS232Port = "/dev/ttyAMA1";
    int RS232Baudrate = 9600;
    int ServerPort = 51534;
}

public class SML_2_Ethernet {
    static public DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss");

    public static AppData SML2EthernetAppData;
    public static byte[] SML2EthernetAppDataArray;
    static cConfig config;

    static float value_180_yesterday = 0.0f;
    static float value_280_yesterday = 0.0f;
    static float value_180_lasthour = 0.0f;
    static float value_280_lasthour = 0.0f;

    public static void main(String... args) {
        System.out.println("SML_2_Ethernet Version 1.1.0");

        // load configuration from XML-file
        System.out.println("Loading configuration file " + System.getProperty("user.dir") + "/settings.json" + "...");
        try{
            JSONObject jsonconfig;
            boolean CreateConfigFile=false;

            File configfile = new File(System.getProperty("user.dir") + "/settings.json");
            if (configfile.exists()){
                // load config-file and parse JSON-content
                StringBuilder contentBuilder = new StringBuilder();
                Stream<String> stream = Files.lines( Paths.get(System.getProperty("user.dir") + "/settings.json"), StandardCharsets.UTF_8);
                stream.forEach(s -> contentBuilder.append(s).append("\n"));
                jsonconfig = new JSONObject(contentBuilder.toString());
                stream.close();
            }else{
                System.out.println("Create new config-file...");
                CreateConfigFile=true;
                jsonconfig = new JSONObject();
            }
            
            // check if all config-entries are available. Otherwise create them
            if (!jsonconfig.has("RS232Port")){jsonconfig.put("RS232Port", "/dev/ttyUSB0");}
            if (!jsonconfig.has("RS232Baudrate")){jsonconfig.put("RS232Baudrate", 9600);}
            if (!jsonconfig.has("ServerPort")){jsonconfig.put("ServerPort", 51534);}
            
            // copy JSON-config to config-class
            config = new cConfig();
            config.RS232Port=jsonconfig.getString("RS232Port");
            config.RS232Baudrate=jsonconfig.getInt("RS232Baudrate");
            config.ServerPort=jsonconfig.getInt("ServerPort");

            System.out.println("Config: RS232-Port     = " + config.RS232Port);
            System.out.println("Config: RS232-Baudrate = " + Integer.toString(config.RS232Baudrate));
            System.out.println("Config: Server-Port    = " + Integer.toString(config.ServerPort));
            
            if (CreateConfigFile){
                // write new config to file
                BufferedWriter writer = new BufferedWriter(new FileWriter(System.getProperty("user.dir") + "/settings.json"));
                jsonconfig.write(writer);
                writer.close();
            }
        }catch(JSONException | IOException error){
            System.out.println(error.toString());
        }
        
        // create data-classes for communcation
        SML2EthernetAppData = new AppData();
        
        // Timer starten
        Timer timer_hour = new Timer();
        Timer timer_second = new Timer();
        timer_hour.schedule( new DoHourTask(), /*Delay*/0, /*Interval*/1000*60*60); // Alle 60 Minuten aufrufen
        timer_second.schedule( new DoSecondTask(), /*Delay*/0, /*Interval*/1000); // Jede Sekunde aufrufen

        // TCP-Server starten
        new TCPCMDServer().start();
        
        // SML-Reader via RS232 starten
        new SMLReader().start();
    }

    static void UpdateELKSensors(){
        /*
            // Available data
            SML2EthernetAppData.value_180
            SML2EthernetAppData.value_180_day
            SML2EthernetAppData.value_280
            SML2EthernetAppData.value_280_day

            SML2EthernetAppData.Values[0].value_180_hour
            SML2EthernetAppData.Values[0].value_280_hour
            SML2EthernetAppData.Values[0].power_total
            SML2EthernetAppData.Values[0].power_phase1
            SML2EthernetAppData.Values[0].power_phase2
            SML2EthernetAppData.Values[0].power_phase3
            SML2EthernetAppData.Values[0].current_phase1
            SML2EthernetAppData.Values[0].current_phase2
            SML2EthernetAppData.Values[0].current_phase3
            SML2EthernetAppData.Values[0].voltage_phase1
            SML2EthernetAppData.Values[0].voltage_phase2
            SML2EthernetAppData.Values[0].voltage_phase3
            SML2EthernetAppData.Values[0].temperature

            // Sensordata that is useful to store in ELK
            SML2EthernetAppData.Values[0].value_180_hour // energy that has been drawn in the last hour
            SML2EthernetAppData.Values[0].value_280_hour // energy that has been fed in in the last hour
            SML2EthernetAppData.Values[0].power_phase1 // power in phase 1
            SML2EthernetAppData.Values[0].power_phase2 // power in phase 2
            SML2EthernetAppData.Values[0].power_phase3 // power in phase 3

        */

        // TODO
        /*
        HelperFunctions.CallHttp("http://192.168.0.24/post_sensor_data.php?index=TYPBEZEICHNUNG&id=NAME&value="+Float.toString(SML2EthernetAppData.Values[0].value_180_hour));
        HelperFunctions.CallHttp("http://192.168.0.24/post_sensor_data.php?index=TYPBEZEICHNUNG&id=NAME&value="+Float.toString(SML2EthernetAppData.Values[0].value_280_hour));
        HelperFunctions.CallHttp("http://192.168.0.24/post_sensor_data.php?index=TYPBEZEICHNUNG&id=NAME&value="+Float.toString(SML2EthernetAppData.Values[0].power_phase1));
        HelperFunctions.CallHttp("http://192.168.0.24/post_sensor_data.php?index=TYPBEZEICHNUNG&id=NAME&value="+Float.toString(SML2EthernetAppData.Values[0].power_phase2));
        HelperFunctions.CallHttp("http://192.168.0.24/post_sensor_data.php?index=TYPBEZEICHNUNG&id=NAME&value="+Float.toString(SML2EthernetAppData.Values[0].power_phase3));
        */
    }
    
    static void UpdateAppDataHistory(){
        // shift all array-data one element to the left
        for (int i=SML2EthernetAppData.Values.length-1; i>0; i--){
            SML2EthernetAppData.Values[i].value_180_hour=SML2EthernetAppData.Values[i-1].value_180_hour;
            SML2EthernetAppData.Values[i].value_280_hour=SML2EthernetAppData.Values[i-1].value_280_hour;
            SML2EthernetAppData.Values[i].power_total=SML2EthernetAppData.Values[i-1].power_total; // for graphs, but without any useful information as this is only a single spot value
            SML2EthernetAppData.Values[i].energy_phase1=SML2EthernetAppData.Values[i-1].energy_phase1; // Energy are the integrated second-values of the power in W: so we have 3600 second-values summed-up as unit Wh
            SML2EthernetAppData.Values[i].energy_phase2=SML2EthernetAppData.Values[i-1].energy_phase2; // Energy are the integrated second-values of the power in W: so we have 3600 second-values summed-up as unit Wh
            SML2EthernetAppData.Values[i].energy_phase3=SML2EthernetAppData.Values[i-1].energy_phase3; // Energy are the integrated second-values of the power in W: so we have 3600 second-values summed-up as unit Wh
            SML2EthernetAppData.Values[i].current_phase1=SML2EthernetAppData.Values[i-1].current_phase1; // for graphs, but without any useful information as this is only a single spot value
            SML2EthernetAppData.Values[i].current_phase2=SML2EthernetAppData.Values[i-1].current_phase2; // for graphs, but without any useful information as this is only a single spot value
            SML2EthernetAppData.Values[i].current_phase3=SML2EthernetAppData.Values[i-1].current_phase3; // for graphs, but without any useful information as this is only a single spot value
            SML2EthernetAppData.Values[i].voltage_phase1=SML2EthernetAppData.Values[i-1].voltage_phase1; // for graphs, but without any useful information as this is only a single spot value
            SML2EthernetAppData.Values[i].voltage_phase2=SML2EthernetAppData.Values[i-1].voltage_phase2; // for graphs, but without any useful information as this is only a single spot value
            SML2EthernetAppData.Values[i].voltage_phase3=SML2EthernetAppData.Values[i-1].voltage_phase3; // for graphs, but without any useful information as this is only a single spot value
            SML2EthernetAppData.Values[i].temperature=SML2EthernetAppData.Values[i-1].temperature;
        }

        // reset the energy-integrators for all three phases
        SML2EthernetAppData.Values[0].energy_phase1=0.0f;
        SML2EthernetAppData.Values[0].energy_phase2=0.0f;
        SML2EthernetAppData.Values[0].energy_phase3=0.0f;

        // write current values of 1.8.0 and 2.8.0 to the value_x80_lasthour
        SML_2_Ethernet.value_180_lasthour=SML_2_Ethernet.SML2EthernetAppData.value_180;
        SML_2_Ethernet.value_280_lasthour=SML_2_Ethernet.SML2EthernetAppData.value_280;
    }
    
    private static class SMLReader extends Thread{
        @Override
        public void run(){
            try
            {
                CommPortIdentifier portId = CommPortIdentifier.getPortIdentifier(config.RS232Port);
                SerialPort serialPort = (SerialPort) portId.open("SML_2_Ethernet", 2000);
                serialPort.setSerialPortParams(config.RS232Baudrate, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
                DataInputStream is = new DataInputStream(new BufferedInputStream(serialPort.getInputStream()));

                System.out.println("Opened uart " + config.RS232Port + " successfully. Now entering SML-reading-loop...");
                
                while(true)
                {
                    try
                    {
                       SmlFile smlFile = new SmlFile();
                       SmlMessage msg = new SmlMessage();
                       msg.decode(is);
                       //msg.decodeAndCheck(is);

                       EMessageBody tag = msg.getMessageBody().getTag();
                       if (tag!=null) {
                            switch (tag) {
                                case GET_LIST_RESPONSE:
                                    //smlFile.add(msg);
                                    //GenericParser.printFile(smlFile);

                                    /*
                                    GET_LIST_RESPONSE
                                    Found Data from ServerId: 0A014C475A00028C3257
                                    Received 4ListEntries
                                    01 00 60 32 01 01
                                    01 00 60 01 00 FF
                                    01 00 01 08 00 FF
                                    01 00 02 08 00 FF
                                    CLOSE_RESPONSE
                                    Got CloseResponse
                                    SML_PublicCloseRes{
                                    globalSignature:   not set
                                    }
                                    */

									/*
									// mögliche Mitteilungen
									01 00 24 07 00 FF: Wirkleistung L1
									01 00 38 07 00 FF: Wirkleistung L2
									01 00 4C 07 00 FF: Wirkleistung L3
									01 00 60 32 00 02: Aktuelle Chiptemperatur
									01 00 60 32 00 03: Minimale Chiptemperatur
									01 00 60 32 00 04: Maximale Chiptemperatur
									01 00 60 32 00 05: Gemittelte Chiptemperatur
									01 00 60 32 03 03: Spannungsminimum
									01 00 60 32 03 04: Spannungsmaximum
									01 00 1F 07 00 FF: Strom L1
									01 00 20 07 00 FF: Spannung L1
									01 00 33 07 00 FF: Strom L2
									01 00 34 07 00 FF: Spannung L2
									01 00 47 07 00 FF: Strom L3
									01 00 48 07 00 FF: Spannung L3
									*/
                                    
                                    // infos about SML-protocol here: http://www.stefan-weigert.de/php_loader/sml.php
                                    
                                    try{
                                        SmlGetListRes getListResult = (SmlGetListRes) msg.getMessageBody().getChoice();
                                        String serverId = HelperFunctions.convertBytesToHexString(getListResult.getServerId().getValue());
                                        SmlListEntry[] ListEntries = getListResult.getValList().getValListEntry();
                                        //System.out.println("Found Data from ServerId: " + serverId);
                                        //System.out.println("Received " + Integer.toString(ListEntries.length) + " ListEntries");
                                        for (SmlListEntry entry : ListEntries) {
                                            if (entry.getObjName().toString().equals("01 00 01 08 00 FF")) {
                                                // Energiebezugsdaten
                                                HelperFunctions.ValueContainer value = HelperFunctions.extractValueOf(entry);
                                                SML2EthernetAppData.value_180=value.value.asFloat();

                                                if (value_180_lasthour==0) value_180_lasthour=SML2EthernetAppData.value_180;

                                                SML2EthernetAppData.Values[0].value_180_hour=SML2EthernetAppData.value_180-value_180_lasthour;
                                                SML2EthernetAppData.value_180_day=SML2EthernetAppData.value_180-value_180_yesterday;
                                                //System.out.println("Bezogene Energie:     " + value.value.asString() + " " + entry.getUnit().toString());
                                            }else if (entry.getObjName().toString().equals("01 00 02 08 00 FF")) {
                                                // Energieeinspeisedaten
                                                HelperFunctions.ValueContainer value = HelperFunctions.extractValueOf(entry);
                                                SML2EthernetAppData.value_280=value.value.asFloat();

                                                if (value_280_lasthour==0) value_280_lasthour=SML2EthernetAppData.value_280;

                                                SML2EthernetAppData.Values[0].value_280_hour=SML2EthernetAppData.value_280-value_280_lasthour;
                                                SML2EthernetAppData.value_280_day=SML2EthernetAppData.value_280-value_280_yesterday;
                                                //System.out.println("Eingespeiste Energie: " + value.value.asString() + " " + entry.getUnit().toString());
                                            }else if (entry.getObjName().toString().equals("01 00 10 07 00 FF")) {
                                                // momentane Gesamtwirkleistung
                                                HelperFunctions.ValueContainer value = HelperFunctions.extractValueOf(entry);
                                                SML2EthernetAppData.Values[0].power_total=value.value.asFloat();
                                                //System.out.println("Mom. Ges.P: " + value.value.asString() + " " + entry.getUnit().toString());
                                            }else if (entry.getObjName().toString().equals("01 00 24 07 00 FF")) {
                                                // Wirkleistung Phase L1
                                                HelperFunctions.ValueContainer value = HelperFunctions.extractValueOf(entry);
                                                SML2EthernetAppData.power_phase1=value.value.asFloat();
                                                SML2EthernetAppData.Values[0].energy_phase1+=SML2EthernetAppData.power_phase1; // integrating the power to energy
                                                //System.out.println("P_L1: " + value.value.asString() + " " + entry.getUnit().toString());
                                            }else if (entry.getObjName().toString().equals("01 00 38 07 00 FF")) {
                                                // Wirkleistung Phase L2
                                                HelperFunctions.ValueContainer value = HelperFunctions.extractValueOf(entry);
                                                SML2EthernetAppData.power_phase2=value.value.asFloat();
                                                SML2EthernetAppData.Values[0].energy_phase2+=SML2EthernetAppData.power_phase2; // integrating the power to energy
                                                //System.out.println("P_L2: " + value.value.asString() + " " + entry.getUnit().toString());
                                            }else if (entry.getObjName().toString().equals("01 00 4C 07 00 FF")) {
                                                // Wirkleistung Phase L3
                                                HelperFunctions.ValueContainer value = HelperFunctions.extractValueOf(entry);
                                                SML2EthernetAppData.power_phase3=value.value.asFloat();
                                                SML2EthernetAppData.Values[0].energy_phase3+=SML2EthernetAppData.power_phase3; // integrating the power to energy
                                                //System.out.println("P_L3: " + value.value.asString() + " " + entry.getUnit().toString());
                                            }else if (entry.getObjName().toString().equals("01 00 1F 07 00 FF")) {
                                                // Strom Phase L1
                                                HelperFunctions.ValueContainer value = HelperFunctions.extractValueOf(entry);
                                                SML2EthernetAppData.Values[0].current_phase1=value.value.asFloat();
                                            }else if (entry.getObjName().toString().equals("01 00 33 07 00 FF")) {
                                                // Strom Phase L2
                                                HelperFunctions.ValueContainer value = HelperFunctions.extractValueOf(entry);
                                                SML2EthernetAppData.Values[0].current_phase2=value.value.asFloat();
                                            }else if (entry.getObjName().toString().equals("01 00 47 07 00 FF")) {
                                                // Strom Phase L3
                                                HelperFunctions.ValueContainer value = HelperFunctions.extractValueOf(entry);
                                                SML2EthernetAppData.Values[0].current_phase3=value.value.asFloat();
                                            }else if (entry.getObjName().toString().equals("01 00 20 07 00 FF")) {
                                                // Spannung Phase L1
                                                HelperFunctions.ValueContainer value = HelperFunctions.extractValueOf(entry);
                                                SML2EthernetAppData.Values[0].voltage_phase1=value.value.asFloat();
                                            }else if (entry.getObjName().toString().equals("01 00 34 07 00 FF")) {
                                                // Spannung Phase L2
                                                HelperFunctions.ValueContainer value = HelperFunctions.extractValueOf(entry);
                                                SML2EthernetAppData.Values[0].voltage_phase2=value.value.asFloat();
                                            }else if (entry.getObjName().toString().equals("01 00 48 07 00 FF")) {
                                                // Spannung Phase L3
                                                HelperFunctions.ValueContainer value = HelperFunctions.extractValueOf(entry);
                                                SML2EthernetAppData.Values[0].voltage_phase3=value.value.asFloat();
                                            }else if (entry.getObjName().toString().equals("01 00 60 32 00 02")) {
                                                // Chiptemperatur
                                                HelperFunctions.ValueContainer value = HelperFunctions.extractValueOf(entry);
                                                SML2EthernetAppData.Values[0].temperature=value.value.asFloat();
                                            }else{
                                                // unbekannte oder weitere Daten
                                                HelperFunctions.ValueContainer value = HelperFunctions.extractValueOf(entry);
                                                //System.out.println(entry.getObjName().toString() + ": " + value.value.asString() + " " + entry.getUnit().toString());
                                            }
                                        }
                                    }catch(Exception e){
                                        System.out.println(e.toString());
                                    }
                                    break;
                                case OPEN_RESPONSE:
                                    // wird jede Sekunde ausgeführt
                                    /*
                                    Got OpenResponse
                                    SML_PublicOpenRes{
                                    codepage:   not set
                                    clientId:   FF FF FF FF FF FF
                                    reqFileId:  00 17 3C 17
                                    serverId:   0A 01 4C 47 5A 00 02 8C 32 57
                                    refTime:    SECINDEX value: 1523939
                                    smlVersion: 0
                                     */
                                    //System.out.println("Tic");
                                    //smlFile.add(msg);
                                    //GenericParser.printFile(smlFile);
                                    break;
                                case CLOSE_RESPONSE:
                                    /*
                                    Got CloseResponse
                                    SML_PublicCloseRes{
                                    globalSignature:   not set
                                    }
                                    */
                                    //smlFile.add(msg);
                                    //GenericParser.printFile(smlFile);
                                    break;
                                default:
                                    System.out.println(tag.toString());
                                    smlFile.add(msg);
                                    GenericParser.printFile(smlFile);
                                    break;
                            }
                       }
                    }catch (NegativeArraySizeException e){
                        // do nothing
                    }catch (final Exception e)
                    {
                       System.out.println("Error getting SML file. Retrying. Error was " + e);
                    }
                }
            }catch(NoSuchPortException | PortInUseException e)
            {
                System.out.println("The desired port " + config.RS232Port + " is not available or already in use...");
            }catch(UnsupportedCommOperationException e)
            {
                System.out.println("The settings for port " + config.RS232Port + " are not valid...");
            }catch(IOException e){
                System.out.println("Error on opening " + config.RS232Port);
            }
        }
    }
    
    private static class TCPCMDServer extends Thread{
        @Override
        public void run(){
            ServerSocket CMDSocket;
            Socket connectionSocket=null;

            try
            {
                // Socket etablieren
                System.out.println("Neuen Socket für TCPCMDServer etablieren...");
                CMDSocket = new ServerSocket(config.ServerPort); // neuen Server-Socket anlegen
                //CMDSocket.setSoTimeout(10000); // Gibt Timeout, wenn nicht innerhalb der Zeit eine Verbindung hergestellt wird -> macht aber hier keinen Sinn, da wir unendlich auf neue Verbindungen warten wollen

                while(true)
                {
                    try
                    {
                        //System.out.println("Neuer Socket etabliert. Warte auf eingehende Verbindung...");
                        connectionSocket = CMDSocket.accept(); // Eingehende Verbindungen akzeptieren (Thread wartet hier so lange, bis eine neue Verbindung eingeht!!!)
                        connectionSocket.setSoTimeout(10000); // 30 Sekunden Timeout fürs Datenlesen setzen
                    } catch (IOException e) {
                        System.out.println("I/O error: " + e);
                    }
                    // start new thread for the new client
                    //System.out.println("Eingehende Verbindung erkannt -> Starte ClientThread...");
                    new ClientThread(connectionSocket).start();
                }
            }catch (IOException error){
                System.out.println(error.toString());
                System.out.println("Schwerwiegender Netzwerkfehler! TCP-Kommandoserver wurde beendet. Manueller Eingriff erforderlich!");
            }
        }
    }

    public static class ClientThread extends Thread {
        protected Socket connectionSocket;

        public ClientThread(Socket clientSocket) {
            this.connectionSocket = clientSocket;
        }

        @Override
        public void run() {
            int ReceiveLength;
            char[] Buffer;
            String ReceivedData;
            String Answer;
            boolean KeepThreadAlive=true;
            
            //System.out.println("ClientThread gestartet.");
            
            try
            {
                // mit offenem Socket verbinden
                BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
                ObjectOutputStream outToClient = new ObjectOutputStream(connectionSocket.getOutputStream());

                // Meldung im Systemlog
                //System.out.println("IP="+connectionSocket.getRemoteSocketAddress().toString()+" verbunden.");
                
                // Dauerschleife für Datenempfang (max. 30 Sekunden auf neue Daten warten...)
                while(KeepThreadAlive)
                {
                    // Daten empfangen
                    ReceiveLength=inFromClient.read(); // Programm wartet hier, bis neue Daten eingehen (max. 30 Sekunden)

                    if (ReceiveLength>0)
                    {
                        //System.out.println("Daten von Client empfangen.");

                        Buffer = new char[ReceiveLength];
                        inFromClient.read(Buffer, 0, ReceiveLength);
                        ReceivedData = String.valueOf(Buffer, 0, ReceiveLength);

                        Answer = ProcessTCPCMD(ReceivedData);

                        switch (Answer) {
                            case "DATA":
                                // compress the data and send GetAllTemperature-Data to client
                                SML2EthernetAppDataArray=Compression.CompressByteArray(SML2EthernetAppDataArray, false);
                                
                                int ChunkSize=10000; // at the moment we can transmit everything within one single chunk... maybe on larger data it is nescessary
                                int NumberOfChunks=(int)Math.ceil((float)SML2EthernetAppDataArray.length/(float)ChunkSize);
                                int ChunkPointer=0;
                                byte[] Chunk;

                                // Transmit in multiple chunks because temperature-data is quite huge
                                outToClient.writeInt(SML2EthernetAppDataArray.length); // data-length
                                outToClient.writeInt(ChunkSize); // chunksize
                                for (int i=0; i<NumberOfChunks;i++) {
                                    Chunk = Arrays.copyOfRange(SML2EthernetAppDataArray, ChunkPointer, Math.min((ChunkPointer+ChunkSize), SML2EthernetAppDataArray.length));
                                    ChunkPointer=ChunkSize*(i+1);
                                    outToClient.write(Chunk, 0, Chunk.length);
                                }
                                outToClient.flush();
                                break;
                            default:
                                // unknown command. So send nothing
                                // send Data to client
                                outToClient.writeInt(0);
                                outToClient.flush();
                                break;
                        }

                        //System.out.println("Daten an Client gesendet.");
                    }else{
                        //System.out.println("IP="+connectionSocket.getRemoteSocketAddress().toString()+" getrennt.");
                        KeepThreadAlive=false; // Thread sauber beenden
                    }
                }
            }catch(IOException error)
            {
                System.out.println(error.toString());
            }
        }
    }    

    static String ProcessTCPCMD(String cmd){
        if (cmd.contains("C:DATA")) {
            // C:DATA=168
            int Elements=Integer.parseInt(String.copyValueOf(cmd.toCharArray(), cmd.indexOf("=")+1, cmd.length()-cmd.indexOf("=")-1));
            SML2EthernetAppDataArray = SML2EthernetAppData.toByteBuffer(Elements).array();
            return "DATA";
        }else{
            return "Unknown command!";
        }
    }
}