package edu.umro.EventNetClient

import scala.xml.Elem

class PingEvent(text: String) extends IncomingEvent(text) {

    val Status = doc("Status")
    
    override val xml: Elem = {
        <PingEvent xmlns="urn:PingEvent">
            <Status>{ Status }</Status>
            { header.xml }
        </PingEvent>
    }

}