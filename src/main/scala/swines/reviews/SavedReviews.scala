package swines.reviews

import java.nio.file.{Path, Paths}

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.alpakka.file.scaladsl.Directory
import akka.stream.scaladsl.{Sink, Source}
import com.typesafe.scalalogging.Logger
import swines.cfg
import swines.data.WineVintages

import scala.concurrent.{ExecutionContext, Future}

object SavedReviews {

  def listFilenames(dir: String): Source[Path, _] = {
    Directory.ls(Paths.get(dir))
      .filter(_.getFileName.toString.matches(regWinesReview.regex))
  }

  val regWinesReview = ".*wine-(\\d+)-reviews-page-(\\d+)-?.json".r

  def wineIdsForSavedReviews(dir: String): Source[Int, _] = {
    listFilenames(dir).map { filename =>
      filename.getFileName.toString match {
        case regWinesReview(id, _) => Some(id.toInt)
        case _ =>
          println(s"Error while reading id from file $filename")
          None
      }
    }.filter(_.nonEmpty).map(_.get)
  }

  //  protected val reviewsFilename ="wine-(\\d+)-reviews-page-(\\d+)-\\.json".r

  def loadReviewsMap(implicit executionContext: ExecutionContext,
                     actorMaterializer: ActorMaterializer,
                     actorSystem: ActorSystem): Future[Map[Int, Int]] = {
    listFilenames(cfg.reviews.warehouse)
      .map {
        _.getFileName.toString match {
          case regWinesReview(wineID, pageNo) => wineID.toInt -> pageNo.toInt
        }
      }
      .runWith(Sink.seq)
      .map {
        _.groupBy(_._1).mapValues(_.map(_._2).max)
      }

  }

  val log = Logger("ReviewsScraper")

  def unloadedReviews(implicit executionContext: ExecutionContext,
                      actorSystem: ActorSystem,
                      actorMaterializer: ActorMaterializer)
  : Future[Set[(WineVintages, Int)]] = {

    val wineVintagesF: Future[Set[WineVintages]] =
      SavedWineWintages.loadWineVintages(cfg.files.winesVintages).runWith(Sink.seq).map { d =>
        log.info(s"${cfg.files.winesVintages} has ${d.length} entries")
        d.toSet
      }.recoverWith { case e =>
        log.error(s"Error while loading `${cfg.files.winesVintages}`: ${e.getMessage}")
        Future.failed(e)
      }

    val successfullyLoadedWineIdsF: Future[Set[Int]] =
//      GlobalLog.succesfullWineIdsFromGlobalLog(cfg.files.globalLog)
//        .concat(GlobalLog.succesfullWineIdsFromGlobalLog(cfg.files.myGlobalLog))
      GlobalLog.succesfullWineIdsFromGlobalLog(cfg.files.myGlobalLog)
        .runWith(Sink.seq)
        .map(_.toSet)
        .recoverWith { case e =>
          log.error(s"Error while loading global logs: ${e.getMessage}")
          Future.failed(e)
        }

    val warehouseF: Future[Map[Int, Int]] = loadReviewsMap
      .recoverWith { case e =>
        log.error(s"Error while loading warehouse Ids & lastPageNumbers: ${e.getMessage}")
        Future.failed(e)
      }

    (for {
      wv: Set[WineVintages] <- wineVintagesF.map(_.toSet)
      allWineIds = wv.map(_.wineId).toSet
      _ = log.info(s"`${cfg.files.winesVintages}` contains ${allWineIds.size} wine Ids")
      successfullyLoadedWineIds <- successfullyLoadedWineIdsF.map(_.toSet)
      _ = log.info(s"Number of successfully loaded WineIds (from global logs) ${successfullyLoadedWineIds.size}")
      lastSavedPages: Map[Int, Int] <- warehouseF
      _ = log.info(s"warehouse contains reviews for ${lastSavedPages.keySet.size} wines")
      idsInWarehouse = lastSavedPages.keySet
    } yield {
      val idsWithErrors: Set[Int] = idsInWarehouse -- successfullyLoadedWineIds
      val failedPagesToLoad: Set[(WineVintages, Int)] =
        wv.filter { wv => idsWithErrors.contains(wv.wineId) }
          .map { wv =>
            val lastSuccessfulPageNo = lastSavedPages(wv.wineId)
            wv -> (lastSuccessfulPageNo + 1)
          }
      val notYetLoadedIds: Set[Int] =
        allWineIds -- successfullyLoadedWineIds -- idsWithErrors
      val notYetLoadedPages = wv.filter { wv => notYetLoadedIds.contains(wv.wineId) }
        .map { wv => wv -> 1 }
      log.info(s"notYetLoadedPages=${notYetLoadedPages.size} failedPages=${failedPagesToLoad.size}")
      failedPagesToLoad ++ notYetLoadedPages
    }).recoverWith { case e =>
      log.error(s"Error while loading UnloadedReviews: ${e.getMessage}")
      Future.failed(e)
    }
  }


}
