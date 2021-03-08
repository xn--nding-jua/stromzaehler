/*
 * Copyright (C) 2014 Daniel Hirscher
 */

package de.phoenixstudios.stromzaehler;

import java.text.NumberFormat;

import android.app.Activity;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * Display current values.
 */
public class CurrentTCPUpdaterTask extends TCPUpdaterTask
{
   private final NumberFormat nf = NumberFormat.getInstance();

   public CurrentTCPUpdaterTask(final Activity activity)
   {
      super(activity);
   }

   @Override
   protected void updateView(final AppData value, final Activity activity)
   {
      final int current = Math.round(value.Values[0].power_total); // Stromzähler liefert Kommastellen mit

      // *********************************************************************************
      // update the text-field and show/hide pictures
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


      // *********************************************************************************
      // move the energy bar depending on current power
      float Pmax=Float.parseFloat(PrefUtils.getFromPrefs(activity, "powerMax", "5000")); // +100%
      float Pmin=-Float.parseFloat(PrefUtils.getFromPrefs(activity, "powerMin", "600"));; // -100%
      ((TextView)activity.findViewById(R.id.text100Percent)).setText(Integer.toString(Math.round(Pmax))+"W");
      ((TextView)activity.findViewById(R.id.textMinus100Percent)).setText(Integer.toString(Math.round(Pmin))+"W");

      // get display-width
      //DisplayMetrics displaymetrics = activity.getResources().getDisplayMetrics();
      //float width = displaymetrics.density * displaymetrics.widthPixels;
      DisplayMetrics displaymetrics = new DisplayMetrics();
      activity.getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
      float screenWidth = 0.85f*displaymetrics.widthPixels;

      /*
      // handle landscape/portrait differently
      int orientation = activity.getResources().getConfiguration().orientation;
      if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
         // In landscape
      } else {
         // In portrait
      }
      */

      // calculate the position depending on the power and move arrow
      int leftMargin;
      if (current<0) {
         if (current>=Pmin) {
            leftMargin = Math.round((screenWidth / 2) - ((screenWidth / 2) * (current / Pmin))); // 0=-100%, (width/2)=0%, width=100%
         }else{
            // end of bar
            leftMargin = 0;
         }
      }else{
         if (current<=Pmax) {
            leftMargin = Math.round((screenWidth/2) + ((screenWidth/2) * (current / Pmax))); // 0=-100%, (width/2)=0%, width=100%
         }else{
            // end of bar
            leftMargin = Math.round(screenWidth);
         }
      }
      final ImageView arrow = (ImageView)activity.findViewById(R.id.imageArrow);
      RelativeLayout.LayoutParams arrow_lp = (RelativeLayout.LayoutParams)arrow.getLayoutParams();
      arrow_lp.setMargins(leftMargin, -300, 0, 0);
      arrow.setLayoutParams(arrow_lp);

      // show allowed loads
      switchImage(activity, current, -Integer.parseInt(PrefUtils.getFromPrefs(activity, "powerCharger", "100")), R.id.imageCharge, R.mipmap.charge, R.mipmap.charge_off);
      switchImage(activity, current, -Integer.parseInt(PrefUtils.getFromPrefs(activity, "powerWashingMachine", "300")), R.id.imageWama, R.mipmap.wama, R.mipmap.wama_off);
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
