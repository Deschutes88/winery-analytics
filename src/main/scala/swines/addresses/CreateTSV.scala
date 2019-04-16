package swines.addresses

import java.io.StringWriter
import java.nio.file.{Files, Paths}

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.alpakka.file.scaladsl.Directory
import akka.stream.scaladsl.{FileIO, Source}
import akka.util.ByteString
import com.typesafe.scalalogging.Logger
import org.apache.commons.csv.{CSVFormat, CSVPrinter}
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import swines.cfg

import collection.JavaConverters._
import collection.JavaConverters._
import scala.util.{Failure, Success, Try}

object CreateTSV {
  val log = Logger("addresses.CreateTSV")

  val winaryHtmlFileRegex = ".*winary-(\\d+)-(.*)\\.html".r

  def main(args: Array[String]): Unit = {
    implicit val ec = concurrent.ExecutionContext.global
    implicit val actorSystem = ActorSystem("SwinesActorSystem")
    implicit val actorMaterializer = ActorMaterializer()

    readTSV()
      .map(ByteString.apply)
      .runWith(FileIO.toPath(Paths.get(cfg.addresses.exportTsv.saveTo)))
      .onComplete { case r =>
        actorSystem.terminate()
        log.info("Finished")
      }

  }

  def readTSV(warehouse: String = cfg.addresses.warehouse): Source[String, _] = {
    Directory.ls(Paths.get(warehouse))
      .filter { p =>
        p.getFileName.toString.matches(winaryHtmlFileRegex.regex) &&
          Files.isRegularFile(p)
      }.map { p =>
      Try {
        val textHtml: String = Files.readAllLines(p).asScala.mkString("\n")
        val soup = Jsoup.parse(textHtml)
        val winemaker = soup.select(".winery-page__header__winemaker").text().stripPrefix("Winemaker:").trim
        val content_columns: List[Element] = soup.select("div.winery-page__contact__content__column").asScala.toList
        val adr = Try {
          content_columns(0).text()
        }.recover { case e => log.warn(s"Error reading address from $p: ${e.getMessage}!"); "" }.getOrElse("")
        val web = Try {
          content_columns(1).text()
        }.recover { case e => log.warn(s"Error reading web contacts from $p: ${e.getMessage}!"); "" }.getOrElse("")
        val email = Try {
          content_columns(2).text()
        }.recover { case e => log.warn(s"Error reading emails $p: ${e.getMessage}!"); "" }.getOrElse("")
        val (winaryId, seoName) = p.getFileName.toString match {
          case winaryHtmlFileRegex(winaryId, seoName) => winaryId.toInt -> seoName
        }
        val sw = new StringWriter()
        val csv = new CSVPrinter(
          sw, CSVFormat.DEFAULT
            .withDelimiter("\t".charAt(0))
            .withEscape("\\".charAt(0))
        )
        try {
          csv.printRecord(winaryId.toString, seoName, winemaker, adr, web, email)
          csv.flush()
          sw.toString
        } finally {
          csv.close()
          sw.close()
        }
      }.recoverWith { case e =>
        log.error(s"Error parsing file `$p`: ${e.getMessage}")
        Failure(e)
      }
    }.collect { case Success(tsvLine) => tsvLine }
  }
}
