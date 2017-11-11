name := "online-shop"
version := "0.1"
scalaVersion := "2.12.3"

resolvers += Resolver.jcenterRepo

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.5.6"
libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % "2.5.6" % "test"
libraryDependencies += "com.typesafe.akka" %% "akka-persistence" % "2.5.6"

libraryDependencies += "org.iq80.leveldb"            % "leveldb"          % "0.9"
libraryDependencies += "org.fusesource.leveldbjni"   % "leveldbjni-all"   % "1.8"

libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.4" % "test"

libraryDependencies += "com.github.dnvriend" %% "akka-persistence-inmemory" % "2.5.1.1"