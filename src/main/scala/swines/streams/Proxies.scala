package swines.streams

import java.nio.file.Paths

import akka.stream.IOResult
import akka.stream.scaladsl.{FileIO, Framing, Source}
import akka.util.ByteString
import swines.data.cfg

import scala.concurrent.Future
import scala.util.Try

object Proxies {

  def allProxies(filename: String = cfg.files.uncheckedProxyList): Source[(String, Int), _] = {
//  def allProxies(filename: String = "cleaned-super"): Source[(String, Int), _] = {
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
