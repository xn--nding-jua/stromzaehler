/*
 * Copyright (C) 2014 Daniel Hirscher
 */

package de.hackerdan.android.stromzaehler;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import de.hackerdan.sml.model.PvValue;

public class StatsActivity extends Activity
{
   private AsyncTask<Void, PvValue, Void> task;

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
      task = new StatsBroadcastUpdaterTask(this).execute();
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
}
