/*
 * 	OSCReceiver.scala
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
import _root_.java.net.{ DatagramPacket, DatagramSocket, InetAddress,
					     InetSocketAddress, Socket, SocketAddress,
					     UnknownHostException }
import _root_.java.nio.{ BufferUnderflowException, ByteBuffer }
import _root_.java.nio.channels.{ AlreadyConnectedException, ClosedChannelException,
	 							  DatagramChannel, SelectableChannel,
	 							  SocketChannel }
import OSCChannel._

object OSCReceiver {
	/**
	 *	Creates a new instance of a revivable <code>OSCReceiver</code>, using
	 *	default codec and a specific transport protocol and port. It
	 *	uses the local machine's IP or the &quot;loopback&quot; address.
	 *	Note that the <code>port</code> specifies the
	 *	local socket (at which the receiver listens), it does not determine the
	 *	remote sockets from which messages can be received. If you want to filter
	 *	out a particular remote (or target) socket, this can be done
	 *	using the <code>setTarget</code> method!
	 *	<P>
	 *	<B>TCP</B> receivers are required
	 *	to be connected to one particular target, so <code>setTarget</code> is
	 *	must be called prior to <code>connect</code> or <code>startListening</code>! 
	 *
	 *	@param	protocol	the protocol to use, currently either <code>UDP</code> or <code>TCP</code>
	 *	@param	port		the port number for the OSC socket, or <code>0</code> to use an arbitrary free port
	 *	@param	loopBack	if <code>true</code>, the &quot;loopback&quot; address (<code>&quot;127.0.0.0.1&quot;</code>)
	 *						is used which limits communication to the local machine. If <code>false</code>, the
	 *						local machine's regular IP address is used.
	 *	
	 *	@return				the newly created receiver
	 *
	 *	@throws	IOException					if a networking error occurs while creating the socket
	 *	@throws	IllegalArgumentException	if an illegal protocol is used
	 */
	@throws( classOf[ IOException ])
	def apply( protocol: Symbol, port: Int = 0, loopBack: Boolean = false, c: OSCPacketCodec = OSCPacketCodec.default ) : OSCReceiver = {
		val localAddress = new InetSocketAddress( if( loopBack ) "127.0.0.1" else "0.0.0.0", port )
		withAddress( protocol, localAddress, c )
	}

	/**
	 *	Creates a new instance of a revivable <code>OSCReceiver</code>, using
	 *	default codec and a specific transport protocol and local socket address.
	 *	Note that <code>localAdress</code> specifies the
	 *	local socket (at which the receiver listens), it does not determine the
	 *	remote sockets from which messages can be received. If you want to filter
	 *	out a particular remote (or target) socket, this can be done
	 *	using the <code>setTarget</code> method!
	 *	<P>
	 *	<B>TCP</B> receivers are required
	 *	to be connected to one particular target, so <code>setTarget</code> is
	 *	must be called prior to <code>connect</code> or <code>startListening</code>! 
	 *
	 *	@param	protocol		the protocol to use, currently either <code>UDP</code> or <code>TCP</code>
	 *	@param	localAddress	a valid address to use for the OSC socket. If the port is <code>0</code>,
	 *							an arbitrary free port is picked when the receiver is started. (you can find out
	 *							the actual port in this case by calling <code>getLocalAddress()</code> after the
	 *							receiver was started).
	 *	
	 *	@return					the newly created receiver
	 *
	 *	@throws	IOException					if a networking error occurs while creating the socket
	 *	@throws	IllegalArgumentException	if an illegal protocol is used
	 */
	@throws( classOf[ IOException ])
	def withAddress( protocol: Symbol, localAddress: InetSocketAddress, c: OSCPacketCodec = OSCPacketCodec.default ) : OSCReceiver = {
		if( protocol == 'udp ) {
			new UDPOSCReceiver( localAddress, c )
			
//		} else if( protocol == 'tcp ) {
//			new TCPOSCReceiver( localAddress )
			
		} else {
			throw new IllegalArgumentException( ScalaOSC.getResourceString( "errUnknownProtocol" ) + protocol )
		}
	}

	/**
	 *	Creates a new instance of a non-revivable <code>OSCReceiver</code>, using
	 *	default codec and UDP transport on a given channel. The caller should ensure that
	 *	the provided channel's socket was bound to a valid address
	 *	(using <code>dch.socket().bind( SocketAddress )</code>).
	 *	Note that <code>dch</code> specifies the
	 *	local socket (at which the receiver listens), it does not determine the
	 *	remote sockets from which messages can be received. If you want to filter
	 *	out a particular remote (or target) socket, this can be done
	 *	using the <code>setTarget</code> method!
	 *
	 *	@param	dch			the <code>DatagramChannel</code> to use as UDP socket.
	 *	@return				the newly created receiver
	 *
	 *	@throws	IOException	if a networking error occurs while configuring the socket
	 */
	@throws( classOf[ IOException ])
	def withChannel( dch: DatagramChannel, c: OSCPacketCodec = OSCPacketCodec.default ) : OSCReceiver = {
		new UDPOSCReceiver( dch, c )
	}

