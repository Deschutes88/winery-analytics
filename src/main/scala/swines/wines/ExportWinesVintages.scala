package swines.wines

import java.nio.file.{Files, Paths}
import java.util.concurrent.atomic.AtomicLong

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.alpakka.file.scaladsl.Directory
import akka.stream.scaladsl.FileIO
import akka.util.ByteString
import com.typesafe.scalalogging.Logger
import swines.cfg
import swines.data.{JsonUtil, Wines}

import scala.util.{Failure, Success, Try}

object ExportWinesVintages {

  //    val WAREHOUSE = "/home/cloudera-user/Wine Project/Wine Smoker/resources/storage"
  //  val WAREHOUSE = "storage"
  val CELL_SEP = "\t"
  val ROW_SEP = "\n"
  val ESC_CHAR = "\""
  //  val export_file_name = "wines_vintages_new_rt_clean.tsv"
  private val log = Logger("reviews.CreateDataToLoad")
  val wines_count = new AtomicLong(0L)
  var wines_vintages_count = new AtomicLong(0)

  private val isWineFile = "^.*[\\\\\\/]?wines-(\\d+)-(\\d+)-(\\d+)-?.json$".r

  def main(args: Array[String]): Unit = {
    import concurrent.ExecutionContext.Implicits.global
    implicit val system = ActorSystem("SwinesActorSystem")
    implicit val materializer = ActorMaterializer()
    log.info(s"Reading files from `${cfg.wines.warehouse}` ...")

    Directory.ls(Paths.get(cfg.wines.warehouse))
      .filter(p => Files.isRegularFile(p))
      .filter(p => p.getFileName.toString match {
        case isWineFile(_, _, _) => log.info(s"Ok: File is read: `$p`"); true
        case _ => log.warn(s"Warning skipping file:`${p}`"); false
      })
      .map { p => p -> ByteString(Files.readAllBytes(p)).utf8String }
      .map { case (p, jsonStr) =>
        Try {
          JsonUtil.fromJson[Wines](jsonStr)
        }.recoverWith { case e =>
          log.error(s"Error parsing json from `$p`: ${e.getMessage}")
          Failure(e)
        }
      }
      .collect { case Success(wines) => wines }
      .mapConcat { wines: Wines =>
        wines.wines.groupBy(_.id).map { case (wineId, wines) =>
          wines_count.incrementAndGet()
          val vintages = wines.flatMap(_.vintages.map(_.id))
          wines_vintages_count.updateAndGet(_ + vintages.length)
          s"${wineId}=${vintages.mkString(",")}\n"
        }
      }
      .map(ByteString.apply)
      .runWith(FileIO.toPath(Paths.get("data-to-load.new")))
      .onComplete {
        case Success(_) =>
          log.info(s"Finished wines_count=${wines_count.get()} vintages_count=${wines_vintages_count.get()}")
          system.terminate()
        case Failure(e) =>
          log.error(e.getMessage)
          system.terminate()
      }
    //
    //        val warehouseDir = new File(WAREHOUSE)
    //        val files = warehouseDir.listFiles(new FileFilter {
    //            override def accept(file: File): Boolean = file.isFile && file.getName.startsWith("wines-")
    //        })
    //        val wines: List[Wines] = files.map{ f => scala.io.Source.fromFile(f).getLines().mkString}
    //                    .map{JsonUtil.fromJson[Wines]}.iterator.toList
    //        val data: immutable.Seq[String] = wines.flatMap{wines =>
    //            wines.wines.groupBy(_.id).map { case (wineId, wines) =>
    //                s"${wineId}=${wines.flatMap(_.vintages.map(_.id)).mkString(",")}"
    //            }
    //        }
    //        Files.write(Paths.get("_Data-To-Load.txt"), data.mkString("\n").getBytes(StandardCharsets.UTF_8))

  }
}
