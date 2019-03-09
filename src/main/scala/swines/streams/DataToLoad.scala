package swines.streams

import java.nio.file.Paths

import akka.stream.IOResult
import akka.stream.scaladsl.{FileIO, Framing, Source}
import akka.util.ByteString
import swines.data.cfg

import concurrent.Future

object DataToLoad {

  case class WineVintages(wineId: Int, vintagesIds: List[Int])

  def dataToLoad(filename: String=cfg.files.dataToLoad): Source[WineVintages, _] = {
    FileIO.fromPath(Paths.get(filename))
            .via(Framing.delimiter(ByteString("\n"), 1024, true))
            .map(_.utf8String)
            .map{s =>
                val Array(winery, vintages) = s.split("\\=")
                WineVintages(winery.toInt, vintages.split("\\,").toList.map(_.toInt))
            }
  }
}
