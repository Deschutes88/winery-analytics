-- /user/hdfs/swines/wine-prices
CREATE TABLE wine_prices(
        id int,
        currency string,
        currency_name string,
        median__amount float,
        median__type string,
        price__amount string,
        price__discounted_from string,
        price__type string
        )
ROW FORMAT SERDE 'org.apache.hadoop.hive.serde2.OpenCSVSerde'
WITH SERDEPROPERTIES (
   "separatorChar" = "\t",
   "escapeChar"    = "\\",
   "skip.header.line.count"="1"
)
STORED AS TEXTFILE
LOCATION  'hdfs://cdh.equineintel.com:8020/user/hdfs/wine-prices';