/*
 * Copyright (C) 2014 Daniel Hirscher
 */

package de.hackerdan.sml;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class Main
{
   private static Logger logger = LogManager.getLogger(Main.class);

   private Main()
   {
      // main class
   }

   public static void main(final String[] args)
   {
      if (logger.isInfoEnabled())
      {
         logger.info("SML tool started.");
      }

      final Consumer consumer = new Consumer();
      final Producer producer = new Producer(consumer);

      producer.start();
   }
}
