#!/usr/bin/env bash
java -Dreviews.scraper.parallelizm=500 \
    -cp swines.jar swines.reviews.ScrapReviews
