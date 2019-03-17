package swines

import java.nio.file.Paths

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

object Test {

  def main(args: Array[String]): Unit = {
    import concurrent.ExecutionContext.Implicits.global
    implicit val system = ActorSystem("SwinesActorSystem")
    implicit val materializer = ActorMaterializer()

//    Paths
  }

}
