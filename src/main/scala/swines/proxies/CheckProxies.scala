package swines.proxies

import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import java.nio.file.{OpenOption, Paths, StandardOpenOption}
import java.time.{Instant, ZoneId}
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicLong

import akka.actor.ActorSystem
import akka.http.scaladsl.{ClientTransport, Http}
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.HttpEncoding
import akka.http.scaladsl.settings.{ClientConnectionSettings, ConnectionPoolSettings}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{FileIO, Sink, Source}
import akka.util.ByteString
import swines.cfg
import swines.proxies.MyHttpClient.{Error, OK}

import concurrent.duration._
import scala.concurrent.Future
import scala.io.StdIn
import scala.util.{Failure, Success}
import Proxies._

object CheckProxies {
  val counter = new AtomicLong(0)

  def main(args: Array[String]): Unit = {
    import concurrent.ExecutionContext.Implicits.global
    implicit val system = ActorSystem("SwinesActorSystem")
    implicit val materializer = ActorMaterializer()
    val dateTimeFormatter = DateTimeFormatter
      .ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    val proxies = allProxies(cfg.files.uncheckedProxyList)
      .runWith(Sink.seq)
      .map { proxies =>
        val unique = proxies.distinct
        println(s"Loaded ${unique.length} proxies to test from `${cfg.files.uncheckedProxyList}`.")
        unique
      }

    val checkedProxies = Source.fromFuture(proxies)
      .mapConcat(identity)
      .mapAsyncUnordered(cfg.checkProxies.parallelizm) { case proxy@(host, port) =>
        val url = cfg.checkProxies.url
        MyHttpClient.getJson(url, host, port).map {
          case OK(s) => Right(proxy)
          case e => Left(e.toString)
        }
      }
      .map { r => println(r); r }
      .map {
        case Right((proxy, port)) =>
          val time = dateTimeFormatter.format(Instant.now)
          counter.incrementAndGet()
          ByteString(s"$proxy\t$port\t$time\n")
        case _ => ByteString.empty
      }
      .filter(_.nonEmpty)
//      .runWith(Sink.seq)
//      .map { p =>
//        println(s"There are ${p.length} working proxies `${cfg.files.uncheckedProxyList}`")
//        p
//      }
//
//    Source.fromFuture(checkedProxies)
//      .mapConcat(identity)
      .runWith(FileIO.toPath(Paths.get(cfg.files.checkedProxyList)))
      .onComplete { _ =>
        println(s"Finished! Checked proxies ${counter.get()} are written in `${cfg.files.checkedProxyList}`")
        system.terminate()
      }

//    StdIn.readLine("Enter to quit")
//    system.terminate()
  }

}
