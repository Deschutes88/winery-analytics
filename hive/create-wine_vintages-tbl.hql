-- / user/ admin/ swines
CREATE TABLE wine_vintages(
          id int, 
          name string, 
          type_id int, 
          rgn__id int, 
          rgn__name string, 
          rgn__seo_name string, 
          rgn__cntry__code string, 
          rgn__cntry__name string, 
          rgn__cntry__regions_count int, 
          rgn__cntry__users_count int, 
          rgn__cntry__wines_count int, 
          rgn__cntry__wineries_count int, 
          winery__id int, 
          winery__name string, 
          winery__seo_name string, 
          winery__stats__ratings_count int, 
          winery__stats__ratings_average float, 
          winery__stats__wines_count int, 
          stats__ratings_count int, 
          stats__ratings_average float, 
          stats__labels_count int, 
          hidden boolean, 
          vintages__id int, 
          vintages__seo_name string, 
          vintages__year string, 
          vintages__name string, 
          vintages__stats__ratings_count int, 
          vintages__stats__ratings_average float, 
          vintages__stats__labels_count int)
ROW FORMAT SERDE 'org.apache.hadoop.hive.serde2.OpenCSVSerde'
WITH SERDEPROPERTIES (
   "separatorChar" = "\t",
   "escapeChar"    = "\\",
   "skip.header.line.count"="1"
)
STORED AS TEXTFILE
LOCATION  'hdfs://cdh.equineintel.com:8020/user/hdfs/wine_vintages';