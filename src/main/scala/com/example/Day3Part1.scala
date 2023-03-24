package com.example

import akka.actor._
import java.io._
import scala.io.Source

object Server3a extends App {
  case class Store(key: String, value: String)
  case class Lookup(key: String)
  case class Delete(key: String)
}

class Cache3a(keyValueStore: ActorRef) extends Actor {
  import Server3a._
  var cache: Map[String, String] = Map.empty

  override def receive: Receive = {
    case Store(key, value) =>
     
      cache += key -> value
      if (cache.size > 1000) {
        // remove the least recently used key from the cache
        cache = cache.tail
      }
      println("Stored key in cache")
      keyValueStore ! Store(key, value)

    case Lookup(key) =>
      cache.get(key) match {
        case Some(v) =>
          if (v != "deleted") {
            println(s"Found key $key with value $v (from cache)")
          } else {
            println("Key not found (from cache)")
          }
        case None =>
          keyValueStore ! (Lookup(key),sender)
      }

    case Delete(key) =>
      cache -= key
      keyValueStore ! Delete(key)
  }
}

class KeyValueStore3a extends Actor {
  import Server3a._
  var store: Map[String, String] = Map.empty

  override def receive: Receive = {
    case Store(key, value) =>
      Thread.sleep(1000)
      store += key -> value
      println(s"Stored key $key")

    case (Lookup(key),server: ActorRef) =>
        Thread.sleep(1000)
        store.get(key) match {
        case Some(v) => 
          if (v!="deleted"){
            println( s"Found key $key with value $v in memory")}
          else{
            println("Key not found")
          }

        case None => server ! key
      }


    case Delete(key) =>
      Thread.sleep(1000)
      store += key -> "deleted"
      println(s"Deleted key $key")
  }
}

class Server3a(cache: ActorRef) extends Actor {
  import Server3a._
  val filename: File = new File("storeD3P1.txt")
  
  override def receive: Receive = {
    case Store(key, value) =>
        cache ! Store(key, value)
        val writer = new FileWriter(filename, true)
        writer.write(s"$key $value\n")
        writer.close()
        println("Key stored.")        
    case Lookup(key) =>
            cache ! Lookup(key)
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
        cache ! Delete(key)
        val writer = new FileWriter(filename, true)
        writer.write(s"$key deleted\n")
        writer.close()
        println("Key deleted.")
        
 
  }
}

class ConsoleActor3a(server: ActorRef) extends Actor{
  import Server3a._
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



object Day3Part1 extends App {
  val as = ActorSystem("Main")
  val keyValueStore = as.actorOf(Props[KeyValueStore3a], "keyValueStore")
  val cache = as.actorOf(Props(new Cache3a(keyValueStore)), "cache")
  val server = as.actorOf(Props(new Server3a(cache)), "server")
  val console = as.actorOf(Props(new ConsoleActor3a(server)), "console")
 
  console ! "start"
}