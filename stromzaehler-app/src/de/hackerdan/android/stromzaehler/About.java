/*
 * Copyright (C) 2014 Daniel Hirscher
 */

package de.hackerdan.android.stromzaehler;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class About extends Activity
{
   @Override
   protected void onCreate(final Bundle savedInstanceState)
   {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.about);
   }

   public void goToHomepage(final View view)
   {
      if (view instanceof TextView)
      {
         final String urlString = ((TextView) view).getText().toString();
         final Intent intent = new Intent(Intent.ACTION_VIEW);
         intent.setData(Uri.parse(urlString));
         startActivity(intent);
      }
   }
}
