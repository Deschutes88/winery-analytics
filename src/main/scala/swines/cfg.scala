package swines

import com.typesafe.config.ConfigFactory

object cfg {
  protected val c = ConfigFactory.load()

  object wines {
    val warehouse = c.getString("wines.warehouse")

    object scrapper {

      object wineryIds {
        val start = c.getInt("wines.scraper.winery-ids.start")
        val stop = c.getInt("wines.scraper.winery-ids.stop")
      }

      val parallelizm = c.getInt("wines.scraper.parallelizm")
    }

    object exportTsv {
      val saveTo = c.getString("wines.export-tsv.save-to")
      val parallelizm = c.getInt("wines.scraper.parallelizm")
    }

  }

  object reviews {
    val warehouse = c.getString("reviews.warehouse")

    object scrapper {
      val parallelizm = c.getInt("reviews.scraper.parallelizm")
    }

    object exportTsv {
      val saveTo = c.getString("reviews.export-tsv.save-to")
      val parallelizm = c.getInt("reviews.scraper.parallelizm")
    }

  }

  object prices {
    val warehouse = c.getString("prices.warehouse")
    object scrapper {
      val parallelizm = c.getInt("prices.scraper.parallelizm")
    }

    object exportTsv {
      val saveTo = c.getString("prices.export-tsv.save-to")
      val parallelizm = c.getInt("prices.scraper.parallelizm")
    }


  }

  object checkProxies {
    val parallelizm = c.getInt("check-proxies.parallelizm")
    val url = c.getString("check-proxies.url")
  }

  object files {
//    val warehouse = c.getString("files.warehouse")
    val globalLog = c.getString("files.global-log")
    val myGlobalLog = c.getString("files.my-global-log")
//    val storage = c.getString("files.storage")
    val proxyList = c.getString("files.proxy-list")
    val uncheckedProxyList = c.getString("files.unchecked-proxy-list")
    val checkedProxyList = c.getString("files.checked-proxy-list")
    val winesVintages = c.getString("files.data-to-load")

    //    val failedReviews = c.getString("files.failed-reviews")


  }

}
