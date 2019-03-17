//package swines
//
//import java.nio.file._
//
//import akka.actor.ActorSystem
//import akka.stream.ActorMaterializer
//
//object MoveReviewsToSeparateDir {
//
//  def main(args: Array[String]): Unit = {
//    import concurrent.ExecutionContext.Implicits.global
//    implicit val actorSystem = ActorSystem("SwinesActorSys")
//    implicit val actorMaterializer = ActorMaterializer()
//
//    val dstDir = Paths.get(cfg.reviews.warehouse)
//    if(Files.notExists(dstDir) || !Files.isDirectory(dstDir)) {
//      print(s"Creating dir $dstDir")
//      Files.createDirectory(dstDir)
//    }
//
//    val moveReviewsFiles = files.SavedReviews.listFilenames(cfg.files.warehouse)
//      .runForeach { src =>
//        val dst = Paths.get(cfg.reviews.warehouse, src.getFileName.toString)
//        Files.move(src, dst, StandardCopyOption.REPLACE_EXISTING)
//        println(s"$src was moved to $dst")
//      }
//
//    moveReviewsFiles.onComplete { r =>
//      println(s"Finished with ${r}")
//      actorSystem.terminate()
//    }
//  }
//
//}
