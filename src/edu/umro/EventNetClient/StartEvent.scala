package edu.umro.EventNetClient

import scala.xml.Elem

class StartEvent(Status: String, agentIdentification: AgentIdentification) extends OriginatingEvent(agentIdentification) {

    override val xml: Elem = {
        <StartEvent xmlns="urn:StartEvent">
            <Status>{ Status }</Status>
            { header.xml }
        </StartEvent>
    }

}