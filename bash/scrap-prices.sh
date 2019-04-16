#!/usr/bin/env bash
java -Dprices.scraper.parallelizm=500 \
     -cp swines.jar swines.prices.ScrapPrices