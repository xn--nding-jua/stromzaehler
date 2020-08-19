/*
 * Copyright (C) 2014 Daniel Hirscher
 */

package de.hackerdan.stromzaehler;

import java.text.NumberFormat;

import android.app.Activity;
import android.widget.TextView;
import de.hackerdan.sml.model.PvValue;

/**
 * Display stats values.
 */
public class StatsTCPUpdaterTask extends TCPUpdaterTask
{
   private static final String KWH = " kWh";
   private final NumberFormat nf1 = NumberFormat.getInstance();
   private final NumberFormat nf2 = NumberFormat.getInstance();

   public StatsTCPUpdaterTask(final Activity activity)
   {
      super(activity);
      nf1.setMinimumFractionDigits(1);
      nf2.setMinimumFractionDigits(3);
   }

   @Override
   protected void updateView(final PvValue value, final Activity activity)
   {
      final double current = value.getCurrent() / 10.0;
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

      ((TextView) activity.findViewById(R.id.value180)).setText(nf2.format(value.get180() / 10000.0) + KWH);
      ((TextView) activity.findViewById(R.id.value280)).setText(nf2.format(value.get280() / 10000.0) + KWH);

      ((TextView) activity.findViewById(R.id.value180day))
            .setText(nf2.format((value.get180() - value.getDay180()) / 10000.0) + KWH);
      ((TextView) activity.findViewById(R.id.value280day))
            .setText(nf2.format((value.get280() - value.getDay280()) / 10000.0) + KWH);
   }
}
