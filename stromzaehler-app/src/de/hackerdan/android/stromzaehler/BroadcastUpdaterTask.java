/*
 * Copyright (C) 2014 Daniel Hirscher
 */

package de.hackerdan.android.stromzaehler;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.text.DateFormat;
import java.util.Date;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.widget.TextView;
import android.widget.Toast;
import de.hackerdan.sml.model.FullDataConverter;
import de.hackerdan.sml.model.PvValue;

/**
 * Updates the current values from UDP broadcast messages.
 */
public abstract class BroadcastUpdaterTask extends AsyncTask<Void, PvValue, Void>
{
   private final DateFormat dateFormat = DateFormat.getTimeInstance();
   private final FullDataConverter converter = new FullDataConverter();

   private final Activity activity;

   public BroadcastUpdaterTask(final Activity activity)
   {
      this.activity = activity;
   }

   @Override
   protected void onPreExecute()
   {
      final ConnectivityManager connManager = (ConnectivityManager) activity
            .getSystemService(Context.CONNECTIVITY_SERVICE);
      final NetworkInfo wifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

      if (!wifi.isConnected())
      {
         Toast.makeText(activity, "WLAN nicht verfügbar!", Toast.LENGTH_SHORT).show();
      }
   }

   @Override
   protected Void doInBackground(final Void... params)
   {
      DatagramSocket socket = null;
      do
      {
         try
         {
            socket = new DatagramSocket(51354);
            final DatagramPacket packet = new DatagramPacket(new byte[44], 44);

            do
            {
               try
               {
                  socket.receive(packet);
                  final PvValue value = converter.convert(packet.getData());
                  publishProgress(value);
               }
               catch (final IOException e) // NOPMD ignore and retry
               {
                  // simply retry
               }
            }
            while (!isCancelled());

         }
         catch (final IOException e) // NOPMD ignore and retry
         {
            // simply retry
         }
         finally
         {
            if (null != socket)
            {
               socket.close();
            }
         }
      }
      while (!isCancelled());

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
