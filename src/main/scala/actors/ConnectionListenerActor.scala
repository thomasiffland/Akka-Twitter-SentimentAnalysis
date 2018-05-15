package actors

import akka.actor.{Actor, ActorRef, Props, Terminated}
import com.corundumstudio.socketio.{Configuration, SocketIOClient, SocketIOServer}
import models.{AnnounceFrontendCommunicationActorMessage, ShutdownCommand}
import org.apache.logging.log4j.scala.Logging

class ConnectionListenerActor extends Actor with Logging {

  var connectionRequestServer: SocketIOServer = null
  var socket: SocketIOClient = null
  var iOActor: ActorRef = null

  var sessions: Map[Int, Tuple3[ActorRef, ActorRef, SocketIOServer]] = Map.empty
  var portNumber: Int = 49152

  override def receive = {
    case Terminated(who) => {
      logger.info(s"${who} died")
      context.unwatch(who)
      // TODO: remove from sessions map and release the port

      if (context.children.isEmpty) {
        logger.debug("All children terminated")
        context.stop(self)
      }
    }
    case ShutdownCommand if sender() == iOActor => {
      logger.info("Received shutdown command, try to terminate cleanly")
      context.children.foreach(child => context.stop(child))
    }
    case _ => {
      logger.warn(s"Does nothing when receiving such a message")
    }
  }

  override def preStart(): Unit = {
    iOActor = context.actorOf(Props(new IOActor()), name = "io")
    context.watch(iOActor)

    val staticConfig: Configuration = new Configuration()
    staticConfig.setHostname("localhost")
    staticConfig.setPort(8080)
    staticConfig.setOrigin("http://localhost:4200")

    connectionRequestServer = new SocketIOServer(staticConfig)

    connectionRequestServer.addConnectListener(socket => {
      if (this.socket == null) this.socket = socket
      logger.info(s"Session count: ${sessions.size}")
      val staticSessionId = socket.getSessionId()
      logger.info(s"Initial connect of: ${staticSessionId}, reserved exclusive port: ${portNumber}")


      val exclusiveConfig: Configuration = new Configuration()
      exclusiveConfig.setHostname("localhost")
      exclusiveConfig.setPort(portNumber)
      exclusiveConfig.setOrigin("http://localhost:4200")
      val exclusiveConnection = new SocketIOServer(exclusiveConfig)

      logger.debug(s"Creating actors for ${staticSessionId} or ${portNumber} respectively")
      val tra = context.actorOf(Props(new TwitterReceiverActor()), name = "tra@"+ portNumber)
      val fca = context.actorOf(Props(
        new FrontendCommunicationActor(tra, exclusiveConnection)), name = "fca@" + portNumber)
      tra ! new AnnounceFrontendCommunicationActorMessage(fca)

      sessions += portNumber -> (fca, tra, exclusiveConnection)
      context.watch(tra)
      context.watch(fca)
      socket.sendEvent("connectionRequestAccepted", portNumber.asInstanceOf[Object])
      portNumber += 1

      if (socket.isChannelOpen()) {
        logger.info(s"Disconnect of: ${staticSessionId}, in ConnectionRequestServer, " +
          s"after sending connectionRequestAccpeted, initiated by backend")
        socket.disconnect()
      }
    })

    connectionRequestServer.addDisconnectListener(socket => {
      if (socket.isChannelOpen()) {
        logger.info(s"Disconnect of: ${socket.getSessionId}, in ConnectionRequestServer, initiated by client")
        socket.disconnect()
      }
    })

    connectionRequestServer.start()
    logger.info("Ready for listening to initial connection requests")
  }

  override def postStop(): Unit = {
    logger.info("Shutdown connectionRequestServer")
    if (socket != null) socket.disconnect()
    connectionRequestServer.stop()

    logger.info("All actors terminated, terminate actor system now")
    context.system.terminate()
  }

}
