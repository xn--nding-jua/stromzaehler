/*
 * Copyright (C) 2014 Daniel Hirscher
 */

package de.hackerdan.sml;

import org.junit.Test;

import de.hackerdan.sml.model.PvValue;

public class ConsumerTest
{
   @Test
   public void coverage() throws Exception
   {
      final Consumer consumer = new Consumer();
      consumer.consume(new PvValue(10, 100, 1000, 20, 200));
   }
}
