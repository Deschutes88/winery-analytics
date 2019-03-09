package swines.proxies

import java.net.InetSocketAddress

import scala.concurrent.Future
import akka.actor.ActorSystem

import concurrent.duration._
import akka.http.scaladsl.{ClientTransport, Http}
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCode, StatusCodes}
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.settings.{ClientConnectionSettings, ConnectionPoolSettings}
import akka.stream.Materializer

import scala.concurrent.ExecutionContext

object ClientJson {

  sealed trait JsonResult
  case class JsonOk(json: String) extends JsonResult
  case class BadResponse(statusCode: StatusCode, body: String) extends JsonResult
  case class Error(except: Throwable) extends JsonResult

  def getJson(url: String, proxy: String, proxyPort: Int)
             (implicit executionContext: ExecutionContext,
              system: ActorSystem,
              materializer: Materializer): Future[JsonResult] = {
    val httpsProxyTransport = ClientTransport.httpsProxy(InetSocketAddress.createUnresolved(proxy, proxyPort))
    val settings = ConnectionPoolSettings(system)
      .withConnectionSettings(ClientConnectionSettings(system)
        .withTransport(httpsProxyTransport)
        .withIdleTimeout(10 seconds)
        .withConnectingTimeout(10 seconds)
        )
    val request = HttpRequest(uri = url).addHeader(RawHeader("accept", "application/json"))
    Http()
      .singleRequest(request, settings = settings)
      .flatMap{
        case HttpResponse(StatusCodes.OK, h, e, p) =>
          e.toStrict(10 seconds).map(r => JsonOk(r.data.utf8String))
        case HttpResponse(statusCode, _, e, _) =>
          e.toStrict(10 seconds).map(r => BadResponse(statusCode, r.data.utf8String))
      }
      .recoverWith {
        case exception => Future.successful(Error(exception))
      }
  }

}
