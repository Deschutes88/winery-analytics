set hive.support.quoted.identifiers=none;

create table wine_joined as select
    V.*,
    P.`(id)?+.+`,
    R.`(id)?+.+`
    from wine_vintages_0321 V
        left join wine_prices_0321 P on
            (P.id = V.vintages__id)
        left join wine_reviews_0321 R on
            (R.vintage_id = V.vintages__id) and
            (R.vintge_wine_id  = V.wine_id)
    where (V.vintages__id != "") and
          (V.wine_id  != "")
;
