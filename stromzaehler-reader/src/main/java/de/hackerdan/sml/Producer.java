/*
 * Copyright (C) 2014 Daniel Hirscher
 */

package de.hackerdan.sml;

import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Enumeration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openmuc.jsml.tl.SMLMessageExtractor;

import de.hackerdan.sml.model.PvValue;

/**
 * Produces SML data.
 */
public class Producer
{
   private static Logger logger = LogManager.getLogger(Producer.class);

   private static final int POS_180 = 147;
   private static final int POS_280 = 171;
   private static final int POS_CWL = 276;

   private final Consumer consumer;

   private SerialPort serialPort;
   private DataInputStream is;

   private long day180;
   private long day280;

   public Producer(final Consumer consumer)
   {
      super();
      this.consumer = consumer;
   }

   public void start()
   {
      if (logger.isInfoEnabled())
      {
         logger.info("Producer started.");
      }

      try
      {
         setupComPort("/dev/ttyUSB0");

         while (true)
         {
            try
            {
               final SMLMessageExtractor extractor = new SMLMessageExtractor(is, 5000);
               final byte[] smlMessage = extractor.getSmlMessage();
               processSmlMessage(smlMessage);
            }
            catch (final IOException e)
            {
               logger.warn("Error getting SML file. Retrying.", e);
            }
         }

      }
      catch (final IOException e)
      {
         logger.warn("Could not configure serial receiver.", e);
      }
      finally
      {
         try
         {
            close();
         }
         catch (final IOException e)
         {
            logger.warn("Error closing serial port.", e);
         }
      }
   }

   private void processSmlMessage(final byte[] smlMessage)
   {
      // CSOFF: BooleanExpressionComplexity
      final long value180 = ((0xFFL & smlMessage[POS_180]) << 32) | ((0xFF & smlMessage[POS_180 + 1]) << 24)
            | ((0xFF & smlMessage[POS_180 + 2]) << 16) | ((0xFF & smlMessage[POS_180 + 3]) << 8)
            | (0xFF & smlMessage[POS_180 + 4]);
      final long value280 = ((0xFFL & smlMessage[POS_280]) << 32) | ((0xFF & smlMessage[POS_280 + 1]) << 24)
            | ((0xFF & smlMessage[POS_280 + 2]) << 16) | ((0xFF & smlMessage[POS_280 + 3]) << 8)
            | (0xFF & smlMessage[POS_280 + 4]);
      final int current = ((0xFF & smlMessage[POS_CWL]) << 24) | ((0xFF & smlMessage[POS_CWL + 1]) << 16)
            | ((0xFF & smlMessage[POS_CWL + 2]) << 8) | (0xFF & smlMessage[POS_CWL + 3]);
      // CSON: BooleanExpressionComplexity

      if (0 == day180 && 0 == day280)
      {
         day180 = value180;
         day280 = value280;
      }

      final PvValue model = new PvValue(value180, value280, current, day180, day280);
      consumer.consume(model);
   }

   public void setupComPort(final String serialPortName) throws IOException
   {
      boolean portFound = false;
      Enumeration<?> portList;
      CommPortIdentifier portId = null;

      // parse ports and if the default port is found, initialized the reader
      portList = CommPortIdentifier.getPortIdentifiers();
      while (portList.hasMoreElements())
      {
         portId = (CommPortIdentifier) portList.nextElement();
         if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL)
         {
            if (portId.getName().equals(serialPortName))
            {
               portFound = true;
               break;
            }
         }
      }

      if (!portFound || null == portId)
      {
         throw new IOException("Port not found: " + serialPortName);
      }

      try
      {
         serialPort = (SerialPort) portId.open("SimpleReadApp", 2000);
      }
      catch (final PortInUseException e)
      {
         throw new IOException("Cannot open serial port: " + serialPortName, e);
      }

      try
      {
         serialPort.setSerialPortParams(9600, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
         serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_RTSCTS_IN);
      }
      catch (final UnsupportedCommOperationException e)
      {
         throw new IOException("Error configuring serial port: " + serialPortName, e);
      }

      is = new DataInputStream(new BufferedInputStream(serialPort.getInputStream()));
   }

   public void close() throws IOException
   {
      is.close();
      serialPort.close();
   }
}
