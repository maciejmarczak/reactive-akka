package org.mmarczak.reactive.store

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer

import scala.io.StdIn
import scala.util.Random

object PaymentServer {
  def main(args: Array[String]) {

    implicit val system = ActorSystem("my-system")
    implicit val materializer = ActorMaterializer()

    implicit val executionContext = system.dispatcher

    val route =
      path("pay") {
        get {
          val status = if (Random.nextInt(100) > 50) "success" else "failure"
          val responseJson = "{ \"status\": \"" + status + "\" }"

          // mock some processing...
          Thread.sleep(1000)
          complete(HttpEntity(ContentTypes.`application/json`, responseJson))
        }
      }

    val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)

    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
  }
}
