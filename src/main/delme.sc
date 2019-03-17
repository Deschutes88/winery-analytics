
val badResponseFromLog =
  "^.*Bad response\\(404 Not Found\\) wineryId\\=\\[(\\d+)\\].*$".r
val s= "Bad response(404 Not Found) wineryId=[1] "
s match {
  case badResponseFromLog(w) => w
  case _ => -1
}

s.matches(badResponseFromLog.regex)