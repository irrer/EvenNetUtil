package edu.umro.EventNetClient

class HeaderKeyValue(val key: String, val value: String) {
  override def toString: String = key + " = " + value
}
