/*
 * Copyright (C) 2014 Daniel Hirscher
 */

package de.hackerdan.stromzaehler;

import java.text.NumberFormat;

import android.app.Activity;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.DisplayMetrics;
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
      final int current = value.getCurrent() / 10; // Stromzähler liefert Kommastellen mit

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
      float Pmax=5000; // +100%
      float Pmin=-600; // -100%

      Point displaySize = new Point();
      activity.getWindowManager().getDefaultDisplay().getRealSize(displaySize);
      float width = displaySize.x;
      int leftMargin;

      if (current<0) {
         leftMargin = Math.round((width/2) - ((width/2)*(current/Pmin))); // 0=-100%, (width/2)=0%, width=100%
      }else{
         if (current<=Pmax) {
            leftMargin = Math.round((width/2) + 0.9f*((width/2) * (current / Pmax))); // 0=-100%, (width/2)=0%, width=100%
         }else{
            // end of bar
            leftMargin = Math.round(width);
         }
      }
      final ImageView arrow = (ImageView)activity.findViewById(R.id.imageArrow);
      RelativeLayout.LayoutParams arrow_lp = (RelativeLayout.LayoutParams)arrow.getLayoutParams();
      arrow_lp.setMargins(leftMargin, -100, 0, 0);
      arrow.setLayoutParams(arrow_lp);

      // show allowed loads
      switchImage(activity, current, -100, R.id.imageCharge, R.mipmap.charge, R.mipmap.charge_off);
      switchImage(activity, current, -300, R.id.imageWama, R.mipmap.wama, R.mipmap.wama_off);
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
