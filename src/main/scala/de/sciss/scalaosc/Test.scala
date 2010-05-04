/*
 *  Test.scala
 *  (ScalaOSC)
 *
 *  Copyright (c) 2008-2009 Hanns Holger Rutz. All rights reserved.
 *
 *	 This library is free software; you can redistribute it and/or
 *	 modify it under the terms of the GNU Lesser General Public
 *	 License as published by the Free Software Foundation; either
 *	 version 2.1 of the License, or (at your option) any later version.
 *
 *	 This library is distributed in the hope that it will be useful,
 *	 but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *	 Lesser General Public License for more details.
 *
 *	 Below is a copy of the GNU Lesser General Public License
 *
 *	 For further information, please contact Hanns Holger Rutz at
 *	 contact@sciss.de
 */

package de.sciss.scalaosc

import java.io.IOException
import java.net.{ InetAddress, InetSocketAddress }
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel

/**
 *	   @version	0.11, 24-Nov-09
 */
object Test {
	def codec {
// NOTE: scalacheck doesn't seem to be compatible with
//		 scala 2.8 BETA, and i cannot get the sources
// 		 to compile due to more stupid dependancies.
// 	     so screw scalacheck for the moment....
/*
		import _root_.org.scalacheck._
		import _root_.org.scalacheck.Arbitrary._
		import _root_.org.scalacheck.Prop._
		
		var c: OSCPacketCodec = null
		val b = ByteBuffer.allocate( 8192 )
		val str = arbitrary[String] suchThat (_.indexOf(0) == -1) // null-character not allowed
		val strictArgGen = Gen.oneOf( arbitrary[Int], arbitrary[Float], str )
		val strictListGen = Gen.listOf[Any]( strictArgGen )
		val fatArgGen = Gen.oneOf( arbitrary[Int], arbitrary[Float], arbitrary[Long], arbitrary[Double], str )
		val fatListGen = Gen.listOf[Any]( fatArgGen )
// how do we limit the list size? no clue... seems to work nevertheless
//		val sizedGen = Gen.sized { size => (size < 100) ==> listGen }
		val checka = (list: List[Any]) => {
			val msg = OSCMessage( "/test", list:_* )
			b.clear
			msg.encode( c, b )
			b.flip
			val msgOut = c.decode( b ).asInstanceOf[OSCMessage]
			val decArgs = msgOut.args
			(msgOut.name == msg.name) :| "name" &&
			(decArgs == msg.args) :| ("args before: " + msg.args + " / after: " + decArgs.toList )
		}
		c = new OSCPacketCodec( OSCPacketCodec.MODE_STRICT_V1 )
		val strictProp = forAll( strictListGen )( checka ) 
		strictProp.check
		c = new OSCPacketCodec( OSCPacketCodec.MODE_FAT_V1 )
		val fatProp = forAll( fatListGen )( checka ) 
		fatProp.check
*/
	}
	
  def receiver {
	    val rcv = OSCReceiver.apply( 'udp, 0, true )
	    rcv.start
	    
	    println( "Test.receiver\n\n" +
               "  is waiting for an incoming message " +
               "on UDP port " + rcv.localAddress.getPort + ".\n" +
               "  Send \"/quit\" to terminate.\n" )
	    
	    val sync = new AnyRef
	    
	    rcv.dumpOSC( OSCChannel.DUMP_BOTH )
	    rcv.action = (msg, addr, when) => {
	    	System.out.println( "Received message '" + msg.name + "'" )
//	    	OSCPacket.printTextOn( System.out, msg )
	    	if( msg.name == "/quit" ) sync.synchronized( sync.notifyAll )
	    }
	    sync.synchronized( sync.wait )
  }
  
  def transmitter {
//	  import StrictV1._
   
	  println( "Test.transmitter\n\n" +
               "  assumes that scsynth is running on" +
               "  localhost port 57110\n" )
   
	  var trns : OSCTransmitter	= null
      var dch : DatagramChannel = null

      try {
          val addr		= new InetSocketAddress( InetAddress.getLocalHost, 57110 )
          val notify	= new AnyRef
          
          dch     = DatagramChannel.open
          dch.socket.bind( null )    // assigns an automatic local socket address
          trns    = OSCTransmitter.withChannel( dch )
          trns.target = addr
          trns.dumpOSC( OSCChannel.DUMP_TEXT, System.out )

          trns.send( OSCMessage( "/s_new", "default", 1000, 0, 0, "amp", 0f ))
          for( i <- (1 to 8) ) {
        	  trns.send( OSCMessage( "/n_set", 1000, "freq", i * 333, "amp", 0.5f ))
        	  Thread.sleep( 200 )
          }
          
          import de.sciss.scalaosc.{ OSCMessage => M }
          
//        trns.send( OSCMessage( "/n_free", 1000 ))
          trns ! M( "/n_free", 1000 )
      }
      catch {
        case e1: InterruptedException => ()
        case e2: IOException =>
          println( e2.getClass.getName + " : " + e2.getLocalizedMessage )
      }
      finally {
//          if( rcv != null ) {
//              rcv.dispose();
//          } else
          if( dch != null ) {
              try {
                  dch.close
              }
              catch { case e: IOException => }
          }
      }
  }
}
