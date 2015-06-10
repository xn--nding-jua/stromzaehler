/*
 * Copyright (C) 2014 Daniel Hirscher
 */

package de.hackerdan.sml.consumers;

import org.junit.Test;

import de.hackerdan.sml.model.PvValue;

public class LedHardwareFeedbackTest
{
   @Test
   public void wiringPiNotAvailable() throws Exception
   {
      final LedHardwareFeedback feedback = new LedHardwareFeedback();

      final PvValue value1 = new PvValue(12345, 3210, -2000, 30, 300);
      feedback.compute(value1);

      final PvValue value2 = new PvValue(12345, 3210, 2000, 30, 300);
      feedback.compute(value2);
   }
}
