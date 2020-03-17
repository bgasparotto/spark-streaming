package com.bgasparotto.sparkstreaming.parser

import java.nio.charset.CodingErrorAction

import scala.io.{Codec, Source}

object FileParser {
  implicit val codec: Codec = Codec("UTF-8")
  codec.onMalformedInput(CodingErrorAction.REPLACE)
  codec.onUnmappableCharacter(CodingErrorAction.REPLACE)

  def open(path: String): Source = {
    Source.fromFile(path)
  }
}
