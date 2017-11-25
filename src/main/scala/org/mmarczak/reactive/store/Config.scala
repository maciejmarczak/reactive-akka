package org.mmarczak.reactive.store

import com.typesafe.config.{Config, ConfigFactory}

object Config {

  private lazy val config = ConfigFactory.load()

  lazy val localConfig: Config = config.getConfig("local").withFallback(config)
  lazy val catalogConfig: Config = config.getConfig("catalog").withFallback(config)

  lazy val catalogPath: String = config.getString("catalog.path")
  lazy val minKeywordLen: Int = config.getInt("catalog.keyword.minlen")
  lazy val minEntryLen: Int = config.getInt("catalog.entry.minlen")

  lazy val cartTimeout: Long  = config.getLong("storeApp.cart.timeout")
  lazy val checkoutTimeout: Long = config.getLong("storeApp.checkout.timeouts.checkout")
  lazy val paymentTimeout: Long = config.getLong("storeApp.checkout.timeouts.payment")
}
