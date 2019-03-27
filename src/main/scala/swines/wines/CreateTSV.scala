package swines.wines

/**
  * OLD WinesCounter, written by prev developer
  */

import java.io._
import java.nio.file.{Files, Paths}

import org.apache.commons.csv.{CSVFormat, CSVPrinter}
import swines.cfg
import swines.data._

import scala.collection.JavaConverters._

object CreateTSV {

  //    val WAREHOUSE = "/home/cloudera-user/Wine Project/Wine Smoker/resources/storage"
//  val CELL_SEP = "\t"
//  val ROW_SEP = "\n"
//  val ESC_CHAR = "\""
  val WAREHOUSE = cfg.wines.warehouse
  val export_file_name = cfg.wines.exportTsv.saveTo

  var wines_count = 0
  var wines_vintages_count = 0

  def main(args: Array[String]) {

    prepareExportFile

    val files = new File(s"$WAREHOUSE").list
    val pretty = ".*(pretty).*".r
    val not_pretty = ".*(wines).*".r


    for (file <- files) {
      file match {
        case pretty(c)     => println(s"'$file' IS PRETTY -> not handled")
        case not_pretty(a) => handle(file)
        case _             => println("TOTALLY OTHER FILE")
      }
    }

    println(s"$export_file_name: wines = $wines_count, wines vintages = $wines_vintages_count")
  }

  def prepareExportFile() {
    val headers = "" //"id\tname\ttype_id\trgn.id\trgn.name\trgn.seo_name\trgn.ctry.code\trgn.ctry.name\trgn.ctry.rgns_count\trgn.ctry.users_count\trgn.ctry.wines_count\trgn.ctry.wineries_count\twinery.id\twinery.name\twinery.seo_name\twinery.stats.ratings_count\twinery.stats.ratings_average\twinery.stats.wines_count\tstats.ratings_count\tstats.ratings_average\tstats.labels_count\thidden\tvintages.id\tvintages.seo_name\tvintages.year\tvintages.name\tvintages.stats.ratings_count\tvintages.stats.ratings_average\tvintages.stats.labels_count\n"
    flush2file(headers, export_file_name);
  }

  def handle(file: String) {

    val fn = s"$WAREHOUSE/$file"

    val wines: Wines = {
      val txt = asScalaIterator(Files.readAllLines(Paths.get(fn)).iterator()).mkString("\n")
      JsonUtil.fromJson[Wines](txt)
    }

    var data = new StringBuilder

    for (wine: Wine <- wines.wines) {
      for (vintage: Vintage <- wine.vintages) {
//        data.append( mkWineRow(wine, vintage) ).append( ROW_SEP )
        data.append( mkWineRow2(wine, vintage) )
      }
      wines_vintages_count += wine.vintages.size
    }
    wines_count += wines.wines.size

    append2file(data.toString, export_file_name)

    println(s"'$file' contains ${wines.wines.size} wines description")
  }

  implicit class ToStr[T](v: T) {
    def toStr: String = if (v == null) "null" else v.toString
    def as[F]: F =
      if (v == null)
        throw new Exception(s"$v can't be casted to ...")
      else v.asInstanceOf[F]
  }


