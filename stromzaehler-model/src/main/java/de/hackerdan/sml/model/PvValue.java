/*
 * Copyright (C) 2014 Daniel Hirscher
 */

package de.hackerdan.sml.model;

/**
 * Holds all values.
 */
public class PvValue
{
   private final long value180;
   private final long value280;
   private final int valueCurrent;
   private final long day180;
   private final long day280;
   private final long timestamp;

   public PvValue(final long value180, final long value280, final int valueCurrent, final long day180, final long day280)
   {
      this(System.currentTimeMillis(), value180, value280, valueCurrent, day180, day280);
   }

   public PvValue(final long timestamp, final long value180, final long value280, final int valueCurrent)
   {
      this(timestamp, value180, value280, valueCurrent, 0, 0);
   }

   public PvValue(final long timestamp, final long value180, final long value280, final int valueCurrent,
         final long day180, final long day280)
   {
      super();
      this.timestamp = timestamp;
      this.value180 = value180;
      this.value280 = value280;
      this.valueCurrent = valueCurrent;
      this.day180 = day180;
      this.day280 = day280;
   }

   public long get180()
   {
      return value180;
   }

   public long get280()
   {
      return value280;
   }

   public long getDay180()
   {
      return day180;
   }

   public long getDay280()
   {
      return day280;
   }

   public int getCurrent()
   {
      return valueCurrent;
   }

   public long getTimestamp()
   {
      return timestamp;
   }

   @Override
   public String toString()
   {
      return "PvValue [timestamp=" + timestamp + ", value180=" + value180 + ", value280=" + value280 + ", current="
            + valueCurrent + ", day180=" + day180 + ", day280=" + day280 + "]";
   }
}
