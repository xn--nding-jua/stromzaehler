/*
 * Copyright (C) 2014 Daniel Hirscher
 */

package de.hackerdan.sml.model;

import static org.junit.Assert.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

// CSOFF: IllegalCatch

/**
 * Helper to get full coverage of enums.
 * <p>
 * From a post from the EMMA forum.
 */
public final class CoverageHelper
{
   private CoverageHelper()
   {
      // utility method
   }

   @SuppressWarnings({"unchecked", "rawtypes"})
   public static void executeEnum(final Class targetEnum) throws Exception // NOPMD
   {
      if (!targetEnum.isEnum())
      {
         fail("class passed was not an enumeration " + targetEnum.getName());
      }

      // get an array of the enum constants
      final Object[] constants = targetEnum.getEnumConstants();

      // taking each constant in turn call the valueOf method with the string
      // value of the constant
      for (final Object constant : constants)
      {
         final Method methodValueOf = targetEnum.getMethod("valueOf", new Class[]{String.class});
         methodValueOf.invoke(null, new Object[]{constant.toString()});
      }

      // handle the case of passing an illegal constant name to the enum
      final Method methodValueOf = targetEnum.getMethod("valueOf", new Class[]{String.class});
      methodValueOf.invoke(null, new Object[]{"junkEnumStringValue"});

      // invoke the values method of the enum
      final Method methodValues = targetEnum.getMethod("values", new Class[]{});
      methodValues.invoke(null, new Object[]{});
   }

   @SuppressWarnings({"unchecked", "rawtypes"})
   public static void executePrivateConstructor(final Class targetClass) throws Exception // NOPMD
   {
      final Constructor c = targetClass.getDeclaredConstructor(new Class[]{});
      c.setAccessible(true);
      c.newInstance(new Object[]{});
   }
}
