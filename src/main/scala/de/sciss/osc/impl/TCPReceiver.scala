/*
 *  TCPReceiver.scala
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

package de.sciss.osc.impl

import java.io.IOException
import java.net.{SocketAddress, InetSocketAddress}
import de.sciss.osc.{OSCException, TCP, OSCReceiver, OSCPacketCodec}
import java.nio.channels.{ClosedChannelException, AlreadyConnectedException, SelectableChannel, SocketChannel}

class TCPReceiver private( _localAddress: InetSocketAddress, private var sch: SocketChannel, _c: OSCPacketCodec )
extends OSCReceiver( TCP, _localAddress, sch == null, _c ) {
   def this( localAddress: InetSocketAddress, c: OSCPacketCodec ) =
      this( localAddress, null, c )

   def this( sch: SocketChannel, c: OSCPacketCodec ) =
      this( new InetSocketAddress( sch.socket().getLocalAddress(), sch.socket().getLocalPort() ), sch, c )

   @throws( classOf[ IOException ])
   private[ osc ] def channel_=( ch: SelectableChannel ) {
      generalSync.synchronized {
         if( listening ) throw new IllegalStateException( "Cannot be called while receiver is active" )

         sch	= ch.asInstanceOf[ SocketChannel ]
         if( !sch.isBlocking() ) {
            sch.configureBlocking( true )
         }
      }
   }
   private[ osc ] def channel : SelectableChannel = sch

   def localAddress : InetSocketAddress = {
      generalSync.synchronized {
         if( sch != null ) {
            val s = sch.socket()
            getLocalAddress( s.getLocalAddress(), s.getLocalPort() )
         } else {
            getLocalAddress( addr.getAddress(), addr.getPort() )
         }
      }
   }

   def target_=( t: SocketAddress ) {
      generalSync.synchronized {
         if( isConnected ) throw new AlreadyConnectedException()
         tgt = t
      }
   }

   @throws( classOf[ IOException ])
   def connect {
      generalSync.synchronized {
         if( listening ) throw new IllegalStateException( "Cannot be called while receiver is active" )

         if( (sch != null) && !sch.isOpen() ) {
            if( !revivable ) throw new IOException( "Channel cannot be revived" )
            sch = null
         }
         if( sch == null ) {
            val newCh = SocketChannel.open()
            newCh.socket().bind( localAddress )
            sch = newCh
         }
         if( !sch.isConnected() ) {
            sch.connect( target )
         }
      }
   }

   def isConnected : Boolean = {
      generalSync.synchronized {
         (sch != null) && sch.isConnected()
      }
   }

   @throws( classOf[ IOException ])
   protected def closeChannel {
      if( sch != null ) {
         try {
            sch.close()
         }
         finally {
            sch = null
         }
      }
   }

   def run {
      val sender = sch.socket().getRemoteSocketAddress()
      checkBuffer

      try {
   		while( listening ) {
            try {
               byteBuf.rewind().limit( 4 )	// in TCP mode, first four bytes are packet size in bytes
               do {
                  val len = sch.read( byteBuf )
                  if( len == -1 ) return
               } while( byteBuf.hasRemaining() )

               byteBuf.rewind()
               val packetSize = byteBuf.getInt()
               byteBuf.rewind().limit( packetSize )

               while( byteBuf.hasRemaining() ) {
                  val len = sch.read( byteBuf )
                  if( len == -1 ) return
               }

               flipDecodeDispatch( sender )
            }
            catch {
               case e1: IllegalArgumentException =>	// thrown on illegal byteBuf.limit() calls
                  if( listening ) {
                     val e2 = new OSCException( OSCException.RECEIVE, e1.toString() )
                     System.err.println( "OSCReceiver.run : " + e2.getClass().getName() + " : " + e2.getLocalizedMessage() )
                  }
               case e1: ClosedChannelException =>	// bye bye, we have to quit
                  if( listening ) {
                     System.err.println( "OSCReceiver.run : " + e1.getClass().getName() + " : " + e1.getLocalizedMessage() );
                  }
               case e1: IOException =>
                  if( listening ) {
                     System.err.println( "OSCReceiver.run : " + e1.getClass().getName() + " : " + e1.getLocalizedMessage() );
                  }
            }
         }
      }
      finally {
         threadSync.synchronized {
            thread = null
            threadSync.notifyAll()   // stopListening() might be waiting
         }
      }
   }

   /**
    *	@warning	this calls socket().shutdownInput()
    *				to unblock the listening thread. unfortunately this
    *				cannot be undone, so it's not possible to revive the
    *				receiver in TCP mode ;-( have to check for alternative ways
    */
   @throws( classOf[ IOException ])
   protected def sendGuardSignal {
      sch.socket().shutdownInput()
   }
}