package edu.umro.EventNetClientTest

import edu.umro.EventNetClient.EventNetClientConfig
import edu.umro.EventNetClient.EventNetClient
import java.util.Date
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory

object ConsumeEverything {

    def processing(msg: Array[Byte]): Unit = {
        val line = "\n-------------------------------------------------------\n"
        println(line + new String(msg) + line)
    }

    def main(args: Array[String]): Unit = {
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
        println("config: " + config)
        val enc = new EventNetClient(config, "ConsumeEverything", 10, 1 * 60 * 1000)
        enc.consumeNonDurable(exchangeName, "#", processing)
        for (i <- (1 to 0)) {
            Thread.sleep(1000L)
            val msg = "ConsumeEverything " + (new Date).toString
            //println("Sending message: " + i)
            enc.sendEvent(exchangeName, "ConsumeEverything", msg.getBytes)
        }
        Thread.sleep(24 * 60 * 60 * 1000L)
        println("Done.")
    }

}