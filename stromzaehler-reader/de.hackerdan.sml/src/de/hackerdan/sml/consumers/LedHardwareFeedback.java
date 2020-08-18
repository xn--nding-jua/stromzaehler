/*
 * Copyright (C) 2014 Daniel Hirscher
 */

package de.hackerdan.sml.consumers;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.hackerdan.sml.Main;
import de.hackerdan.sml.SmlConfig;
import de.hackerdan.sml.model.PvValue;

/**
 * Switches green LED on or off as hardware feedback device depending on power availability.
 * <p>
 * States: <code>true</code> green LED in on when current value is negative, <code>false</code>
 * green LED is off when current value is zero or positive.
 */
public class LedHardwareFeedback
{
   private static Logger logger = LogManager.getLogger(Main.class);
   private boolean laststate; // start with off

   public LedHardwareFeedback()
   {
      initGpio(SmlConfig.getInstance().getWiringPiLEDGPIO());
   }

   public void compute(final PvValue values)
   {
      final boolean state = values.getCurrent() < 0;
      if (state != laststate)
      {
         setGpio(SmlConfig.getInstance().getWiringPiLEDGPIO(), state);
         laststate = state;
      }
   }

   private void initGpio(final int gpioPort)
   {
      exec(SmlConfig.getInstance().getWiringPiBinaryPath() + " mode " + gpioPort + " out");
      setGpio(gpioPort, laststate);
   }

   private static void setGpio(final int gpioPort, final boolean state)
   {
      exec(SmlConfig.getInstance().getWiringPiBinaryPath() + " write " + gpioPort + ' ' + (state ? '1' : '0'));
   }

   private static void exec(final String command)
   {
      try
      {
         Runtime.getRuntime().exec(command);
      }
      catch (final IOException e)
      {
         logger.error("Could not execute: " + command, e);
      }
   }
}
