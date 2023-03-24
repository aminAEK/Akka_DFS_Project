package com.example


import akka.actor._
import java.io._
import scala.io.Source

object Server2a extends App {
  case class Store(key: String, value: String)
  case class Lookup(key: String)
  case class Delete(key: String)
 
}

class Server2a extends Actor {
  import Server2a._
  val file: File = new File("storeD2P1.txt")
 
  override def receive: Receive = {
    case Store(key, value) =>
        val writer = new FileWriter(file, true)
        writer.write(s"$key $value\n")
        writer.close()
        println("Key stored.")
    case Lookup(key) =>
    val source = Source.fromFile(file)
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
        val writer = new FileWriter(file, true)
        writer.write(s"$key deleted\n")
        writer.close()
        println("Key deleted.")
  }
}

class ConsoleActor2a(server: ActorRef) extends Actor{
  import Server2a._
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

object Day2Part1 extends App {
  val as = ActorSystem("Main")
  val server = as.actorOf(Props[Server2a], "server")
  val client = as.actorOf(Props(new ConsoleActor2a(server)), "consoleActor")
  client ! "start"
}