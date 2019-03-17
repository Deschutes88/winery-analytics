package swines.proxies

import java.nio.file.Paths

import akka.stream.scaladsl.{FileIO, Framing, Source}
import akka.util.ByteString

import scala.util.Try

object Proxies {

  def allProxies(filename: String): Source[(String, Int), _] = {
    FileIO.fromPath(Paths.get(filename))
      .via(Framing.delimiter(ByteString("\n"), 1024))
      .map(_.utf8String)
      .map { r =>
        Try {
          val cols = r.split("\\t")
          val h = cols(0)
          val p = cols(1).toInt
          h -> p
        }.toOption
      }
      .collect { case Some(proxy@(h, p)) => proxy }
  }

}
