/*
 * Copyright (C) 2014 Daniel Hirscher
 */

package de.hackerdan.android.stromzaehler;

import java.text.NumberFormat;

import android.app.Activity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import de.hackerdan.sml.model.PvValue;

/**
 * Display current values.
 */
public class CurrentBroadcastUpdaterTask extends BroadcastUpdaterTask
{
   private final NumberFormat nf = NumberFormat.getInstance();

   public CurrentBroadcastUpdaterTask(final Activity activity)
   {
      super(activity);
   }

   @Override
   protected void updateView(final PvValue value, final Activity activity)
   {
      final int current = value.getCurrent() / 10;

      final TextView textViewCurrent = (TextView) activity.findViewById(R.id.valueCurrent);
      textViewCurrent.setText(nf.format(current) + " W");
      final int color;
      if (current > 0)
      {
         color = R.color.red;
      }
      else if (current < 0)
      {
         color = R.color.green;
      }
      else
      {
         color = R.color.black;
      }
      textViewCurrent.setTextColor(activity.getResources().getColor(color));

      activity.findViewById(R.id.sun).setVisibility(current < 0 ? View.VISIBLE : View.INVISIBLE);
      activity.findViewById(R.id.plant).setVisibility(current > 0 ? View.VISIBLE : View.INVISIBLE);

      switchImage(activity, current, -100, R.id.imageCharge, R.drawable.charge, R.drawable.charge_off);
      switchImage(activity, current, -1500, R.id.imageWama, R.drawable.wama, R.drawable.wama_off);
   }

   private static void switchImage(final Activity activity, final int current, final int minWatt, final int imageId,
         final int drawableOn, final int drawableOff)
   {
      final int newImage;
      if (current < minWatt)
      {
         newImage = drawableOn;
      }
      else
      {
         newImage = drawableOff;
      }
      ((ImageView) activity.findViewById(imageId)).setImageResource(newImage);
   }
}
