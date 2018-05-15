package actors

import akka.actor.Actor
import models.ShutdownCommand
import org.apache.logging.log4j.scala.Logging

import scala.io.StdIn

class IOActor extends Actor with Logging {

  override def receive = {
    case _ => {
      logger.warn(s"Does nothing when receiving such a message")
    }
  }

  override def preStart(): Unit = {
    logger.debug("Listen to input commands")

    var command = ""
    while (command != null) {
      command = StdIn.readLine()

      command match {
        case "shutdown" => {
          context.parent ! ShutdownCommand
          command = null
        }
        case _ if command != "" => logger.warn(s"Unknown command: ${command}")
      }
    }
  }

  override def postStop(): Unit = {
    logger.debug("stopped")
  }

}
