package services

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import com.typesafe.config.{Config, ConfigFactory}
import akka.pattern._
import akka.util.Timeout
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

object ATMKeeperService {

  private case class Command(cmd: String)
  private case class InitBBBForDispenser(remote: ActorRef)

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
    """) // "10.50.1.61" BMSTU

  def remotingSystem(name: String, port: Int): ActorSystem = ActorSystem(name, remotingConfig(port))

  class DispenserService extends Actor {

    var remoteActorBBB: ActorRef = null
    var dispenserBlocked = false

    override def receive: Receive = {
      case InitBBBForDispenser(remote) =>
        remoteActorBBB = remote
        remoteActorBBB ! s"Connection established with ${self.path}"
        println(remoteActorBBB)

      case command: String if command == "Command(Снять блокировку диспенсера)" =>
        val realBeagleBoneSender: ActorRef = sender
        dispenserBlocked = false
        realBeagleBoneSender ! Right(s"ДИСПЕНСЕР: блокировка снята")

      case command: String if dispenserBlocked =>
        val realBeagleBoneSender: ActorRef = sender
        realBeagleBoneSender ! Right(s"ДИСПЕНСЕР: заблокирован")

      case command: String if command == "Command(Заблокировать диспенсер)" =>
        val realBeagleBoneSender: ActorRef = sender
        dispenserBlocked = true
        realBeagleBoneSender ! Right(s"ДИСПЕНСЕР: заблокирован")

      case command: String if (command == "Command(Сообщить состояние устройств)" ||
                               command == "Command(Сообщить состояние диспенсера)") =>
        val realBeagleBoneSender: ActorRef = sender
        println(s"Dispenser Recieved COMMAND: ${command}")
        realBeagleBoneSender ! Right(s"ДИСПЕНСЕР: состояние стабильно")

      case command: String if command == "Command(Инкассация банкомата)" ||
                              command == "Command(Тест контроллера ББ)" ||
                              command == "Command(Тест датчиков КББ)" ||
                              command == "Command(Тест Д)" ||
                              command == "Command(Отключить КББ)" =>
        val realBeagleBoneSender: ActorRef = sender
        println(s"Dispenser Recieved COMMAND: ${command}")
        realBeagleBoneSender ! Right(s"ДИСПЕНСЕР: получена команда $command")

      case msg: String => println(msg)
    }

  }

  case class Dispenser() {
    val system: ActorSystem = remotingSystem("DispenserSystem", 24325)
    val localActorDispenser: ActorRef = system.actorOf(Props[DispenserService], "dispenser")
  }

}
