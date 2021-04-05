package edu.umro.EventNetClient

import scala.xml.{Elem, XML}

abstract class IncomingEvent(text: String) extends Event {

  protected val doc: Elem = XML.loadString(text)

  override val header = new Header(doc)

  protected def docError(element: String, e: Throwable): String = {
    throw new RuntimeException("Unable to get element " + element + " from Event: " + e)
    "NA"
  }

  //noinspection SameParameterValue
  protected def doc(name: String): String =
    try { (doc \ name).head.text }
    catch { case e: Exception => docError(name, e) }

}
