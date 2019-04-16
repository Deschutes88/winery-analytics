package swines.addresses

import java.nio.file.{Files, Paths}

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.alpakka.file.scaladsl.Directory
import akka.stream.scaladsl.{Sink, Source}
import com.typesafe.scalalogging.Logger
import swines.cfg
import swines.data.{JsonUtil, Wine, Winery, Wines}
import swines.wines.SavedWines

import scala.collection.JavaConverters._
import scala.collection.immutable
import scala.concurrent.{ExecutionContext, Future}
import scala.util.matching.Regex
import scala.util.{Failure, Try}

object SavedWinaries {
  private val log = Logger("ScrapAddresses")
  val isHtmlFile: Regex = "winary-(\\d+)-(.*?).html".r

  case class WineryFileInfo(wineryId: Int, seoName: String, filename: String)

  def fromDir(wineryWarehouseDir: String)(
    implicit executionContext: ExecutionContext
  ): Source[WineryFileInfo, _] = {
    val warehouse = Paths.get(wineryWarehouseDir)
    log.info(s"Reading wineries from dir $warehouse ...")
    Directory.ls(warehouse)
      .filter { p => p.getFileName.toString.matches(SavedWines.isWineFile.regex) }
      .mapAsyncUnordered(cfg.addresses.scraper.parallelizm) { p =>
        Future {
//          log.info(s"Reading `$p` ...")
          val jsonText = Files.readAllLines(p).asScala.mkString("\n")
          (Try {
            val wines: Wines = JsonUtil.fromJson[Wines](jsonText)
            val winariesList: Seq[Winery] = wines.wines.map(_.winery).distinct
            assert((winariesList.length == 1) || winariesList.isEmpty)
            winariesList.headOption.map { w: Winery =>
              WineryFileInfo(w.id, w.seo_name, p.getFileName.toString)
            }
          }.recoverWith { case e =>
            log.error(s"Error reading json from `$p`: ${e.getMessage}")
            Failure(e)
          }).toOption.flatten
        }
      }.collect { case Some(wineryFileInfo: WineryFileInfo) => wineryFileInfo }
  }

  def fromDirWithoutSaved(wineryWarehouseDir: String, htmlDir: String)(
    implicit executionContext: ExecutionContext,
    actorSystem: ActorSystem,
    actorMaterializer: ActorMaterializer
  ): Future[List[WineryFileInfo]] = {
    val savedWines: Future[Seq[WineryFileInfo]] = fromDir(wineryWarehouseDir).runWith(Sink.seq)
    val savedHtmlIds: Future[Set[Int]] = Directory.ls(Paths.get(htmlDir))
      .map { p =>
        p.getFileName.toString match {
          case isHtmlFile(wineryId, seoName) => Some(wineryId)
          case _ => None
        }
      }.collect{case Some(wineryId) => wineryId.toInt}
        .runWith(Sink.seq).map(_.toSet)
    val unsavedWines: Future[List[WineryFileInfo]] = for{
      wfi <- savedWines
      htmlIds <- savedHtmlIds
    } yield {
      wfi.filterNot{case WineryFileInfo(wineryId, _, p) =>
        htmlIds.contains(wineryId)
      }.toList
    }
    unsavedWines
  }
}
