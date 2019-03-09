package swines.data

//import java.util.Date
import io.circe._, io.circe.generic.auto._, io.circe.parser._, io.circe.syntax._

case class ActivityStats(likes_count: Integer, comments_count: Integer)
case class Activity(id: Integer, statistics: ActivityStats)
case class User(id: Integer, seo_name: String, alias: String, visibility: String, image: Image,
                followers_count: Integer, following_count: Integer, ratings_count: Integer)
case class Review(id: Integer, rating: Float, note: String, language: String, created_at: String, user: User,
                  vintage: Vintage, activity: Activity)
case class Reviews(reviews: Seq[Review])

