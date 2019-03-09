package swines.streams

import java.nio.file.Paths

import akka.stream.alpakka.file.scaladsl.Directory
import akka.stream.scaladsl.Source
import swines.data.cfg

object WineIdsForSavedPrices {

  def wineIdsForSavedPrices(dir: String = cfg.files.warehouse): Source[Int, _] = {
    val regWinePricesFilename = "wine-(\\d+)-prices-?.json".r
    Directory.ls(Paths.get(dir)).map(_.getFileName.toString).filter(_.matches(regWinePricesFilename.regex))
      .map { filename =>
        filename match {
          case regWinePricesFilename(id) => Some(id.toInt)
          case _ =>
            println(s"Error while reading id from file $filename")
            None
        }
      }.filter(_.nonEmpty).map(_.get)
  }


}
