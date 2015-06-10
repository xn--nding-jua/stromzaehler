/*
 * Copyright (C) 2014 Daniel Hirscher
 */

package de.hackerdan.sml.consumers;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import de.hackerdan.sml.model.PvValue;

public class BroadcasterTest
{
   @Test
   public void sendAndReceive() throws Exception
   {
      final Broadcaster broadcaster = new Broadcaster();
      final PvValue value = new PvValue(12345, 3210, -2000, 30, 300);

      final DatagramSocket socket = new DatagramSocket(51354);
      final DatagramPacket packet = new DatagramPacket(new byte[1024], 1024);

      final CountDownLatch latch = new CountDownLatch(1);

      final Runnable runnable = new Runnable() {
         @Override
         public void run()
         {
            try
            {
               socket.receive(packet);
            }
            catch (final IOException e)
            {
               fail();
            }
            latch.countDown();
         }
      };
      final Thread waiter = new Thread(runnable);
      waiter.start();

      // give thread some time to go into socket blocking I/O
      Thread.sleep(50);

      broadcaster.broadcast(value);

      final boolean success = latch.await(2, TimeUnit.SECONDS);
      assertTrue(success);

      assertTrue(packet.getAddress().getHostAddress().startsWith("192.168.1."));
      assertEquals(8 + 8 + 8 + 4 + 8 + 8, packet.getLength());
      // final byte[] data = packet.getData();

      socket.close();
      broadcaster.close();
   }
}
