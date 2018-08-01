package edu.umro.EventNetClient

import scala.xml.Elem
import java.io.File
import scala.xml.XML
import java.lang.Class
import java.text.SimpleDateFormat
import java.util.Date
import java.util.logging.Logger
import edu.umro.util.OpSys
import scala.xml.NodeSeq

class Header(
    val AgentId: String,
    val AgentName: String,
    val EventId: String,
    val EventDateTime: Date,
    val EventSourceName: String,
    val InResponseTo: String,
    val IpAddress: String,
    val ProcessName: String,
    val KeyValue: Option[HeaderKeyValue]) {

    def this(document: Elem) = this(
        Header.docHeader(document, "AgentId"),
        Header.docHeader(document, "AgentName"),
        Header.docHeader(document, "EventId"),
        Header.parseStandardDateSafely(Header.docHeader(document, "EventDateTime")),
        Header.docHeader(document, "EventSourceName"),
        Header.docHeader(document, "InResponseTo"),
        Header.docHeader(document, "IpAddress"),
        Header.processName,
        Header.docHeaderKeyValue(document))

    def this(inResponseTo: String, agentIdentification: AgentIdentification) = this(
        agentIdentification.AgentId,
        agentIdentification.AgentName,
        Util.makeUID,
        new Date,
        agentIdentification.AgentName,
        inResponseTo,
        OpSys.getHostIPAddress,
        Header.processName,
        null)

    def this(agentIdentification: AgentIdentification) = this("", agentIdentification)

    private val log = Logger.getLogger(this.getClass.getName)

    private def xmlKey = {
        if ((KeyValue != null) && (KeyValue.isDefined))
            <KeyName>{ KeyValue.get.key }</KeyName>
        else
            NodeSeq.Empty
    }

    private def xmlValue = {
        if ((KeyValue != null) && (KeyValue.isDefined))
            <KeyValue>{ KeyValue.get.value }</KeyValue>
        else
            NodeSeq.Empty
    }

    val xml: Elem = {
        <Header>
            <AgentId>{ AgentId }</AgentId>
            <AgentName>{ AgentName }</AgentName>
            <EventId>{ EventId }</EventId>
            <EventDateTime>{ Util.dateToText(EventDateTime) }</EventDateTime>
            <EventSourceName>{ EventSourceName }</EventSourceName>
            <InResponseTo>{ InResponseTo }</InResponseTo>
            <IpAddress>{ IpAddress }</IpAddress>
            <ProcessName>{ ProcessName }</ProcessName>
            { xmlKey }
            { xmlValue }
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
        }
        catch {
            case e: Exception => new Date(0)
        }
    }

    def doc(document: Elem, name: String): String = try { (document \ name).head.text } catch { case e: Exception => docError(name, e) }

    def docHeader(document: Elem, name: String): String = try { (document \ "Header" \ name).head.text } catch { case e: Exception => docError("Header/" + name, e) }

    def processName = System.getProperty("sun.java.command")

    def docHeaderKeyValue(document: Elem): Option[HeaderKeyValue] = {
        try {
            Some(new HeaderKeyValue(docHeader(document, "KeyName"), docHeader(document, "KeyValue")))
        }
        catch {
            case e: Exception => None
        }
    }

}