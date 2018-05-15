import java.util.concurrent.TimeUnit

import actors.ConnectionListenerActor
import akka.actor.{ActorSystem, Props}

import scala.concurrent.Await
import scala.concurrent.duration.Duration


object Main extends App {
  // PythonLoader.apply.runSentimentModel("Just to force loading theano backend")
  implicit val system = ActorSystem("TwitterSystem")
  val connectionListenerActor = system.actorOf(Props[ConnectionListenerActor], name = "connectionListenerActor")
  //Await.ready(system.whenTerminated, Duration(1, TimeUnit.MINUTES))
}