//	/**
//	 *	Creates a new instance of a non-revivable <code>OSCReceiver</code>, using
//	 *	default codec and TCP transport on a given channel. The caller should ensure that
//	 *	the provided channel's socket was bound to a valid address
//	 *	(using <code>sch.socket().bind( SocketAddress )</code>). Furthermore,
//	 *	the channel must be connected (using <code>connect()</code>) before
//	 *	being able to receive messages. Note that <code>sch</code> specifies the
//	 *	local socket (at which the receiver listens), it does not determine the
//	 *	remote sockets from which messages can be received. The remote (or target)
//	 *	socket must be explicitly specified using <code>setTarget</code> before
//	 *	trying to connect!
//	 *
//	 *	@param	sch			the <code>SocketChannel</code> to use as TCP socket.
//	 *	@return				the newly created receiver
//	 *
//	 *	@throws	IOException	if a networking error occurs while configuring the socket
//	 */
//	@throws( classOf[ IOException ])
//	def apply( sch: SocketChannel ) : OSCReceiver = new TCPOSCReceiver( sch )

	protected def debugTimeString : String = {
		new java.text.SimpleDateFormat( "HH:mm:ss.SSS" ).format( new java.util.Date )
	}
}

abstract class OSCReceiver( val protocol: Symbol, localAddr: InetSocketAddress,
                            protected val revivable: Boolean, var codec: OSCPacketCodec )
