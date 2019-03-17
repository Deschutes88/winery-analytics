package swines.data

case class Statistics(ratings_count: Integer, ratings_average: Double, wines_count: Integer, labels_count: Integer)
case class Vintage(id: Integer, seo_name: String, year: String, name: String, statistics: Statistics, wine: Wine)

case class Variations(
                       bottle_large: String,
                       bottle_medium: String, bottle_medium_square: String,
                       bottle_small: String, bottle_small_square: String,
                       label: String, label_large: String, label_medium: String,
                       label_medium_square: String, label_small_square: String,
                       large: String, medium: String, medium_square: String, small_square: String)

case class Image(location: String, variations: Variations)
case class Winery(id: Integer, name: String, seo_name: String, statistics: Statistics)
case class Grape(id: Integer, name: String , seo_name: String, has_detailed_info: Boolean, wines_count: Integer)
case class Currency(code: String, name: String, prefix: String, suffix: String)

case class Country(
                    code: String, name: String, seo_name: String, currency: Currency,
                    regions_count: Integer, users_count: Integer, wines_count: Integer, wineries_count: Integer,
                    most_used_grapes: Seq[Grape])

case class Region(id: Integer, name: String, seo_name: String, country: Country, background_image: Image)

case class Wine(
                 id: Integer, name: String, type_id: Integer, region: Region, winery: Winery,
                 image: Image, statistics: Statistics, hidden: Boolean, vintages: Seq[Vintage])

case class Wines(wines: Seq[Wine])

case class WineVintages(wineId: Int, vintagesIds: List[Int])