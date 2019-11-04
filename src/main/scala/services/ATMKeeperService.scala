package services

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import com.typesafe.config.{Config, ConfigFactory}
import authenticator.Authenticator._
import akka.pattern._
import akka.util.Timeout
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

object ATMKeeperService {

  private case class Command(cmd: String, signature: String)
  private case class Init(remote: ActorRef)

  def remotingConfig(port: Int): Config = ConfigFactory.parseString(
    s"""
        akka {
          actor.warn-about-java-serializer-usage = off
          actor.provider = "akka.remote.RemoteActorRefProvider"
          remote {
            enabled-transports = ["akka.remote.netty.tcp"]
            netty.tcp {
              hostname = "localhost"
              port = $port
            }
          }
        }
    """) // "192.168.0.161"

  def remotingSystem(name: String, port: Int): ActorSystem = ActorSystem(name, remotingConfig(port))

  class DispenserService extends Actor {

    var remoteActorBBB: ActorRef = _

    override def receive: Receive = {

      case Init(remote) =>
        remoteActorBBB = remote
        println(remoteActorBBB)

      case msg: String => println(msg)

      case command: Command =>
        val realBeagleBoneSender: ActorRef = sender
        println(s"Dispenser Recieved COMMAND: ${command.cmd}")
        checkSignatures(command.signature, command.cmd) match {
          case Right(msg) =>
            realBeagleBoneSender ! Right(msg)
          case Left(errMsg) =>
            realBeagleBoneSender ! Left(errMsg)
        }
    }

  }

  case class Dispenser() {
    val system: ActorSystem = remotingSystem("DispenserSystem", 24325)
    val localActorDispenser: ActorRef = system.actorOf(Props[DispenserService], "dispenser")
  }

}
