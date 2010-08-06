/*
 *  OSCTransmitter.scala
 *  (ScalaOSC)
 *
 *  Copyright (c) 2008-2010 Hanns Holger Rutz. All rights reserved.
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

import impl.{TCPTransmitter, UDPTransmitter}
import java.io.IOException
import java.net.{ InetAddress, InetSocketAddress, SocketAddress }
import java.nio.{ BufferOverflowException, ByteBuffer }
import OSCChannel._
import ScalaOSC._
import java.nio.channels.{SocketChannel, DatagramChannel, SelectableChannel}

/**
 * 	@author	Hanns Holger Rutz
 * 	@version 0.11, 23-Nov-09
 */
object OSCTransmitter {
	/**
	 *	Creates a new instance of an <code>OSCTransmitter</code>, using
	 *	default codec and a specific transport protocol and port. It
	 *	uses the local machine's IP or the &quot;loopback&quot; address.
	 *	Note that the <code>port</code> specifies the
	 *	local socket, not the remote (or target) port. This can be set
	 *	using the <code>setTarget</code> method!
	 *
	 *	@param	transport   the protocol to use, currently either <code>UDP</code> or <code>TCP</code>
	 *	@param	port		the port number for the OSC socket, or <code>0</code> to use an arbitrary free port
	 *	@param	loopBack	if <code>true</code>, the &quot;loopback&quot; address (<code>&quot;127.0.0.0.1&quot;</code>)
	 *						is used which limits communication to the local machine. If <code>false</code>, the
	 *						local machine's regular IP address is used.
	 *	
	 *	@return				the newly created transmitter
	 *
	 *	@throws	IOException					if a networking error occurs while creating the socket
	 *	@throws	IllegalArgumentException	if an illegal protocol is used
	 */
	@throws( classOf[ IOException ])
	def apply( transport: OSCTransport, port: Int = 0, loopBack: Boolean = false,
              codec: OSCPacketCodec = OSCPacketCodec.default ) : OSCTransmitter = {
		val localAddress = if( loopBack )
			new InetSocketAddress( "127.0.0.1", port ) else
			new InetSocketAddress( InetAddress.getLocalHost, port )
		withAddress( transport, localAddress, codec )
	}

	/**
	 *	Creates a new instance of an <code>OSCTransmitter</code>, using
	 *	default codec and a specific transport protocol and local socket address.
	 *	Note that <code>localAddress</code> specifies the
	 *	local socket, not the remote (or target) socket. This can be set
	 *	using the <code>setTarget</code> method!
	 *
	 *	@param	transport		the protocol to use, currently either <code>UDP</code> or <code>TCP</code>
	 *	@param	localAddress	a valid address to use for the OSC socket. If the port is <code>0</code>,
	 *							an arbitrary free port is picked when the transmitter is connected. (you can find out
	 *							the actual port in this case by calling <code>getLocalAddress()</code> after the
	 *							transmitter was connected).
	 *	
	 *	@return					the newly created transmitter
	 *
	 *	@throws	IOException					if a networking error occurs while creating the socket
	 *	@throws	IllegalArgumentException	if an illegal protocol is used
	 */
	@throws( classOf[ IOException ])
	def withAddress( transport: OSCTransport, localAddress: InetSocketAddress,
                    codec: OSCPacketCodec = OSCPacketCodec.default ) : OSCTransmitter = {
      transport match {
         case UDP => new UDPTransmitter( localAddress, codec )
         case TCP => new TCPTransmitter( localAddress, codec )
		}
	}

	/**
	 *	Creates a new instance of an <code>OSCTransmitter</code>, using
	 *	default codec and UDP transport on a given channel. The caller should ensure that
	 *	the provided channel's socket was bound to a valid address
	 *	(using <code>dch.socket().bind( SocketAddress )</code>).
	 *	Note that <code>dch</code> specifies the
	 *	local socket, not the remote (or target) socket. This can be set
	 *	using the <code>setTarget</code> method!
	 *
	 *	@param	dch			the <code>DatagramChannel</code> to use as UDP socket.
	 *	@return				the newly created transmitter
	 *
	 *	@throws	IOException	if a networking error occurs while configuring the socket
	 */
	@throws( classOf[ IOException ])
	def withChannel( dch: DatagramChannel, codec: OSCPacketCodec ) : OSCTransmitter = {
		new UDPTransmitter( dch, codec )
	}

   @throws( classOf[ IOException ])
   def withChannel( dch: DatagramChannel ) : OSCTransmitter = {
      new UDPTransmitter( dch, OSCPacketCodec.default )
   }

