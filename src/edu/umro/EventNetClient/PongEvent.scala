package edu.umro.EventNetClient

import scala.xml.Elem

class PongEvent(Status: String, inResponseTo: String, agentIdentification: AgentIdentification) extends RespondingEvent(inResponseTo, agentIdentification) {

    override val xml: Elem = {
        <PongEvent xmlns="urn:PongEvent">
            <Status>{ Status }</Status>
            { header.xml }
        </PongEvent>
    }

}