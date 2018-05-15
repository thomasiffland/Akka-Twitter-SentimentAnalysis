package actors

import akka.actor.{Actor, ActorRef, Terminated}
import com.corundumstudio.socketio.{AckRequest, SocketIOClient, SocketIOServer}
import models.{ChangeTweetFilterMessage, TwitterMessage}
import org.apache.logging.log4j.scala.Logging

import akka.actor.SupervisorStrategy.Stop

/**
  * This actor is listening to commands of the frontend
  * and sends tweets to it.
  */
class FrontendCommunicationActor(twitterReceiverActor: ActorRef,
                                 server: SocketIOServer) extends Actor with Logging {

  var socket: SocketIOClient = null

  def receive = {
    case TwitterMessage(message, location, emotion) => {
      logger.debug("Incoming message, try to send to Frontend")
      logger.trace(s"Sending Tweet to Frontend: EMOTION<${emotion}>, MESSAGE<${message}>, LOCATION<${location}>")

      socket.sendEvent("tweet", message, location, emotion)
    }
    case Terminated(who) => {
      context.unwatch(who)
      socket.disconnect()
      server.stop()
      context.stop(self)
    }
    case _ => {
      logger.warn(s"Does nothing when receiving such a message")
    }
  }

  override def preStart(): Unit = {

    server.addConnectListener(socket => {
      logger.info(s"Connect of: ${socket.getSessionId()} on port ${server.getConfiguration().getPort()}")
      this.socket = socket
    })

    server.addDisconnectListener(socket => {
      logger.info(s"Disconnect of: ${socket.getSessionId} on port ${server.getConfiguration().getPort()}")
      context.watch(twitterReceiverActor)
      twitterReceiverActor ! Stop
    })

    server.addEventListener("changeFilters", classOf[Array[Array[String]]],
      (client: SocketIOClient, data: Array[Array[String]], ackRequest: AckRequest) => {
        val hashtags: List[String] = data(0).toList
        val languages: List[String] = data(1).toList
        val quantityFilters: List[String] = data(2).toList

        logger.debug(s"changeFilters: hashtags - ${hashtags}, languages - ${languages}, filters - ${quantityFilters}")

        twitterReceiverActor ! new ChangeTweetFilterMessage(hashtags, languages, quantityFilters)
      })

    server.start()
  }

  override def postStop(): Unit = {
    logger.debug("stopped")

    socket.disconnect()
    server.stop()
  }

}