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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
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
            int energy_180_lasthour = SML_2_Ethernet.SML2EthernetAppData.history_value_180_hour[0];
            int energy_280_lasthour = SML_2_Ethernet.SML2EthernetAppData.history_value_280_hour[0];
            
            // add the values of last hour for 180 and 280 to longhistory and save it to file
            SML_2_Ethernet.SML2EthernetLongHistory.add(energy_180_lasthour, energy_280_lasthour);
            SML_2_Ethernet.SaveLongHistoryData(SML_2_Ethernet.SML2EthernetLongHistory, System.getProperty("user.dir") + "/longhistory.gz");
            
            // upload data to ELK
            SML_2_Ethernet.UpdateELKSensors(energy_180_lasthour, energy_280_lasthour);

            // shift history-data by 1 hour, set value_x80_lasthour to current value and save ring-buffer to file
            SML_2_Ethernet.UpdateHourlyAppDataHistory();
            SML_2_Ethernet.SaveHistoryData(SML_2_Ethernet.SML2EthernetAppData, System.getProperty("user.dir") + "/history.gz"); // save history to file so that we are loosing only 1 hour on reboot
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
    public static LongHistory SML2EthernetLongHistory;
    static PowerController myPowerController;
    static cConfig config;

    static int value_180_yesterday = 0;
    static int value_280_yesterday = 0;
    static int value_180_lasthour = 0;
    static int value_280_lasthour = 0;

    public static void main(String... args) {
        System.out.println("SML_2_Ethernet Version 1.4.0 vom 10.04.2021");

        // create power-controller
        myPowerController = new PowerController();
        
        // load the config-file
        LoadConfigFile();
        
        // create data-classes for communcation
        SML2EthernetAppData = LoadOrCreateHistoryData(System.getProperty("user.dir") + "/history.gz");
        SML2EthernetLongHistory = LoadOrCreateLongHistoryData(System.getProperty("user.dir") + "/longhistory.gz");
        
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
            if (!jsonconfig.has("Controller_DesiredPower")){jsonconfig.put("Controller_DesiredPower", -50.0);}
            if (!jsonconfig.has("Controller_Normalization")){jsonconfig.put("Controller_Normalization", 600.0);}
            if (!jsonconfig.has("Controller_Kp")){jsonconfig.put("Controller_Kp", 0.15);}
            if (!jsonconfig.has("Controller_Ki")){jsonconfig.put("Controller_Ki", 0.1);}
            if (!jsonconfig.has("Controller_Kd")){jsonconfig.put("Controller_Kd", 0.05);}
            if (!jsonconfig.has("Controller_Ta")){jsonconfig.put("Controller_Ta", 1.0);}
            if (!jsonconfig.has("Controller_emax")){jsonconfig.put("Controller_emax", 2.0);}
            if (!jsonconfig.has("Controller_emin")){jsonconfig.put("Controller_emin", 0.0);}
            
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
            myPowerController.emin=(float)jsonconfig.getDouble("Controller_emin");

            System.out.println("Config: RS232-Port     = " + config.RS232Port);
            System.out.println("Config: RS232-Baudrate = " + Integer.toString(config.RS232Baudrate));
            System.out.println("Config: Server-Port    = " + Integer.toString(config.ServerPort));
            System.out.println("Config: Ctrl_DsrdPwr   = " + Float.toString(myPowerController.power_desired));
            System.out.println("Config: Ctrl_Norml     = " + Float.toString(myPowerController.normalization));
            System.out.println("Config: Ctrl_Kp        = " + Float.toString(myPowerController.kp));
            System.out.println("Config: Ctrl_Ki        = " + Float.toString(myPowerController.ki));
            System.out.println("Config: Ctrl_Kd        = " + Float.toString(myPowerController.kd));
            System.out.println("Config: Ctrl_Ta        = " + Float.toString(myPowerController.Ta));
            System.out.println("Config: Ctrl_emax/emin = " + Float.toString(myPowerController.emax) + "/" + Float.toString(myPowerController.emin));
            
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
    
    static void SaveHistoryData(AppData History, String Filename) {
        byte[] HistoryArray = History.toByteBuffer(2, null).array(); // on HistoryLevel=2 we do not need to give LongHistory as parameter
        // compress the data
        HistoryArray=Compression.CompressByteArray(HistoryArray, false);

        try (FileOutputStream fos = new FileOutputStream(Filename)) {
           fos.write(HistoryArray);
           //fos.close(); There is no more need for this line since you had created the instance of "fos" inside the try. And this will automatically close the OutputStream
        }catch(IOException error){
            System.out.println(error.toString());
        }
    }
    
    static AppData LoadOrCreateHistoryData(String Filename) {
        // load history from GZIP-file
        System.out.print("Try to load history file " + Filename + "...");

        AppData History = new AppData();

        try{
            File historyfile = new File(Filename);
            if (historyfile.exists()){
                byte[] HistoryArray;

                try (FileInputStream fis = new FileInputStream(historyfile)) {
                   HistoryArray = new byte[fis.available()];
                   fis.read(HistoryArray);
                   //fos.close(); There is no more need for this line since you had created the instance of "fos" inside the try. And this will automatically close the OutputStream
                }
                System.out.print("Read " + HistoryArray.length/1024 + "kB...");
                
                HistoryArray=Compression.DecompressByteArray(HistoryArray, false);
                System.out.print("Decompressed " + HistoryArray.length/1024/1024 + "MB...");

                History.fromByteBuffer(ByteBuffer.wrap(HistoryArray), 2, null); // on HistoryLevel=2 we do not need to give LongHistory as parameter
                System.out.println(" Done.");
            }else{
                System.out.println(" No history-file found.");
            }
        }catch(IOException error){
            System.out.println(error.toString());
        }
        
        return History;
    }

    static void SaveLongHistoryData(LongHistory History, String Filename){
        byte[] LongHistoryArray = History.toByteBuffer().array();
        // compress the data
        LongHistoryArray=Compression.CompressByteArray(LongHistoryArray, false);

        try (FileOutputStream fos = new FileOutputStream(Filename)) {
           fos.write(LongHistoryArray);
           //fos.close(); There is no more need for this line since you had created the instance of "fos" inside the try. And this will automatically close the OutputStream
        }catch(IOException error){
            System.out.println(error.toString());
        }
    }
    
    static LongHistory LoadOrCreateLongHistoryData(String Filename){
        // load history from GZIP-file
        System.out.print("Try to load long-history file " + Filename + "...");

        LongHistory History = new LongHistory();
        try{
            File longhistoryfile = new File(Filename);
            if (longhistoryfile.exists()){
                byte[] HistoryArray;

                try (FileInputStream fis = new FileInputStream(longhistoryfile)) {
                   HistoryArray = new byte[fis.available()];
                   fis.read(HistoryArray);
                   //fos.close(); There is no more need for this line since you had created the instance of "fos" inside the try. And this will automatically close the OutputStream
                }
                System.out.print("Read " + HistoryArray.length/1024 + "kB...");
                HistoryArray=Compression.DecompressByteArray(HistoryArray, false);
                System.out.print("Decompressed " + HistoryArray.length/1024/1024 + "MB...");
                History.fromByteBuffer(ByteBuffer.wrap(HistoryArray));
                System.out.println(" Done.");
            }else{
                System.out.println(" No long-history-file found.");
            }
        }catch(IOException error){
            System.out.println(error.toString());
        }
        
        return History;
    }
    
    static void UpdateELKSensors(int value_180, int value_280){
        // Upload energy-data of the last hour to ELK
        //HelperFunctions.CallHttp("http://192.168.0.24/post_sensor_data.php?index=power&id=k4-meter-180&value="+Integer.toString(value_180));
        //HelperFunctions.CallHttp("http://192.168.0.24/post_sensor_data.php?index=power&id=k4-meter-280&value="+Integer.toString(value_280));
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
        value_180_lasthour=SML2EthernetAppData.value_180;
        value_280_lasthour=SML2EthernetAppData.value_280;
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
                                                
                                                // update internal history
                                                UpdateAppDataPowerHistory(SML2EthernetAppData.power);
                                                
                                                // calculate the power-controller e.g. for the ESP8266 WiFi-PV-Load-device
                                                myPowerController.Calculate(SML2EthernetAppData.power);
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
            String ReceivedData;
            boolean KeepThreadAlive=true;
            
            //System.out.println("ClientThread gestartet.");
            
            try
            {
                // mit offenem Socket verbinden
                BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
                ObjectOutputStream outToClient = new ObjectOutputStream(connectionSocket.getOutputStream());
                BufferedWriter outToClient2 = new BufferedWriter(new OutputStreamWriter(connectionSocket.getOutputStream()));

                // Meldung im Systemlog
                //System.out.println("IP="+connectionSocket.getRemoteSocketAddress().toString()+" verbunden.");
                
                // Dauerschleife für Datenempfang (max. 30 Sekunden auf neue Daten warten...)
                while(KeepThreadAlive)
                {
                    // Daten empfangen
                    ReceivedData=inFromClient.readLine(); // Programm wartet hier, bis neue Daten eingehen (max. 30 Sekunden)

                    if ((ReceivedData!=null) && (ReceivedData.length()>0))
                    {
                        //System.out.println("Daten von Client empfangen.");

                        if (ReceivedData.contains("C:DATA")) {
                            // C:DATA=0, C:DATA=1, C:DATA=2, C:DATA=3
                            int HistoryLevel = Integer.parseInt(String.copyValueOf(ReceivedData.toCharArray(), ReceivedData.indexOf("=")+1, ReceivedData.length()-ReceivedData.indexOf("=")-1));

                            byte[] AppDataArray = SML2EthernetAppData.toByteBuffer(HistoryLevel, SML2EthernetLongHistory).array();
                            // compress the data and send GetAllTemperature-Data to client
                            AppDataArray=Compression.CompressByteArray(AppDataArray, false);

                            int ChunkSize=250000; // at the moment we can transmit everything within one single chunk... maybe on larger data it is nescessary
                            int NumberOfChunks=(int)Math.ceil((float)AppDataArray.length/(float)ChunkSize);
                            int ChunkPointer=0;
                            byte[] Chunk;
                            // Transmit in multiple chunks because temperature-data is quite huge
                            outToClient.writeInt(AppDataArray.length); // data-length
                            outToClient.writeInt(ChunkSize); // chunksize
                            for (int i=0; i<NumberOfChunks;i++) {
                                Chunk = Arrays.copyOfRange(AppDataArray, ChunkPointer, Math.min((ChunkPointer+ChunkSize), AppDataArray.length));
                                ChunkPointer=ChunkSize*(i+1);
                                outToClient.write(Chunk, 0, Chunk.length);
                            }
                            outToClient.flush();
                        }else if (ReceivedData.equals("C:POWERCONTROLLER?")) {
                            // send calculated output of controller back to client
                            // a conversion to DMX- or percent-values have to be done within the client
                            outToClient2.write(Float.toString(myPowerController.GetOutput()) + "\n");
                            outToClient2.flush();
                            
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
                        }else if (ReceivedData.equals("C:POWERCONTROLLER>RESET")) {
                            myPowerController.ResetController();
                            outToClient2.write("OK\n");
                            outToClient2.flush();
                        }else if (ReceivedData.equals("C:POWER?")) {
                            // return the current power-value
                            outToClient2.write(Integer.toString(SML2EthernetAppData.power) + "\n");
                            outToClient2.flush();
                        }else if (ReceivedData.equals("C:RELOADCONFIG")) {
                            // this command is useful to update the PowerController-parameters without rebooting.
                            // changes for UART and/or TCP will have no effect until a reboot of the application
                            LoadConfigFile();
                            outToClient2.write("OK\n");
                            outToClient2.flush();
                        }else{
                            // Unknown Command
                            outToClient2.write("?\n");
                            outToClient2.flush();
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