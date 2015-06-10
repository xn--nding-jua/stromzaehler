/*
 * Copyright (C) 2014 Daniel Hirscher
 */

package de.hackerdan.sml.model;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class CompressedDataConverterTest
{
   private CompressedDataConverter converter;

   @Before
   public void setUp() throws Exception
   {
      converter = new CompressedDataConverter();
   }

   @Test
   public void toBytes() throws Exception
   {
      final byte[] actuals = converter.convert(new PvValue(7, 6, 5, 4));
      final byte[] expected = new byte[]{0, 0, 7, 0, 0, 0, 0, 6, 0, 0, 0, 0, 5, 0, 0, 0, 4};
      assertArrayEquals(expected, actuals);
   }

   @Test
   public void toValue() throws Exception
   {
      final byte[] bytes = new byte[]{0, 0, 5, 0, 0, 0, 0, 6, 0, 0, 0, 0, 5, 0, 0, 0, 4};
      final PvValue values = converter.convert(3, bytes);
      assertEquals(8, values.getTimestamp());
      assertEquals(6, values.get180());
      assertEquals(5, values.get280());
      assertEquals(4, values.getCurrent());
   }

   @Test(expected = IllegalArgumentException.class)
   public void toBytesNull() throws Exception
   {
      converter.convert((PvValue) null);
   }

   @Test(expected = IllegalArgumentException.class)
   public void toValueNull() throws Exception
   {
      converter.convert(0, (byte[]) null);
   }

   @Test(expected = IllegalArgumentException.class)
   public void toValueTooShort() throws Exception
   {
      final byte[] bytes = new byte[]{0, 0, 0, 0, 0, 0, 0, 7, 0, 0, 0, 0, 1, 2, 3, 5};
      converter.convert(0, bytes);
   }

   @Test(expected = IllegalArgumentException.class)
   public void toValueTooLong() throws Exception
   {
      final byte[] bytes = new byte[]{0, 0, 0, 0, 0, 0, 0, 7, 0, 0, 0, 0, 0, 0, 0, 6, 0, 4};
      converter.convert(0, bytes);
   }

   @Test(expected = IllegalArgumentException.class)
   public void toValueEmpty() throws Exception
   {
      final byte[] bytes = new byte[0];
      converter.convert(0, bytes);
   }

   @Test
   public void multipleConversions() throws Exception
   {
      assertArrayEquals(new byte[]{0, 0, 7, 0, 0, 0, 0, 8, 0, 0, 0, 0, 5, 0, 0, 0, 4},
            converter.convert(new PvValue(7, 8, 5, 4)));

      assertArrayEquals(new byte[]{0, 0, 7, 0, 0, 0, 0, 6, 0, 0, 0, 0, 5, 0, 0, 0, 4},
            converter.convert(new PvValue(7, 6, 5, 4)));

      PvValue values = converter.convert(0, new byte[]{0, 0, 5, 0, 0, 0, 0, 6, 0, 0, 0, 0, 5, 0, 0, 0, 4});
      assertEquals(5, values.getTimestamp());
      assertEquals(6, values.get180());
      assertEquals(5, values.get280());
      assertEquals(4, values.getCurrent());

      assertArrayEquals(new byte[]{0, 0, 1, 0, 0, 0, 0, 2, 0, 0, 0, 0, 3, 0, 0, 0, 4},
            converter.convert(new PvValue(1, 2, 3, 4)));

      values = converter.convert(0, new byte[]{0, 0, 1, 0, 0, 0, 0, 2, 0, 0, 0, 0, 3, 0, 0, 0, 4});
      assertEquals(1, values.getTimestamp());
      assertEquals(2, values.get180());
      assertEquals(3, values.get280());
      assertEquals(4, values.getCurrent());

      values = converter.convert(0, new byte[]{0, 0, 5, 0, 0, 0, 0, 8, 0, 0, 0, 0, 8, 0, 0, 0, 1});
      assertEquals(5, values.getTimestamp());
      assertEquals(8, values.get180());
      assertEquals(8, values.get280());
      assertEquals(1, values.getCurrent());

      assertArrayEquals(new byte[]{0, 0, 5, 0, 0, 0, 0, 2, 0, 0, 0, 0, 7, 0, 0, 0, 9},
            converter.convert(new PvValue(5, 2, 7, 9)));

   }
}