extends OSCChannel with Runnable {
//	private val		collListeners   			= new ArrayList[ OSCListener ]
  	var				action						= (msg: OSCMessage, sender: SocketAddress, time: Long ) => ()
	protected var	thread : Thread				= null

	protected val	generalSync					= new AnyRef	// mutual exclusion startListening / stopListening
	protected val	threadSync					= new AnyRef	// communication with receiver thread

	protected var	listening					= false
	
	private val		bufSync						= new AnyRef	// buffer (re)allocation
	private var		bufSize						= DEFAULTBUFSIZE
	protected var	byteBuf : ByteBuffer		= null

	protected var	tgt : SocketAddress			= null

//	/**
//	 *	Queries the receiver's local socket address.
//	 *	You can determine the host and port from the returned address
//	 *	by calling <code>getHostName()</code> (or for the IP <code>getAddress().getHostAddress()</code>)
//	 *	and <code>getPort()</code>. This port number may be <code>0</code>
//	 *	if the receiver was called with an unspecified port and has not yet been
//	 *	started. In this case, to determine the port actually used, call this
//	 *	method after the receiver has been started.
//	 *	
//	 *	@return				the address of the receiver's local socket.
//	 *
//	 *	@see	java.net.InetSocketAddress#getHostName()
//	 *	@see	java.net.InetSocketAddress#getAddress()
//	 *	@see	java.net.InetSocketAddress#getPort()
//	 */
//	def getLocalAddress : InetSocketAddress

//	var   codec						= OSCPacketCodec.default

//	def getProtocol = protocol

	/**
	 *	Queries the receiver's local socket address.
	 *	You can determine the host and port from the returned address
	 *	by calling <code>getHostName()</code> (or for the IP <code>getAddress().getHostAddress()</code>)
	 *	and <code>getPort()</code>. This port number may be <code>0</code>
	 *	if the receiver was called with an unspecified port and has not yet been
	 *	started. In this case, to determine the port actually used, call this
	 *	method after the receiver has been started.
	 *	
	 *	@return				the address of the receiver's local socket.
	 *
	 *	@see	java.net.InetSocketAddress#getHostName()
	 *	@see	java.net.InetSocketAddress#getAddress()
	 *	@see	java.net.InetSocketAddress#getPort()
	 */
//	def getLocalAddress : InetSocketAddress
	def localAddress : InetSocketAddress

//	def setTarget( target: SocketAddress ) : Unit
	def target: SocketAddress = tgt
	def target_=( t: SocketAddress ) : Unit
	
//	def decoder_=( dec: OSCPacketCodec ) {
//		decoder = dec
//	}
	
//	def getDecoder : OSCPacketCodec = decoder

//	/**
//	 *  Registers a listener that gets informed
//	 *  about incoming messages. You can call this
//	 *  both when listening was started and stopped.
//	 *
//	 *  @param  listener	the listener to register
//	 */
//	def addOSCListener( listener: OSCListener ) {
//		collListeners.synchronized {
//			collListeners.add( listener )
//		}
//	}
//
//	/**
//	 *  Unregisters a listener that gets informed
//	 *  about incoming messages
//	 *
//	 *  @param  listener	the listener to remove from
//	 *						the list of notified objects.
//	 */
//	def removeOSCListener( listener: OSCListener ) {
//		collListeners.synchronized {
//			collListeners.remove( listener )
//		}
//	}

	/**
	 *  Starts to wait for incoming messages.
	 *	See the class constructor description to learn how
	 *	connected and unconnected channels are handled.
	 *	You should never modify the
	 *	the channel's setup between the constructor and calling
	 *	<code>startListening</code>. This method will check
	 *	the connection status of the channel, using <code>isConnected</code>
	 *	and establish the connection if necessary. Therefore,
	 *	calling <code>connect</code> prior to <code>startListening</code>
	 *	is not necessary.
	 *	<p>
	 *	To find out at which port we are listening, call
	 *	<code>getLocalAddress().getPort()</code>.
	 *	<p>
	 *	If the <code>OSCReceiver</code> is already listening,
	 *	this method does nothing.
     *
     *  @throws IOException when an error occurs
     *          while establishing the channel connection.
     *          In that case, no thread has been started
     *          and hence stopListening() needn't be called
	 *
	 *	@throws	IllegalStateException	when trying to call this method from within the OSC receiver thread
	 *									(which would obviously cause a loop)
	 */
	@throws( classOf[ IOException ])
	def start {
		generalSync.synchronized {
			if( Thread.currentThread == thread ) throw new IllegalStateException( ScalaOSC.getResourceString( "errNotInThisThread" ))

			if( listening && ((thread == null) || !thread.isAlive) ) {
				listening		= false
			}
			if( !listening ) {
				if( !isConnected ) connect
				listening		= true
				thread			= new Thread( this, "OSCReceiver" )
				thread.setDaemon( true )
				thread.start
			}
		}
	}

	/**
	 *	Queries whether the <code>OSCReceiver</code> is
	 *	listening or not.
	 */
	def isActive : Boolean = {
        generalSync.synchronized {
			listening
		}
	}
	
	/**
	 *  Stops waiting for incoming messages. This
	 *	method returns when the receiving thread has terminated.
     *  To prevent deadlocks, this method cancels after
     *  five seconds, calling <code>close()</code> on the datagram
	 *	channel, which causes the listening thread to die because
	 *	of a channel-closing exception.
     *
     *  @throws IOException if an error occurs while shutting down
	 *
	 *	@throws	IllegalStateException	when trying to call this method from within the OSC receiver thread
	 *									(which would obviously cause a loop)
	 */
	@throws( classOf[ IOException ])
	def stop {
        generalSync.synchronized {
			if( Thread.currentThread == thread ) throw new IllegalStateException( ScalaOSC.getResourceString( "errNotInThisThread" ))

			if( listening ) {
				listening = false
				if( (thread != null) && thread.isAlive ) {
					try {
						threadSync.synchronized {
							sendGuardSignal
							threadSync.wait( 5000 )
						}
					}
					catch { case e2: InterruptedException =>
						e2.printStackTrace
					}
					finally {
						if( (thread != null) && thread.isAlive ) {
							try {
								System.err.println( "OSCReceiver.stopListening : rude task killing (" + this.hashCode + ")" )
								closeChannel
							}
							catch { case e3: IOException =>
								e3.printStackTrace
							}
						}
						thread = null
					}
				}
			}
		}
	}

	def bufferSize_=( size: Int ) {
		bufSync.synchronized {
			if( listening ) throw new IllegalStateException( ScalaOSC.getResourceString( "errNotWhileActive" ))
			bufSize	= size
		}
	}

	def bufferSize : Int = {
		bufSync.synchronized {
			bufSize
		}
	}

	def dispose {
		try {
			stop
		}
		catch { case e1: IOException =>
			e1.printStackTrace
		}
		try {
			closeChannel
		}
		catch { case e1: IOException =>
			e1.printStackTrace
		}
//		collListeners.clear
		byteBuf	= null
	}
	
	@throws( classOf[ IOException ])
	protected def sendGuardSignal : Unit
	
	@throws( classOf[ IOException ])
//	protected def channel_=( ch: SelectableChannel ) : Unit
// XXX just to make it compile
	private[ scalaosc ] def channel_=( ch: SelectableChannel ) : Unit

	@throws( classOf[ IOException ])
	protected def closeChannel : Unit

	@throws( classOf[ IOException ])
	protected def flipDecodeDispatch( sender: SocketAddress ) {
		try {
			byteBuf.flip
			val p = codec.decode( byteBuf )
			
			if( dumpMode != DUMP_OFF ) {
				printStream.print( "r: " )
				if( (dumpMode & DUMP_TEXT) != 0 ) OSCPacket.printTextOn( codec, printStream, p )
				if( (dumpMode & DUMP_HEX)  != 0 ) {
					byteBuf.flip
					OSCPacket.printHexOn( printStream, byteBuf )
				}
			}
			dispatchPacket( p, sender, OSCBundle.NOW )	// OSCBundles will override this dummy time tag
		}
		catch { case e1: BufferUnderflowException =>
			if( listening ) {
				System.err.println( new OSCException( OSCException.RECEIVE, e1.toString ))
			}
		}
	}

	private def dispatchPacket( p: OSCPacket, sender: SocketAddress, time: Long ) {
		if( p.isInstanceOf[ OSCMessage ]) {
			dispatchMessage( p.asInstanceOf[ OSCMessage ], sender, time )
		} else
//		if( p.isInstanceOf[ OSCBundle ])
		{
			val bndl	= p.asInstanceOf[ OSCBundle ]
			val time2	= bndl.timetag
			bndl.foreach( dispatchPacket( _, sender, time2 ))
//		} else {
//			assert false : p.getClass().getName();
		}
	}

	private def dispatchMessage( msg: OSCMessage, sender: SocketAddress, time: Long ) {
//		generalSync.synchronized {
//			if( action != null ) {
				action.apply( msg, sender, time )
//			}
//		}
	}
	
	protected def checkBuffer {
		bufSync.synchronized {
			if( (byteBuf == null) || (byteBuf.capacity != bufSize) ) {
				byteBuf	= ByteBuffer.allocateDirect( bufSize )
			}
//			allocBuf = false;
		}
	}

	@throws( classOf[ UnknownHostException ])
	protected def getLocalAddress( addr: InetAddress, port: Int ) : InetSocketAddress = {
		new InetSocketAddress( if( addr.getHostName == "0.0.0.0" ) InetAddress.getLocalHost else addr, port )
	}

	/**
	 *	Establishes connection for transports requiring
	 *	connectivity (e.g. TCP). For transports that do not require connectivity (e.g. UDP),
	 *	this ensures the communication channel is created and bound.
	 *  <P>
	 *  Having a connected channel without actually listening to incoming messages
	 *  is usually not making sense. You can call <code>startListening</code> without
	 *  explicit prior call to <code>connect</code>, because <code>startListening</code>
	 *  will establish the connection if necessary.
	 *  <P>
	 *	When a <B>UDP</B> transmitter
	 *	is created without an explicit <code>DatagramChannel</code> &ndash; say by
	 *	calling <code>OSCReceiver.newUsing( &quot;udp&quot; )</code>, calling
	 *	<code>connect()</code> will actually create and bind a <code>DatagramChannel</code>.
	 *	For a <B>UDP</B> receiver which was created with an explicit
	 *	<code>DatagramChannel</code>. However, for <B>TCP</B> receivers, 
	 *	this may throw an <code>IOException</code> if the receiver
	 *	was already connected, therefore be sure to check <code>isConnected()</code> before.
	 *	
	 *	@throws	IOException	if a networking error occurs. Possible reasons: - the underlying
	 *						network channel had been closed by the server. - the transport
	 *						is TCP and the server is not available.
	 *
	 *	@see	#isConnected()
	 *	@see	#startListening()
	 *	@throws IOException
	 */
	@throws( classOf[ IOException ])
	def connect : Unit
	
	/**
	 *	Queries the connection state of the receiver.
	 *
	 *	@return	<code>true</code> if the receiver is connected, <code>false</code> otherwise. For transports that do not use
	 *			connectivity (e.g. UDP) this returns <code>false</code>, if the
	 *			underlying <code>DatagramChannel</code> has not yet been created.
	 *
	 *	@see	#connect()
	 */
	def isConnected : Boolean
}

