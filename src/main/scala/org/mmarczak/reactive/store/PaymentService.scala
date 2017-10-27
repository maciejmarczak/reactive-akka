package org.mmarczak.reactive.store

import akka.actor.{Actor, Props}
import org.mmarczak.reactive.store.CheckoutProtocol.PaymentReceived
import org.mmarczak.reactive.store.CustomerProtocol.PaymentConfirmed
import org.mmarczak.reactive.store.PaymentProtocol.DoPayment

object PaymentService {
  def props(): Props = Props[PaymentService]
}

class PaymentService extends Actor {

  import context._
  override def receive: Receive = {
    case DoPayment => {
      sender ! PaymentConfirmed
      parent ! PaymentReceived
    }
  }

}
