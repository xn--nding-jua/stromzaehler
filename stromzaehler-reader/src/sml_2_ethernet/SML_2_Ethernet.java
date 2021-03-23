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
            SML_2_Ethernet.UpdateHourlyAppDataHistory();
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
    static PowerController myPowerController;
    public static byte[] SML2EthernetAppDataArray;
    static cConfig config;

    static int value_180_yesterday = 0;
    static int value_280_yesterday = 0;
    static int value_180_lasthour = 0;
    static int value_280_lasthour = 0;

    public static void main(String... args) {
        System.out.println("SML_2_Ethernet Version 1.3.0 vom 23.03.2021");

        // create power-controller
        myPowerController = new PowerController();

        // load the config-file
        LoadConfigFile();
        
        // create data-classes for communcation
        SML2EthernetAppData = new AppData();
        
        // start timers
        Timer timer_hour = new Timer();
        Timer timer_second = new Timer();
        timer_hour.schedule( new DoHourTask(), /*Delay*/0, /*Interval*/1000*60*60); // Alle 60 Minuten aufrufen
        timer_second.schedule( new DoSecondTask(), /*Delay*/0, /*Interval*/1000); // Jede Sekunde aufrufen

        // start TCP-server
        new TCPCMDServer().start();
        
        // start UART-SML-Reader
        new SMLReader().start();
    }

    static void LoadConfigFile() {
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
            if (!jsonconfig.has("Controller_DesiredPower")){jsonconfig.put("Controller_DesiredPower", -100.0);}
            if (!jsonconfig.has("Controller_Normalization")){jsonconfig.put("Controller_Normalization", 600.0);}
            if (!jsonconfig.has("Controller_Kp")){jsonconfig.put("Controller_Kp", 0.1);}
            if (!jsonconfig.has("Controller_Ki")){jsonconfig.put("Controller_Ki", 0.5);}
            if (!jsonconfig.has("Controller_Kd")){jsonconfig.put("Controller_Kd", 0.01);}
            if (!jsonconfig.has("Controller_Ta")){jsonconfig.put("Controller_Ta", 1.0);}
            if (!jsonconfig.has("Controller_emax")){jsonconfig.put("Controller_emax", 30.0);}
            
            // copy JSON-config to config-class
            config = new cConfig();
            config.RS232Port=jsonconfig.getString("RS232Port");
            config.RS232Baudrate=jsonconfig.getInt("RS232Baudrate");
            config.ServerPort=jsonconfig.getInt("ServerPort");
            myPowerController.power_desired=jsonconfig.getInt("Controller_DesiredPower");
            myPowerController.normalization=jsonconfig.getInt("Controller_Normalization");
            myPowerController.kp=(float)jsonconfig.getDouble("Controller_Kp");
            myPowerController.ki=(float)jsonconfig.getDouble("Controller_Ki");
            myPowerController.kd=(float)jsonconfig.getDouble("Controller_Kd");
            myPowerController.Ta=(float)jsonconfig.getDouble("Controller_Ta");
            myPowerController.emax=(float)jsonconfig.getDouble("Controller_emax");

            System.out.println("Config: RS232-Port     = " + config.RS232Port);
            System.out.println("Config: RS232-Baudrate = " + Integer.toString(config.RS232Baudrate));
            System.out.println("Config: Server-Port    = " + Integer.toString(config.ServerPort));
            System.out.println("Config: Ctrl_DsrdPwr   = " + Float.toString(myPowerController.power_desired));
            System.out.println("Config: Ctrl_Norml     = " + Float.toString(myPowerController.normalization));
            System.out.println("Config: Ctrl_Kp        = " + Float.toString(myPowerController.kp));
            System.out.println("Config: Ctrl_Ki        = " + Float.toString(myPowerController.ki));
            System.out.println("Config: Ctrl_Kd        = " + Float.toString(myPowerController.kd));
            System.out.println("Config: Ctrl_Ta        = " + Float.toString(myPowerController.Ta));
            System.out.println("Config: Ctrl_emax      = " + Float.toString(myPowerController.emax));
            
            if (CreateConfigFile){
                // write new config to file
                BufferedWriter writer = new BufferedWriter(new FileWriter(System.getProperty("user.dir") + "/settings.json"));
                jsonconfig.write(writer);
                writer.close();
            }
        }catch(JSONException | IOException error){
            System.out.println(error.toString());
        }
    }
    
    static void UpdateELKSensors(){
        /*
            // Available data
            SML2EthernetAppData.value_180
            SML2EthernetAppData.value_180_day
            SML2EthernetAppData.value_280
            SML2EthernetAppData.value_280_day
            SML2EthernetAppData.power

            // Available history-data
            SML2EthernetAppData.history_value_180_hour[168] // values for the last 7 days
            SML2EthernetAppData.history_value_280_hour[168] // values for the last 7 days
            SML2EthernetAppData.history_power_seconds[604800] // values for the last 7 days = 168h * 60min * 60s = 604800
        
        */

        // TODO: Upload data to ELK
        /*
        HelperFunctions.CallHttp("http://192.168.0.24/post_sensor_data.php?index=TYPBEZEICHNUNG&id=NAME&value="+Float.toString(SML2EthernetAppData.Values[0].value_180_hour));
        HelperFunctions.CallHttp("http://192.168.0.24/post_sensor_data.php?index=TYPBEZEICHNUNG&id=NAME&value="+Float.toString(SML2EthernetAppData.Values[0].value_280_hour));
        HelperFunctions.CallHttp("http://192.168.0.24/post_sensor_data.php?index=TYPBEZEICHNUNG&id=NAME&value="+Float.toString(SML2EthernetAppData.Values[0].energy_phase1));
        HelperFunctions.CallHttp("http://192.168.0.24/post_sensor_data.php?index=TYPBEZEICHNUNG&id=NAME&value="+Float.toString(SML2EthernetAppData.Values[0].energy_phase2));
        HelperFunctions.CallHttp("http://192.168.0.24/post_sensor_data.php?index=TYPBEZEICHNUNG&id=NAME&value="+Float.toString(SML2EthernetAppData.Values[0].energy_phase3));
        HelperFunctions.CallHttp("http://192.168.0.24/post_sensor_data.php?index=TYPBEZEICHNUNG&id=NAME&value="+Float.toString(SML2EthernetAppData.power_phase1));
        HelperFunctions.CallHttp("http://192.168.0.24/post_sensor_data.php?index=TYPBEZEICHNUNG&id=NAME&value="+Float.toString(SML2EthernetAppData.power_phase2));
        HelperFunctions.CallHttp("http://192.168.0.24/post_sensor_data.php?index=TYPBEZEICHNUNG&id=NAME&value="+Float.toString(SML2EthernetAppData.power_phase3));
        */
    }
    
    static void UpdateHourlyAppDataHistory(){
        // shift all array-data one element to the left
        for (int i=SML2EthernetAppData.history_value_180_hour.length-1; i>0; i--){
            SML2EthernetAppData.history_value_180_hour[i]=SML2EthernetAppData.history_value_180_hour[i-1];
        }
        for (int i=SML2EthernetAppData.history_value_280_hour.length-1; i>0; i--){
            SML2EthernetAppData.history_value_280_hour[i]=SML2EthernetAppData.history_value_280_hour[i-1];
        }

        // history_power_seconds will be shifted within UpdateAppDataPowerHistory
        
        // write current values of 1.8.0 and 2.8.0 to the value_x80_lasthour
        SML_2_Ethernet.value_180_lasthour=SML_2_Ethernet.SML2EthernetAppData.value_180;
        SML_2_Ethernet.value_280_lasthour=SML_2_Ethernet.SML2EthernetAppData.value_280;
    }

    static void UpdateAppDataPowerHistory(int Power){
        // shift array-data one element to the left
        for (int i=SML2EthernetAppData.history_power_seconds.length-1; i>0; i--){
            SML2EthernetAppData.history_power_seconds[i]=SML2EthernetAppData.history_power_seconds[i-1];
        }
        
        // write current valu of power to the first array-element
        SML2EthernetAppData.history_power_seconds[0]=Power;
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
                                                SML2EthernetAppData.value_180=value.value.asInt();

                                                if (value_180_lasthour==0) value_180_lasthour=SML2EthernetAppData.value_180;
                                                if (value_180_yesterday==0) value_180_yesterday=SML2EthernetAppData.value_180;

                                                SML2EthernetAppData.history_value_180_hour[0]=SML2EthernetAppData.value_180-value_180_lasthour;
                                                SML2EthernetAppData.value_180_day=SML2EthernetAppData.value_180-value_180_yesterday;
                                                //System.out.println("Bezogene Energie:     " + value.value.asString() + " " + entry.getUnit().toString());
                                            }else if (entry.getObjName().toString().equals("01 00 02 08 00 FF")) {
                                                // Energieeinspeisedaten
                                                HelperFunctions.ValueContainer value = HelperFunctions.extractValueOf(entry);
                                                SML2EthernetAppData.value_280=value.value.asInt();

                                                if (value_280_lasthour==0) value_280_lasthour=SML2EthernetAppData.value_280;
                                                if (value_280_yesterday==0) value_280_yesterday=SML2EthernetAppData.value_280;

                                                SML2EthernetAppData.history_value_280_hour[0]=SML2EthernetAppData.value_280-value_280_lasthour;
                                                SML2EthernetAppData.value_280_day=SML2EthernetAppData.value_280-value_280_yesterday;
                                                //System.out.println("Eingespeiste Energie: " + value.value.asString() + " " + entry.getUnit().toString());
                                            }else if (entry.getObjName().toString().equals("01 00 10 07 00 FF")) {
                                                // momentane Gesamtwirkleistung
                                                HelperFunctions.ValueContainer value = HelperFunctions.extractValueOf(entry);
                                                SML2EthernetAppData.power=value.value.asInt();
                                                UpdateAppDataPowerHistory(SML2EthernetAppData.power);
                                                //System.out.println("Mom. Ges.P: " + value.value.asString() + " " + entry.getUnit().toString());
                                            }else if (entry.getObjName().toString().equals("01 00 24 07 00 FF")) {
                                                // Wirkleistung Phase L1
                                                HelperFunctions.ValueContainer value = HelperFunctions.extractValueOf(entry);
                                                //SML2EthernetAppData.power_phase1=value.value.asFloat();
                                                //SML2EthernetAppData.Values[0].energy_phase1+=SML2EthernetAppData.power_phase1; // integrating the power to energy
                                                //System.out.println("P_L1: " + value.value.asString() + " " + entry.getUnit().toString());
                                            }else if (entry.getObjName().toString().equals("01 00 38 07 00 FF")) {
                                                // Wirkleistung Phase L2
                                                HelperFunctions.ValueContainer value = HelperFunctions.extractValueOf(entry);
                                                //SML2EthernetAppData.power_phase2=value.value.asFloat();
                                                //SML2EthernetAppData.Values[0].energy_phase2+=SML2EthernetAppData.power_phase2; // integrating the power to energy
                                                //System.out.println("P_L2: " + value.value.asString() + " " + entry.getUnit().toString());
                                            }else if (entry.getObjName().toString().equals("01 00 4C 07 00 FF")) {
                                                // Wirkleistung Phase L3
                                                HelperFunctions.ValueContainer value = HelperFunctions.extractValueOf(entry);
                                                //SML2EthernetAppData.power_phase3=value.value.asFloat();
                                                //SML2EthernetAppData.Values[0].energy_phase3+=SML2EthernetAppData.power_phase3; // integrating the power to energy
                                                //System.out.println("P_L3: " + value.value.asString() + " " + entry.getUnit().toString());
                                            }else if (entry.getObjName().toString().equals("01 00 1F 07 00 FF")) {
                                                // Strom Phase L1
                                                HelperFunctions.ValueContainer value = HelperFunctions.extractValueOf(entry);
                                                //SML2EthernetAppData.Values[0].current_phase1=value.value.asFloat();
                                                //System.out.println("I_L1: " + value.value.asString() + " " + entry.getUnit().toString());
                                            }else if (entry.getObjName().toString().equals("01 00 33 07 00 FF")) {
                                                // Strom Phase L2
                                                HelperFunctions.ValueContainer value = HelperFunctions.extractValueOf(entry);
                                                //SML2EthernetAppData.Values[0].current_phase2=value.value.asFloat();
                                                //System.out.println("I_L2: " + value.value.asString() + " " + entry.getUnit().toString());
                                            }else if (entry.getObjName().toString().equals("01 00 47 07 00 FF")) {
                                                // Strom Phase L3
                                                HelperFunctions.ValueContainer value = HelperFunctions.extractValueOf(entry);
                                                //SML2EthernetAppData.Values[0].current_phase3=value.value.asFloat();
                                                //System.out.println("I_L3: " + value.value.asString() + " " + entry.getUnit().toString());
                                            }else if (entry.getObjName().toString().equals("01 00 20 07 00 FF")) {
                                                // Spannung Phase L1
                                                HelperFunctions.ValueContainer value = HelperFunctions.extractValueOf(entry);
                                                //SML2EthernetAppData.Values[0].voltage_phase1=value.value.asFloat();
                                                //System.out.println("U_L1: " + value.value.asString() + " " + entry.getUnit().toString());
                                            }else if (entry.getObjName().toString().equals("01 00 34 07 00 FF")) {
                                                // Spannung Phase L2
                                                HelperFunctions.ValueContainer value = HelperFunctions.extractValueOf(entry);
                                                //SML2EthernetAppData.Values[0].voltage_phase2=value.value.asFloat();
                                                //System.out.println("U_L2: " + value.value.asString() + " " + entry.getUnit().toString());
                                            }else if (entry.getObjName().toString().equals("01 00 48 07 00 FF")) {
                                                // Spannung Phase L3
                                                HelperFunctions.ValueContainer value = HelperFunctions.extractValueOf(entry);
                                                //SML2EthernetAppData.Values[0].voltage_phase3=value.value.asFloat();
                                                //System.out.println("U_L3: " + value.value.asString() + " " + entry.getUnit().toString());
                                            }else if (entry.getObjName().toString().equals("01 00 60 32 00 02")) {
                                                // Chiptemperatur
                                                HelperFunctions.ValueContainer value = HelperFunctions.extractValueOf(entry);
                                                //SML2EthernetAppData.Values[0].temperature=value.value.asFloat();
                                                //System.out.println("Temperatur: " + value.value.asString() + " " + entry.getUnit().toString());
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

                        if (ReceivedData.contains("C:DATA")) {
                            // C:DATA=0, C:DATA=1, C:DATA=2
                            int HistoryLevel = Integer.parseInt(String.copyValueOf(ReceivedData.toCharArray(), ReceivedData.indexOf("=")+1, ReceivedData.length()-ReceivedData.indexOf("=")-1));
                            SML2EthernetAppDataArray = SML2EthernetAppData.toByteBuffer(HistoryLevel).array();
                            // compress the data and send GetAllTemperature-Data to client
                            SML2EthernetAppDataArray=Compression.CompressByteArray(SML2EthernetAppDataArray, false);

                            int ChunkSize=250000; // at the moment we can transmit everything within one single chunk... maybe on larger data it is nescessary
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
                        }else if (ReceivedData.equals("C:PWRCTRL_CALC")) {
                            // calculate the controller and send calculated output back to client
                            // a conversion to DMX- or percent-values have to be done within the client
                            outToClient.writeFloat(myPowerController.Calculate(SML2EthernetAppData.power));
                            outToClient.flush();
                            
                            /*
                            This controller will control a dynamic load for an optimal use of a small PV-system
                            so that no power will be fed into the public power-grid - if everything is working well.
                            
                            Connect an ohmic load to a DMX-dimmerpack or any other device that receives control-messages.
                            
                            The client should poll every second the calculation of the power-controller and this app
                            will return the calculated output of the controller. This output-value will increase, if
                            the feed-in-power is to high (power-value is below the desired value) or will decrease, if the
                            feed-in-power is to low (power-value is above the desired-value). You have to scale it to DMX-
                            values (0...255) or percentage (0...100) by your own within the client
                            */
                        }else if (ReceivedData.equals("C:PWRCTRL_RST")) {
                            myPowerController.ResetController();
                            outToClient.writeInt(1);
                            outToClient.flush();
                        }else if (ReceivedData.equals("C:PWR")) {
                            // return the current power-value
                            outToClient.writeInt(SML2EthernetAppData.power);
                            outToClient.flush();
                        }else if (ReceivedData.equals("C:LOADCFG")) {
                            // this command is useful to update the PowerController-parameters without rebooting.
                            // changes for UART and/or TCP will have no effect until a reboot of the application
                            LoadConfigFile();
                            outToClient.writeInt(1);
                            outToClient.flush();
                        }else{
                            // Unknown Command
                            outToClient.writeInt(0);
                            outToClient.flush();
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
}