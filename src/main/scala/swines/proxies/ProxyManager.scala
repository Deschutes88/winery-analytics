package swines.proxies

import akka.actor.Actor

class ProxyManager(proxyList: List[(String, Int)]) extends Actor{

  override def receive: Receive = {
    case x => sender() ! x
  }

}
