/*
 * Copyright (C) 2014 Daniel Hirscher
 */

package de.hackerdan.stromzaehler;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import de.hackerdan.sml.model.PvValue;

// CSOFF: SuppressWarnings

public class CurrentActivity extends Activity
{
   private AsyncTask<Void, PvValue, Void> task;

   @Override
   public void onCreate(final Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.main);
   }

   @Override
   protected void onResume()
   {
      super.onResume();
      ((TextView) findViewById(R.id.valueCurrent)).setText(null);
      findViewById(R.id.sun).setVisibility(View.INVISIBLE);
      findViewById(R.id.plant).setVisibility(View.INVISIBLE);
      ((ImageView) findViewById(R.id.imageCharge)).setImageResource(R.mipmap.charge_off);
      ((ImageView) findViewById(R.id.imageWama)).setImageResource(R.mipmap.wama_off);
      task = new CurrentTCPUpdaterTask(this).execute();
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

   public void openStats(@SuppressWarnings("unused") final View view)
   {
      final Intent intent = new Intent(this, StatsActivity.class);
      startActivity(intent);
   }

   public void openMeter(@SuppressWarnings("unused") final View view)
   {
      final Intent intent = new Intent(this, MeterActivity.class);
      startActivity(intent);
   }

   public void openSetup(final View view)
   {
      final Intent intent = new Intent(this, SettingsActivity.class);
      startActivity(intent);
   }

   @Override
   public boolean onCreateOptionsMenu(final Menu menu)
   {
      final MenuInflater inflater = getMenuInflater();
      inflater.inflate(R.menu.menu, menu);
      return true;
   }

   @Override
   public boolean onOptionsItemSelected(final MenuItem item)
   {
      final boolean result;
      if (R.id.menu_about == item.getItemId())
      {
         final Intent intent = new Intent(this, About.class);
         startActivity(intent);
         result = true;
      }
      else
      {
         result = super.onOptionsItemSelected(item);
      }
      return result;
   }
}
