/*
 * Copyright (C) 2014 Daniel Hirscher
 */

package de.hackerdan.stromzaehler;

import java.text.NumberFormat;

import android.app.Activity;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
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

      // show sun or plant depending on current power
      activity.findViewById(R.id.sun).setVisibility(current < 0 ? View.VISIBLE : View.INVISIBLE);
      activity.findViewById(R.id.plant).setVisibility(current > 0 ? View.VISIBLE : View.INVISIBLE);

      // move the energy bar depending on current power
      RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(50, 50);
      float Pmax=5000; // +100%
      float Pmin=-600; // -100%
      int leftMargin;
      if (current<0) {
         leftMargin = Math.round(175 - (175*(current/Pmin))); // 0=-100%, 175=0%, 350=100%
      }else{
         if (current<=Pmax) {
            leftMargin = Math.round(175 + (175 * (current / Pmax))); // 0=-100%, 175=0%, 350=100%
         }else{
            // end of bar
            leftMargin = 350;
         }
      }
      params.setMargins(leftMargin, -100, 0, 0);
      activity.findViewById(R.id.imageArrow).setLayoutParams(params);

      // show allowed loads
      switchImage(activity, current, -100, R.id.imageCharge, R.mipmap.charge, R.mipmap.charge_off);
      switchImage(activity, current, -1500, R.id.imageWama, R.mipmap.wama, R.mipmap.wama_off);
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
