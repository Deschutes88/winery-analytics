#!/usr/bin/env bash
curl -sSf "https://raw.githubusercontent.com/clarketm/proxy-list/master/proxy-list.txt" | sed '1,3d; $d; s/\s.*//; /^$/d' > proxy-list.txt