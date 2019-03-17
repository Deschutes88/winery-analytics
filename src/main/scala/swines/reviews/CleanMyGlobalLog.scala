package swines.reviews

import java.nio.file.{Files, Paths, StandardOpenOption}

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{FileIO, Framing, Source}
import akka.util.ByteString
import swines.cfg

import scala.util.{Failure, Success}

object CleanMyGlobalLog extends App {

  import concurrent.ExecutionContext.Implicits.global

  implicit val system = ActorSystem("SwinesActorSystem")
  implicit val materializer = ActorMaterializer()

  import StandardOpenOption._

  FileIO.fromPath(Paths.get(cfg.files.myGlobalLog))
    .via(Framing.delimiter(ByteString("\n"), 1024))
    .filter(_.utf8String match {
      case GlobalLog.regWineIdFromGlobalLog(wineId, pages) => true
      case _ => false
    })
    .intersperse(ByteString("\n"))
    .runReduce(_ ++ _)
    .flatMap{ byteString =>
        Source.single(byteString ++ ByteString("\n")).runWith(FileIO.toPath(Paths.get(cfg.files.myGlobalLog)))
    }
    //    .runWith(FileIO.toPath(Paths.get(cfg.files.myGlobalLog)))
    .onComplete(_ => system.terminate())

}
