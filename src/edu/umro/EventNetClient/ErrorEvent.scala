package edu.umro.EventNetClient

import scala.xml.Elem

class ErrorEvent(
    val Context: String,
    val ShortExceptionInfo: String,
    val ServiceNameInError: String,
    val ServiceIdInError: String,
    val ExceptionInfo: String,
    agentIdentification: AgentIdentification) extends OriginatingEvent(agentIdentification) {

    override val xml: Elem = {
        <ErrorEvent xmlns="urn:ErrorEvent">
            <Context>{ Context }</Context>
            <ShortExceptionInfo>{ ShortExceptionInfo }</ShortExceptionInfo>
            <ServiceNameInError>{ ServiceNameInError }</ServiceNameInError>
            <ServiceIdInError>{ ServiceIdInError }</ServiceIdInError>
            <ExceptionInfo>{ ExceptionInfo }</ExceptionInfo>
            { header.xml }
        </ErrorEvent>
    }

}