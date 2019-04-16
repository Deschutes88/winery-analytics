package swines.wines

import java.io.StringWriter
import java.nio.file.{Files, Path, Paths}

import akka.actor.ActorSystem
import akka.stream.alpakka.file.scaladsl.Directory
import akka.stream.scaladsl.{FileIO, Source}
import akka.stream.{ActorAttributes, ActorMaterializer, Supervision}
import akka.util.ByteString
import swines.cfg
import swines.data._
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import java.util.concurrent.atomic.AtomicLong

import akka.NotUsed
import com.typesafe.scalalogging.Logger

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import collection.JavaConverters._

object CreateTSV {

  private val log = Logger("wines.CreateTSV")
  private val rowCounter = new AtomicLong(0L)


  def main(args: Array[String]) {

    import concurrent.ExecutionContext.Implicits.global
    implicit val system = ActorSystem("SwinesActorSystem")
    implicit val materializer = ActorMaterializer()
    log.info(s"CreateTSV for wines. Reading files from `${cfg.wines.warehouse}` ...")

    val rows: Source[String, _] =
      Directory.ls(Paths.get(cfg.wines.warehouse))
        .recoverWithRetries(1,
          { case e =>
            log.error(s"Error reading files in `${cfg.wines.warehouse}`: ${e.getMessage}")
            Source.failed(e)
          })
        .filter(Files.isRegularFile(_))
        .filter {
          _.toString matches (SavedWines.isWineFile.regex)
        }
        .mapAsyncUnordered(cfg.wines.exportTsv.parallelizm) { p =>
          log.info(s"Reading `$p` ...")
          FileIO.fromPath(p).runFold(ByteString.empty)(_ ++ _)
            .recoverWith { case e =>
              log.error(s"Error reading `$p`: ${e.getMessage}!")
              Future.failed(e)
            }
            .map(_.utf8String)
            .map { jsonStr: String =>
              Try {
                JsonUtil.fromJson[Wines](jsonStr)
              }.recoverWith { case e =>
                log.error(s"Error converting JSON to TSV in `$p`: ${e.getMessage}")
                Failure(e)
              }.flatMap { wines =>
                Try(winesToTsv(wines, p))
                  .recoverWith { case e =>
                    log.error(s"Error while making TSV from `$p`:  ${e.getMessage}")
                    Failure(e)
                  }
              }.getOrElse("")
            }
        }

    //    val headerRow = "wine_id\trating\tnote\tlanguage\tcreated_at\tuser_id\tuser_seo_name\tuser_alias\tuser_visblty\tuser_followers_count\tuser_following_count\tuser_ratings_count\tvintage_id\tvintage_seo_name\tvintage_year\tvintage_name\tvintge_stats_ratings_count\tvintge_stats_ratings_average\tvintge_stats_labels_count\tvintge_wine_id\tvintge_wine_name\tvintge_wine_region_id\tvintge_wine_rgn_name\tvintge_wine_rgn_cntry_code\tvintge_wine_rgn_cntry_name\tactivity_id\tactivity_stats_likes_count\tactivity_stats_comments_count\n"
    val headerRow = "wine_id\twine_name\twine_type_id\trgn__id\trgn__name\trgn__seo_name\trgn__cntry__code\trgn__cntry__name\trgn__cntry__regions_count\trgn__cntry__users_count\trgn__cntry__wines_count\trgn__cntry__wineries_count\twinery__id\twinery__name\twinery__seo_name\twinery__stats__ratings_count\twinery__stats__ratings_average\twinery__stats__wines_count\tstats__ratings_count\tstats__ratings_average\tstats__labels_count\thidden\tvintages__id\tvintages__seo_name\tvintages__year\tvintages__name\tvintages__stats__ratings_count\tvintages__stats__ratings_average\tvintages__stats__labels_count\twine_file\n"
    Source.single(headerRow)
      .concat(rows)
      //    rows
      .map(ByteString.apply)
      .runWith(FileIO.toPath(Paths.get(cfg.wines.exportTsv.saveTo)))
      .onComplete {
        case Success(value) =>
          log.info(s"File `${cfg.wines.exportTsv.saveTo}` is created with ${rowCounter.get()} rows")
          system.terminate()
        case Failure(exception) =>
          log.error(s"Error creating TSV file for wines/vintages `${cfg.wines.exportTsv.saveTo}`: ${exception.getMessage}")
          system.terminate()
      }

  }

  implicit class ToStr[T](v: T) {
    def toStr: String = if (v == null) "null" else v.toString
  }

  val vintagesSet = collection.mutable.Set.empty[(Int, Int, Int)]

  def winesToTsv(wines: Wines, p: Path): String = {
    val sw = new StringWriter()
    val csvPrinter = new CSVPrinter(sw,
      CSVFormat.DEFAULT.withDelimiter("\t".charAt(0))
        .withEscape("\\".charAt(0))
    )
    val wineryIdFromFilename = p.getFileName.toString match {
      case SavedWines.isWineFile(id, _, _) => id.toInt
    }

    try {
      for {wine <- wines.wines if wine != null && wineryIdFromFilename==wine.winery.id
           //           region = if (wine.region != null) wine.region
           //           else Region(-1, "", "", Country("", "", "", null, -1, -1, -1, -1, null), null)
           //           _ = if (wine.vintages.length != wine.vintages.map(_.id).toSet.size)
           //             log.error(s"Doubled vintages ids for wineId=${wine.id}")
           vintage <- wine.vintages if wine.vintages != null && vintage != null
      } {
        //        if(vintagesSet.contains((vintage.wine.winery.id, wine.id, vintage.id)))
        //          log.error(s"Error double entries in `$p` ${(vintage.wine.winery.id, wine.id, vintage.id)} already contained")
        //        vintagesSet.add((vintage.wine.winery.id, wine.id, vintage.id))

        csvPrinter.printRecord(
          safeStr(wine.id),
          safeStr(wine.name),
          safeStr(wine.type_id),
          safeStr(wine.region.id),
          safeStr(wine.region.name),
          safeStr(wine.region.seo_name),
          safeStr(wine.region.country.code),
          safeStr(wine.region.country.name),
          safeStr(wine.region.country.regions_count),
          safeStr(wine.region.country.users_count),
          safeStr(wine.region.country.wines_count),
          safeStr(wine.region.country.wineries_count),
          safeStr(wine.winery.id),
          safeStr(wine.winery.name),
          safeStr(wine.winery.seo_name),
          safeStr(wine.winery.statistics.ratings_count),
          safeStr(wine.winery.statistics.ratings_average),
          safeStr(wine.winery.statistics.wines_count),
          safeStr(wine.statistics.ratings_count),
          safeStr(wine.statistics.ratings_average),
          safeStr(wine.statistics.labels_count),
          safeStr(wine.hidden),
          safeStr(vintage.id),
          safeStr(vintage.seo_name),
          safeStr(vintage.year),
          safeStr(vintage.name),
          safeStr(vintage.statistics.ratings_count),
          safeStr(vintage.statistics.ratings_average),
          safeStr(vintage.statistics.labels_count),
          p.getFileName.toString
        )
        rowCounter.incrementAndGet()
      }
      csvPrinter.flush()
      val tsvRows = sw.toString
      tsvRows
    } finally {
      csvPrinter.close()
      sw.close()
    }
  }

  def safeStr[T](a: => T): String = {
    val nullVal = ""
    Try(a).recover { case e =>
      //      println(s"Error reading value: ${e.getMessage}")
      nullVal
    }.map { a =>
      if (a != null) a.toString.replace("\t", " ") else nullVal
    }.getOrElse(nullVal)
  }

}
