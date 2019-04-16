set hive.support.quoted.identifiers=none;

create table wine_joined as select
    R.`(vintage_id)?+.+`,
    P.`(vintage_id)?+.+`,
    V.*
    from wine_reviews R
        left join wine_prices P on
            (P.vintage_id = R.vintage_id)
        left join wine_vintages V on
            (V.vintages__id = R.vintage_id)
    where R.vintage_id is not null
    ;