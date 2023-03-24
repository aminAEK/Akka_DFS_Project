package com.example

import scala.collection.mutable.HashMap

object StoreSystem {
  val store: HashMap[String, String] = HashMap.empty[String, String]
  def Store(key: String, value: String): Unit = {store.put(key, value)}
  def Lookup(key: String): Option[String] = {store.get(key)}
  def Delete(key: String): Unit = {store.remove(key)}
}

object Day1Part1 extends App {
  var running = true
  while (running) {
    println("Enter a command (store, lookup, delete):")
    val command = scala.io.StdIn.readLine().toLowerCase.trim
    command match {
      case "store" =>
        println("Enter a key:")
        val key = scala.io.StdIn.readLine().trim
        println("Enter a value:")
        val value = scala.io.StdIn.readLine().trim
        StoreSystem.Store(key, value)
        println(s"Stored key $key with value $value.")
      case "lookup" =>
        println("Enter a key:")
        val key = scala.io.StdIn.readLine().trim
        val value = StoreSystem.Lookup(key)
        value match {
          case Some(v) => println(s"Found key $key with value $v.")
          case None => println(s"Key $key not found.")
        }
      case "delete" =>
        println("Enter a key:")
        val key = scala.io.StdIn.readLine().trim
        StoreSystem.Delete(key)
        println(s"Deleted key $key.")
      case _ =>
        println(s"Unknown command $command.")
    }
  }
}