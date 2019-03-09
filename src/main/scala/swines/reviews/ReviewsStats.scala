package swines.reviews

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import com.typesafe.scalalogging.Logger
import swines.data.cfg
import swines.streams

object ReviewsStats {
  val myGlobalLog = Logger("MyGlobalLog")

  def main(args: Array[String]): Unit = {
    import concurrent.ExecutionContext.Implicits.global
    implicit val system = ActorSystem("SwinesActorSystem")
    implicit val materializer = ActorMaterializer()

    val wineIdsFromGlobalLog = streams.GlobalLog.succesfullWineIdsFromGlobalLog()
      .concat(streams.GlobalLog.succesfullWineIdsFromGlobalLog(cfg.files.myGlobalLog)).runWith(Sink.seq)


    val winesIdInDataToLoad = streams.DataToLoad.dataToLoad(cfg.files.dataToLoad).map(_.wineId).runWith(Sink.seq)
      .map { ids => ; ids }

    val r = for {
      saved <- wineIdsFromGlobalLog
      toLoad <- winesIdInDataToLoad
    } yield {
      println(s"DataToLoad contains ${toLoad.length} wineIds")
      println(s"GlobalLog /already saved/ contains ${saved.length} wineIds")
      val u = toLoad.toSet.diff(saved.toSet)
      println(s"So, there are ${u.size} wine reviews streams to be loaded from vivino.com")
      u
    }


    r.onComplete(_=> system.terminate())

  }

}
