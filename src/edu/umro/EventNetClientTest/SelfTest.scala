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

package edu.umro.EventNetClientTest

import edu.umro.EventNetClient.{EventNetClient, EventNetClientConfig}

/**
  * Perform a test in a production environment where a temporary service is set up that
  * consumes all messages and maintains the ping+pong overhead to EventNet.
  *
  * There is code that will send an event, but it is not well formed XML.  A good way
  * to test is to do a Winston-Lutz recalculation that will send a Aria.Event.EventWLQASRSDone
  * event for this to consume.
  */
object SelfTest {

  val exchangeName = "gbtopic"

  val defaultSendRoutingKey = "Aria.Event.EventWLQASRSDone"

  def main(args: Array[String]): Unit = {
    val exchangeName = "gbtopic"
    println("Usage: ")
    println("java -cp EventNetClient_2.12-0.0.4-jar-with-dependencies.jar edu.umro.EventNetClientTest.SelfTest [exchange, such as Aria.Event.EventWLQASRSDone]")
    println("exchange: " + exchangeName)

    val routingKey: Option[String] = args.headOption

    if (routingKey.isDefined)
      println("Sending messages with routing key: " + routingKey.get)
    else
      println("No routing key given, so no messages will be sent.")

    println("starting ...")

    val broker = "localhost"
    val port = 5672
    val config = EventNetClientConfig.construct(broker, port)

    def processing(msg: Array[Byte]): Unit = {
      val line = "\n-------------------------------------------------------\n"
      println(line + "Received:\n" + new String(msg) + line)
    }

    println("broker       : " + broker)
    println("port         : " + port)
    println("exchangeName : " + exchangeName)
    println("routingKey   : " + routingKey)

    println("config     : " + config)
    val enc = new EventNetClient(config, "EventNetClient.SelfTest", 10, 1 * 60 * 1000)

    /**
      * Send a non-XML message every 2 seconds.
      */
    def sendStuff(): Unit = {
      val timeout = System.currentTimeMillis() + 30 * 1000L
      def remaining = timeout - System.currentTimeMillis()
      while (remaining > 0) {
        val msg = "hello there.  Remaining time to send messages (ms): " + remaining
        // enc.sendEvent(exchangeName, routingKey, msg.getBytes())
        enc.sendEvent(exchangeName, routingKey.get, msg.getBytes())
        Thread.sleep(2000)
      }
    }

    /**
      * Wait for a while, consuming events and printing them.  When done, call System.exit.  The
      * exit is necessary to force the consume loop to stop.
      */
    def waitThenExit(): Unit = {
      val timeout = System.currentTimeMillis() + (10 * 60 * 1000L)
      def remaining = timeout - System.currentTimeMillis()
      while (remaining > 0) {
        println("ms to exit: " + remaining)
        Thread.sleep(5 * 1000)
      }
      println("Done.  Exiting.")
      System.exit(0)
    }

    println("Starting background consumer.")
    enc.consumeNonDurable(exchangeName, "#", processing)

    Thread.sleep(1 * 1000L)
    if (routingKey.isDefined) sendStuff() // set to true to send messages to self

    waitThenExit()
  }

}
