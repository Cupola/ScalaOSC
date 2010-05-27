package de.sciss.scalaosc

sealed abstract class OSCTransport( val name: String )
case object UDP extends OSCTransport( "UDP" )
case object TCP extends OSCTransport( "TCP" )