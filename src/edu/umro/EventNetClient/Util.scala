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

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.UUID
import scala.xml.Elem
import scala.xml.PrettyPrinter

/**
  * Common utilities for API.
  */
object Util {

  def makeUID: String = {
    UUID.randomUUID.toString
  }

  def xmlToText(document: Elem): String = new PrettyPrinter(1024, 2).format(document)

  /**
    * Format a <code>Throwable</code>.
    *
    * @param throwable Contains description and stack trace.
    *
    * @return Human readable version of <code>Throwable</code> and stack trace.
    */
  def fmtEx(throwable: Throwable): String = {
    val msg = {
      if (throwable.getMessage == null)
        ""
      else { " : " + throwable.getMessage }
    }
    val text = throwable + "\n    " + throwable.getStackTrace.toList.mkString("\n    ")
    text
  }

  //noinspection SpellCheckingInspection
  private val standardDateFormatList = List(
    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX"),
    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX"),
    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS"),
    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"),
    new SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss")
  )

  def dateToText(date: Date): String = standardDateFormatList.head.format(date)

  def textToDate(text: String): Date = {
    val maxTextLength = 23
    val t = if (text.length > maxTextLength) text.substring(0, 23) else text
    def parse(dateFormat: SimpleDateFormat): Either[String, Date] = {
      try {
        Right(dateFormat.parse(t))
      } catch {
        case e: ParseException => Left("Attempted to use date format " + dateFormat.toPattern + " but failed with " + e.getMessage)
      }
    }

    val dateList = standardDateFormatList.map(fmt => parse(fmt))
    val validList = dateList.filter(d => d.isRight)
    if (validList.isEmpty) throw new ParseException("Unable to parse '" + text + "' as a date string", 0)
    val x = validList.head.right.get
    x
  }
}
