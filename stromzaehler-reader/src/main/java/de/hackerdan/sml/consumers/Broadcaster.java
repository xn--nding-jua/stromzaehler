/*
 * Copyright (C) 2014 Daniel Hirscher
 */

package de.hackerdan.sml.consumers;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.hackerdan.sml.model.FullDataConverter;
import de.hackerdan.sml.model.PvValue;

/**
 * Broadcasts the current values over UDP in the local network.
 */
public final class Broadcaster
{
   private static final String BROADCAST_ADDRESS = "192.168.1.255"; // NOPMD broadcast address
   private static final int UDP_PORT = 51354;

   private static Logger logger = LogManager.getLogger(Broadcaster.class);

   private final FullDataConverter converter = new FullDataConverter();

   private DatagramSocket socket;
   private DatagramPacket packet;

   public Broadcaster()
   {
      try
      {
         final InetAddress inetAddress = InetAddress.getByName(BROADCAST_ADDRESS);
         socket = new DatagramSocket();
         socket.setBroadcast(true);
         packet = new DatagramPacket(new byte[FullDataConverter.LEN], FullDataConverter.LEN, inetAddress, UDP_PORT);
      }
      catch (final IOException e)
      {
         logger.error("Could not initialize.", e);
      }
   }

   public void broadcast(final PvValue value)
   {
      try
      {
         packet.setData(converter.convert(value));
         socket.send(packet);
      }
      catch (final IOException e)
      {
         logger.error("Could not send data.", e);
      }
   }

   public void close()
   {
      if (null != socket)
      {
         socket.close();
      }
   }
}
