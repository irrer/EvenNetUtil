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

package edu.umro.EventNetClient

import java.io.File
import java.util.logging.Logger
import scala.xml.XML

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

  private val log = Logger.getLogger(this.getClass.getName)

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
    val conf = {
      try {
        constructFromWindowsMachineFile(new File(WindowsMachineConfigFileName))
      } catch {
        case e: Exception =>
          log.warning(
            "Unable to construct EventNet configuration from standard Windows file " +
              WindowsMachineConfigFileName + " so instead using default hard-coded configuration.  Error: " + e
          )
          constructDefault
      }
    }
    conf
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
