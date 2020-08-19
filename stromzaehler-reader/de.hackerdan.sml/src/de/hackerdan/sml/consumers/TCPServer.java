/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.hackerdan.sml.consumers;

import de.hackerdan.sml.SmlConfig;
import de.hackerdan.sml.model.FullDataConverter;
import de.hackerdan.sml.model.PvValue;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 *
 * @author Dr.-Ing. Christian Nöding
 */
public class TCPServer {
    public static PvValue model;
    
    public static class TCPCMDServer extends Thread{
        @Override
        public void run(){
            ServerSocket CMDSocket;
            Socket connectionSocket=null;

            try
            {
                // Socket etablieren
                //System.out.println("Neuen Socket für TCPCMDServer etablieren...");
                CMDSocket = new ServerSocket(SmlConfig.getInstance().getServerIPPort()); // neuen Server-Socket anlegen
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

    private static class ClientThread extends Thread {
        protected Socket connectionSocket;

        public ClientThread(Socket clientSocket) {
            this.connectionSocket = clientSocket;
        }

        @Override
        public void run() {
            int ReceiveLength;
            char[] Buffer;
            boolean KeepThreadAlive=true;
            final FullDataConverter converter = new FullDataConverter();
            
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
                        System.out.println("Daten von Client empfangen.");

                        Buffer = new char[ReceiveLength];
                        inFromClient.read(Buffer, 0, ReceiveLength);
                        
                        // TODO: here different commands could be processed
                        //System.out.println(String.valueOf(Buffer, 0, ReceiveLength));

                        // at the moment just send the PvValue-model to client
                        // send Data to client
                        //System.out.println(model.getTimestamp());
                        //System.out.println(model.getCurrent());
                        outToClient.write(44); // length of model
                        outToClient.write(converter.convert(model)); // model itself
                        outToClient.flush();

                        System.out.println("Daten an Client gesendet.");
                    }else{
                        System.out.println("IP="+connectionSocket.getRemoteSocketAddress().toString()+" getrennt.");
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
