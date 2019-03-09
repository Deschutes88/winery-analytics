package swines

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}
import java.time.{Duration, Instant}
import java.util.concurrent.atomic.{AtomicBoolean, AtomicInteger}

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.{ActorAttributes, ActorMaterializer, KillSwitches, Supervision}
import com.typesafe.scalalogging.Logger
import swines.proxies.ClientJson
import swines.proxies.ClientJson.{BadResponse, Error, JsonOk}
import swines.streams.{DataToLoad, GlobalLog}
import swines.streams.DataToLoad.WineVintages

import scala.collection.immutable
import scala.concurrent.Future
import scala.io.StdIn
import swines.data.{JsonUtil, Reviews, cfg}

import scala.util.{Failure, Try}

object ScrapReviews {
  //  val WAREHOUSE = cfg.files.warehouse////////////////////////////////////////////////
  val REVIEWS_URL = "https://www.vivino.com/api/wines/%d/reviews?wine_id=%d&year=null&page=%d"
  val myLog = cfg.files.myGlobalLog
  val proxyList = cfg.files.proxyList
  val myGlobalLog = Logger("MyGlobalLog")

  def main(args: Array[String]): Unit = {
    import concurrent.ExecutionContext.Implicits.global
    implicit val system = ActorSystem("SwinesActorSystem")
    implicit val materializer = ActorMaterializer()

    val dataToLoad = streams.DataToLoad.dataToLoad().runWith(Sink.seq)
    val scrapedIds = streams.GlobalLog.succesfullWineIdsFromGlobalLog()
      .concat(streams.GlobalLog.succesfullWineIdsFromGlobalLog(cfg.files.myGlobalLog)).runWith(Sink.seq)

    val winesToLoad: Future[immutable.Seq[DataToLoad.WineVintages]] = for {
      d <- dataToLoad
      s <- scrapedIds
      sSet = s.toSet
    } yield {
      val w=d.filter(r => !sSet.contains(r.wineId))
      myGlobalLog.info(s"Loaded ${w.length} wines to scrap reviews = ${d.length}-${s.length}")
      w
    }

    val proxyEndlessStream: Source[(String, Int), NotUsed] =
      Source.repeat(streams.Proxies.allProxies(proxyList)).flatMapConcat(identity)

    def mkSaveFilename(wineId: Int) = s"${cfg.files.warehouse}/wine-${wineId}-prices-.json"

    val stopDecider: Supervision.Decider = {
      case _ => Supervision.Stop
    }

    val killSwitch = KillSwitches.shared("stop-stream")

    val reviews = Source.fromFuture(winesToLoad)
      .mapConcat(identity)
      .zip(proxyEndlessStream)
      .via(killSwitch.flow)
      .mapAsyncUnordered(cfg.reviewsScrapper.parallelizm) { case (WineVintages(wineId, vintagesIds), (proxy: String, port: Int)) =>
        val confirmedSuccessfulLastPageNumber = new AtomicInteger(-1)
        Source.unfoldAsync(1) { pageNo =>
          val reviewsUrl = REVIEWS_URL.format(wineId, wineId, pageNo)
          val logMsgId = s"wineId=[$wineId] page=$pageNo proxy=$proxy:$port"
          val time = Instant.now()
          ClientJson.getJson(reviewsUrl, proxy, port)
            .map {
              case JsonOk(json) =>
                Try(JsonUtil.fromJson[Reviews](json))
                  .recoverWith { case exception =>
                    killSwitch.shutdown()
                    myGlobalLog.error(s"Error parsing json $logMsgId: ${exception.getMessage}")
                    Failure(exception)
                  }
                  .toOption
                  .flatMap { r =>
                    val time2 = Instant.now()
                    val duration = Duration.between(time, time2).getSeconds
                    println(s"Response time $duration secs for $logMsgId");
                    if (r.reviews.nonEmpty) {
                      Some(pageNo + 1, json)
                    } else {
                      confirmedSuccessfulLastPageNumber.set(pageNo - 1)
                      None
                    }
                  }
              case BadResponse(statusCode, body) =>
//                killSwitch.shutdown()
                myGlobalLog.error(s"Bad response for $logMsgId: statusCode=$statusCode: $body")
                None
              case Error(except) =>
                myGlobalLog.error(s"Exception for $logMsgId: ${except.getMessage}")
                None
            }
        }
          .runWith(Sink.seq)
          .map {
            case reviews =>
              val cslpn = confirmedSuccessfulLastPageNumber.get()
              val numberOfScrapedPages = reviews.length
              if (reviews.length == cslpn) {
                reviews.zipWithIndex
                  .map { case (json, pageNo) =>
                    val saveToPath = Paths.get(cfg.files.warehouse, s"wine-${wineId}-reviews-page-${pageNo + 1}-.json")
                    Files.write(saveToPath, json.getBytes(StandardCharsets.UTF_8))
                  }
                myGlobalLog.info(GlobalLog.successMsg(wineId, cslpn))
              } else {
                myGlobalLog.error(s"Error getting reviews for wineId=[$wineId] pages=$numberOfScrapedPages confirmedPage=$cslpn")
              }
              wineId -> reviews
          }
      }
      .runWith(Sink.ignore)
        .onComplete{_=>
          system.terminate()
          StdIn.readLine("Press ENTER to quit.")
        }
    StdIn.readLine("Press ENTER to quit.")
    system.terminate()


  }

}
