-- / user/ admin/ swines
CREATE TABLE wine_reviews(
        review_id                  	int,
        rating              	float,
        note                	string,
        language            	string,
        created_at          	date,
        user_id             	int,
        user_seo_name       	string,
        user_alias          	string,
        user_visblty        	boolean,
        user_followers_count	int,
        user_following_count	int,
        user_ratings_count  	int,
        vintage_id          	int,
        vintage_seo_name    	string,
        vintage_year        	string,
        vintage_name        	string,
        vintge_stats_ratings_count	int,
        vintge_stats_ratings_average	float,
        vintge_stats_labels_count	int,
        vintge_wine_id      	int,
        vintge_wine_name    	string,
        vintge_wine_region_id	int,
        vintge_wine_rgn_name	string,
        vintge_wine_rgn_cntry_code	string,
        vintge_wine_rgn_cntry_name	string,
        activity_id         	int,
        activity_stats_likes_count	int,
        activity_stats_comments_count	int
        )
ROW FORMAT SERDE 'org.apache.hadoop.hive.serde2.OpenCSVSerde'
WITH SERDEPROPERTIES (
   "separatorChar" = "\t",
   "escapeChar"    = "\\",
   "skip.header.line.count"="1"
)
STORED AS TEXTFILE
LOCATION  'hdfs://cdh.equineintel.com:8020/user/hdfs/wine-reviews';



-- / user/ admin/ swines
CREATE TABLE wine_reviews(
        review_id                  	int,
        rating              	float,
        note                	string,
        language            	string,
        created_at          	date,
        user_id             	int,
        user_seo_name       	string,
        user_alias          	string,
        user_visblty        	boolean,
        user_followers_count	int,
        user_following_count	int,
        user_ratings_count  	int,
        vintage_id          	int,
        vintage_seo_name    	string,
        vintage_year        	string,
        vintage_name        	string,
        vintge_stats_ratings_count	int,
        vintge_stats_ratings_average	float,
        vintge_stats_labels_count	int,
        vintge_wine_id      	int,
        vintge_wine_name    	string,
        vintge_wine_region_id	int,
        vintge_wine_rgn_name	string,
        vintge_wine_rgn_cntry_code	string,
        vintge_wine_rgn_cntry_name	string,
        activity_id         	int,
        activity_stats_likes_count	int,
        activity_stats_comments_count	int
        )
ROW FORMAT DELIMITED FIELDS TERMINATED BY '\t'
STORED AS TEXTFILE
LOCATION  'hdfs://cdh.equineintel.com:8020/user/hdfs/wine-reviews'
TBLPROPERTIES('serialization.null.format'='',
                "skip.header.line.count"="1");
