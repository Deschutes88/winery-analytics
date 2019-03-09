package swines.streams

import java.nio.file.Paths

import akka.NotUsed
import akka.stream.alpakka.file.scaladsl.Directory
import akka.stream.javadsl.{FileIO, Framing}
import akka.stream.scaladsl.{Sink, Source}
import akka.util.ByteString

import scala.concurrent.Future

object WineIdsForSavedReviews {

  def wineIdsForSavesdReviews(dir: String = "warehouse2"): Source[Int, _] = {
    val regWinesReview = ".*wine-(\\d+)-reviews-page-\\d+\\.json".r
    Directory.ls(Paths.get(dir)).map(_.getFileName.toString).filter(_.matches(regWinesReview.regex))
      .map { filename =>
        filename match {
          case regWinesReview(id) => Some(id.toInt)
          case _ =>
            println(s"Error while reading id from file $filename")
            None
        }
      }.filter(_.nonEmpty).map(_.get)
  }


}
