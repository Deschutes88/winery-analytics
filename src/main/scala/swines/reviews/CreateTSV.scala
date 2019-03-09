package swines.reviews


import java.nio.file.{Files, Paths}

import akka.actor.ActorSystem
import akka.stream.alpakka.file.scaladsl.Directory
import akka.stream.scaladsl.{FileIO, Source}
import akka.stream.{ActorAttributes, ActorMaterializer, Supervision}
import akka.util.ByteString
import swines.data._

import scala.util.{Failure, Success}

object CreateTSV {

  private val CELL_SEP = "\t"
  private val ROW_SEP = "\n"
  private val export_file_name = swines.data.cfg.files.extortTSV
  private val headerRow = "id\trating\tnote\tlanguage\tcreated_at\tuser_id\tuser_seo_name\tuser_alias\tuser_visblty\tuser_followers_count\tuser_following_count\tuser_ratings_count\tvintage_id\tvintage_seo_name\tvintage_year\tvintage_name\tvintge_stats_ratings_count\tvintge_stats_ratings_average\tvintge_stats_labels_count\tvintge_wine_id\tvintge_wine_name\tvintge_wine_region_id\tvintge_wine_rgn_name\tvintge_wine_rgn_cntry_code\tvintge_wine_rgn_cntry_name\tactivity_id\tactivity_stats_likes_count\tactivity_stats_comments_count\n"


  def main(args: Array[String]) {

    import concurrent.ExecutionContext.Implicits.global
    implicit val system = ActorSystem("SwinesActorSystem")
    implicit val materializer = ActorMaterializer()

    val rows = Directory.ls(Paths.get(cfg.files.warehouse))
      .filter(Files.isRegularFile(_))
      .filter {
        _.toString matches (".*(reviews-page).*")
      }
      .mapAsyncUnordered(1000) { p =>
        FileIO.fromPath(p).runReduce(_ ++ _)
          .map(_.utf8String)
          .map {
            JsonUtil.fromJson[Reviews](_)
          }
          .map(_.reviews.map {
            mkReviewRow
          }.mkString("\n"))

      }.withAttributes(ActorAttributes.withSupervisionStrategy({ case e => Supervision.Stop }))

    Source.single(headerRow)
      .concat(rows)
      .map(ByteString.apply)
      .runWith(FileIO.toPath(Paths.get(cfg.files.extortTSV)))
      .onComplete {
        case Success(value) =>
          println(s"File ${cfg.files.extortTSV} is created")
          system.terminate()
        case Failure(exception) =>
          println(s"Error ${exception.getMessage}")
          system.terminate()
      }

  }

  def mkReviewRow(review: Review) = {
    try {
      val activity = if (review.activity != null) review.activity else
        Activity(-1, ActivityStats(-1, -1))
      val region = if (review.vintage.wine.region != null) review.vintage.wine.region else
        Region(-1, "", "", Country("", "", "", null, -1, -1, -1, -1, null), null)

      var row = new StringBuilder
      row.append(review.id).append(CELL_SEP)
        .append(review.rating).append(CELL_SEP)
        .append(clean(review.note)).append(CELL_SEP)
        .append(review.language).append(CELL_SEP)
        .append(review.created_at).append(CELL_SEP)
        .append(review.user.id).append(CELL_SEP)
        .append(review.user.seo_name).append(CELL_SEP)
        .append(review.user.alias).append(CELL_SEP)
        .append(review.user.visibility).append(CELL_SEP)
        .append(review.user.followers_count).append(CELL_SEP)
        .append(review.user.following_count).append(CELL_SEP)
        .append(review.user.ratings_count).append(CELL_SEP)
        .append(review.vintage.id).append(CELL_SEP)
        .append(review.vintage.seo_name).append(CELL_SEP)
        .append(review.vintage.year).append(CELL_SEP)
        .append(review.vintage.name).append(CELL_SEP)
        .append(review.vintage.statistics.ratings_count).append(CELL_SEP)
        .append(review.vintage.statistics.ratings_average).append(CELL_SEP)
        .append(review.vintage.statistics.labels_count).append(CELL_SEP)
        .append(review.vintage.wine.id).append(CELL_SEP)
        .append(review.vintage.wine.name).append(CELL_SEP)
        .append(region.id).append(CELL_SEP)
        .append(region.name).append(CELL_SEP)
        .append(region.country.code).append(CELL_SEP)
        .append(region.country.name).append(CELL_SEP)
        .append(activity.id).append(CELL_SEP)
        .append(activity.statistics.likes_count).append(CELL_SEP)
        .append(activity.statistics.comments_count).append(CELL_SEP)

      row.toString
    } catch {
      case e: Exception => ""
    }
  }

  def clean(text: String) = {
    if (text != null) text.replaceAll("\\s", " ") else ""
  }
}
