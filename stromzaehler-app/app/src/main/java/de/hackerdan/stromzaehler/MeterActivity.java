/*
 * Copyright (C) 2014 Daniel Hirscher
 */

package de.hackerdan.stromzaehler;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import de.hackerdan.sml.model.PvValue;

// CSOFF: SuppressWarnings

public class MeterActivity extends Activity
{
   private AsyncTask<Void, PvValue, Void> task;

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
}
