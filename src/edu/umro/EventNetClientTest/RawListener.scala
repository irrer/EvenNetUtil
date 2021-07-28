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

import com.rabbitmq.client.{CancelCallback, ConnectionFactory, DeliverCallback, Delivery}

object RawListener {

  private val routingKey = "Aria.Event.EventWLQASRSDone"

  private val exchange = "gbtopic"

  private var latest = System.currentTimeMillis

  def elapsed: String = {
    val now = System.currentTimeMillis()
    val e = now - latest
    latest = now
    e.formatted("%4d")
  }

  def trc(msg: String = ""): Unit = {
    val se = Thread.currentThread.getStackTrace()(2)
    val line = se.getLineNumber
    val method = se.getMethodName.replace('$', '%').replaceAll(".*%", "")

    println(method + ":" + line + "  elapsed: " + elapsed + " : " + msg)
  }

  private def receive(): Unit = {
    val factory = new ConnectionFactory
    trc()
    factory.setHost("localhost")
    trc()
    val connection = factory.newConnection
    trc("got connection: " + connection)
    val channel = connection.createChannel
    trc("got channel: " + channel)

    // channel.queueDeclare(QUEUE_NAME, false, false, true, null)
    val declareOk = channel.queueDeclare()
    val queueName = declareOk.getQueue
    trc("queueName: " + queueName)

    channel.queueBind(queueName, exchange, routingKey)
    trc()
    System.out.println(" [*] Waiting for messages. To exit press CTRL+C")
    trc()

    var count = 0
    val deliverCallback = new DeliverCallback {
      override def handle(s: String, delivery: Delivery): Unit = {
        count = count + 1
        val message = new String(delivery.getBody)
        println("received " + elapsed + " : " + count.formatted("%3d") + "  message: " + message)
        channel.basicAck(delivery.getEnvelope.getDeliveryTag, false)
        println("after ack")
      }
    }

    val cancelCallback: CancelCallback = (s: String) => {
      println("cancelCallback s: " + s)
    }

    if (true) {
      trc("gonna consume...")
      channel.basicConsume(queueName, false, deliverCallback, cancelCallback)
      trc("after consume...")
    } else {
      trc("Not doing basicConsume")
    }
  }

  def main(args: Array[String]): Unit = {
    println("\n\n\n\n\nStarting...")

    class Later extends Runnable {
      override def run(): Unit = {
        trc("starting listen")
        receive()
        trc("after listen")
      }
    }

    new Thread(new Later).start()
    trc("started the thread")

    val timeout = System.currentTimeMillis() + 60 * 1000
    def remaining() = timeout - System.currentTimeMillis()
    while (remaining() > 0) {
      trc("after thread started.  Waiting ... " + remaining())
      Thread.sleep(1 * 1000)
    }

    println("Done waiting.  Exiting...")

    System.exit(0) // aborts channel in an ugly way, but this works
  }
}
