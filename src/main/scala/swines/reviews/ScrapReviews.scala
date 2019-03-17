package swines.reviews

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}
import java.time.{Duration, Instant}
import java.util.concurrent.atomic.AtomicBoolean

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.{ActorMaterializer, KillSwitches, Supervision}
import com.typesafe.scalalogging.Logger
import swines.data.{JsonUtil, Reviews, WineVintages}
import swines.proxies.{ClientJson, Proxies}
import swines.proxies.ClientJson.{BadResponse, Error, JsonOk}
import swines.{cfg}

import scala.io.StdIn
import scala.util.{Failure, Try}

object ScrapReviews {
  val REVIEWS_URL = "https://www.vivino.com/api/wines/%d/reviews?wine_id=%d&year=null&page=%d"
  //  val myLog = cfg.files.myGlobalLog
  val proxyList = cfg.files.proxyList
  val myGlobalLog = Logger("MyGlobalLog")
  val log = Logger("ReviewsScraper")

  def main(args: Array[String]): Unit = {
    import concurrent.ExecutionContext.Implicits.global
    implicit val system = ActorSystem("SwinesActorSystem")
    implicit val materializer = ActorMaterializer()

    val winesToLoad = SavedReviews.unloadedReviews

    val proxyEndlessStream: Source[(String, Int), NotUsed] =
      Source.repeat(Proxies.allProxies(proxyList)).flatMapConcat(identity)

    def mkSaveFilename(wineId: Int) = s"${cfg.reviews.warehouse}/wine-${wineId}-prices-.json"

    val stopDecider: Supervision.Decider = {
      case _ => Supervision.Stop
    }

    val killSwitch = KillSwitches.shared("stop-stream")

    val reviews =
      Source.fromFuture(winesToLoad)
        .mapConcat(identity)
        .zip(proxyEndlessStream)
        .via(killSwitch.flow)
        .mapAsyncUnordered(cfg.reviews.scrapper.parallelizm) {
          case ((WineVintages(wineId, vintagesIds), startPageNo), (proxy: String, port: Int)) =>
            val successfulFinish = new AtomicBoolean(false)
            Source.unfoldAsync(startPageNo) { pageNo =>
              val reviewsUrl = REVIEWS_URL.format(wineId, wineId, pageNo)
              val logMsgId = s"wineId=[$wineId] page=$pageNo proxy=$proxy:$port"
              val time = Instant.now()
              ClientJson.getJson(reviewsUrl, proxy, port)
                .map {
                  case JsonOk(json) =>
                    Try(JsonUtil.fromJson[Reviews](json))
                      .recoverWith { case exception =>
                        log.error(s"Error parsing json $logMsgId: ${exception.getMessage}")
                        Failure(exception)
                      }
                      .toOption
                      .flatMap { r =>
                        val time2 = Instant.now()
                        val duration = Duration.between(time, time2).getSeconds
                        println(s"Response time $duration secs for $logMsgId");
                        if (r.reviews.nonEmpty) {
                          Some(pageNo + 1, pageNo -> json)
                        } else {
                          successfulFinish.set(true)
                          None
                        }
                      }
                  case BadResponse(statusCode, body) =>
                    //                killSwitch.shutdown()
                    val m = s"Bad response for $logMsgId: statusCode=$statusCode: $body"
                    log.error(m)
                    None
                  case Error(except) =>
                    val m = s"Exception for $logMsgId: ${except.getMessage}"
                    log.error(m)
                    None
                }
            }
              .runWith(Sink.seq)
              .map {
                case reviews =>
                  val allPagesAreDownloaded = successfulFinish.get()
                  if (reviews.nonEmpty) {
                    reviews.zipWithIndex
                      .map { case ((pageNo, json), idx) =>
                        val saveToPath = Paths.get(cfg.reviews.warehouse, s"wine-${wineId}-reviews-page-${pageNo}-.json")
                        Files.write(saveToPath, json.getBytes(StandardCharsets.UTF_8))
                      }
                  }
                  if (allPagesAreDownloaded) {
                    val numberOfScrapedPages = reviews.length + startPageNo - 1
                    val m = GlobalLog.successMsg(wineId, numberOfScrapedPages)
                    myGlobalLog.info(m)
                    log.info(m)
                  }
                  wineId -> reviews
              }
        }
        .runWith(Sink.ignore)
        .onComplete { _ =>
          system.terminate()
          StdIn.readLine("Press ENTER to quit.")
        }
    StdIn.readLine("Press ENTER to quit.")
    system.terminate()


  }

}
