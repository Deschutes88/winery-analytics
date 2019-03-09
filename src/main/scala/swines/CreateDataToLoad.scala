package swines

import java.io.{File, FileFilter}
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}

import swines.data.{JsonUtil, Wines}

import scala.collection.immutable
import scala.io.StdIn

object CreateDataToLoad {

//    val WAREHOUSE = "/home/cloudera-user/Wine Project/Wine Smoker/resources/storage"
    val WAREHOUSE = "storage"
    val CELL_SEP = "\t"
    val ROW_SEP = "\n"
    val ESC_CHAR = "\""
    val export_file_name = "wines_vintages_new_rt_clean.tsv"

    var wines_count = 0
    var wines_vintages_count = 0

    def main(args: Array[String]): Unit = {
        val warehouseDir = new File(WAREHOUSE)
        val files = warehouseDir.listFiles(new FileFilter {
            override def accept(file: File): Boolean = file.isFile && file.getName.startsWith("wines-")
        })
        val wines: List[Wines] = files.map{ f => scala.io.Source.fromFile(f).getLines().mkString}
                    .map{JsonUtil.fromJson[Wines]}.iterator.toList
        val data: immutable.Seq[String] = wines.flatMap{wines =>
            wines.wines.groupBy(_.id).map { case (wineId, wines) =>
                s"${wineId}=${wines.flatMap(_.vintages.map(_.id)).mkString(",")}"
            }
        }
        Files.write(Paths.get("_Data-To-Load.txt"), data.mkString("\n").getBytes(StandardCharsets.UTF_8))

    }
}
