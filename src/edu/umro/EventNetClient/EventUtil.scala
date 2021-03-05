package edu.umro.EventNetClient

import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import java.util.logging.Logger
import java.util.concurrent.LinkedBlockingQueue
import resource.managed
import java.io.Closeable
import java.util.HashSet
import com.rabbitmq.client.DefaultConsumer
import com.rabbitmq.client.Delivery
import com.rabbitmq.client.CancelCallback
import com.rabbitmq.client.DeliverCallback

class EventUtil(config: EventNetClientConfig, channelLimit: Int, restartDelayMs: Long) {

  private val log = Logger.getLogger(this.getClass.getName())

  /** All channels declared at start and kept here. */
  private val channelList = new LinkedBlockingQueue[Channel]

  private case class QueueNameRoutingKeyPair(val queueName: String, val routingKey: String)

  private val establishedList = new HashSet[QueueNameRoutingKeyPair]

  private class ManagedChannel extends Closeable {

    /** Referenced by callers. */
    val channel = take

    override def close: Unit = {
      put(channel)
    }
  }

  def sendAdminEvent(event: Event): Unit = {
    log.fine("Sending admin event:\n" + event)
    def sendEvent(channel: Channel) =
      channel.basicPublish(config.AdminExchange, config.RoutingKeyPrefix + event.eventName, null, event.toText.getBytes())
    performSafelyUsingChannel(sendEvent(_))
    // printf("Sent admin event:\n" + event)
  }

  def performSafelyUsingChannel[T](func: Channel => T): Option[T] = {
    managed(new ManagedChannel) acquireAndGet {
      manChan =>
        {
          try {
            Some(func(manChan.channel))
          } catch {
            case e: Exception =>
              log.severe("Unexpected error : " + Util.fmtEx(e))
              None
          }
        }
    }
  }

  /**
   * Declare a durable queue from which to read messages.  Ignore errors
   * in case it is already declared.
   */
  private def queueDeclareDurable(queueName: String): Unit = {
    def qd(channel: Channel) = {
      val durable = true
      val exclusive = false
      val autoDelete = false
      val arguments = null
      val queueOk = channel.queueDeclare(queueName, durable, exclusive, autoDelete, arguments)
    }
    performSafelyUsingChannel(qd)
  }

  /**
   * Bind a queue to the standard EventNet exchange using the given routing key.
   */
  private def queueBindApplication(queueName: String, routingKey: String): Unit = {
    performSafelyUsingChannel(channel => channel.queueBind(queueName, config.Exchange, routingKey))
  }

  private def makeAmqpConnection: Connection = {
    try {
      val fctry = new ConnectionFactory
      log.fine("Constructing AMQP connection RabbitMQ broker at " + config.Broker + ":" + config.Port)
      fctry.setHost(config.Broker)
      fctry.setPort(config.Port)
      val cnct = fctry.newConnection
      log.info("Connection with RabbitMQ broker established")
      cnct
    } catch {
      case e: Exception => {
        log.severe("Unable to connect with RabbitMQ broker at " + config.Broker + ":" + config.Port + " : " + Util.fmtEx(e))
        null
      }
    }
  }

  /** The single connection that is used by all channels. */
  private var connection: Connection = null

  private def createChannel: Channel = {
    this.synchronized {
      if ((connection == null) || (!connection.isOpen)) connection = makeAmqpConnection
      try {
        connection.createChannel
      } catch {
        case e: Exception => {
          log.severe("Unable to create AMQP channel: " + Util.fmtEx(e))
          null
        }
      }
    }
  }

  private def put(channel: Channel) = {
    if ((channel != null) && channel.isOpen()) channelList.put(channel)
    else channelList.put(createChannel)
  }

  private def take: Channel = {
    channelList.poll match {
      case channel: Channel if (channel.isOpen) => channel
      case channel: Channel if (!channel.isOpen) => createChannel
      case _ => {
        log.severe("Internal error.  Unable to get AMQP channel.  Maximum number " + channelLimit + " have already been used")
        null
      }
    }
  }

  private def establishDurable(queueName: String, routingKey: String): Unit = {
    establishedList.synchronized {
      val qk = new QueueNameRoutingKeyPair(queueName, routingKey)
      if (!establishedList.contains(qk)) {
        establishedList.add(qk)
        log.info("Establishing queue " + queueName + " with routing key " + routingKey)
        queueDeclareDurable(queueName)
        queueBindApplication(queueName, routingKey)
      }
    }
  }

  private def delayBeforeResuming = Thread.sleep(restartDelayMs)

  /**
   * Create the QueueingConsumer and execute the application's event processor in a loop.
   *
   * Isolate exceptions from the application processing from exceptions from the AMQP calls.
   *
   * If <code>processing</code> throws an exception, mark the incoming message as
   * not-acknowledged and re-queue it. 
   */
  private def consumeLoop(channel: Channel, queueName: String, processing: Array[Byte] => Unit) = {
    val consumer = new DefaultConsumer(channel)
    val multiple = false
    val requeue = true
    val autoAck = false

    def fail(deliveryTag: Long) = {
      channel.basicNack(deliveryTag, multiple, requeue)
      delayBeforeResuming
    }

    class Deliver extends DeliverCallback {
      def handle(consumerTag: String, message: Delivery) = {
        val data = message.getBody
        val deliveryTag = message.getEnvelope.getDeliveryTag

        try {
          processing(data)
          channel.basicAck(deliveryTag, multiple)
        } catch {
          case t: Throwable => {
            log.severe("Unexpected application error : " + Util.fmtEx(t))
            fail(deliveryTag)
          }
        }
      }
    }

    class Cancel extends CancelCallback {
      def handle(consumerTag: String) = {
        log.severe("Unexpected cancel of AMQP message for queue " + queueName)
      }

    }

    channel.basicConsume(queueName, autoAck, new Deliver, new Cancel)

    /* TODO In the event that this is used before being properly implemented, this code is a
     * safeguard to prevent going into an infinite loop and consuming lots of CPU.  This
     * should be removed when the code is made to work.
     */
    if (true) {
      println("the consumeLoop " + System.currentTimeMillis())
      Thread.sleep(1000)
    }
  }

  private class RunLoop(queueName: String, processing: Array[Byte] => Unit) extends Runnable {
    def run: Unit = {
      while (true) {
        try {
          managed(new ManagedChannel) acquireAndGet {
            manChan =>
              {
                consumeLoop(manChan.channel, queueName, processing)
              }
          }
        } catch {
          // Only catch AMQP errors (originating in this class), not application errors.
          case e: Exception =>
            log.severe("Unexpected AMQP error : " + Util.fmtEx(e))
            delayBeforeResuming
        }
      }
    }
    (new Thread(this)).start
  }

  def consumeDurable(queueName: String, exchange: String, routingKey: String, processing: Array[Byte] => Unit): Unit = {
    establishDurable(queueName, routingKey)
    new RunLoop(queueName, processing)
  }

  def consumeNonDurable(exchange: String, routingKey: String, processing: Array[Byte] => Unit): Unit = {
    def bindToNonDurableQueue(channel: Channel): String = {
      val queue = channel.queueDeclare
      channel.queueBind(queue.getQueue, exchange, routingKey)
      queue.getQueue
    }
    val queueName = performSafelyUsingChannel(bindToNonDurableQueue)

    if (queueName.isDefined)
      new RunLoop(queueName.get, processing)
  }

  // Make a finite number of channels that is more than what will be needed for the life of
  // the service.  If there is a problem with not returning them when finished, then this
  // service will fail instead of affecting the AMQP broker.
  (1 to channelLimit).map(c => channelList.put(createChannel))

}
