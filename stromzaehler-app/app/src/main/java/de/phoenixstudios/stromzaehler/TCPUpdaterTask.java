/*
 * Copyright (C) 2014 Daniel Hirscher
 */

package de.phoenixstudios.stromzaehler;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.util.Date;

import android.app.Activity;
import android.os.AsyncTask;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;

import androidx.constraintlayout.solver.widgets.Helper;
import de.phoenixstudios.stromzaehler.R;

/**
 * Requests the current values via TCP-communication and updates current view.
 */
public abstract class TCPUpdaterTask extends AsyncTask<Void, AppData, Void>
{
   private final DateFormat dateFormat = DateFormat.getTimeInstance();

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
      char[] myCharBuffer;
      byte[] myByteBuffer;
	  byte[] ValuesArray;
      int ReceiveLength;
      int ChunkSize;
      int ChunkPointer;
      int NumberOfChunks;
      int ReadBytes;
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

            // send command to server if command is available
            if (HelperFunctions.TCPCommandString.length()>0) {
                outToServer.write(HelperFunctions.TCPCommandString + "\n");
                outToServer.flush(); // force flushing the buffer to send immediatly

                // at the moment this app is only requesting the command C:DATA=0, ..., C:DATA=2
                // the SML_2_Ethernet-Java-Server will then reply binary-data instead of strings
                // so we have to read Integers and Byte-arrays
                // if you want to receive other data, you have to parse the returned strings

                // wait for response and read data
                if (inFromServer != null) {
                    ReceiveLength = inFromServer.readInt();
                } else {
                    ReceiveLength = 0;
                }

                // check if the data is OK and convert received bytes
                if (ReceiveLength > 0) {
                    if (HelperFunctions.TCPCommandString.contains("C:DATA")) {
                        ChunkSize = inFromServer.readInt();
                        if (ChunkSize > 0) {
                            // receive data-ByteArray and store it to ValuesArray
                            ValuesArray = new byte[ReceiveLength];
                            ChunkPointer = 0;
                            NumberOfChunks = (int) Math.ceil((float) ReceiveLength / (float) ChunkSize);
                            for (int i = 0; i < NumberOfChunks; i++) {
                                myByteBuffer = new byte[Math.min(ChunkSize, (ReceiveLength - ChunkPointer))]; // last chunk needs less space
                                inFromServer.readFully(myByteBuffer, 0, myByteBuffer.length);
                                System.arraycopy(myByteBuffer, 0, ValuesArray, ChunkPointer, myByteBuffer.length);
                                ChunkPointer = ChunkSize * (i + 1);
                            }

                            // Decompress data if desired
                            ValuesArray = Compression.DecompressByteArray(ValuesArray, false);
                            if (ValuesArray != null) {
                                AppData values = new AppData();
                                values.fromByteBuffer(ByteBuffer.wrap(ValuesArray));
                                publishProgress(values);
                            }
                        }
                    }
                }

                // set command to "nothing" if full data has been requested
                if (!HelperFunctions.TCPCommandString.equals("C:DATA=0")) {
                    HelperFunctions.TCPCommandString = "";
                }
            }
            try {
               Thread.sleep(1000); // wait 1 Second to reduce update-rate in GUI
            } catch (InterruptedException e){
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
   protected void onProgressUpdate(final AppData... values)
   {
      final AppData value = values[0];
      updateView(value, activity);

      Timestamp timestamp = new Timestamp(System.currentTimeMillis());
      ((TextView) activity.findViewById(R.id.timestamp)).setText(dateFormat.format(new Date(timestamp.getTime())));
   }

   protected abstract void updateView(AppData value, @SuppressWarnings("hiding") Activity activity);
}
