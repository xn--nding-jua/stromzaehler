/*
 * Copyright (C) 2014 Daniel Hirscher
 */

package de.phoenixstudios.stromzaehler;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;

public class StatsActivity extends Activity
{
   private AsyncTask<Void, AppData, Void> task;

   @Override
   public void onCreate(final Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.stats);
   }

   @Override
   protected void onResume()
   {
      super.onResume();
      task = new StatsTCPUpdaterTask(this).execute();
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
      ((StatsTCPUpdaterTask) task).reset();
   }
}
