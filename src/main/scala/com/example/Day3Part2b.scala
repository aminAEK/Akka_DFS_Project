package com.example

import akka.actor._
import java.io._
import scala.util.Random
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.io.Source

object Server3b2 {
  case class Store(key: String, value: String)
  case class Lookup(key: String)
  case class Delete(key: String)}
 

class KeyValueStore3b2 extends Actor {
  import Server3b2._
  var store: Map[String, String] = Map.empty

  override def receive: Receive = {
    case Store(key, value) =>
      Thread.sleep(1000)
      store += key -> value

    case Lookup(key) =>
      Thread.sleep(1000)
      store.get(key) match {
        case Some(v) =>
          if (v!="deleted"){
            println( s"Found key $key with value $v in memory")}
          else{
            println("Key not found")
          }

        case None => sender ! key
      }

    case Delete(key) =>
      Thread.sleep(1000)
      store += key -> "deleted"
      println(s"Deleted key $key")
  }
}

class Server3b2(keyValueStore: ActorRef) extends Actor {
  import Server3b2._
  val filename: File = new File("storeD3P2b.txt")
 
  override def receive: Receive = {
    case Store(key, value) =>
        keyValueStore ! Store(key, value)
        val writer = new FileWriter(filename, true)
        writer.write(s"$key $value\n")
        writer.close()
        
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
        
  }
}



class ClientActor3b2(server: ActorRef) extends Actor{
  import Server3b2._
  val random = new Random()
  var lastKey: Option[String] = None
  var numRequests = 0
  var totalTime = 0L
  val start = System.nanoTime()
 

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


object Day3Part2b extends App {
  val as = ActorSystem("Main")
  val keyValueStore = as.actorOf(Props[KeyValueStore3b2], "keyValueStore")
  val server = as.actorOf(Props(new Server3b2(keyValueStore)), "server")
  val client = as.actorOf(Props(new ClientActor3b2(server)), "client")
  client ! "start"
 
}