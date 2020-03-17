package com.bgasparotto.sparkstreaming.infrastructure

import com.bgasparotto.sparkstreaming.parser.FileParser

object TwitterConfig {

  /** Configures Twitter service credentials using twitter.txt in the main workspace directory */
  def setupTwitter(): Unit = {

    for (line <- FileParser.open("twitter.txt").getLines) {
      val fields = line.split(" ")
      if (fields.length == 2) {
        System.setProperty("twitter4j.oauth." + fields(0), fields(1))
      }
    }
  }
}
