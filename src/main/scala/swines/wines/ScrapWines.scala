package swines.wines

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}
import java.time.Instant

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import com.typesafe.scalalogging.Logger
import swines.cfg
import swines.data.{JsonUtil, Wines}
import swines.proxies.ClientJson.{BadResponse, Error, JsonOk}
import swines.proxies.{ClientJson, Proxies}

import scala.concurrent.Future

object ScrapWines {
  private val log = Logger("WinesScraper")
  private val myGlobalLog = Logger("MyGlobalLog")

  private def mkUrl(wineryId: Int) = {
    s"https://www.vivino.com/api/wineries/${wineryId}/wines?sort=most_rated&include_all_vintages=true"
    //    s"https://www.vivino.com/api/wineries/${wineryId}/wines?page=1&per_page=5&sort=most_rated&include_all_vintages=true"
    //    s"https://www.vivino.com/api/wineries/${wineryId}/wines?start_from=${from}&limit=${limit}&sort=most_rated&include_all_vintages=true"
    //    //    s"https://www.vivino.com/api/wineries/${wineryId}/wines?start_from=${from}&limit=${limit}&sort=most_rated&include_all_vintages=true"
  }

  private def mkSaveFilename(wineryId: Int, winesCount: Int) =
    s"${cfg.wines.warehouse}/wines-$wineryId-0-$winesCount-.json"


  def main(args: Array[String]): Unit = {
    import concurrent.ExecutionContext.Implicits.global
    implicit val system = ActorSystem("SwinesActorSystem")
    implicit val materializer = ActorMaterializer()

    val unscrapedWineriesIds = SavedWines
      .listForRange(
        cfg.wines.scrapper.wineryIds.start,
        cfg.wines.scrapper.wineryIds.stop)

    val proxyEndlessStream: Source[(String, Int), NotUsed] =
      Source.repeat(0).flatMapConcat(_ => Proxies.allProxies(cfg.files.proxyList))

    val wines = Source
      .fromFuture(unscrapedWineriesIds)
      .mapConcat(identity)
      .zip(proxyEndlessStream)
      .mapAsyncUnordered(cfg.wines.scrapper.parallelizm) {
        case (wineryId, (proxy, port)) =>
          val url = mkUrl(wineryId)
          val time = Instant.now
          ClientJson.getJson(url, proxy, port)
            .flatMap {
              case JsonOk(json) => Future {
                val wines = JsonUtil.fromJson[Wines](json)
                val winesCount = wines.wines.length
                val filename = mkSaveFilename(wineryId, winesCount)
                val p = Paths.get(filename)
                Files.write(p, json.getBytes(StandardCharsets.UTF_8))
                val m = s"$winesCount wines are successfully saved: $filename"
                log.info(m)
                myGlobalLog.info(m)
              }.recoverWith { case e =>
                log.error(s"Error handing response for $wineryId: ${e.getMessage} ")
                Future.failed(e)
              }
              case BadResponse(statusCode, body) =>
                Future {
                  log.error(s"Bad response($statusCode) wineryId=[$wineryId] proxy=$proxy:$port => $body\n")
                }
              case Error(e) =>
                Future(log.error(s"Exception wineryId=[$wineryId] proxy=$proxy:$port=> ${e.getMessage}\n"))

            }
      }


    wines.runWith(Sink.fold(0) { case (a, _) => a + 1 })
      .map { c => log.info(s"\nCount = $c\n") }
        .onComplete{r =>
          log.info(s"Finishing with $r")
          system.terminate()
        }


  }

}

