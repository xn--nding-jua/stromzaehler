/*
 * Copyright (C) 2014 Daniel Hirscher
 */

package de.hackerdan.sml;

import de.hackerdan.sml.consumers.TCPServer.TCPCMDServer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Stream;
import org.json.JSONException;
import org.json.JSONObject;

public final class Main
{
   private static Logger logger = LogManager.getLogger(Main.class);

   private Main()
   {
      // main class
   }

   public static void main(final String[] args)
   {
      if (logger.isInfoEnabled())
      {
         logger.info("SML tool started.");
      }

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
            if (!jsonconfig.has("SerialPort")){jsonconfig.put("SerialPort", "/dev/ttyUSB0");}
            if (!jsonconfig.has("Baudrate")){jsonconfig.put("Baudrate", 9600);}
            if (!jsonconfig.has("IPBroadcastAddress")){jsonconfig.put("IPBroadcastAddress", "192.168.0.255");}
            if (!jsonconfig.has("IPBroadcastPort")){jsonconfig.put("IPBroadcastPort", 51354);}
            if (!jsonconfig.has("WiringPiBinaryPath")){jsonconfig.put("WiringPiBinaryPath", "/usr/local/bin/gpio");}
            if (!jsonconfig.has("WiringPiLEDGPIO")){jsonconfig.put("WiringPiLEDGPIO", 0);}

            // copy JSON-config to config-class
            SmlConfig.getInstance().setSerialPort(jsonconfig.getString("SerialPort"));
            SmlConfig.getInstance().setBaudrate(jsonconfig.getInt("Baudrate"));
            SmlConfig.getInstance().setServerIPPort(jsonconfig.getInt("ServerIPPort"));
            SmlConfig.getInstance().setWiringPiBinaryPath(jsonconfig.getString("WiringPiBinaryPath"));
            SmlConfig.getInstance().setWiringPiLEDGPIO(jsonconfig.getInt("WiringPiLEDGPIO"));
            
            if (CreateConfigFile){
                // write new config to file
                BufferedWriter writer = new BufferedWriter(new FileWriter(System.getProperty("user.dir") + "/settings.json"));
                jsonconfig.write(writer);
                writer.close();
            }
        }catch(JSONException | IOException error){
            System.out.println(error.toString());
        }

      final Consumer consumer = new Consumer();
      final Producer producer = new Producer(consumer);

      new TCPCMDServer().start();
      
      producer.start();
   }
}
