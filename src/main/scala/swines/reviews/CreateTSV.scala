package swines.reviews


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

  private val log = Logger("reviews.CreateTSV")
  private val rowCounter = new AtomicLong(0L)


  def main(args: Array[String]) {

    import concurrent.ExecutionContext.Implicits.global
    implicit val system = ActorSystem("SwinesActorSystem")
    implicit val materializer = ActorMaterializer()
    log.info(s"CreateTSV for reviews. Reading files from `${cfg.reviews.warehouse}` ...")

    val rows: Source[String, _] =
      Directory.ls(Paths.get(cfg.reviews.warehouse))
        .recoverWithRetries(1,
          { case e =>
            log.error(s"Error reading files in `${cfg.reviews.warehouse}`: ${e.getMessage}")
            Source.failed(e)
          })
        .filter(Files.isRegularFile(_))
        .filter {
          _.toString matches (".*(reviews-page).*")
        }
        .mapAsyncUnordered(cfg.reviews.exportTsv.parallelizm) { p =>
          log.info(s"Reading `$p` ...")
          FileIO.fromPath(p).runFold(ByteString.empty)(_ ++ _)
            .recoverWith { case e =>
              log.error(s"Error reading `$p`: ${e.getMessage}!")
              Future.failed(e)
            }
            .map(_.utf8String)
            .map { jsonStr: String =>
              Try {
                val reviews = JsonUtil.fromJson[Reviews](jsonStr)
                reviewsToTsv(reviews)
              }.recoverWith { case e =>
                log.error(s"Error converting JSON to TSV in `$p`: ${e.getMessage}")
                Success("")
              }.get
            }
          //            .map { s => log.trace(s); s }
        }

//    val headerRow = "id\trating\tnote\tlanguage\tcreated_at\tuser_id\tuser_seo_name\tuser_alias\tuser_visblty\tuser_followers_count\tuser_following_count\tuser_ratings_count\tvintage_id\tvintage_seo_name\tvintage_year\tvintage_name\tvintge_stats_ratings_count\tvintge_stats_ratings_average\tvintge_stats_labels_count\tvintge_wine_id\tvintge_wine_name\tvintge_wine_region_id\tvintge_wine_rgn_name\tvintge_wine_rgn_cntry_code\tvintge_wine_rgn_cntry_name\tactivity_id\tactivity_stats_likes_count\tactivity_stats_comments_count\n"
    val headerRow = "review_id\trating\tnote\tlanguage\tcreated_at\tuser_id\tuser_seo_name\tuser_alias\tuser_visblty\tuser_followers_count\tuser_following_count\tuser_ratings_count\tvintage_id\tvintage_seo_name\tvintage_year\tvintage_name\tvintge_stats_ratings_count\tvintge_stats_ratings_average\tvintge_stats_labels_count\tvintge_wine_id\tvintge_wine_name\tvintge_wine_region_id\tvintge_wine_rgn_name\tvintge_wine_rgn_cntry_code\tvintge_wine_rgn_cntry_name\tactivity_id\tactivity_stats_likes_count\tactivity_stats_comments_count\n"
    Source.single(headerRow)
      .concat(rows)
//      rows
      .map(ByteString.apply)
      .runWith(FileIO.toPath(Paths.get(cfg.reviews.exportTsv.saveTo)))
      .onComplete {
        case Success(value) =>
          log.info(s"File `${cfg.reviews.exportTsv.saveTo}` is created with ${rowCounter.get()} rows")
          system.terminate()
        case Failure(exception) =>
          log.error(s"Error creating TSV file for reviews `${cfg.reviews.exportTsv.saveTo}`: ${exception.getMessage}")
          system.terminate()
      }

  }

  implicit class ToStr[T](v: T) {
    def toStr: String = if (v == null) "" else v.toString.replace("\t", " ")
  }

  def reviewsToTsv(reviews: Reviews): String = {
    val sw = new StringWriter()
    val csvPrinter = new CSVPrinter(
      sw, CSVFormat.DEFAULT
        .withDelimiter("\t".charAt(0))
        .withEscape("\\".charAt(0))
    )
    try {
      reviews.reviews.map { review: Review =>
        val activity = if (review.activity != null) review.activity else
          Activity(-1, ActivityStats(-1, -1))
        val region = if (review.vintage.wine.region != null) review.vintage.wine.region else
          Region(-1, "", "", Country("", "", "", null, -1, -1, -1, -1, null), null)
        csvPrinter.printRecord(
          review.id.toStr,
          review.rating.toStr,
          clean(review.note.toStr),
          review.language.toStr,
          review.created_at.toStr,
          review.user.id.toStr,
          review.user.seo_name.toStr,
          review.user.alias.toStr,
          review.user.visibility.toStr,
          review.user.followers_count.toStr,
          review.user.following_count.toStr,
          review.user.ratings_count.toStr,
          review.vintage.id.toStr,
          review.vintage.seo_name.toStr,
          review.vintage.year.toStr,
          review.vintage.name.toStr,
          review.vintage.statistics.ratings_count.toStr,
          review.vintage.statistics.ratings_average.toStr,
          review.vintage.statistics.labels_count.toStr,
          review.vintage.wine.id.toStr,
          review.vintage.wine.name.toStr,
          region.id.toStr,
          region.name.toStr,
          region.country.code.toStr,
          region.country.name.toStr,
          activity.id.toStr,
          activity.statistics.likes_count.toStr,
          activity.statistics.comments_count.toStr
        )
        rowCounter.incrementAndGet()
      }
      csvPrinter.flush()
      val tsvRow = sw.toString
      tsvRow
    }finally {
      csvPrinter.close()
      sw.close()
    }
  }

  //  def mkReviewRow(review: Review) = {
  //    Try {
  //      val activity = if (review.activity != null) review.activity else
  //        Activity(-1, ActivityStats(-1, -1))
  //      val region = if (review.vintage.wine.region != null) review.vintage.wine.region else
  //        Region(-1, "", "", Country("", "", "", null, -1, -1, -1, -1, null), null)
  //
  //      var row = new StringBuilder
  //      row.append(review.id).append(CELL_SEP)
  //        .append(review.rating).append(CELL_SEP)
  //        .append(clean(review.note)).append(CELL_SEP)
  //        .append(review.language).append(CELL_SEP)
  //        .append(review.created_at).append(CELL_SEP)
  //        .append(review.user.id).append(CELL_SEP)
  //        .append(review.user.seo_name).append(CELL_SEP)
  //        .append(review.user.alias).append(CELL_SEP)
  //        .append(review.user.visibility).append(CELL_SEP)
  //        .append(review.user.followers_count).append(CELL_SEP)
  //        .append(review.user.following_count).append(CELL_SEP)
  //        .append(review.user.ratings_count).append(CELL_SEP)
  //        .append(review.vintage.id).append(CELL_SEP)
  //        .append(review.vintage.seo_name).append(CELL_SEP)
  //        .append(review.vintage.year).append(CELL_SEP)
  //        .append(review.vintage.name).append(CELL_SEP)
  //        .append(review.vintage.statistics.ratings_count).append(CELL_SEP)
  //        .append(review.vintage.statistics.ratings_average).append(CELL_SEP)
  //        .append(review.vintage.statistics.labels_count).append(CELL_SEP)
  //        .append(review.vintage.wine.id).append(CELL_SEP)
  //        .append(review.vintage.wine.name).append(CELL_SEP)
  //        .append(region.id).append(CELL_SEP)
  //        .append(region.name).append(CELL_SEP)
  //        .append(region.country.code).append(CELL_SEP)
  //        .append(region.country.name).append(CELL_SEP)
  //        .append(activity.id).append(CELL_SEP)
  //        .append(activity.statistics.likes_count).append(CELL_SEP)
  //        .append(activity.statistics.comments_count).append(CELL_SEP)
  //
  //      row.toString
  //    } getOrElse {
  //      println(s"Error making tsv row $review")
  //      ""
  //    }
  //  }

  def clean(text: String): String = {
    if (text != null) text.replaceAll("\\s", " ") else ""
  }
}
