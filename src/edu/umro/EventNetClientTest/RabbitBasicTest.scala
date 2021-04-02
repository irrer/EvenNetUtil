package edu.umro.EventNetClientTest

import com.rabbitmq.client.{CancelCallback, ConnectionFactory, DeliverCallback, Delivery}

import java.nio.charset.StandardCharsets
// import scala.concurrent.Future

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
          System.out.println("sent: " + elapsed + " : " + message)
          Thread.sleep(250)
        })
      } finally {
        if (connection != null) connection.close()
        println("connection.isOpen: " + connection.isOpen)
        // if (channel != null) channel.close()
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

    var count = 0
    val deliverCallback = new DeliverCallback {
      override def handle(s: String, delivery: Delivery): Unit = {
        count = count + 1
        val message = new String(delivery.getBody)
        println("received " + elapsed + " : " + count.formatted("%3d") + "  message: " + message)
        channel.basicAck(delivery.getEnvelope.getDeliveryTag, false)
      }
    }

    val cancelCallback: CancelCallback = (s: String) => {
      println("cancelCallback s: " + s)
    }

    channel.basicConsume(QUEUE_NAME, false, deliverCallback, cancelCallback)
  }

  def main(args: Array[String]): Unit = {
    println("Starting...")

    /*
    val future = Future {
      receive()
    }
     */

    class Later() extends Runnable {
      override def run(): Unit = receive()

      new Thread(this).start()
    }
    new Later

    send()

    Thread.sleep(500)
    println("Done.  Exiting...")

    System.exit(0) // aborts channel in an ugly way, but this works
  }
}
