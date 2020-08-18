/*
 * Copyright (C) 2014 Daniel Hirscher
 */

package de.hackerdan.strom.converter;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.hackerdan.sml.model.CompressedDataConverter;
import de.hackerdan.sml.model.PvValue;

/**
 * Converts data files to CSV.
 */
public final class Main
{
   private static final Logger LOGGER = LogManager.getLogger(Main.class);

   private Main()
   {
      // main entry point
   }

   public static void main(final String[] args)
   {
      LOGGER.info("Stromzaehler data files to CSV converter.");
      for (final String arg : args)
      {
         final File file = new File(arg);
         if (file.exists())
         {
            convert(file);
         }
         else
         {
            LOGGER.warn("Could not find file: {}", file.getAbsolutePath());
         }
      }
   }

   private static void convert(final File file)
   {
      final File outFile = new File(file.getAbsolutePath() + ".csv");
      try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(outFile))))
            {
         final byte version = in.readByte();
         if (1 == version)
         {
            LOGGER.info("Converting file: {}", file);
            final long timestampBase = in.readLong();
            final CompressedDataConverter converter = new CompressedDataConverter();
            final byte[] buffer = new byte[CompressedDataConverter.LEN];
            int len = in.read(buffer);
            while (len == CompressedDataConverter.LEN)
            {
               final PvValue value = converter.convert(timestampBase, buffer);
               final StringBuilder csv = new StringBuilder();
               csv.append(value.getTimestamp()).append(',').append(value.getCurrent()).append(',')
                     .append(value.get180()).append(',').append(value.get280()).append('\n');
               out.writeBytes(csv.toString());
               len = in.read(buffer);
            }
         }
         else
         {
            LOGGER.error("Unsupported version: {} on file: {}", Integer.valueOf(version), file.getAbsolutePath());
         }
      }
      catch (final IOException e)
      {
         LOGGER.error("Error reading file: {}", file.getAbsolutePath(), e);
      }
   }
}
