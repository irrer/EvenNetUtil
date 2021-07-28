/*
 * Copyright 2021 Regents of the University of Michigan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.umro.EventNetClient

import edu.umro.util.OpSys

import java.util.Date
import scala.xml.{Elem, NodeSeq}

class Header(
    val AgentId: String,
    val AgentName: String,
    val EventId: String,
    val EventDateTime: Date,
    val EventSourceName: String,
    val InResponseTo: String,
    val IpAddress: String,
    val ProcessName: String,
    val KeyValue: Option[HeaderKeyValue]
) {

  def this(document: Elem) =
    this(
      Header.docHeader(document, "AgentId"),
      Header.docHeader(document, "AgentName"),
      Header.docHeader(document, "EventId"),
      Header.parseStandardDateSafely(Header.docHeader(document, "EventDateTime")),
      Header.docHeader(document, "EventSourceName"),
      Header.docHeader(document, "InResponseTo"),
      Header.docHeader(document, "IpAddress"),
      Header.processName,
      Header.docHeaderKeyValue(document)
    )

  def this(inResponseTo: String, agentIdentification: AgentIdentification) =
    this(agentIdentification.AgentId, agentIdentification.AgentName, Util.makeUID, new Date, agentIdentification.AgentName, inResponseTo, OpSys.getHostIPAddress, Header.processName, null)

  def this(agentIdentification: AgentIdentification) = this("", agentIdentification)

  private def xmlKey = {
    if ((KeyValue != null) && KeyValue.isDefined)
      <KeyName>{KeyValue.get.key}</KeyName>
    else
      NodeSeq.Empty
  }

  private def xmlValue = {
    if ((KeyValue != null) && KeyValue.isDefined)
      <KeyValue>{KeyValue.get.value}</KeyValue>
    else
      NodeSeq.Empty
  }

  val xml: Elem = {
    <Header>
            <AgentId>{AgentId}</AgentId>
            <AgentName>{AgentName}</AgentName>
            <EventId>{EventId}</EventId>
            <EventDateTime>{Util.dateToText(EventDateTime)}</EventDateTime>
            <EventSourceName>{EventSourceName}</EventSourceName>
            <InResponseTo>{InResponseTo}</InResponseTo>
            <IpAddress>{IpAddress}</IpAddress>
            <ProcessName>{ProcessName}</ProcessName>
            {xmlKey}
            {xmlValue}
        </Header>
  }

  override def toString: String = Util.xmlToText(xml)
}

object Header {
  def docError(element: String, e: Throwable): String = {
    throw new RuntimeException(this.getClass.getName + " Unable to get element " + element + " from Event: " + e)
    "NA"
  }

  def parseStandardDateSafely(text: String): Date = {
    try {
      Util.textToDate(text)
    } catch {
      case _: Exception => new Date(0)
    }
  }

  def doc(document: Elem, name: String): String =
    try { (document \ name).head.text }
    catch { case e: Exception => docError(name, e) }

  def docHeader(document: Elem, name: String): String =
    try { (document \ "Header" \ name).head.text }
    catch { case e: Exception => docError("Header/" + name, e) }

  def processName: String = System.getProperty("sun.java.command")

  def docHeaderKeyValue(document: Elem): Option[HeaderKeyValue] = {
    try {
      Some(new HeaderKeyValue(docHeader(document, "KeyName"), docHeader(document, "KeyValue")))
    } catch {
      case _: Exception => None
    }
  }

}
