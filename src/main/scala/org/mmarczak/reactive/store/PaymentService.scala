package org.mmarczak.reactive.store

import akka.actor.SupervisorStrategy.{Restart, Stop}
import akka.actor.{Actor, ActorLogging, OneForOneStrategy, Props, SupervisorStrategy}
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import akka.util.ByteString
import org.mmarczak.reactive.store.CheckoutProtocol.{CheckoutCancelled, PaymentReceived}
import org.mmarczak.reactive.store.PaymentProtocol.{DoPayment, Done}

object PaymentService {
  def props(): Props = Props[PaymentService]
}

class PaymentService extends Actor with ActorLogging {

  private val httpClient = if (Config.testEnv) Props[MockHttpClient] else Props[HttpClient]

  import context._
  override val supervisorStrategy: SupervisorStrategy =
    OneForOneStrategy(loggingEnabled = true) {
    case _: PaymentException => {
      log.info("Payment Exception! Restarting HttpClient actor...")
      Restart
    }
    case _ => {
      log.info("Processing Exception! Cancelling checkout...")
      parent ! CheckoutCancelled
      Stop
    }
  }

  override def receive: Receive = {
    case DoPayment => {
      log.info("Starting HttpClient instance.")
      context.actorOf(httpClient, "HttpClient")
    }
    case Done => {
      log.info("Correctly handled transaction.")
      parent ! PaymentReceived
    }
  }

}

import akka.http.scaladsl.Http
import akka.http.scaladsl.model._

class MockHttpClient extends Actor {

  override def receive: Receive = {
    case _ => ()
  }

  override def preStart(): Unit = context.parent ! Done
}

class HttpClient extends Actor with ActorLogging {

  import akka.pattern.pipe
  import context.dispatcher

  implicit val materializer: ActorMaterializer =
    ActorMaterializer(ActorMaterializerSettings(context.system))
  val http = Http(context.system)

  override def preStart(): Unit = {
    log.info("Issuing GET request")
    http.singleRequest(HttpRequest(uri = "http://localhost:8080/pay")).pipeTo(self)
  }

  override def receive: Receive = {
    case resp @ HttpResponse(StatusCodes.OK, _, entity, _) =>
      entity.dataBytes.runFold(ByteString(""))(_ ++ _).foreach(body => {
        log.info("Received following JSON: " + body.utf8String)
        if (body.utf8String.contains("success")) {
          context.parent ! Done
          resp.discardEntityBytes()
          shutdown()
        }
        else
          self ! "failure"
      })
    case "failure" => throw new PaymentException
  }

  private def shutdown(): Unit = {
    context.stop(self)
  }
}

class PaymentException extends Exception {}