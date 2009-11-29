/*
 * 	OSCBundle.scala
 *  (ScalaOSC)
 *
 *  Copyright (c) 2008-2009 Hanns Holger Rutz. All rights reserved.
 *
 *	This library is free software; you can redistribute it and/or
 *	modify it under the terms of the GNU Lesser General Public
 *	License as published by the Free Software Foundation; either
 *	version 2.1 of the License, or (at your option) any later version.
 *
 *	This library is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *	Lesser General Public License for more details.
 *
 *	Below is a copy of the GNU Lesser General Public License
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 *
 */
package de.sciss.scalaosc

import _root_.java.io.{ IOException, PrintStream }
import _root_.java.nio.ByteBuffer

import _root_.scala.collection.{ LinearSeqLike }
import _root_.scala.collection.mutable.{ Builder }

/**
 * 	@author		Hanns Holger Rutz
 * 	@version	23-Nov-09	
 */
object OSCBundle {
  /**
   *  This is the initial string
   *  of an OSC bundle datagram
   */
  private[scalaosc] val TAG   = "#bundle"
  private[scalaosc] val TAGB  = "#bundle\0".getBytes
  
  /**
   *  The special timetag value
   *  to indicate that the bundle be
   *  processed as soon as possible
   */
  val NOW   = 1
 
  private val SECONDS_FROM_1900_TO_1970 = 2208988800L
 
  def millis( when: Long, packets: OSCPacket* ) : OSCBundle = {
	val secsSince1900	= when / 1000 + SECONDS_FROM_1900_TO_1970
	val secsFractional	= ((when % 1000) << 32) / 1000
	val timetag			= (secsSince1900 << 32) | secsFractional
	new OSCBundle( timetag, packets: _* )
  }
  
  def apply( packets: OSCPacket* ) : OSCBundle = {
	 new OSCBundle( NOW, packets: _* )
  }

  def apply( timetag: Long, packets: OSCPacket* ) : OSCBundle = {
	 new OSCBundle( timetag, packets: _* )
  }

	@throws( classOf[ IOException ])
	private[scalaosc] def decode( b: ByteBuffer ) : OSCBundle = {
		val	totalLimit  = b.limit
		val p			= new scala.collection.mutable.ListBuffer[ OSCPacket ]
		val timetag 	= b.getLong

		try {
			while( b.hasRemaining ) {
				b.limit( b.getInt + b.position )   // msg size
				p += decode( b )
				b.limit( totalLimit )
			}
			OSCBundle( timetag, p: _* )
		}
		catch { case e : IllegalArgumentException =>	// throws by b.limit if bundle size is corrupted
			throw new OSCException( OSCException.DECODE, e.getLocalizedMessage )
		}
	}
}

class OSCBundle( val timetag: Long, args: OSCPacket* )
extends OSCPacket
with LinearSeqLike[ OSCPacket, OSCBundle ]
{
	// ---- getting LinearSeqLike to work properly ----
	
	def newBuilder : Builder[ OSCPacket, OSCBundle ] = {
		new scala.collection.mutable.ArrayBuffer[ OSCPacket ] mapResult (buf => new OSCBundle( timetag, args: _* )) 
	}

	override def iterator : Iterator[ OSCPacket ] = args.iterator
	override def drop( n: Int ) : OSCBundle = new OSCBundle( timetag, args.drop( n ): _* )

	// ---- OSCPacket implementation ----
	def name: String = OSCBundle.TAG

	@throws( classOf[ OSCException ])
	def encode( c: OSCPacketCodec, b: ByteBuffer ) : Unit = c.encodeBundle( this, b )
  
	def getEncodedSize( c: OSCPacketCodec ) : Int = c.getEncodedBundleSize( this )

	private[scalaosc] def printTextOn( c: OSCPacketCodec, stream: PrintStream, nestCount: Int ) {
		println( "OSCBundle.printTextOn : not yet implemented." )
		// XXX
	}
}