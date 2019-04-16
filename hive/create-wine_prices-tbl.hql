-- /user/hdfs/swines/wine-prices
CREATE TABLE wine_prices(
        wine_id int,
        vintage_id int,
        currency string,
        currency_name string,
        median__amount float,
        median__type string,
        price__amount string,
        price__discounted_from string,
        price__type string
        )
ROW FORMAT DELIMITED FIELDS TERMINATED BY '\t'
STORED AS TEXTFILE
LOCATION  'hdfs://cdh.equineintel.com:8020/user/hdfs/wine-prices'
TBLPROPERTIES('serialization.null.format'='',
              "skip.header.line.count"="1");