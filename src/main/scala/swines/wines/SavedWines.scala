package swines.wines

import java.nio.file.{Files, Paths}

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.alpakka.file.scaladsl.Directory
import akka.stream.scaladsl.{FileIO, Framing, Sink}
import akka.util.ByteString
import com.typesafe.scalalogging.Logger
import swines.cfg

import scala.concurrent.{ExecutionContext, Future}
import scala.util.matching.Regex

object SavedWines {
  private val log = Logger("WinesScraper")
  val isWineFile: Regex = "^.*[\\\\\\/]?wines-(\\d+)-(\\d+)-(\\d+)-\\.json$".r
  private val notFoundLogStr = "^.*Bad response\\(404 Not Found\\) wineryId\\=\\[(\\d+)\\].*$".r


  def listForRange(start: Int, stop: Int)
                  (implicit executionContext: ExecutionContext,
                   actorSystem: ActorSystem,
                   actorMaterializer: ActorMaterializer): Future[List[Int]] = {
    val notFoundWineryIdsF: Future[Set[Int]] =
      FileIO.fromPath(Paths.get("wines-scraper-.log"))
        .via(Framing.delimiter(ByteString("\n"), 1024, true))
        .map(_.utf8String)
        .filter(_.matches(notFoundLogStr.regex))
        .map { s =>
          s match {
            case notFoundLogStr(wineryId) => wineryId.toInt
          }
        }
        .runWith(Sink.seq)
        .map(_.toSet)
        .map { s => log.info(s"${s.size} ids with bad response (Not Found) are read from log"); s }

    val savedWineryIdsF: Future[Set[Int]] =
      Directory.ls(Paths.get(cfg.wines.warehouse))
        .filter(p => Files.isRegularFile(p))
        .map(p => p.getFileName.toString match {
          case isWineFile(wineryId, _, _) => Some(wineryId)
          case _ => None
        })
        .collect { case Some(wineryId) => wineryId.toInt }
        .runWith(Sink.seq)
        .map(_.toSet)

    for {
      badRespIds <- notFoundWineryIdsF
      savedIds <- savedWineryIdsF
    } yield {
      val idsRange = Range(start, stop + 1).toList
      val excludeIds = savedIds ++ badRespIds
      val idsToScrap = idsRange.filterNot(excludeIds)
      log.info(s"savedIds=${savedIds.size} NotFoundIds=${badRespIds.size} excludeIds=${excludeIds.size}")
      log.info(s"Found ${idsToScrap.length} ids to scrap for winery ids in [$start..$stop]")
      idsToScrap
    }
  }


}
