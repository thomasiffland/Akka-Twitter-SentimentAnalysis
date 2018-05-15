package models

import akka.actor.ActorRef
import twitter4j.Status

case class TwitterMessage(message: String, location: String, emotion: String);

case class ChangeTweetFilterMessage(hashtagFilters: List[String], languageFilters: List[String],
                                    quantityFilters: List[String])

case class AnnounceFrontendCommunicationActorMessage(actorRef: ActorRef)

case class SentimentEvaluationRequestMessage(message: String, location: String)

case class SentimentEvaluationResponseMessage(message: String, location: String,
                                              evaledMessage: String)

case class DetermineLocationRequestMessage(status: Status)

case class DetermineLocationResponseMessage(status: Status, location: String)

case class ShutdownCommand()