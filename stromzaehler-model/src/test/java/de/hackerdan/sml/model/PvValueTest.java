/*
 * Copyright (C) 2014 Daniel Hirscher
 */

package de.hackerdan.sml.model;

import static org.junit.Assert.*;

import org.junit.Test;

public class PvValueTest
{
   @Test
   public void simple() throws Exception
   {
      final PvValue values = new PvValue(1, 2, 3, 4, 5);
      assertEquals(1, values.get180());
      assertEquals(2, values.get280());
      assertEquals(3, values.getCurrent());
      assertTrue(values.getTimestamp() > 0);
      assertEquals(4, values.getDay180());
      assertEquals(5, values.getDay280());
   }

   @Test
   public void withTimestamp() throws Exception
   {
      final PvValue values = new PvValue(1, 2, 3, 4);
      assertEquals(1, values.getTimestamp());
      assertEquals(2, values.get180());
      assertEquals(3, values.get280());
      assertEquals(4, values.getCurrent());
   }

   @Test
   public void string() throws Exception
   {
      final PvValue values = new PvValue(3, 4, 5, 6, 7);
      assertTrue(values.toString().startsWith("PvValue [timestamp="));
      assertTrue(values.toString().endsWith(", value180=3, value280=4, current=5, day180=6, day280=7]"));
   }
}
