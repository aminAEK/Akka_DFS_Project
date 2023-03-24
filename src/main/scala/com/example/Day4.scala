package com.example

import akka.actor._
import java.io._
import scala.util.Random
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.collection.mutable.ListBuffer
import scala.io.Source
import _root_.com.typesafe.config.ConfigFactory


object Server {
  def props(cache: ActorRef) = Props(new Server(cache))
  case class Store(key: String, value: String)
  case class Lookup(key: String)
  case class Delete(key: String)}
  

 
class Cache(keyValueStore: ActorRef) extends Actor {
  import Server._
  var cache: Map[String, String] = Map.empty
  

  override def receive: Receive = {
    case Store(key, value) =>
      cache += key -> value
      if (cache.size > 10) {
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
          sender ! v
        case None =>
          keyValueStore ! (Lookup(key),sender)
          
      }

    case Delete(key) =>
      cache -= key
      keyValueStore ! Delete(key)

    
  }
}

class KeyValueStore extends Actor {
  import Server._
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

class Server(cache: ActorRef) extends Actor {
  import Server._
  val filename: File = new File("storeD4.txt")
 
  override def receive: Receive = {
    case Store(key, value) =>
        cache ! Store(key, value)
        val writer = new FileWriter(filename, true)
        writer.write(s"$key $value\n")
        writer.close()
        println("Key stored.")
        
    case Lookup(key) =>
        cache ! Lookup(key)
    case Delete(key) =>
        cache ! Delete(key)
        val writer = new FileWriter(filename, true)
        writer.write(s"$key deleted\n")
        writer.close()
        println("Key deleted.")

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
  
      
  }
}



class ClientActor(server: ActorRef) extends Actor{
  import Server._
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

      
      if (numRequests < 1000) {
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
          if (numRequests == 1000) {
            println(s"Total time: ${totalTime / 1000000}ms")
          }
         
        }
      }
  }
}

import scala.util.{Failure, Success}

object KeyValueStore {
  def props = Props(new KeyValueStore())
}
object Configs {
  private val root = ConfigFactory.load()
  val one          = root.getConfig("system1")
  val two          = root.getConfig("system2")
  val three        = root.getConfig("system3")
  val four         = root.getConfig("system4")
}

object Main1 extends App {
  import Configs._
  val system1 = ActorSystem("system1",one)

  implicit val dspch = system1.dispatcher
  val keyValueStore = system1.actorOf(KeyValueStore.props, "keyValueStore") 
}

object Cache {
  def props(keyValueStore: ActorRef) = Props(new Cache(keyValueStore))
}

object Main2 extends App {
  import Configs._
  val system2 = ActorSystem("system2",two)
  implicit val dspch = system2.dispatcher
  val keyValueStore = system2.actorSelection("akka://system1@127.0.0.1:2552/user/keyValueStore")
  keyValueStore.resolveOne(3 seconds).onComplete {
    case Success(keyValueStore) =>
      val cache = system2.actorOf(Cache.props(keyValueStore), "cache")
    case Failure(e) => println(e)
      
}}

object Main3 extends App {
  import Configs._
  val system3 = ActorSystem("system3",three)
  implicit val dspch = system3.dispatcher
  val cache = system3.actorSelection("akka://system2@127.0.0.1:2553/user/cache")
  cache.resolveOne(3 seconds).onComplete {
    case Success(cache) =>
      val server = system3.actorOf(Server.props(cache), "server")
    case Failure(e) => println(e)
   
      
}}

object ClientActor {
  def props(server: ActorRef) = Props(new ClientActor(server))
}

object Main4 extends App {
  import Configs._
  
  val system4 = ActorSystem("system4",four)
  implicit val dspch = system4.dispatcher
  val server = system4.actorSelection("akka://system3@127.0.0.1:2554/user/server")
  server.resolveOne(3 seconds).onComplete {
    case Success(server) =>
      val client = system4.actorOf(ClientActor.props(server), "client")
      client ! "start"
     case Failure(e) => println(e)
  }
  
}