  def mkWineRow2(wine: Wine, vintage: Vintage) = {
    val sw = new StringWriter
    val csvPrinter = new CSVPrinter(
      sw, CSVFormat.DEFAULT
        .withDelimiter("\t".charAt(0))
        .withEscape("\\".charAt(0))
    )
    try {
      val region = if (wine.region != null) wine.region else
        Region(-1, "", "", Country("", "", "", null, -1, -1, -1, -1, null), null)
      csvPrinter.printRecord(
        wine.id.toStr,
        wine.name.toStr,
        wine.type_id.toStr,
        region.id.toStr,
        region.name.toStr,
        region.seo_name.toStr,
        region.country.code.toStr,
        region.country.name.toStr,
        region.country.regions_count.toStr,
        region.country.users_count.toStr,
        region.country.wines_count.toStr,
        region.country.wineries_count.toStr,
        wine.winery.id.toStr,
        wine.winery.name.toStr,
        wine.winery.seo_name.toStr,
        wine.winery.statistics.ratings_count.toStr,
        wine.winery.statistics.ratings_average.toStr,
        wine.winery.statistics.wines_count.toStr,
        wine.statistics.ratings_count.toStr,
        wine.statistics.ratings_average.toStr,
        wine.statistics.labels_count.toStr,
        wine.hidden.toStr,
        vintage.id.toStr,
        vintage.seo_name.toStr,
        vintage.year.toStr,
        vintage.name.toStr,
        vintage.statistics.ratings_count.toStr,
        vintage.statistics.ratings_average.toStr,
        vintage.statistics.labels_count.toStr
      )
      csvPrinter.flush
      sw.toString
    } finally {
      csvPrinter.close()
      sw.close()
    }

  }

//  def mkWineRow(wine: Wine, vintage: Vintage) = {
//
//    val region = if (wine.region != null) wine.region else
//      Region(-1, "", "", Country("", "", "", null,-1, -1, -1, -1, null), null)
//
//
//    var row = new StringBuilder("\"");
//    row = row.append(wine.id);/*.append(ESC_CHAR)*/row.append(ESC_CHAR).append(CELL_SEP)
//      .append(ESC_CHAR).append(wine.name).append(ESC_CHAR).append(CELL_SEP)
//      .append(ESC_CHAR).append(wine.type_id).append(ESC_CHAR).append(CELL_SEP)
//      .append(ESC_CHAR).append(region.id).append(ESC_CHAR).append(CELL_SEP)
//      .append(ESC_CHAR).append(region.name).append(ESC_CHAR).append(CELL_SEP)
//      .append(ESC_CHAR).append(region.seo_name).append(ESC_CHAR).append(CELL_SEP)
//      .append(ESC_CHAR).append(region.country.code).append(ESC_CHAR).append(CELL_SEP)
//      .append(ESC_CHAR).append(region.country.name).append(ESC_CHAR).append(CELL_SEP)
//      .append(ESC_CHAR).append(region.country.regions_count).append(ESC_CHAR).append(CELL_SEP)
//      .append(ESC_CHAR).append(region.country.users_count).append(ESC_CHAR).append(CELL_SEP)
//      .append(ESC_CHAR).append(region.country.wines_count).append(ESC_CHAR).append(CELL_SEP)
//      .append(ESC_CHAR).append(region.country.wineries_count).append(ESC_CHAR).append(CELL_SEP)
//      .append(ESC_CHAR).append(wine.winery.id).append(ESC_CHAR).append(CELL_SEP)
//      .append(ESC_CHAR).append(wine.winery.name).append(ESC_CHAR).append(CELL_SEP)
//      .append(ESC_CHAR).append(wine.winery.seo_name).append(ESC_CHAR).append(CELL_SEP)
//      .append(ESC_CHAR).append(wine.winery.statistics.ratings_count).append(ESC_CHAR).append(CELL_SEP)
//      .append(ESC_CHAR).append(wine.winery.statistics.ratings_average).append(ESC_CHAR).append(CELL_SEP)
//      .append(ESC_CHAR).append(wine.winery.statistics.wines_count).append(ESC_CHAR).append(CELL_SEP)
//      .append(ESC_CHAR).append(wine.statistics.ratings_count).append(ESC_CHAR).append(CELL_SEP)
//      .append(ESC_CHAR).append(wine.statistics.ratings_average).append(ESC_CHAR).append(CELL_SEP)
//      .append(ESC_CHAR).append(wine.statistics.labels_count).append(ESC_CHAR).append(CELL_SEP)
//      .append(ESC_CHAR).append(wine.hidden).append(ESC_CHAR).append(CELL_SEP)
//      .append(ESC_CHAR).append(vintage.id).append(ESC_CHAR).append(CELL_SEP)
//      .append(ESC_CHAR).append(vintage.seo_name).append(ESC_CHAR).append(CELL_SEP)
//      .append(ESC_CHAR).append(vintage.year).append(ESC_CHAR).append(CELL_SEP)
//      .append(ESC_CHAR).append(vintage.name).append(ESC_CHAR).append(CELL_SEP)
//      .append(ESC_CHAR).append(vintage.statistics.ratings_count).append(ESC_CHAR).append(CELL_SEP)
//      .append(ESC_CHAR).append(vintage.statistics.ratings_average).append(ESC_CHAR).append(CELL_SEP)
//      .append(ESC_CHAR).append(vintage.statistics.labels_count).append(ESC_CHAR).append(CELL_SEP)
//
//    row.toString
//  }


  def flush2file(data: String, filename: String) {
    val bw = new PrintWriter(new BufferedWriter(new FileWriter(filename, false)))
    bw.write(data)
    bw.close()
  }

  def append2file(msg: String, filename: String) {
    val bw = new PrintWriter(new BufferedWriter(new FileWriter(filename, true)))
    bw.write(s"$msg")
    bw.close()
  }
}
