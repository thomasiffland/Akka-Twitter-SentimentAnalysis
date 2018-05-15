name := "TwitterFeed"

version := "0.1"

scalaVersion := "2.12.4"


javaOptions in run += "-Djava.library.path=./customlib"
unmanagedBase := baseDirectory.value / "customlib"


// https://mvnrepository.com/artifact/com.twitter/hbc-core
libraryDependencies += "com.twitter" % "hbc-core" % "2.2.0"

// https://mvnrepository.com/artifact/org.twitter4j/twitter4j-core
libraryDependencies += "org.twitter4j" % "twitter4j-core" % "4.0.6"

// https://mvnrepository.com/artifact/org.twitter4j/twitter4j-stream
libraryDependencies += "org.twitter4j" % "twitter4j-stream" % "4.0.6"


// https://mvnrepository.com/artifact/com.typesafe.akka/akka-actor
libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.5.9"


// https://mvnrepository.com/artifact/com.typesafe.play/play-json
libraryDependencies += "com.typesafe.play" %% "play-json" % "2.6.8"


libraryDependencies +=  "org.scalaj" %% "scalaj-http" % "2.3.0"

// https://mvnrepository.com/artifact/com.corundumstudio.socketio/netty-socketio
libraryDependencies += "com.corundumstudio.socketio" % "netty-socketio" % "1.7.13"
// https://mvnrepository.com/artifact/io.netty/netty-transport
libraryDependencies += "io.netty" % "netty-transport" % "4.1.15.Final"


libraryDependencies += "com.typesafe.akka" %% "akka-stream" % "2.5.3"

// https://mvnrepository.com/artifact/com.typesafe.akka/akka-http
libraryDependencies += "com.typesafe.akka" %% "akka-http" % "10.0.11"

libraryDependencies += "org.apache.logging.log4j" % "log4j-api" % "2.10.0"
libraryDependencies += "org.apache.logging.log4j" % "log4j-core" % "2.10.0"
libraryDependencies += "org.apache.logging.log4j" %% "log4j-api-scala" % "11.0"

// https://mvnrepository.com/artifact/org.slf4j/slf4j-simple
libraryDependencies += "org.slf4j" % "slf4j-simple" % "1.7.25"
// https://mvnrepository.com/artifact/org.slf4j/slf4j-api
libraryDependencies += "org.slf4j" % "slf4j-api" % "1.7.25"
