package edu.umro.EventNetClient

/**
  * An event that is originating from this client and is responding to another
  * event.  The other event may or may not have originated on this client.
  */
abstract class RespondingEvent(inResponseTo: String, agentIdentification: AgentIdentification) extends Event {

  override val header: Header = new Header(inResponseTo, agentIdentification)

}
