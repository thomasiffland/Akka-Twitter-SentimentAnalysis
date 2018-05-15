package actors

import akka.actor.Actor
import models.{SentimentEvaluationRequestMessage, SentimentEvaluationResponseMessage}
import org.apache.logging.log4j.scala.Logging

import scalaj.http.HttpResponse
import scalaj.http.Http

class SentimentEvaluationActor extends Actor with Logging {

  override def receive = {
    case SentimentEvaluationRequestMessage(message, location) => {
      logger.trace(s"Evaluate tweet, whether it is positive or negative")

      //TODO: test language, if it is not english - the tweet have to be translated before
      //sender() ! new SentimentEvaluationResponseMessage(message, location, pythonLoader.runSentimentModel(message))
      val response: HttpResponse[String] = Http("http://localhost:5000")
                  .param("q", message)
                  .asString

      sender() ! new SentimentEvaluationResponseMessage(message, location, response.body)
    }
    case _ => {
      logger.warn(s"Does nothing when receiving such a message")
    }
  }

  override def postStop(): Unit = {
    logger.debug("stopped")
  }

}
