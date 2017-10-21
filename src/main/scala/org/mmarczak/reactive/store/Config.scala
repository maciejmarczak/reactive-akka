package org.mmarczak.reactive.store

import com.typesafe.config.ConfigFactory

object Config {

  private lazy val config = ConfigFactory.load()

  lazy val systemName: String = config.getString("storeApp.name")
  lazy val cartTimeout: Long  = config.getLong("storeApp.cart.timeout")
  lazy val checkoutTimeout: Long = config.getLong("storeApp.checkout.timeouts.checkout")
  lazy val paymentTimeout: Long = config.getLong("storeApp.checkout.timeouts.payment")
}
