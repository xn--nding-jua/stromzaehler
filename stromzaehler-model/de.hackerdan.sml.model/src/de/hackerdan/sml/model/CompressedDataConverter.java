/*
 * Copyright (C) 2014 Daniel Hirscher
 */

package de.hackerdan.sml.model;

import java.nio.ByteBuffer;

/**
 * Converts a {@link PvValue} to and from bytes with only seconds of the day as time information.
 * <p>
 * Bytes length must be 17.
 */
public class CompressedDataConverter
{
   public static final int LEN = 3 + 5 + 5 + 4;

   private final ByteBuffer buffer = ByteBuffer.allocate(LEN);

   public byte[] convert(final PvValue value)
   {
      if (null == value)
      {
         throw new IllegalArgumentException("Value must not be null.");
      }

      buffer.clear();

      buffer.putLong(value.getTimestamp() << 40);
      buffer.position(buffer.position() - 5);

      buffer.putLong(value.get180() << 24);
      buffer.position(buffer.position() - 3);

      buffer.putLong(value.get280() << 24);
      buffer.position(buffer.position() - 3);

      buffer.putInt(value.getCurrent());

      return buffer.array();
   }

   public PvValue convert(final long timestampBase, final byte[] bytes)
   {
      if (null == bytes || LEN != bytes.length)
      {
         throw new IllegalArgumentException("Bytes length must be " + LEN);
      }

      buffer.clear();
      buffer.put(bytes);
      buffer.rewind();

      final long timestampPart = buffer.getLong() >>> 40;
      final long timestamp = timestampBase + timestampPart;
      buffer.position(buffer.position() - 5);

      final long value180 = buffer.getLong() >>> 24;
      buffer.position(buffer.position() - 3);

      final long value280 = buffer.getLong() >>> 24;
      buffer.position(buffer.position() - 3);

      final int valueCurrent = buffer.getInt();

      return new PvValue(timestamp, value180, value280, valueCurrent);
   }
}