	/**
	 *	Creates a new instance of an <code>OSCTransmitter</code>, using
	 *	default codec and TCP transport on a given channel. The caller should ensure that
	 *	the provided channel's socket was bound to a valid address
	 *	(using <code>sch.socket().bind( SocketAddress )</code>). Furthermore,
	 *	the channel must be connected (using <code>connect()</code>) before
	 *	being able to transmit messages.
	 *	Note that <code>sch</code> specifies the
	 *	local socket, not the remote (or target) socket. This can be set
	 *	using the <code>setTarget</code> method!
	 *
	 *	@param	sch			the <code>SocketChannel</code> to use as TCP socket.
	 *	@return				the newly created transmitter
	 *
	 *	@throws	IOException	if a networking error occurs while configuring the socket
	 */
	@throws( classOf[ IOException ])
	def withChannel( sch: SocketChannel, codec: OSCPacketCodec ) : OSCTransmitter = {
		new TCPTransmitter( sch, codec )
	}

   @throws( classOf[ IOException ])
   def withChannel( sch: SocketChannel ) : OSCTransmitter = {
      new TCPTransmitter( sch, OSCPacketCodec.default )
   }
}

abstract class OSCTransmitter( val transport: OSCTransport, protected val addr: InetSocketAddress,
                               protected val revivable: Boolean )
extends OSCChannel {
	protected val sync						= new AnyRef
	protected var allocBuf 					= true
	private var bufSize						= DEFAULTBUFSIZE
	protected var byteBuf : ByteBuffer	= null
 
   var target: SocketAddress		      = null
	
	/**
	 *	Establishes connection for transports requiring
	 *	connectivity (e.g. TCP). For transports that do not require connectivity (e.g. UDP),
	 *	this ensures the communication channel is created and bound.
	 *  <P>
	 *	When a <B>UDP</B> transmitter
	 *	is created without an explicit <code>DatagramChannel</code> &ndash; say by
	 *	calling <code>OSCTransmitter.newUsing( &quot;udp&quot; )</code>, you are required
	 *	to call <code>connect()</code> so that an actual <code>DatagramChannel</code> is
	 *	created and bound. For a <B>UDP</B> transmitter which was created with an explicit
	 *	<code>DatagramChannel</code>, this method does noting, so it is always safe
	 *	to call <code>connect()</code>. However, for <B>TCP</B> transmitters, 
	 *	this may throw an <code>IOException</code> if the transmitter
	 *	was already connected, therefore be sure to check <code>isConnected()</code> before.
	 *	
	 *	@throws	IOException	if a networking error occurs. Possible reasons: - the underlying
	 *						network channel had been closed by the server. - the transport
	 *						is TCP and the server is not available. - the transport is TCP
	 *						and an <code>OSCReceiver</code> sharing the same socket was stopped before (unable to revive).
	 *
	 *	@see	#isConnected()
	 */
	@throws( classOf[ IOException ])
	def connect : Unit
		
	/**
	 *	Queries the connection state of the transmitter.
	 *
	 *	@return	<code>true</code> if the transmitter is connected, <code>false</code> otherwise. For transports that do not use
	 *			connectivity (e.g. UDP) this returns <code>false</code>, if the
	 *			underlying <code>DatagramChannel</code> has not yet been created.
	 *
	 *	@see	#connect()
	 */
	def isConnected : Boolean

	final def !( p: OSCPacket ) : Unit = send( p, target )
 
	final def bufferSize_=( size: Int ) {
		sync.synchronized {
			if( bufSize != size ) {
				bufSize		= size
				allocBuf	= true
			}
		}
	}
	
	final def bufferSize : Int = {
		sync.synchronized {
			bufSize
		}
	}

	def dispose {
		byteBuf	= null
	}

	// @synchronization	caller must ensure synchronization
	protected final def checkBuffer {
		if( allocBuf ) {
			byteBuf		= ByteBuffer.allocateDirect( bufSize )
			allocBuf	= false
		}
	}
	
	private[ scalaosc ] def channel : SelectableChannel

   /**
    *	Sends an OSC packet (bundle or message) to the given
    *	network address, using the current codec.
    *
    *	@param	p		the packet to send
    *	@param	target	the target address to send the packet to
    *
    *	@throws	IOException	if a write error, OSC encoding error,
    *						buffer overflow error or network error occurs
    */
	@throws( classOf[ IOException ])
	def send( p: OSCPacket, target: SocketAddress ) : Unit

   protected final def dumpPacket( p: OSCPacket ) {
      if( (dumpMode != DUMP_OFF) && dumpFilter.apply( p )) {
         printStream.synchronized {
            printStream.print( "s: " )
            if( (dumpMode & DUMP_TEXT) != 0 ) OSCPacket.printTextOn( codec, printStream, p )
            if( (dumpMode & DUMP_HEX)  != 0 ) {
               OSCPacket.printHexOn( printStream, byteBuf )
               byteBuf.flip
            }
         }
      }
   }
}