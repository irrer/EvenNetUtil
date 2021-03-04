package edu.umro.EventNetClientTest

import com.rabbitmq.client.ConnectionFactory
import edu.umro.EventNetClient.{EventNetClient, EventNetClientConfig}

import java.util.Date

object ConsumeEverything {

  def processing(msg: Array[Byte]): Unit = {
    val line = "\n-------------------------------------------------------\n"
    println(line + new String(msg) + line)
  }

  def main(args: Array[String]): Unit = {
    println("starting ...")
    Thread.sleep(1 * 1000L)
    println("actually starting ...")
    val exchangeName = "gbtopic"

    if (false) {
      val fctry = new ConnectionFactory
      fctry.setHost("localhost")
      fctry.setPort(5672)
      val cnct = fctry.newConnection
      val channel = cnct.createChannel

      /*
       *
       *     Exchange.DeclareOk exchangeDeclare(String exchange,
                                        String type,
                                        boolean durable,
                                        boolean autoDelete,
                                        boolean internal,
                                        Map<String, Object> arguments) throws IOException;
       *
       */
      channel.exchangeDelete(exchangeName)
      channel.exchangeDeclare(exchangeName, "topic", true, false, false, null)

    }

    val config = EventNetClientConfig.construct("localhost", 5672)
    // val config = EventNetClientConfig.construct("uhroappwebspr1", 5672)
    if (false) {
      println("before sleep")
      Thread.sleep(10 * 1000L)
      println("after sleep")
      System.exit(0)
    }
    println("config: " + config)
    println("22a")
    //Thread.sleep(100 * 1000L)
    println("22b")
    println("33")
    val enc = new EventNetClient(config, "ConsumeEverything", 10, 1 * 60 * 1000)
    println("33a")
    Thread.sleep(100 * 1000L)
    println("33b")
    enc.consumeNonDurable(exchangeName, "#", processing)
    println("44")
    Thread.sleep(100 * 1000L)
    println("44a")
    for (i <- (1 to 40)) {
      println("55")
      Thread.sleep(10 * 1000L)
      println("55a")
      val msg = "ConsumeEverything " + (new Date).toString
      //println("Sending message: " + i)
      println("59")
      enc.sendEvent(exchangeName, "ConsumeEverything", msg.getBytes)
      println("61")
    }
    //Thread.sleep(24 * 60 * 60 * 1000L)
    println("Done.")
    System.exit(0)
  }

}