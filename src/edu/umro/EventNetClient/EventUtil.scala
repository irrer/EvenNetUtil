package edu.umro.EventNetClient

import com.rabbitmq.client._
import resource.managed

import java.io.Closeable
import java.util
import java.util.concurrent.LinkedBlockingQueue
import java.util.logging.Logger

class EventUtil(config: EventNetClientConfig, channelLimit: Int, restartDelayMs: Long) {

  private val log = Logger.getLogger(this.getClass.getName)

  /** All channels declared at start and kept here. */
  private val channelList = new LinkedBlockingQueue[Channel]

  private case class QueueNameRoutingKeyPair(queueName: String, routingKey: String)

  private val establishedList = new util.HashSet[QueueNameRoutingKeyPair]

  private class ManagedChannel extends Closeable {

    /** Referenced by callers. */
    val channel: Channel = take

    override def close(): Unit = {
      put(channel)
    }
  }

  def sendAdminEvent(event: Event): Unit = {
    log.fine("Sending admin event:\n" + event)
    def sendEvent(channel: Channel): Unit =
      channel.basicPublish(config.AdminExchange, config.RoutingKeyPrefix + event.eventName, null, event.toText.getBytes())
    performSafelyUsingChannel(sendEvent)
    // printf("Sent admin event:\n" + event)
  }

  def performSafelyUsingChannel[T](func: Channel => T): Option[T] = {
    managed(new ManagedChannel) acquireAndGet { manChan =>
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
    def qd(channel: Channel): Unit = {
      val durable = true
      val exclusive = false
      val autoDelete = false
      val arguments = null
      channel.queueDeclare(queueName, durable, exclusive, autoDelete, arguments)
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
      val factory = new ConnectionFactory
      log.fine("Constructing AMQP connection RabbitMQ broker at " + config.Broker + ":" + config.Port)
      factory.setHost(config.Broker)
      factory.setPort(config.Port)
      val connection = factory.newConnection
      log.info("Connection with RabbitMQ broker established")
      connection
    } catch {
      case e: Exception =>
        log.severe("Unable to connect with RabbitMQ broker at " + config.Broker + ":" + config.Port + " : " + Util.fmtEx(e))
        null
    }
  }

  /** The single connection that is used by all channels. */
  private var connection: Option[Connection] = None

  private def createChannel: Channel = {
    this.synchronized {
      if (connection.isEmpty || (!connection.get.isOpen)) connection = Some(makeAmqpConnection)
      try {
        connection.get.createChannel
      } catch {
        case e: Exception =>
          log.severe("Unable to create AMQP channel: " + Util.fmtEx(e))
          null
      }
    }
  }

  private def put(channel: Channel): Unit = {
    if ((channel != null) && channel.isOpen) channelList.put(channel)
    else channelList.put(createChannel)
  }

  private def take: Channel = {
    channelList.poll match {
      case channel: Channel if channel.isOpen  => channel
      case channel: Channel if !channel.isOpen => createChannel
      case _ =>
        log.severe("Internal error.  Unable to get AMQP channel.  Maximum number " + channelLimit + " have already been used")
        null
    }
  }

  private def establishDurable(queueName: String, routingKey: String): Unit = {
    establishedList.synchronized {
      val qk = QueueNameRoutingKeyPair(queueName, routingKey)
      if (!establishedList.contains(qk)) {
        establishedList.add(qk)
        log.info("Establishing queue " + queueName + " with routing key " + routingKey)
        queueDeclareDurable(queueName)
        queueBindApplication(queueName, routingKey)
      }
    }
  }

  private def delayBeforeResuming(): Unit = Thread.sleep(restartDelayMs)

  /**
    * Create the QueueingConsumer and execute the application's event processor in a loop.
    *
    * Isolate exceptions from the application processing from exceptions from the AMQP calls.
    *
    * If <code>processing</code> throws an exception, mark the incoming message as
    * not-acknowledged and re-queue it.
    */
  private def consumeLoop(channel: Channel, queueName: String, processing: Array[Byte] => Unit): Unit = {
    // val consumer = new DefaultConsumer(channel)
    val multiple = false
    val requeue = true
    val autoAck = false

    def fail(deliveryTag: Long): Unit = {
      channel.basicNack(deliveryTag, multiple, requeue)
      delayBeforeResuming()
    }

    val deliver = new DeliverCallback {
      override def handle(consumerTag: String, message: Delivery): Unit = {
        val data = message.getBody
        val deliveryTag = message.getEnvelope.getDeliveryTag

        try {
          processing(data)
          channel.basicAck(deliveryTag, multiple)
        } catch {
          case t: Throwable =>
            log.severe("Unexpected application error : " + Util.fmtEx(t))
            fail(deliveryTag)
        }
      }
    }

    val cancel = new CancelCallback {
      override def handle(consumerTag: String): Unit = {
        log.severe("Unexpected cancel of AMQP message for queue " + queueName)
      }

    }

    channel.basicConsume(queueName, autoAck, deliver, cancel)
  }

  private def consume(queueName: String, processing: Array[Byte] => Unit): Unit = {
    try {
      val manChan = new ManagedChannel
      consumeLoop(manChan.channel, queueName, processing)
    } catch {
      // Only catch AMQP errors (originating in this class), not application errors.
      case e: Exception =>
        log.severe("Unexpected AMQP error while consuming: " + Util.fmtEx(e))
    }
  }

  def consumeDurable(queueName: String, routingKey: String, processing: Array[Byte] => Unit): Unit = {
    establishDurable(queueName, routingKey)
    consume(queueName, processing)
  }

  def consumeNonDurable(exchange: String, routingKey: String, processing: Array[Byte] => Unit): Unit = {
    def bindToNonDurableQueue(channel: Channel): String = {
      val queue = channel.queueDeclare
      channel.queueBind(queue.getQueue, exchange, routingKey)
      queue.getQueue
    }
    val queueName = performSafelyUsingChannel(bindToNonDurableQueue)

    if (queueName.isDefined)
      consume(queueName.get, processing)
  }

  // Make a finite number of channels that is more than what will be needed for the life of
  // the service.  If there is a problem with not returning them when finished, then this
  // service will fail instead of affecting the AMQP broker.
  (1 to channelLimit).foreach(_ => channelList.put(createChannel))

}
