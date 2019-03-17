//package swines.prices
//
//import java.nio.file.{Files, Paths}
//
//import akka.actor.ActorSystem
//import akka.stream.ActorMaterializer
//import akka.stream.scaladsl.Sink
//import swines.cfg
//import swines.files.SavedPrices
//
//import scala.collection.immutable
//import scala.util.{Failure, Success}
//
//object DeleteDoubledFiles {
//
//  import concurrent.ExecutionContext.Implicits.global
//
//  implicit val system = ActorSystem("SwinesActorSystem")
//  implicit val materializer = ActorMaterializer()
//
//  val showDoubledFies = SavedPrices.listSavedPricesFiles()
//    .runWith(Sink.seq)
//    .onComplete {
//      case Failure(exception) =>
//        println(s"Error ${exception.getMessage}")
//        system.terminate()
//      case Success(filenames) =>
//        val doubled: Map[Int, immutable.Seq[String]] =
//          filenames.filter(_.matches(SavedPrices.regWinePricesFilename.regex))
//            .map { filename =>
//              filename match {
//                case SavedPrices.regWinePricesFilename(wineId) =>
//                  wineId.toInt -> filename
//              }
//            }
//            .groupBy(_._1).mapValues(_.map(_._2))
//            .filter { case (wineId, files) => files.length > 1 }
//        doubled.map { case (wineId, files) =>
//          val f = files.filter{_.split("\\.")(0).endsWith("-")}.last
//          val p = Paths.get(s"${cfg.files.warehouse}/$f")
////          Files.delete(p)
//          println(s"File `$p` is deleted.") }
//
//        system.terminate()
//    }
//
//  def main(args: Array[String]): Unit = {
//    showDoubledFies
//  }
//}
