/*
 * Copyright (C) 2014 Daniel Hirscher
 */

package de.hackerdan.sml.consumers;

import static org.junit.Assert.*;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.junit.Before;
import org.junit.Test;

import de.hackerdan.sml.model.PvValue;

public class DailyFileWriterTest
{
   private File file;

   @Before
   public void setUp() throws Exception
   {
      final Date date = new Date();
      final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.GERMANY);
      final String fileName = "strom-" + format.format(date);
      file = new File("strom-data", fileName);

      // CSOFF: RegexpSinglelineJava
      if (file.delete())
      {
         System.out.println("Deleted file: " + file);
      }
      if (file.getParentFile().exists())
      {
         if (file.getParentFile().delete())
         {
            System.out.println("Deleted dir: " + file.getParent());
         }
      }
      // CSON: RegexpSinglelineJava
   }

   @Test
   public void oneValueNoFile() throws Exception
   {
      final DailyFileWriter writer = new DailyFileWriter();
      writer.write(new PvValue(1, 2, 3, 4, 5));
      assertFalse(file.exists());
   }

   @Test
   public void manyValuesCreateOneFile() throws Exception
   {
      final DailyFileWriter writer = new DailyFileWriter();
      for (int i = 0; i < 60 * 31; ++i)
      {
         writer.write(new PvValue(1, 2, 3, 4, 5));
      }
      assertTrue(file.exists());
   }

   @Test
   public void manyValuesAppendOneFile() throws Exception
   {
      final DailyFileWriter writer = new DailyFileWriter();
      for (int i = 0; i < 60 * 61; ++i)
      {
         writer.write(new PvValue(1, 2, 3, 4, 5));
      }
      assertTrue(file.exists());
   }
}
