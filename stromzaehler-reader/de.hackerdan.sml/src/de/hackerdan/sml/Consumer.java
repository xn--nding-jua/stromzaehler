/*
 * Copyright (C) 2014 Daniel Hirscher
 */

package de.hackerdan.sml;

import de.hackerdan.sml.consumers.DailyFileWriter;
import de.hackerdan.sml.consumers.LedHardwareFeedback;
import de.hackerdan.sml.consumers.TCPServer;
import de.hackerdan.sml.model.PvValue;

/**
 * Consumes values.
 * <p>
 * Delegates to multiple consumers.
 */
public class Consumer
{
   private final DailyFileWriter dataWriter = new DailyFileWriter();
   private final LedHardwareFeedback ledFeedback = new LedHardwareFeedback();

   public void consume(final PvValue values)
   {
      dataWriter.write(values);
      TCPServer.model = values;
      ledFeedback.compute(values);
   }
}
