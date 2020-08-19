/*
 * Copyright (C) 2014 Daniel Hirscher
 */

package de.hackerdan.stromzaehler;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.util.Date;

import android.app.Activity;
import android.os.AsyncTask;
import android.widget.TextView;
import de.hackerdan.sml.model.FullDataConverter;
import de.hackerdan.sml.model.PvValue;

/**
 * Requests the current values via TCP-communication and updates current view.
 */
public abstract class TCPUpdaterTask extends AsyncTask<Void, PvValue, Void>
{
   private final DateFormat dateFormat = DateFormat.getTimeInstance();
   private final FullDataConverter converter = new FullDataConverter();

   private String serverAddress;
   private String serverPort;

   private final Activity activity;

   public TCPUpdaterTask(final Activity activity)
   {
      this.activity = activity;
   }

   @Override
   protected void onPreExecute()
   {
      // load settings
      serverAddress = PrefUtils.getFromPrefs(this.activity, "serverAddress", "192.168.0.116");
      serverPort = PrefUtils.getFromPrefs(this.activity, "serverPort", "51354");
   }

   @Override
   protected Void doInBackground(final Void... params)
   {
      BufferedWriter outToServer=null;
      ObjectInputStream inFromServer=null;
      Socket TCPSocket=null;
      String TCPCommand;
      char[] myCharBuffer;
      byte[] myByteBuffer;
      int ReceiveLength;
      InetAddress serverAddr=null;

      while(!isCancelled()) {
         try{
            if (TCPSocket==null) {
               // (re)connect to server
               try{
                  serverAddr = InetAddress.getByName(serverAddress);
               }catch(UnknownHostException error){ }

               TCPSocket = new Socket(serverAddr, Integer.parseInt(serverPort));
               TCPSocket.setSoTimeout(3000); // 3 Sekunden Timeout

               outToServer = new BufferedWriter(new OutputStreamWriter(TCPSocket.getOutputStream()));
               inFromServer = new ObjectInputStream(TCPSocket.getInputStream());
            }

            TCPCommand = "Power?"; // at the moment we have only this single command. Can be extended in future.
            outToServer.write(TCPCommand.length());
            myCharBuffer = TCPCommand.toCharArray();
            outToServer.write(myCharBuffer, 0, TCPCommand.length());
            outToServer.flush(); // force flushing the buffer to send immediatly

            // wait for response and read data
            if (inFromServer != null) {
               ReceiveLength = inFromServer.read();
            }else{
               ReceiveLength = 0;
            }

            // check if the data is OK and convert received bytes
            if (ReceiveLength > 0) {
               myByteBuffer = new byte[ReceiveLength];
               inFromServer.read(myByteBuffer, 0, ReceiveLength);

               // we are expecting 44 byte of data. Otherwise we have corrupted data...
               if (ReceiveLength == 44) {
                  final PvValue value = converter.convert(myByteBuffer);
                  publishProgress(value);
               }
            }
         } catch (IOException error) {
            // close connection and try again
            try {
               if (TCPSocket!=null){
                  if (TCPSocket.isConnected()) {
                     inFromServer.close();
                     outToServer.close();
                     TCPSocket.close();
                  }
               }
               TCPSocket = null;
            }catch(Exception error2){ }
         }
      }

      if (TCPSocket != null) {
         try {
            inFromServer.close();
            outToServer.close();
            TCPSocket.close();
         } catch (IOException error) { }
      }
      return null;
   }

   @Override
   protected void onProgressUpdate(final PvValue... values)
   {
      final PvValue value = values[0];
      updateView(value, activity);

      ((TextView) activity.findViewById(R.id.timestamp)).setText(dateFormat.format(new Date(value.getTimestamp())));
   }

   protected abstract void updateView(PvValue value, @SuppressWarnings("hiding") Activity activity);
}
