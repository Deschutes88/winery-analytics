package swines.reviews

import java.nio.file.Paths

import akka.stream.scaladsl.{FileIO, Framing, Source}
import akka.util.ByteString
import swines.data.WineVintages

object SavedWineWintages {

//  case class WineVintages(wineId: Int, vintagesIds: List[Int])

  def loadWineVintages(filename: String): Source[WineVintages, _] = {
    FileIO.fromPath(Paths.get(filename))
            .via(Framing.delimiter(ByteString("\n"), 2048, true))
            .map(_.utf8String)
            .map{s =>
                val Array(winery, vintages) = s.split("\\=")
                WineVintages(winery.toInt, vintages.split("\\,").toList.map(_.toInt))
            }
  }

}
