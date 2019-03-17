package swines.prices

import java.io.StringWriter
import java.nio.file.{Files, Paths}
import java.util
import java.util.concurrent.atomic.AtomicLong

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.alpakka.file.scaladsl.Directory
import akka.stream.scaladsl.{FileIO, Source}
import akka.util.ByteString
import com.fasterxml.jackson.databind.JsonNode
import com.typesafe.scalalogging.Logger
import org.apache.commons.csv.{CSVFormat, CSVPrinter}
import swines.cfg
import swines.data._

import collection.JavaConverters._
import scala.util.{Failure, Success, Try}

object CreateTSV {

  //  private val CELL_SEP = "\t"
  //  private val ROW_SEP = "\n"
  //  private val export_file_name = cfg.files.extortTSV
  private val log = Logger("prices.CreateTSV")
  private val rowCounter = new AtomicLong(0L)


  def main(args: Array[String]) {

    import concurrent.ExecutionContext.Implicits.global
    implicit val system = ActorSystem("SwinesActorSystem")
    implicit val materializer = ActorMaterializer()
    log.info(s"Reading price files from `${cfg.prices.warehouse}` ...")

    println(cfg)

    val rows: Source[String, _] =
        SavedPrices.listSavedPricesFiles(cfg.prices.warehouse)
        .mapAsyncUnordered(cfg.reviews.exportTsv.parallelizm) { p =>
          log.info(s"Reading `$p` ...")
          FileIO.fromPath(p).runFold(ByteString.empty)(_ ++ _)
            .map(_.utf8String)
            .map { jsonStr: String =>
              Try {
                val prices = JsonUtil.toTree(jsonStr)
                val currency = prices.get("prices").get("market").get("currency").get("prefix")
                val currency_name = prices.get("prices").get("market").get("currency").get("name")
                val vintages: JsonNode = prices.get("prices").get("vintages")
                val ids = asScalaIterator(vintages.fieldNames())
                val rows =
                  for {id: String <- ids}
                    yield {
                        val desc = vintages.get(id)
                        val median = Try{desc.get("median").get("amount").asText}.getOrElse("")
                        val typ = Try{desc.get("median").get("type").asText}.getOrElse("")
                      //{"id":14457849,"amount":34.99,"discounted_from":0.0,"type":"ppc"}
//                        val price = Try{desc.get("price").asText}.getOrElse("")
                        val amount = Try{desc.get("price").get("amount").asText}.getOrElse("")
                        val discounted_from = Try{desc.get("price").get("discounted_from").asText}.getOrElse("")
                        val price__type = Try{desc.get("price").get("type").asText}.getOrElse("")
                        rowCounter.incrementAndGet()
                        s"$id\t$currency\t$currency_name\t$median\t$typ\t$amount\t$discounted_from\t$price__type\n"
                    }
                rows.filter(_.nonEmpty).mkString
              }.recoverWith { case e =>
                log.error(s"Error converting JSON to TSV in `$p`: ${e.getMessage}")
                Failure(e)
              }.getOrElse("")
            }
        }
        .filter(_.nonEmpty)


    val headerRow = "id\tcurrency\tcurrency_name\tmedian.amount\tmedian.type\tprice.amount\tprice.discounted_from\tprice.type\n"

    Source.single(headerRow)
      .concat(rows)
      .map(ByteString.apply)
      .runWith(FileIO.toPath(Paths.get(cfg.prices.exportTsv.saveTo)))
      .onComplete {
        case Success(value) =>
          log.info(s"File `${cfg.prices.exportTsv.saveTo}` is created with ${rowCounter.get()} rows")
          system.terminate()
        case Failure(exception) =>
          log.error(s"Error creating TSV file for prices: ${exception.getMessage}")
          system.terminate()
      }

  }

  implicit class ToStr[T](v: T) {
    def toStr: String = if (v == null) "" else v.toString
  }

  //  def handly(file: String) {
  //    println(s"handling file $file")
  //    val fn = s"$WAREHOUSE/$file"
  //
  //    val str = Source.fromFile(fn).mkString
  //    val prices = JsonUtil.toTree(str)
  //    val currency = prices.get("prices").get("market").get("currency").get("prefix")
  //    val currency_name = prices.get("prices").get("market").get("currency").get("name")
  //    val vintages = prices.get("prices").get("vintages")
  //    val ids = vintages.fieldNames();
  //    var id: String = null
  //    var desc: JsonNode = null
  //    while (ids.hasNext()) {
  //      id = ids.next()
  //      desc = vintages.get(id)
  //      if (desc != null && !desc.isNull()) {
  //        val median = desc.get("median").get("amount")
  //        val typ = desc.get("median").get("type")
  //        val price = desc.get("price")
  //        WineScraper.append2file(s"$id\t$currency\t$currency_name\t$median\t$typ\t$price\n", export_file_name)
  //      }
  //    }
  //  }
  //
  //  def reviewsToTsv(reviews: Reviews): String = {
  //    val sw = new StringWriter()
  //    val csvPrinter = new CSVPrinter(
  //      sw, CSVFormat.DEFAULT
  //        .withDelimiter("\t".charAt(0))
  //        .withEscape("\\".charAt(0))
  //    )
  //    reviews.reviews.map { review: Review =>
  //      val activity = if (review.activity != null) review.activity else
  //        Activity(-1, ActivityStats(-1, -1))
  //      val region = if (review.vintage.wine.region != null) review.vintage.wine.region else
  //        Region(-1, "", "", Country("", "", "", null, -1, -1, -1, -1, null), null)
  //      csvPrinter.printRecord(
  //        review.id.toStr,
  //        review.rating.toStr,
  //        clean(review.note.toStr),
  //        review.language.toStr,
  //        review.created_at.toStr,
  //        review.user.id.toStr,
  //        review.user.seo_name.toStr,
  //        review.user.alias.toStr,
  //        review.user.visibility.toStr,
  //        review.user.followers_count.toStr,
  //        review.user.following_count.toStr,
  //        review.user.ratings_count.toStr,
  //        review.vintage.id.toStr,
  //        review.vintage.seo_name.toStr,
  //        review.vintage.year.toStr,
  //        review.vintage.name.toStr,
  //        review.vintage.statistics.ratings_count.toStr,
  //        review.vintage.statistics.ratings_average.toStr,
  //        review.vintage.statistics.labels_count.toStr,
  //        review.vintage.wine.id.toStr,
  //        review.vintage.wine.name.toStr,
  //        region.id.toStr,
  //        region.name.toStr,
  //        region.country.code.toStr,
  //        region.country.name.toStr,
  //        activity.id.toStr,
  //        activity.statistics.likes_count.toStr,
  //        activity.statistics.comments_count.toStr
  //      )
  //      rowCounter.incrementAndGet()
  //    }
  //    csvPrinter.flush()
  //    val tsvRow = sw.toString
  //    tsvRow
  //  }

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
