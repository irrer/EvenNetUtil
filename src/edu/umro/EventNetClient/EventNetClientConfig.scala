package edu.umro.EventNetClient

import scala.xml.XML
import java.io.File
import java.util.logging.Logger
import java.io.FileInputStream

/**
 * EventNet client configuration; A subset of the Windows machine.config file.
 */
class EventNetClientConfig(val Broker: String, val Port: Int, val Exchange: String, val AdminExchange: String, val RoutingKeyPrefix: String) {
    override def toString: String =
        "Broker: " + Broker +
            "    Port: " + Port +
            "    Exchange: " + Exchange +
            "    AdminExchange: " + AdminExchange +
            "    RoutingKeyPrefix: " + RoutingKeyPrefix
}

object EventNetClientConfig {

    private val log = Logger.getLogger(this.getClass.getName())

    def constructDefault: EventNetClientConfig = {
        new EventNetClientConfig("localhost", 5672, "gbtopic", "admintopic", "Aria.Event.")
    }

    def WindowsMachineConfigFileName = "C:\\Windows\\Microsoft.NET\\Framework\\v4.0.30319\\Config\\machine.config"

    def constructFromWindowsMachineFile(file: File): EventNetClientConfig = {
        val document = XML.loadFile(file)
        def appSetting(name: String): String = {
            val list = document \ "applicationSettings" \ "EventFilterCommon.EventFilter" \ "setting"
            val setting = list.filter(s => (s \ "@name").head.text.equals(name))
            (setting.head \ "value").head.text
        }

        new EventNetClientConfig(appSetting("Broker"), appSetting("Port").toInt, appSetting("Exchange"), appSetting("AdminExchange"), appSetting("RoutingKeyPrefix"))
    }

    def construct: EventNetClientConfig = {
        try {
            constructFromWindowsMachineFile(new File(WindowsMachineConfigFileName))
        }
        catch {
            case e: Exception => {
                log.warning("Unable to construct EventNet configuration from standard Windows file " +
                    WindowsMachineConfigFileName + " so instead using default hard-coded configuration.  Error: " + e)
                constructDefault
            }
        }
    }
    
    /**
     * Construct a configuration overriding the broker and port.
     */
    def construct(Broker: String, Port: Int): EventNetClientConfig = {
        val config = construct
        new EventNetClientConfig(Broker, Port, config.Exchange, config.AdminExchange, config.RoutingKeyPrefix)
    }

    def main(args: Array[String]): Unit = {
        println(construct)
    }

}
