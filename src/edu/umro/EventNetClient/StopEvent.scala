package edu.umro.EventNetClient

import scala.xml.Elem

class StopEvent(Status: String, inResponseTo: String, agentIdentification: AgentIdentification) extends RespondingEvent(inResponseTo, agentIdentification) {

  override val xml: Elem = {
    <StopEvent xmlns="urn:StopEvent">
            <Status>{Status}</Status>
            {header.xml}
        </StopEvent>
  }

}
