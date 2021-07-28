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

import scala.xml.{Elem, XML}

abstract class IncomingEvent(text: String) extends Event {

  protected val doc: Elem = XML.loadString(text)

  override val header = new Header(doc)

  protected def docError(element: String, e: Throwable): String = {
    throw new RuntimeException("Unable to get element " + element + " from Event: " + e)
    "NA"
  }

  //noinspection SameParameterValue
  protected def doc(name: String): String =
    try { (doc \ name).head.text }
    catch { case e: Exception => docError(name, e) }

}
