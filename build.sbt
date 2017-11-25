name := "online-shop"
version := "0.1"
scalaVersion := "2.12.3"

val akkaVersion = "2.5.6"
val akkaHttpVersion = "10.0.10"

resolvers += Resolver.jcenterRepo

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % akkaVersion
libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test"
libraryDependencies += "com.typesafe.akka" %% "akka-persistence" % akkaVersion
libraryDependencies += "com.typesafe.akka" %% "akka-remote" % akkaVersion
libraryDependencies += "com.typesafe.akka" %% "akka-http"   % akkaHttpVersion

libraryDependencies += "org.iq80.leveldb"            % "leveldb"          % "0.9"
libraryDependencies += "org.fusesource.leveldbjni"   % "leveldbjni-all"   % "1.8"

libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.4" % "test"

libraryDependencies += "com.github.dnvriend" %% "akka-persistence-inmemory" % "2.5.1.1"

parallelExecution in Test := false