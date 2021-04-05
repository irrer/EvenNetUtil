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
    println("java -cp EventNetClient_2.12-0.0.4-jar-with-dependencies.jar edu.umro.EventNetClientTest.SelfTest [exchange]")
    println("exchange: " + exchangeName)
    println("Default send routing key is " + defaultSendRoutingKey)
    println("starting ...")

    val routingKey: String = {
      if (args.nonEmpty)
        args.head
      else {
        defaultSendRoutingKey
      }
    }

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
        enc.sendEvent(exchangeName, routingKey, msg.getBytes())
        Thread.sleep(2000)
      }
    }

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
    if (false) sendStuff()  // set to true to send messages to self

    waitThenExit()
  }

}
