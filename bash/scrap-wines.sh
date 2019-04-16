#!/usr/bin/env bash
java -Dwines.scraper.winery-ids.start=10 \
     -Dwines.scraper.winery-ids.stop=270000 \
     -Dwines.scraper.parallelizm=300 \
     -cp swines.jar swines.wines.ScrapWines
