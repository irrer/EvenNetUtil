package edu.umro.EventNetClient

import com.rabbitmq.client.Connection

/**
 * Send PrintPDF test message.
 */

object SendText {

  //  private val config = EventNetClientConfig.construct(host, port)
  //
  //  private val eventUtil = new EventUtil(config, 2, 10 * 1000)

  def createConnection(host: String, port: Int) = {
    val factory = new com.rabbitmq.client.ConnectionFactory
    val cp = factory.getClientProperties();
    val props = cp.keySet.toArray.map(k => (k.toString + "=" + cp.get(k)))
    println("Client Properties: " + props.mkString("\n    ", "\n    ", "\n    "))
    println("Constructing AMQP connection RabbitMQ broker at " + host + ":" + port)
    factory.setHost(host)

    factory.setPort(port)
    val connection = factory.newConnection
    println("Connection with RabbitMQ broker established")
    connection
  }

  private def sendEvent(connection: Connection, exchange: String, routingKey: String, content: String): Unit = {
    //def sendEvent(channel: Channel) = channel.basicPublish(exchange, config.RoutingKeyPrefix + eventName, null, content.getBytes())

    val channel = connection.createChannel
    channel.basicPublish(exchange, routingKey, null, content.getBytes)

    printf("Sent content:\n" + content)
  }

  private val printPdfTestEvent =
    """<Event type="UMRO.UMPLAN.PrintPDF">
  <MetaData>
    <JobID/>
    <User>Irrer</User>
    <Nickname>NickName2</Nickname>
    <Plan>0</Plan>
    <Host>REMUS</Host>
    <Directory>$1$DGA1103:[IRRER]</Directory>
    <Destination>C:\Program Files\UMRO\PrintPDF2-2.0.1\tmp\foo.pdf</Destination>
  </MetaData>
  <FileList>
    <File>TEST3D_01_DVH.TXT</File>
    <File>SMITH307_01_PSOUT.PS</File>
  </FileList>
</Event>
"""

  def main(args: Array[String]): Unit = {
    println("Starting ...")

    // "141.214.124.176" // "rodicom11cet" // "rodicom11prod" // "rodicom11dev" //  "localhost" // "10.30.3.90" // "uhroappwebsdv1" // "rodicom11cet" //

    val host = "172.20.125.28"
    val port = 5672
    val exchange = ""
    val routingKey = "UMRO.UMPlan.PrintPDF"

    val connection = createConnection(host, port)

    sendEvent(connection, exchange, routingKey, printPdfTestEvent)

    println("Sleeping ...")
    Thread.sleep(3 * 1000L)
    println("Done.  Exiting.")
    System.exit(0)
  }

}