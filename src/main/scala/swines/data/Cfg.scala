package swines.data

import com.typesafe.config.ConfigFactory

object cfg {
  protected val c = ConfigFactory.load()
  object reviewsScrapper{
    val parallelizm = c.getInt("reviews-scraper.parallelizm")
  }
  object pricesScrapper{
    val parallelizm = c.getInt("prices-scraper.parallelizm")
  }
  object checkProxies{
    val parallelizm = c.getInt("check-proxies.parallelizm")
    val url = c.getString("check-proxies.url")
  }
  object files {
    val warehouse = c.getString("files.warehouse")
    val globalLog = c.getString("files.global-log")
    val myGlobalLog = c.getString("files.my-global-log")
    val storage = c.getString("files.storage")
    val proxyList = c.getString("files.proxy-list")
    val uncheckedProxyList = c.getString("files.unchecked-proxy-list")
    val checkedProxyList = c.getString("files.checked-proxy-list")
    val dataToLoad = c.getString("files.data-to-load")
    val extortTSV = c.getString("files.export-tsv")

  }

}
