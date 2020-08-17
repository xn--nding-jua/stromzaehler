/*
 * Copyright (C) 2014 Daniel Hirscher
 */

package de.hackerdan.sml;

import de.hackerdan.sml.consumers.Broadcaster;
import de.hackerdan.sml.consumers.DailyFileWriter;
import de.hackerdan.sml.consumers.LedHardwareFeedback;
import de.hackerdan.sml.model.PvValue;

/**
 * Consumes values.
 * <p>
 * Delegates to multiple consumers.
 */
public class Consumer
{
   private final DailyFileWriter dataWriter = new DailyFileWriter();
   private final Broadcaster broadcaster = new Broadcaster();
   private final LedHardwareFeedback ledFeedback = new LedHardwareFeedback();

   public void consume(final PvValue values)
   {
      dataWriter.write(values);
      broadcaster.broadcast(values);
      ledFeedback.compute(values);
   }
}
