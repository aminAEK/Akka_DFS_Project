package com.example
import akka.actor._
import java.io._
import scala.util.Random
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.io.Source

object Server3b1 {
  case class Store(key: String, value: String)
  case class Lookup(key: String)
  case class Delete(key: String)}
 
class Cache3b1(keyValueStore: ActorRef) extends Actor {
  import Server._
  var cache: Map[String, String] = Map.empty

  override def receive: Receive = {
    case Store(key, value) =>
      cache += key -> value
      if (cache.size > 50) {
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

class KeyValueStore3b1 extends Actor {
  import Server3b1._
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

class Server3b1(cache: ActorRef) extends Actor {
  import Server3b1._
  val filename: File = new File("storeD3P2a.txt")
 
  override def receive: Receive = {
    case Store(key, value) =>
        cache ! Store(key, value)
        val writer = new FileWriter(filename, true)
        writer.write(s"$key $value\n")
        writer.close()
        
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
    
  }
}

class ClientActor3b1(server: ActorRef, cache: ActorRef) extends Actor{
  import Server3b1._
  val random = new Random()
  var lastKey: Option[String] = None
  var numRequests = 0
  var totalTime = 0L

  // Populate the storage with keys from 1-500
  for (k <- 1 to 500) {
    server ! Store(k.toString, k.toString)
  }

  override def receive: Receive = {
    case "start" =>
      if (numRequests < 5000) {
        val operation = random.nextInt(3) match {
          case 0 => "store"
          case 1 => "lookup"
          case 2 => "delete"
        }

        // Generate a random key
        val key = if (random.nextBoolean()) {
          lastKey.getOrElse(random.nextInt(500) + 1).toString
        } else {
          val r = random.nextInt(100)
          if (r < 70) {
            random.nextInt(500) + 1
          } else {
            random.nextInt(500) + 501
          }
        }.toString

        // If the operation is store or delete, make sure the key is greater than 500
        if ((operation == "store" || operation == "delete") && key.toInt <= 500) {
          // Retry with a different operation and key
          self ! "start"
        } else {
          val start = System.nanoTime()
          operation match {
            case "store" =>
              server ! Store(key, key)
              
            case "lookup" =>
              server ! Lookup(key)
              lastKey = Some(key)

            case "delete" =>
              server ! Delete(key)
          }
          numRequests += 1
          self ! "start"
         
           totalTime += (System.nanoTime() - start)
          if (numRequests == 5000) {
            println(s"Total time: ${totalTime / 1000000}ms")
          }
         
        }
      }
  }
}


object Day3Part2a extends App {
  val as = ActorSystem("Main")
  val keyValueStore = as.actorOf(Props[KeyValueStore3b1], "keyValueStore")
  val cache = as.actorOf(Props(new Cache3b1(keyValueStore)), "cache")
  val server = as.actorOf(Props(new Server3b1(cache)), "server")
  val client = as.actorOf(Props(new ClientActor3b1(server, cache)), "client")
  client ! "start"
 
}
