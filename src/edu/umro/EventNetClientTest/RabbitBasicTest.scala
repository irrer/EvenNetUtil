package edu.umro.EventNetClientTest

import com.rabbitmq.client.{CancelCallback, ConnectionFactory, DeliverCallback, Delivery}

import java.nio.charset.StandardCharsets
// import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._

object RabbitBasicTest {

  private val QUEUE_NAME = "hey"

  private var latest = System.currentTimeMillis

  def elapsed: String = {
    val now = System.currentTimeMillis()
    val e = now - latest
    latest = now
    e.formatted("%4d")
  }

  private def send(): Unit = {
    System.out.println("Starting...")
    val factory = new ConnectionFactory
    factory.setHost("localhost")
    try {
      val connection = factory.newConnection
      val channel = connection.createChannel
      try {
        channel.queueDeclare(QUEUE_NAME, false, false, false, null)
        (1 until 10).foreach(count => {
          val message = " hello there " + count
          channel.basicPublish("", QUEUE_NAME, null, message.getBytes(StandardCharsets.UTF_8))
          System.out.println(elapsed + " [x] Sent '" + message + "'")
          Thread.sleep(500)
        })
      } finally {
        if (connection != null) connection.close()
        if (channel != null) channel.close()
      }
    }
    System.out.println("send is done.")
  }


  private def receive(): Unit = {
    val factory = new ConnectionFactory
    factory.setHost("localhost")
    val connection = factory.newConnection
    val channel = connection.createChannel

    channel.queueDeclare(QUEUE_NAME, false, false, false, null)
    System.out.println(" [*] Waiting for messages. To exit press CTRL+C")


    val deliverCallback = new DeliverCallback {
      override def handle(s: String, delivery: Delivery): Unit = {
        val message = new String(delivery.getBody)
        println(elapsed + " deliverCallback message: " + message)
      }
    }

    val cancelCallback: CancelCallback = (s: String) => {
      println("cancelCallback s: " + s)
    }

    channel.basicConsume(QUEUE_NAME, true, deliverCallback, cancelCallback)
  }

  def main(args: Array[String]): Unit = {
    println("Starting...")

    val receiver = Future {
      receive()
    }

    send()

    Thread.sleep(500)
    println("Done.  Exiting...")

    System.exit(0)  // aborts channel in an ugly way, but this works
  }
}
