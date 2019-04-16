package swines.addresses

import java.nio.charset.StandardCharsets

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import akka.stream.alpakka.file.scaladsl.Directory
import com.typesafe.scalalogging.Logger
import swines.wines.SavedWines
import java.nio.file.Files
import java.nio.file.Paths
import swines.proxies.MyHttpClient._

import akka.stream.scaladsl.Sink
import swines.addresses.SavedWinaries.WineryFileInfo

import concurrent.Future
import swines.cfg
import swines.proxies.Proxies
import swines.data.{JsonUtil, WineVintages}

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}
import swines.data.Wines
import swines.proxies.MyHttpClient

object ScrapAddresses {
  private val log = Logger("ScrapAddresses")


  def main(args: Array[String]): Unit = {
    implicit val ec = concurrent.ExecutionContext.global
    implicit val actorSystem = ActorSystem("SwinesActorSystem")
    implicit val actorMaterializer = ActorMaterializer()

    val proxyEndlessStream: Source[(String, Int), _] =
      Source.repeat(0)
        .flatMapConcat(_ => Proxies.allProxies(cfg.files.proxyList))

    Source.fromFuture(SavedWinaries.fromDirWithoutSaved(cfg.wines.warehouse, cfg.addresses.warehouse))
      .mapConcat(identity)
//    SavedWinaries.fromDir(cfg.wines.warehouse)
      .zip(proxyEndlessStream)
      .mapAsyncUnordered(cfg.addresses.scraper.parallelizm) {
        case (wfi@WineryFileInfo(wineryId, seoName, filename), (proxy, proxyPort)) =>
          val saveHtmlTo = Paths.get(cfg.addresses.warehouse, s"winary-$wineryId-$seoName.html")
          if (Files.exists(saveHtmlTo)) {
            val m = s"File $saveHtmlTo already exists!"
            log.error(m)
            Future.successful(None)
          } else {
            val url = s"https://www.vivino.com/wineries/${seoName}"
            MyHttpClient.get(url, "text/html", proxy, proxyPort)
              .map {
                case OK(html) =>
                  val bytes = html.getBytes(StandardCharsets.UTF_8)
                  Files.write(saveHtmlTo, bytes)
                  log.info(s"Ok: ${bytes.length} bytes is saved to $saveHtmlTo")
                  Some(wfi -> html)
                case Error(e) =>
                  log.error(s"Error for $wfi url=`$url` : ${e.getMessage} !")
                  None
                case BadResponse(statusCode, body) =>
                  log.error(s"BadResponse($statusCode) for $wfi url=`$url` !")
                  None
                case error =>
                  log.error(s"Error $error for $wfi url=`$url` !")
                  None
              }
          }
      }.collect { case Some(x) => x }
      .map {
        case (wfi@WineryFileInfo(wineryId, seoName, filename), html) =>
      }
      //      .runWith(Sink.foreach(println))
      .runWith(Sink.ignore)
      .onComplete { r =>
        log.info(s"Finished $r")
        actorSystem.terminate()
      }
  }

}
