package actors

import akka.actor.SupervisorStrategy.Stop
import akka.actor.{Actor, ActorRef, Props, Terminated}
import models.{AnnounceFrontendCommunicationActorMessage, ChangeTweetFilterMessage, DetermineLocationRequestMessage, DetermineLocationResponseMessage, SentimentEvaluationRequestMessage, SentimentEvaluationResponseMessage, TwitterMessage}
import org.apache.logging.log4j.scala.Logging
import twitter4j.{ConnectionLifeCycleListener, FilterQuery, StallWarning, Status, StatusDeletionNotice, StatusListener, TwitterStream, TwitterStreamFactory}
import twitter4j.conf.{Configuration, ConfigurationBuilder}

import scala.util.control.Exception.allCatch


/**
  * This actor connects to the Twitter Streaming API and
  * is listening for new incoming Tweets.
  */
class TwitterReceiverActor extends Actor with Logging {
  val consumerKey = ""
  val consumerSecret = ""
  val accessToken = ""
  val accessTokenSecret = ""
  val config: Configuration = new ConfigurationBuilder()
    .setOAuthConsumerKey(consumerKey)
    .setOAuthConsumerSecret(consumerSecret)
    .setOAuthAccessToken(accessToken)
    .setOAuthAccessTokenSecret(accessTokenSecret)
    .build();


  var twitterStream: TwitterStream = _
  var frontendCommunicationActor: ActorRef = null
  var sentimentEvaluationActor: ActorRef = null
  var determineLocationActor: ActorRef = null
  var processEveryXTweet: () => Boolean = processEvery(20)

  def receive = {
    case ChangeTweetFilterMessage(hashtagFilters, languageFilters, quantityFilters) => {
      this.processEveryXTweet =
        if ((allCatch opt quantityFilters(0).toInt).isDefined)
          processEvery(quantityFilters(0).toInt)
        else
          processEveryXTweet

      var tweetFilterQuery = new FilterQuery()
      tweetFilterQuery.track(hashtagFilters: _*)
      tweetFilterQuery.language(languageFilters: _*)

      logger.trace(s"tweetFilterQuery: ${tweetFilterQuery}")
      twitterStream.filter(tweetFilterQuery)
    }
    case AnnounceFrontendCommunicationActorMessage(fca) => {
      frontendCommunicationActor = fca
    }
    case DetermineLocationResponseMessage(status, location) => {
      sentimentEvaluationActor ! new SentimentEvaluationRequestMessage(status.getText, location)
    }
    case SentimentEvaluationResponseMessage(message, location, evaledMessage) => {
      frontendCommunicationActor ! new TwitterMessage(message, location, evaledMessage)
    }
    case Stop => {
      twitterStream.clearListeners()
      twitterStream.cleanUp()
      twitterStream.shutdown()
      context.stop(sentimentEvaluationActor)
      context.stop(determineLocationActor)
    }
    case Terminated(who) => {
      logger.info(s"${who} died")
      context.unwatch(who)
      if (context.children.isEmpty) context.stop(self)
    }
    case _ => {
      logger.warn(s"Does nothing when receiving such a message")
    }
  }

  override def preStart(): Unit = {
    logger.info("Create SentimentEvaluationActor and DetermineLocationActor")
    sentimentEvaluationActor = context.actorOf(Props(new SentimentEvaluationActor()), name = "sea")
    determineLocationActor = context.actorOf(Props(new DetermineLocationActor()), name = "dla")
    context.watch(sentimentEvaluationActor)
    context.watch(determineLocationActor)

    logger.info("Initialize TwitterStream and add Listeners")
    twitterStream = new TwitterStreamFactory(config).getInstance

    twitterStream.addListener(new StatusListener() {

      override def onDeletionNotice(statusDeletionNotice: StatusDeletionNotice) = {}

      override def onScrubGeo(userId: Long, upToStatusId: Long) = {}

      override def onStatus(status: Status) = {
        logger.trace(s"Incoming Tweet from Twitter: ${status.getText()}")

        if (status.getUser.getLocation != null) {
          if (processEveryXTweet()) {
            determineLocationActor ! new DetermineLocationRequestMessage(status)
          }
        }
      }

      override def onTrackLimitationNotice(numberOfLimitedStatuses: Int) = {}

      override def onStallWarning(warning: StallWarning) = {}

      override def onException(ex: Exception) = {}
    })

    twitterStream.addConnectionLifeCycleListener(new ConnectionLifeCycleListener() {
      override def onCleanUp(): Unit = {
        logger.info(s"TwitterStream cleaned up of ${self}")
      }

      override def onConnect(): Unit = {
        logger.info(s"TwitterStream connected of ${self}")
      }

      override def onDisconnect(): Unit = {
        logger.info(s"TwitterStream disconnected of ${self}")
      }
    })
  }

  private def processEvery(number: Int) = {
    var counter = number

    () => {
      logger.trace(s"Tweet filter: ${counter}, ${number}")

      if (counter == number) {
        counter = 0
        true
      } else {
        counter += 1
        false
      }
    }
  }

  override def postStop(): Unit = {
    logger.debug("stopped")
  }
}
