package edu.umro.EventNetClient

import scala.xml.Elem

abstract class Event {

    val header: Header

    def xml: Elem
    
    def eventName = xml.label

    def toText: String = Util.xmlToText(xml)
    
    override def toString = toText

}