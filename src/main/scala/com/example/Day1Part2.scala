package com.example


import akka.actor._
import scala.collection.mutable.HashMap

object Server1 extends App {
  case class Store(key: String, value: String)
  case class Lookup(key: String)
  case class Delete(key: String)
 
}

class Server1 extends Actor {
  import Server1._
  val store: HashMap[String, String] = HashMap.empty[String, String]

  override def receive: Receive = {
    case Store(key, value) =>
        store.put(key, value)
        println("Key stored.")
    case Lookup(key) =>
        store.get(key) match {
        case Some(v) => 
          println( s"Found key $key with value $v")

        case None => println("Key not found.")
      }
    case Delete(key) =>
        store.remove(key)
        println("Key deleted.")
  }
}

class ConsoleActor1(server: ActorRef) extends Actor{
  import Server1._
    override def receive: Receive = {
        case "start" =>
            println("Enter a command (store, lookup, delete):")
            val command = Option(scala.io.StdIn.readLine()).map(_.trim()).getOrElse("")
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
          println(msg)
    }}
}


object Day1Part2 extends App {
  val as = ActorSystem("Main")
  val server = as.actorOf(Props[Server1], "server")
  val client = as.actorOf(Props(new ConsoleActor1(server)), "consoleActor")
  client ! "start"
}