/*
 * Copyright (C) 2014 Daniel Hirscher
 */

package de.hackerdan.stromzaehler;

import java.text.NumberFormat;

import android.app.Activity;
import android.widget.TextView;
import de.hackerdan.sml.model.PvValue;

/**
 * Display meter values.
 */
public class MeterBroadcastUpdaterTask extends BroadcastUpdaterTask
{
   private static final String W = " W";
   private final NumberFormat nf = NumberFormat.getInstance();

   private double base;
   private double min;
   private double max;
   private volatile boolean reset = true;

   public MeterBroadcastUpdaterTask(final Activity activity)
   {
      super(activity);
      nf.setMinimumFractionDigits(1);
   }

   @Override
   protected void updateView(final PvValue value, final Activity activity)
   {
      final double current = value.getCurrent() / 10.0;

      if (reset)
      {
         base = current;
         reset = false;
         min = 0;
         max = 0;
      }

      final double diff = current - base;
      min = Math.min(min, diff);
      max = Math.max(max, diff);

      ((TextView) activity.findViewById(R.id.valueDiff)).setText(nf.format(diff) + W);
      ((TextView) activity.findViewById(R.id.valueMin)).setText(nf.format(min) + W);
      ((TextView) activity.findViewById(R.id.valueMax)).setText(nf.format(max) + W);
   }

   public void reset()
   {
      reset = true;
   }
}
