/*
 *  ScalaOSC.scala
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

import java.util.{ MissingResourceException, ResourceBundle }

object ScalaOSC {
	private def VERSION		= 0.13
	private def resBundle	= ResourceBundle.getBundle( "ScalaOSCStrings" )
 
	def main( args: Array[ String ]) {
		var testo = false
	
		if( args.length == 1 ) {
		  args( 0 ) match {
		    case "--testTransmitter" => {
				testo	= true
				Test.transmitter
			}
		    case "--testReceiver" => {
		    	testo = true
		    	Test.receiver
		    }
		    case "--runChecks" => {
		    	testo = true
		    	Test.codec
		    }
		  }
		}

		if( !testo ) {
			System.err.println( "\nScalaOSC v" + VERSION + "\n" +
				getCopyrightString + "\n\n" +
				getCreditsString + "\n\n  " +
				getResourceString( "errIsALibrary" ))

			System.out.println( "\nThe following demos are available:\n" +
				"  --testTransmitter\n"
			)
			System.exit( 1 )
		}
	}
 
 	/**
	 *	Returns the library's version.
	 *
	 *	@return	the current version of NetUtil
	 */
	def getVersion = VERSION

	/**
	 *	Returns a copyright information string
	 *	about the library
	 *
	 *	@return	text string which can be displayed
	 *			in an about box
	 */
	def getCopyrightString = getResourceString( "copyright" )

	/**
	 *	Returns a license and website information string
	 *	about the library
	 *
	 *	@return	text string which can be displayed
	 *			in an about box
	 */
	def getCreditsString = getResourceString( "credits" )

	/**
	 *	Returns a string from the library's string
	 *	resource bundle (currently localized
	 *	english and german). This is used by the
	 *	classes of the library, you shouldn't use
	 *	it yourself.
	 *
	 *	@param	key	lookup dictionary key
	 *	@return	(localized) human readable string for
	 *			the given key or placeholder string if
	 *			the resource wasn't found
	 */
	def getResourceString( key: String ) : String = {
		try {
			resBundle.getString( key )
		}
		catch { case e: MissingResourceException =>
			"[Missing Resource: " + key + "]"
		}
	}
}