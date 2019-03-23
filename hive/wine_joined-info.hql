select "price__amount", count(*) from wine_joined
where price__amount is not null
union all
select "note", count(*) from wine_joined
where note is not null
union all
select "note and price__amount", count(*) from wine_joined
where (note is not null) and (price__amount is not null)
union all
select "all", count(*) from wine_joined;

-- price_amount	 84 282 032
-- 2	note	103 093 154
-- 3	all	    112 330 596
-- note and price__amount 172

-- note and price__amount	79 157 452
-- 2	price__amount	    79 157 452
-- 3	note	            103 093 154
-- 4	all	                112 327 466
--
SELECT count(DISTINCT vintages__id) from wine_joined;
-- 5 865 245
