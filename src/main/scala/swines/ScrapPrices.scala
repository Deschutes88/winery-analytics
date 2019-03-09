package swines

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}
import java.time.{Duration, Instant}

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.{ActorAttributes, ActorMaterializer, KillSwitches, Supervision}
import com.typesafe.scalalogging.Logger
import swines.data.cfg
import swines.proxies.ClientJson
import swines.proxies.ClientJson.{BadResponse, Error, JsonOk}
import swines.streams.DataToLoad
import swines.streams.DataToLoad.WineVintages

import scala.collection.immutable
import scala.concurrent.Future
import scala.io.StdIn

object ScrapPrices {
  val log = Logger("PricesScraper")

  def main(args: Array[String]): Unit = {
    import concurrent.ExecutionContext.Implicits.global
    implicit val system = ActorSystem("SwinesActorSystem")
    implicit val materializer = ActorMaterializer()

    val dataToLoad = streams.DataToLoad.dataToLoad().runWith(Sink.seq)
    val scrapedIds = streams.GlobalLog.succesfullWineIdsFromGlobalLog().runWith(Sink.seq)
    val savedIds = streams.WineIdsForSavedPrices.wineIdsForSavedPrices(cfg.files.warehouse).runWith(Sink.seq)

    val winesToLoad: Future[immutable.Seq[DataToLoad.WineVintages]] = for {
      d <- dataToLoad
      s <- scrapedIds
      ss <- savedIds
      sSet = s.toSet ++ ss.toSet
    } yield {
      log.info(s"${ss.size} wines prices ids are read from json files in folder ${cfg.files.warehouse}")
      d.filter(r => !sSet.contains(r.wineId))
    }

    val proxyEndlessStream: Source[(String, Int), NotUsed] =
      Source.repeat(0).flatMapConcat(_ => streams.Proxies.allProxies(cfg.files.proxyList))

    def mkSaveFilename(wineId: Int) = s"${cfg.files.warehouse}/wine-${wineId}-prices-.json"

    val stopDecider: Supervision.Decider = {
      case _ => Supervision.Stop
    }

    val ks = KillSwitches.shared("kill-prices")

    val prices = Source.fromFuture(winesToLoad)
      .mapConcat(identity)
      .zip(proxyEndlessStream)
      .via(ks.flow)
      .mapAsyncUnordered(cfg.pricesScrapper.parallelizm) { case (review, (proxy, port)) =>
        val url = "https://www.vivino.com/api/prices?" + review.vintagesIds.map { vintage => s"vintage_ids[]=$vintage" }.mkString("&")
        println(s"querying $url")
        val time = Instant.now
        ClientJson.getJson(url, proxy, port)
          .map { r =>
            val time2 = Instant.now()
            println(s"Response time for [${review.wineId}] is ${Duration.between(time, time2).getSeconds} sec. through $proxy:$port. $r\n")
            review.wineId -> r
          }
      }
      .mapAsyncUnordered(cfg.pricesScrapper.parallelizm / 2) {
        case (wineId, JsonOk(json)) =>
          Future {
            val fname = mkSaveFilename(wineId)
            Files.write(Paths.get(fname),
              json.getBytes(StandardCharsets.UTF_8))
            log.info(s"Prices for wineId=[$wineId] are saved to $fname")
          }
        case (wineId, BadResponse(statusCode, body)) =>
          Future{
            ks.shutdown()
            log.error(s"\nBad response for wineId=[$wineId] code=$statusCode: $body\n")
          }
        case (wineId, Error(except)) => Future(log.error(s"\nException for wineId=[$wineId] => ${except.getMessage}\n"))
      }.withAttributes(ActorAttributes.supervisionStrategy(stopDecider))

    prices.runWith(Sink.fold(0) { case (a, _) => a + 1 })
      .map { c => println(s"\nCount = $c\n") }

    StdIn.readLine("Enter to quit.")
    system.terminate()


  }

}
