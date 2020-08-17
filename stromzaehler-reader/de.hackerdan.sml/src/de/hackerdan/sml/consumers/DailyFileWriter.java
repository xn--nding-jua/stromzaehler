/*
 * Copyright (C) 2014 Daniel Hirscher
 */

package de.hackerdan.sml.consumers;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.hackerdan.sml.model.CompressedDataConverter;
import de.hackerdan.sml.model.PvValue;

/**
 * Writes the data to files.
 * <p>
 * Creates one file per day.
 * <p>
 * Format:
 * <ul>
 * <li>1 byte version</li>
 * <li>8 bytes time stamp</li>
 * <li>repeatedly:
 * <ul>
 * <li>3 bytes lower part of the time stamp</li>
 * <li>5 bytes 1.8.0</li>
 * <li>5 bytes 2.8.0</li>
 * <li>4 bytes current value</li>
 * </ul>
 * </li>
 * </ul>
 */
public class DailyFileWriter
{
   private static final String DIRECTORY = "strom-data";
   private static final String FILENAME = "strom-";

   private static Logger logger = LogManager.getLogger(DailyFileWriter.class);

   private final CompressedDataConverter converter = new CompressedDataConverter();
   private final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.GERMANY);

   /** Buffer holds bytes for about 30 minutes. */
   private final ByteBuffer buffer = ByteBuffer.allocate(CompressedDataConverter.LEN * 60 * 30);

   public void write(final PvValue value)
   {
      buffer.put(converter.convert(value));
      if (!buffer.hasRemaining())
      {
         try
         {
            writeToFile(value.getTimestamp());
         }
         catch (final IOException e)
         {
            logger.error("Could not write data to file.", e);
         }
         buffer.clear();
      }
   }

   private void writeToFile(final long timestamp) throws IOException
   {
      final Date date = new Date(timestamp);
      final String fileName = FILENAME + format.format(date);
      final File file = new File(DIRECTORY, fileName);

      // check if there is already a file for today
      if (file.exists())
      {
         appendToFile(file);
      }
      else
      {
         // create directory if not available
         if (!file.getParentFile().exists())
         {
            final boolean success = file.getParentFile().mkdirs();
            if (!success)
            {
               throw new IOException("Could not create directory: " + file.getParent());
            }
         }
         createFile(file, timestamp);
      }
   }

   private void createFile(final File file, final long timestamp) throws IOException
   {
      try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file))))
      {
         // version
         out.writeByte(1);
         // full time stamp
         out.writeLong(timestamp);
         // write buffer
         out.write(buffer.array());
      }
   }

   private void appendToFile(final File file) throws IOException
   {
      try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file, true))))
      {
         // write buffer
         out.write(buffer.array());
      }
   }
}
