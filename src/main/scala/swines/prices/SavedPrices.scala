package swines.prices

import java.nio.file.{Path, Paths}

import akka.stream.alpakka.file.scaladsl.Directory
import akka.stream.scaladsl.Source

object SavedPrices {

  val regWinePricesFilename = "wine-(\\d+)-prices-?.json".r

  def listSavedPricesFiles(dir: String): Source[Path, _] = {
    Directory.ls(Paths.get(dir))
      .filter(_.getFileName.toString.matches(regWinePricesFilename.regex))
  }

  def wineIdsForSavedPrices(dir: String): Source[Int, _] = {
    listSavedPricesFiles(dir)
      .map { p =>
        val filename = p.getFileName.toString
        filename match {
          case regWinePricesFilename(id) => Some(id.toInt)
          case _ =>
            println(s"Error while reading id from file $filename")
            None
        }
      }
      .collect{case Some(id) => id }
  }


}
