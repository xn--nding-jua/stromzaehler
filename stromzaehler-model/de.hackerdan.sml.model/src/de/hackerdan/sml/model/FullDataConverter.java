/*
 * Copyright (C) 2014 Daniel Hirscher
 */

package de.hackerdan.sml.model;

import java.nio.ByteBuffer;

/**
 * Converts a {@link PvValue} to and from bytes with full time stamp.
 * <p>
 * Bytes length must be 28.
 */
public class FullDataConverter
{
   public static final int LEN = 8 + 8 + 8 + 4 + 8 + 8;

   private final ByteBuffer buffer = ByteBuffer.allocate(LEN);

   public byte[] convert(final PvValue value)
   {
      if (null == value)
      {
         throw new IllegalArgumentException("Value must not be null.");
      }

      buffer.clear();
      return buffer.putLong(value.getTimestamp()).putLong(value.get180()).putLong(value.get280())
            .putInt(value.getCurrent()).putLong(value.getDay180()).putLong(value.getDay280()).array();

   }

   public PvValue convert(final byte[] bytes)
   {
      if (null == bytes || LEN != bytes.length)
      {
         throw new IllegalArgumentException("Bytes length must be " + LEN);
      }

      buffer.clear();
      buffer.put(bytes);
      buffer.rewind();
      final long timestamp = buffer.getLong();
      final long value180 = buffer.getLong();
      final long value280 = buffer.getLong();
      final int valueCurrent = buffer.getInt();
      final long day180 = buffer.getLong();
      final long day280 = buffer.getLong();

      return new PvValue(timestamp, value180, value280, valueCurrent, day180, day280);
   }
}
