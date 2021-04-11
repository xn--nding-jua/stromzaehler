/*
 * Copyright (C) 2014 Daniel Hirscher
 */

package de.phoenixstudios.stromzaehler;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;

// CSOFF: SuppressWarnings

public class MeterActivity extends Activity
{
   private AsyncTask<Void, AppData, Void> task;

   @Override
   public void onCreate(final Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.meter);
   }

   @Override
   protected void onResume()
   {
      super.onResume();
      task = new MeterTCPUpdaterTask(this).execute();
   }

   @Override
   protected void onPause()
   {
      if (null != task)
      {
         task.cancel(false);
      }
      super.onPause();
   }

   public void resetMeter(@SuppressWarnings("unused") final View view)
   {
      ((MeterTCPUpdaterTask) task).reset();
   }

   public void historyMeter(@SuppressWarnings("unused") final View view)
   {
      ((MeterTCPUpdaterTask) task).showHistory(2);
   }

   public void longHistoryMeter(@SuppressWarnings("unused") final View view)
   {
      ((MeterTCPUpdaterTask) task).showHistory(3);
   }

   public void switchXYMeter(@SuppressWarnings("unused") final View view)
   {
      ((MeterTCPUpdaterTask) task).switchXY(this);
   }
}
