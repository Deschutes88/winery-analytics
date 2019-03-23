set hive.support.quoted.identifiers=none;

create table wine_joined as select
    V.*,
    R.`(id)?+.+`,
    P.`(id)?+.+`
    from wine_vintages_0321 V
        left join wine_reviews_0321 R on (V.vintages__id = R.vintage_id)
        left join wine_prices_0321 P on (R.vintage_id = P.id);