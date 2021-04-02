package edu.umro.EventNetClient

/**
  * An event that is originating from this client but is not
  * responding to any previous event.
  */
abstract class OriginatingEvent(agentIdentification: AgentIdentification) extends Event {

  val header: Header = new Header(agentIdentification)

}
