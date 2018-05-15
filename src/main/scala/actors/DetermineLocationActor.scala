package actors

import akka.actor.Actor
import models.{DetermineLocationRequestMessage, DetermineLocationResponseMessage}
import org.apache.logging.log4j.scala.Logging
import java.net.ConnectException

import scalaj.http.{Http, HttpResponse}

class DetermineLocationActor extends Actor with Logging {

  override def receive = {
    case DetermineLocationRequestMessage(status) => {
      logger.trace(s"Determine location of tweet")

      try {
        // blocking call - could happen that openstreetmap blocks it, because of too much requests
        val response: HttpResponse[String] = Http("https://nominatim.openstreetmap.org/search")
          .param("q", status.getUser.getLocation)
          .param("format", "json")
          .param("addressdetails", "1")
          .asString

        sender() ! new DetermineLocationResponseMessage(status, response.body)
      } catch {
        case ce: ConnectException => logger.error(s"Request to OpenStreetMap failed - ${ce.getMessage}")
        case _: Throwable => logger.error(s"Got some other kind of exception")
      }
    }
    case _ => {
      logger.warn(s"Does nothing when receiving such a message")
    }
  }

  override def postStop(): Unit = {
    logger.debug("stopped")
  }

}
