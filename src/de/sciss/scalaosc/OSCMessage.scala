/*
 * 	OSCMessage.scala
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
import _root_.java.nio.{ BufferOverflowException, ByteBuffer }

import _root_.scala.collection.{ LinearSeqLike }
import _root_.scala.collection.mutable.{ Builder }

import OSCPacket._

/**
 *	@author		Hanns Holger Rutz
 *	@version	0.12, 26-Nov-09
 */
case class OSCMessage( name: String, args: Any* )
extends OSCPacket
with LinearSeqLike[ Any, OSCMessage ]
{
	// ---- getting LinearSeqLike to work properly ----
	
	def newBuilder : Builder[ Any, OSCMessage ] = {
		new scala.collection.mutable.ArrayBuffer[ Any ] mapResult (buf => new OSCMessage( name, buf: _* )) 
	}

	override def iterator : Iterator[ Any ] = args.iterator
	override def drop( n: Int ) : OSCMessage = new OSCMessage( name, args.drop( n ): _* )

	def encode( c: OSCPacketCodec, b: ByteBuffer ) : Unit = c.encodeMessage( this, b )
	def getEncodedSize( c: OSCPacketCodec ) : Int = c.getEncodedMessageSize( this )

	// ---- OSCPacket implementation ----

	private[scalaosc] def printTextOn( c: OSCPacketCodec, stream: PrintStream, nestCount: Int ) {
		var i = 0
		val numSpaces = nestCount << 1
		while( i < numSpaces ) { stream.print( ' ' ); i += 1 }
		stream.print( "[ " )
		printEscapedStringOn( stream, name )
		for( v <- args ) {
			stream.print( ", " )
			// XXX eventually encoder and decoder should be strictly separated,
			// and hence we would integrate the printing of the incoming messages
			// directly into the decoder!
//			c.atomEncoders( v.asInstanceOf[ AnyRef ].getClass ).printTextOn( c, stream, nestCount, v )
			c.atomEncoders( v ).printTextOn( c, stream, nestCount, v )
		}
		if( nestCount == 0 ) stream.println( " ]" ) else stream.print( " ]" )
	}
}