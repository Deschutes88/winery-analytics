package swines.addresses

import java.nio.file.{Files, Paths}

import akka.stream.alpakka.file.scaladsl.Directory
import akka.stream.scaladsl.Source
import com.typesafe.scalalogging.Logger
import swines.cfg
import swines.data.{JsonUtil, Wine, Wines, Winery}
import swines.wines.SavedWines

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Try}

object SavedWinaries {
  private val log = Logger("ScrapAddresses")

  case class WineryFileInfo(wineryId: Int, seoName: String, filename: String)

  def fromDir(warehouseDir: String)(
    implicit executionContext: ExecutionContext
  ): Source[WineryFileInfo, _] = {
    val warehouse = Paths.get(cfg.wines.warehouse)
    log.info(s"Reading wineries from dir $warehouse ...")
    Directory.ls(warehouse)
      .filter { p => p.getFileName.toString.matches(SavedWines.isWineFile.regex) }
      .mapAsyncUnordered(cfg.addresses.scraper.parallelizm) { p =>
        Future {
          log.info(s"Reading `$p` ...")
          val jsonText = Files.readAllLines(p).asScala.mkString("\n")
          (Try {
            val wines = JsonUtil.fromJson[Wines](jsonText)
            val winariesList = wines.wines.map(_.winery).distinct
            assert((winariesList.length == 1) || winariesList.isEmpty)
            winariesList.headOption.map { w: Winery =>
              WineryFileInfo(w.id, w.seo_name, p.getFileName.toString)
            }
          }.recoverWith { case e =>
            log.error(s"Error reading json from `$p`: ${e.getMessage}")
            Failure(e)
          }).toOption.flatten
        }
      }.collect { case Some(seoName) => seoName }
  }

}
