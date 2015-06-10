/*
 * Copyright (C) 2014 Daniel Hirscher
 */

package de.hackerdan.sml.model;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class FullDataConverterTest
{
   private FullDataConverter converter;

   @Before
   public void setUp() throws Exception
   {
      converter = new FullDataConverter();
   }

   @Test
   public void toBytes() throws Exception
   {
      final byte[] actuals = converter.convert(new PvValue(7, 6, 5, 4, 3, 2));
      final byte[] expected = new byte[]{0, 0, 0, 0, 0, 0, 0, 7, 0, 0, 0, 0, 0, 0, 0, 6, 0, 0, 0, 0, 0, 0, 0, 5, 0, 0,
            0, 4, 0, 0, 0, 0, 0, 0, 0, 3, 0, 0, 0, 0, 0, 0, 0, 2};
      assertArrayEquals(expected, actuals);
   }

   @Test
   public void toValue() throws Exception
   {
      final byte[] bytes = new byte[]{0, 0, 0, 0, 0, 0, 0, 7, 0, 0, 0, 0, 0, 0, 0, 6, 0, 0, 0, 0, 0, 0, 0, 5, 0, 0, 0,
            4, 0, 0, 0, 0, 0, 0, 0, 3, 0, 0, 0, 0, 0, 0, 0, 2};
      final PvValue values = converter.convert(bytes);
      assertEquals(7, values.getTimestamp());
      assertEquals(6, values.get180());
      assertEquals(5, values.get280());
      assertEquals(4, values.getCurrent());
      assertEquals(3, values.getDay180());
      assertEquals(2, values.getDay280());
   }

   @Test(expected = IllegalArgumentException.class)
   public void toBytesNull() throws Exception
   {
      converter.convert((PvValue) null);
   }

   @Test(expected = IllegalArgumentException.class)
   public void toValueNull() throws Exception
   {
      converter.convert((byte[]) null);
   }

   @Test(expected = IllegalArgumentException.class)
   public void toValueTooShort() throws Exception
   {
      final byte[] bytes = new byte[]{0, 0, 0, 0, 0, 0, 0, 7, 0, 0, 0, 0, 0, 0, 0, 6, 0, 0, 0, 0, 0, 0, 0, 5, 0, 0, 0};
      converter.convert(bytes);
   }

   @Test(expected = IllegalArgumentException.class)
   public void toValueTooLong() throws Exception
   {
      final byte[] bytes = new byte[]{0, 0, 0, 0, 0, 0, 0, 7, 0, 0, 0, 0, 0, 0, 0, 6, 0, 0, 0, 0, 0, 0, 0, 5, 0, 0, 0,
            4, 1};
      converter.convert(bytes);
   }

   @Test(expected = IllegalArgumentException.class)
   public void toValueEmpty() throws Exception
   {
      final byte[] bytes = new byte[0];
      converter.convert(bytes);
   }

   @Test
   public void multipleConversions() throws Exception
   {
      assertArrayEquals(new byte[]{0, 0, 0, 0, 0, 0, 0, 9, 0, 0, 0, 0, 0, 0, 0, 8, 0, 0, 0, 0, 0, 0, 0, 7, 0, 0, 0, 6,
            0, 0, 0, 0, 0, 0, 0, 3, 0, 0, 0, 0, 0, 0, 0, 2}, converter.convert(new PvValue(9, 8, 7, 6, 3, 2)));

      assertArrayEquals(new byte[]{0, 0, 0, 0, 0, 0, 0, 7, 0, 0, 0, 0, 0, 0, 0, 6, 0, 0, 0, 0, 0, 0, 0, 5, 0, 0, 0, 4,
            0, 0, 0, 0, 0, 0, 0, 3, 0, 0, 0, 0, 0, 0, 0, 2}, converter.convert(new PvValue(7, 6, 5, 4, 3, 2)));

      PvValue values = converter.convert(new byte[]{0, 0, 0, 0, 0, 0, 0, 7, 0, 0, 0, 0, 0, 0, 0, 6, 0, 0, 0, 0, 0, 0,
            0, 5, 0, 0, 0, 4, 0, 0, 0, 0, 0, 0, 0, 3, 0, 0, 0, 0, 0, 0, 0, 2});
      assertEquals(7, values.getTimestamp());
      assertEquals(6, values.get180());
      assertEquals(5, values.get280());
      assertEquals(4, values.getCurrent());
      assertEquals(3, values.getDay180());
      assertEquals(2, values.getDay280());

      assertArrayEquals(new byte[]{0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 3, 0, 0, 0, 4,
            0, 0, 0, 0, 0, 0, 0, 3, 0, 0, 0, 0, 0, 0, 0, 1}, converter.convert(new PvValue(1, 2, 3, 4, 3, 1)));

      values = converter.convert(new byte[]{0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 3, 0,
            0, 0, 4, 0, 0, 0, 0, 0, 0, 0, 9, 0, 0, 0, 0, 0, 0, 0, 8});
      assertEquals(1, values.getTimestamp());
      assertEquals(2, values.get180());
      assertEquals(3, values.get280());
      assertEquals(4, values.getCurrent());
      assertEquals(9, values.getDay180());
      assertEquals(8, values.getDay280());

      values = converter.convert(new byte[]{0, 0, 0, 0, 0, 0, 0, 5, 0, 0, 0, 0, 0, 0, 0, 8, 0, 0, 0, 0, 0, 0, 0, 8, 0,
            0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 8, 0, 0, 0, 0, 0, 0, 0, 9});
      assertEquals(5, values.getTimestamp());
      assertEquals(8, values.get180());
      assertEquals(8, values.get280());
      assertEquals(1, values.getCurrent());
      assertEquals(8, values.getDay180());
      assertEquals(9, values.getDay280());

      assertArrayEquals(new byte[]{0, 0, 0, 0, 0, 0, 0, 5, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 7, 0, 0, 0, 9,
            0, 0, 0, 0, 0, 0, 0, 4, 0, 0, 0, 0, 0, 0, 0, 8}, converter.convert(new PvValue(5, 2, 7, 9, 4, 8)));
   }
}
