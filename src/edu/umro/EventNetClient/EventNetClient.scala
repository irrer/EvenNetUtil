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

import java.util.logging.Logger

class EventNetClient(val config: EventNetClientConfig, serviceName: String, channelLimit: Int, restartDelayMs: Long) {

  private val log = Logger.getLogger(this.getClass.getName)

  private val EventNetExchangeType = "topic"

  val agentIdentification = new AgentIdentification(serviceName)

  private val GoodPongStatus = "respond to ping"

  private val eventUtil = new EventUtil(config, channelLimit, restartDelayMs)

  private def pingPong(): Unit = {
    def respondToPing(message: Array[Byte]): Unit = {
      val pingEvent = new PingEvent(new String(message))
      log.finer("Received ping:\n" + pingEvent)
      val pongEvent = new PongEvent(GoodPongStatus, pingEvent.header.EventId, agentIdentification)
      eventUtil.sendAdminEvent(pongEvent)
    }

    val EventNetAdminPingRoutingKey = config.RoutingKeyPrefix + "PingEvent"
    eventUtil.consumeNonDurable(config.AdminExchange, EventNetAdminPingRoutingKey, respondToPing)
  }

  private def initClient(): Unit = {
    val startEvent = new StartEvent("Starting agent " + serviceName, agentIdentification)
    log.info("startEvent:\n" + startEvent)
    def setupShutdown() {
      class Shutdown extends Runnable {
        override def run(): Unit = {
          val stopEvent = new StopEvent("shutdown", startEvent.header.EventId, agentIdentification)
          eventUtil.sendAdminEvent(stopEvent)
          log.info("Sent EventNet stop event:\n" + stopEvent)
        }
      }
      Runtime.getRuntime.addShutdownHook(new Thread(new Shutdown))
    }

    setupShutdown()
    pingPong()
    eventUtil.sendAdminEvent(startEvent)
  }

  /**
    * Send an error to the EventNet monitor.
    */
  def sendErrorEvent(Context: String, ShortExceptionInfo: String, ServiceNameInError: String, ServiceIdInError: String, ExceptionInfo: String): Unit = {

    val errorEvent = new ErrorEvent(Context, ShortExceptionInfo, ServiceNameInError, ServiceIdInError, ExceptionInfo, agentIdentification)

    eventUtil.sendAdminEvent(errorEvent)
  }

  def consumeDurable(queueName: String, routingKey: String, processing: Array[Byte] => Unit): Unit =
    eventUtil.consumeDurable(queueName, routingKey, processing)

  def consumeNonDurable(exchange: String, routingKey: String, processing: Array[Byte] => Unit): Unit =
    eventUtil.consumeNonDurable(exchange, routingKey, processing)

  /** Send an event on the given exchange with the given routing key. */
  def sendEvent(exchangeName: String, routingKey: String, message: Array[Byte]): Unit = {
    eventUtil.performSafelyUsingChannel(channel => channel.basicPublish(exchangeName, routingKey, null, message))
  }

  /**
    * Send an error to the EventNet monitor indicating that the error originated from this server.
    */
  def sendErrorEvent(Context: String, ShortExceptionInfo: String, ExceptionInfo: String): Unit = {
    sendErrorEvent(Context, ShortExceptionInfo, agentIdentification.AgentName, agentIdentification.AgentId, ExceptionInfo)
  }

  log.fine("Using configuration: " + config)

  // Create the common EventNet exchange.  If it already exists, then this will have no effect.
  eventUtil.performSafelyUsingChannel(channel => channel.exchangeDeclare(config.AdminExchange, EventNetExchangeType, true))

  initClient()
}

object EventNetClient {

  def main(args: Array[String]): Unit = {

    val prop = System.getProperty("sun.java.command")
    println(prop.replace(',', '\n'))

    if (false) {
      val env = System.getenv
      val keyList = env.keySet.toArray.toList.map(k => k.asInstanceOf[String])
      keyList.foreach(k => println("env " + k + " : " + System.getenv.get(k)))
      System.exit(99)
    }

    val config = EventNetClientConfig.construct("localhost", 5672)
    //val config = EventNetClientConfig.construct

    val enc = new EventNetClient(config, "JVMEventNetClientTest", 10, 1 * 60 * 1000)

    def procEventPlanApproval(message: Array[Byte]): Unit = {
      println("procEventPlanApproval received:\n" + new String(message))
    }

    enc.consumeDurable("Aria.Event.MobiusControl", "Aria.Event.EventPlanApproval", procEventPlanApproval)

    println("Sleeping ...")
    val stopTime = System.currentTimeMillis + (1 * 60 * 1000)
    while (System.currentTimeMillis < stopTime) {
      Thread.sleep(1000)
      val remainingTime = (stopTime - System.currentTimeMillis) / 1000.0
      println("Remaining time in sec: " + remainingTime)
      if (remainingTime.toInt == 10) {
        println("Sending test error")
        enc.sendErrorEvent("Testing", "Testing ErrorEvent", "No need for concern, just testing, move along...")
      }
    }
    println("Exiting")
    System.exit(0)
  }

}
