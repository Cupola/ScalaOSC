package de.sciss.osc

sealed abstract class OSCTransport( val name: String )
case object UDP extends OSCTransport( "UDP" )
case object TCP extends OSCTransport( "TCP" )