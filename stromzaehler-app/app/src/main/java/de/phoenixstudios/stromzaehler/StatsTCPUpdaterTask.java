/*
 * Copyright (C) 2014 Daniel Hirscher
 */

package de.phoenixstudios.stromzaehler;

import java.text.NumberFormat;

import android.app.Activity;
import android.widget.TextView;

/**
 * Display stats values.
 */
public class StatsTCPUpdaterTask extends TCPUpdaterTask
{
   private static final String W = " W";
   private static final String KWH = " kWh";
   private final NumberFormat nf1 = NumberFormat.getInstance();
   private final NumberFormat nf2 = NumberFormat.getInstance();

   private double base;
   private double min;
   private double max;
   private volatile boolean reset = true;

   public StatsTCPUpdaterTask(final Activity activity)
   {
      super(activity);
      nf1.setMinimumFractionDigits(1);
      nf2.setMinimumFractionDigits(3);
   }

   @Override
   protected void updateView(final AppData value, final Activity activity)
   {
      final double current = value.Values[0].power_total;

      final TextView textViewCurrent = (TextView) activity.findViewById(R.id.valueCurrent);
      textViewCurrent.setText(nf1.format(current) + " W");
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

      ((TextView) activity.findViewById(R.id.value180)).setText(nf2.format(value.value_180 / 1000.0) + KWH);
      ((TextView) activity.findViewById(R.id.value280)).setText(nf2.format(value.value_280 / 1000.0) + KWH);
      ((TextView) activity.findViewById(R.id.value180day)).setText(nf2.format(value.value_180_day / 1000.0) + KWH);
      ((TextView) activity.findViewById(R.id.value280day)).setText(nf2.format(value.value_280_day / 1000.0) + KWH);

      if (reset) {
         base = current;
         reset = false;
         min = 0;
         max = 0;
      }

      final double diff = current - base;
      min = Math.min(min, diff);
      max = Math.max(max, diff);

      ((TextView) activity.findViewById(R.id.valueDiff)).setText(nf1.format(diff) + W);
      ((TextView) activity.findViewById(R.id.valueMin)).setText(nf1.format(min) + W);
      ((TextView) activity.findViewById(R.id.valueMax)).setText(nf1.format(max) + W);
   }

   public void reset()
   {
      reset = true;
   }
}
