package org.mmarczak.reactive.store

import akka.actor.{Actor, Props}
import org.mmarczak.reactive.store.ProductCatalogProtocol.FindItems

import scala.io.Source

object ProductCatalog {
  def props(): Props = Props[ProductCatalog]
}

class ProductCatalog extends Actor {

  override def receive: Receive = {
    case FindItems(query) =>
      sender ! findByQuery(query)
  }

  private def findByQuery(query: String): List[Item] = {
    val queryKeywords = toKeywordSet(query)

    val itemsMatches = Source.fromResource(Config.catalogFilename)
      .getLines
      .filter(_.length >= Config.minEntryLen)
      .map(line => {
        val item = parseItem(line)
        val nameKeywords = toKeywordSet(item.name)

        val matches = (nameKeywords intersect queryKeywords).size

        (item, matches)
      })
      .toList
      .groupBy(_._1.name)
      .mapValues(_.head)
      .values
      .toList

    itemsMatches.sortBy(-_._2).map(_._1).take(Config.resultSize)
  }

  private def parseItem(line: String): Item  = {
    val cols = line.split(",", 2).map(col => col.replaceAll("\"", ""))
    Item(cols(0), cols(1).replaceAll(",", " "))
  }

  private def toKeywordSet(str: String): Set[String] = {
    str
      .split(' ')
      .filter(_.length > Config.minKeywordLen)
      .map(_.trim.toLowerCase)
      .toSet
  }
}