private class UDPOSCReceiver( addr: InetSocketAddress, private var dch: DatagramChannel, c: OSCPacketCodec )
extends OSCReceiver( 'udp, addr, dch == null, c ) {
	def this( localAddress: InetSocketAddress, c: OSCPacketCodec ) = this( localAddress, null, c )
	def this( dch: DatagramChannel, c: OSCPacketCodec ) {
		this( new InetSocketAddress( dch.socket.getLocalAddress, dch.socket.getLocalPort ), dch, c )
	}
			
//	def codec : OSCPacketCodec = c
//	def codec_=( cdc: OSCPacketCodec ) {
//		c = cdc
//	}
	
	@throws( classOf[ IOException ])
	private[ scalaosc ] def channel_=( ch: SelectableChannel ) {
		generalSync.synchronized {
			if( listening ) throw new IllegalStateException( ScalaOSC.getResourceString( "errNotWhileActive" ))
		
			val dchTmp	= ch.asInstanceOf[ DatagramChannel ]
			if( !dchTmp.isBlocking ) {
				dchTmp.configureBlocking( true )
			}
			if( dchTmp.isConnected ) throw new IllegalStateException( ScalaOSC.getResourceString( "errChannelConnected" ))
			dch = dchTmp
		}
	}
		
	def localAddress : InetSocketAddress = {
		generalSync.synchronized {
			if( dch != null ) {
				val ds = dch.socket
				getLocalAddress( ds.getLocalAddress, ds.getLocalPort )
			} else {
				getLocalAddress( addr.getAddress, addr.getPort )
			}
		}
	}

	def target_=( t: SocketAddress ) {
		tgt = t
	}
		
	@throws( classOf[ IOException ])
	def connect {
		generalSync.synchronized {
			if( listening ) throw new IllegalStateException( ScalaOSC.getResourceString( "errNotWhileActive" ))

			if( (dch != null) && !dch.isOpen ) {
				if( !revivable ) throw new IOException( ScalaOSC.getResourceString( "errCannotRevive" ))
				dch = null
			}
			if( dch == null ) {
				val newCh = DatagramChannel.open
				newCh.socket.bind( localAddress )
//				channel = newCh
				channel_=( newCh )
			}
		}
	}

	def isConnected : Boolean = {
		generalSync.synchronized {
			(dch != null) && dch.isOpen
		}
	}

	@throws( classOf[ IOException ])
	protected def closeChannel {
		if( dch != null ) {
			try {
				dch.close
			}
			finally {
				dch = null
			}
		}
	}

	/**
	 *	This is the body of the listening thread
	 */
	def run {
		checkBuffer

		try {
			while( listening ) {
				try {
					byteBuf.clear
//println( "in run : " + dch )
					val sender = dch.receive( byteBuf )

					if( listening && (sender != null) &&
						((tgt == null) || tgt.equals( sender ))) {
						
						flipDecodeDispatch( sender )
					}
				}
				catch {
					case e1: ClosedChannelException => {	// bye bye, we have to quit
						if( listening ) {
							System.err.println( "OSCReceiver.run : " + e1.getClass.getName + " : " + e1.getLocalizedMessage )
						}
						return
					  }
					  case e2: IOException =>
						if( listening ) {
							System.err.println( "OSCReceiver.run : " + e2.getClass.getName + " : " + e2.getLocalizedMessage )
						}
				}
			} // while( listening )
		}
		finally {
			threadSync.synchronized {
				thread = null
				threadSync.notifyAll   // stopListening() might be waiting
			}
		}
	}
		
	@throws( classOf[ IOException ])
	protected def sendGuardSignal {
		val guard		= new DatagramSocket
		val guardPacket	= new DatagramPacket( new Array[ Byte ]( 0 ), 0 )
		guardPacket.setSocketAddress( localAddress )
		guard.send( guardPacket )
		guard.close
	}
}
