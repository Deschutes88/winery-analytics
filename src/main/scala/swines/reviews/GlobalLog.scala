package swines.reviews

import java.nio.file.Paths

import akka.stream.scaladsl.{FileIO, Framing, Source}
import akka.util.ByteString

object GlobalLog {
  val regWineIdFromGlobalLog = "wine \\[(\\d+)\\] HANDLED with (\\d+) review pages".r

  def successMsg(wineId: Int, numPages: Int) = s"wine [$wineId] HANDLED with $numPages review pages"

  //wine [1864499] HANDLED with 9 review pages
  def succesfullWineIdsFromGlobalLog(globalLogFilename: String)
      : Source[Int, _] = {
    FileIO.fromPath(Paths.get(globalLogFilename))
      .via(Framing.delimiter(ByteString("\n"), 1024, true))
      .map(_.utf8String)
      .map { s =>
        s match {
          case regWineIdFromGlobalLog(wineId, numPages) =>
            wineId.toInt
          case _ =>
            -1
        }
      }
      .filter(_ != -1)

  }


}
