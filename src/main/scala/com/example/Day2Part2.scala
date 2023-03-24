package com.example

import akka.actor._
import java.io._
import scala.io.Source

object Server2b extends App {
  case class Store(key: String, value: String)
  case class Lookup(key: String)
  case class Delete(key: String)
}

class KeyValueStore2b extends Actor {
  import Server2b._
  var store: Map[String, String] = Map.empty

  override def receive: Receive = {
    case Store(key, value) =>
      store += key -> value
      println(s"Stored key $key")

    case Lookup(key) =>
        store.get(key) match {
        case Some(v) => 
          if (v!="deleted"){
            println( s"Found key $key with value $v")}
          else{
            println("Key not found")
          }

        case None => sender ! key
      }


    case Delete(key) =>
      store += key -> "deleted"
      println(s"Deleted key $key")
  }
}

class Server2b(keyValueStore: ActorRef) extends Actor {
  import Server2b._
  val filename: File = new File("storeD2P2.txt")
 
  override def receive: Receive = {
    case Store(key, value) =>
        keyValueStore ! Store(key, value)
        val writer = new FileWriter(filename, true)
        writer.write(s"$key $value\n")
        writer.close()
        println("Key stored.")
        
    case Lookup(key) =>
   
            keyValueStore ! Lookup(key)
    case key: String =>
    val source = Source.fromFile(filename)
    try {
      val lines = source.getLines().toVector.reverseIterator
      var found = false
      while (lines.hasNext && !found) {
        val line = lines.next()
        val parts = line.split(" ")
        if (parts.length == 2 && parts(0) == key&& parts(1) != "deleted") {
            val v=parts(1)
            println(s"Found the key with value $v")
            found = true
            source.close()
          
        } else if (parts.length == 2 && parts(0) == key && parts(1) == "deleted") {
          found = true
          println("The key is not in the file")
          source.close()
        }
      }
      
    } finally {
      source.close()
    }
    case Delete(key) =>
        keyValueStore ! Delete(key)
        val writer = new FileWriter(filename, true)
        writer.write(s"$key deleted\n")
        writer.close()
        println("Key deleted.")
 
  }
}

class ConsoleActor2b(server: ActorRef) extends Actor{
  import Server2b._
    override def receive: Receive = {
        case "start" =>
            println("Enter a command (store, lookup, delete):")
            val command = scala.io.StdIn.readLine().trim
            command match {
            case "store" =>
                println("Enter a key:")
                val key = scala.io.StdIn.readLine().trim
                println("Enter a value:")
                val value = scala.io.StdIn.readLine().trim
                server! Store(key, value)
                println("Stored key")
                self ! "start"
            case "lookup" =>
                println("Enter a key:")
                val key = scala.io.StdIn.readLine().trim
                server ! Lookup(key)
                self ! "start"
            case "delete" =>
                println("Enter a key:")
                val key = scala.io.StdIn.readLine().trim
                server! Delete(key)
                println("Deleted key")
                self ! "start"
            case msg =>
                println(s"Invalid command: $msg")
                self ! "start"
            }
    }
}


object Day2Part2 extends App {
  val as = ActorSystem("Main")
  val keyValueStore = as.actorOf(Props[KeyValueStore2b], "keyValueStore")
  val server = as.actorOf(Props(new Server2b(keyValueStore)), "server")
  val console = as.actorOf(Props(new ConsoleActor2b(server)), "console")
  console ! "start"
}

