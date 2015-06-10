/*
 * Copyright (C) 2014 Daniel Hirscher
 */

package de.hackerdan.strom.converter;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.Test;

public class MainTest
{
   @Test
   public void convert() throws Exception
   {
      Main.main(new String[]{"src/test/resources/strom-2014-07-10"});
      assertTrue(new File("src/test/resources/strom-2014-07-10.csv").exists());
   }
}
