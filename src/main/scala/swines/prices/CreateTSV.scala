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
  private val zeroRowsCounter = new AtomicLong(0L)


  def main(args: Array[String]) {

    import concurrent.ExecutionContext.Implicits.global
    implicit val system = ActorSystem("SwinesActorSystem")
    implicit val materializer = ActorMaterializer()
    log.info(s"Reading price files from `${cfg.prices.warehouse}` ...")


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
                val ids = asScalaIterator(vintages.fieldNames()).toList
                val rows: List[String] =
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
                log.info(s"${rows.length} rows created from `$p`")
                if(rows.isEmpty) zeroRowsCounter.incrementAndGet()
                if(rows.length != ids.length)
                  log.error(s"Number of rows ${rows.length} != number of ids ${ids.length} fpr `$p`!")
                rows.mkString
              }.recoverWith { case e =>
                log.error(s"Error converting JSON to TSV in `$p`: ${e.getMessage}!")
                Failure(e)
              }.getOrElse("")
            }
        }
        .filter(_.nonEmpty)


    val headerRow = "id\tcurrency\tcurrency_name\tmedian.amount\tmedian.type\tprice.amount\tprice.discounted_from\tprice.type\n"

//    Source.single(headerRow)
//      .concat(rows)
      rows
      .map(ByteString.apply)
      .runWith(FileIO.toPath(Paths.get(cfg.prices.exportTsv.saveTo)))
      .onComplete {
        case Success(value) =>
          log.info(s"File `${cfg.prices.exportTsv.saveTo}` is created with ${rowCounter.get()} rows. ${zeroRowsCounter.get()} wines had no prices.")
          system.terminate()
        case Failure(exception) =>
          log.error(s"Error creating TSV file for prices `${cfg.prices.exportTsv.saveTo}`: ${exception.getMessage}")
          system.terminate()
      }

  }

  implicit class ToStr[T](v: T) {
    def toStr: String = if (v == null) "" else v.toString
  }

}
