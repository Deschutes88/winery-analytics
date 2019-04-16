package swines.prices

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import com.typesafe.scalalogging.Logger
import swines.data.WineVintages
import swines.proxies.{MyHttpClient, Proxies}
import swines.proxies.MyHttpClient.{BadResponse, Error, OK}
import swines.reviews.SavedWineWintages
import swines.{cfg}

import scala.concurrent.Future

object ScrapPrices {
  val log = Logger("PricesScraper")

  def main(args: Array[String]): Unit = {
    import concurrent.ExecutionContext.Implicits.global
    implicit val system = ActorSystem("SwinesActorSystem")
    implicit val materializer = ActorMaterializer()

    log.info("Starting ...")
    val wineVintagesF: Future[Seq[WineVintages]] =
      SavedWineWintages.loadWineVintages(cfg.files.winesVintages).runWith(Sink.seq)
//        .recoverWith { case e =>
//          log.error(s"Error while reading `${cfg.files.winesVintages}: ${e.getMessage}")
//          Future.failed(e)
//        }
    val savedIds: Future[Seq[Int]] =
      SavedPrices.wineIdsForSavedPrices(cfg.prices.warehouse)
        .runWith(Sink.seq)
//        .recoverWith { case e =>
//          log.error(s"Error reading scraped ids from `${cfg.prices.warehouse}: ${e.getMessage}")
//          Future.failed(e)
//        }

    val wineVintagesToLoadF: Future[Set[WineVintages]] = (for {
      wv: Set[WineVintages] <- wineVintagesF.map(_.toSet)
      uniqueWineIdsNumber = wv.map(_.wineId).toSet.size
      s: Set[Int]           <- savedIds.map(_.toSet)
    } yield {
      log.info(s"`${cfg.files.winesVintages}` contains ${wv.size} rows with $uniqueWineIdsNumber unique ids.")
      log.info(s"`${cfg.prices.warehouse}` contains ${s.size} saved elements.")
      val toLoad: Set[WineVintages] = wv.filterNot(r => s.contains(r.wineId))
      log.info(s"${toLoad.size} prices ids are to be loaded")
      toLoad
    }).recoverWith { case e =>
      log.error(s"Error while calculating wines to load: ${e.getMessage}")
      Future.failed(e)
    }

    val proxyEndlessStream: Source[(String, Int), NotUsed] =
      Source.repeat(0)
        .flatMapConcat(_ => Proxies.allProxies(cfg.files.proxyList))

    def mkSaveFilename(wineId: Int) =
      s"${cfg.prices.warehouse}/wine-${wineId}-prices-.json"

    val prices = Source.fromFuture(wineVintagesToLoadF)
      .mapConcat(identity)
      .zip(proxyEndlessStream)
      .mapAsyncUnordered(cfg.prices.scrapper.parallelizm) { case (review, (proxy, port)) =>
        val url = "https://www.vivino.com/api/prices?" + review.vintagesIds.map { vintage => s"vintage_ids[]=$vintage" }.mkString("&")
        val wineId = review.wineId
        MyHttpClient.getJson(url, proxy, port)
          .map {
            case OK(json) =>
              Future {
                val fname = mkSaveFilename(wineId)
                val p = Paths.get(fname)
                if (Files.notExists(p)) {
                  Files.write(p, json.getBytes(StandardCharsets.UTF_8))
                  log.info(s"Ok: Prices for wineId=[$wineId] (${json.length} bytes) are saved to $fname")
                } else {
                  log.error(s"Error file `$p` already exists!")
                }
              }
            case BadResponse(statusCode, body) =>
              Future {
                log.error(s"\nBad response for wineId=[$wineId] code=$statusCode: $body\n")
              }
            case Error(except) =>
              Future(log.error(s"\nException for wineId=[$wineId] => ${except.getMessage}\n"))
          }
      }
    //      .withAttributes(ActorAttributes.supervisionStrategy(stopDecider))

    prices.runWith(Sink.fold(0) { case (a, _) => a + 1 })
      .map { c => log.info(s"Processed row number = $c\n") }

      .onComplete { r =>
        log.info(r.toString)
        system.terminate()
      }


  }

}
