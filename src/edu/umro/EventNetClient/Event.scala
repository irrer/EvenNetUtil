package edu.umro.EventNetClient

import scala.xml.Elem

abstract class Event {

  val header: Header

  def xml: Elem

  def eventName: String = xml.label

  def toText: String = Util.xmlToText(xml)

  override def toString: String = toText

}
