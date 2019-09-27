#!/bin/bash

test -d /var/spool/cyrus/meta || exit 1

# leaf directories: https://stackoverflow.com/a/50548890/909674
cd /var/spool/cyrus/meta && \
    find . -type d -maxdepth 7 | cut -b3- | \
    sort -r | awk 'a!~"^"$0{a=$0;print}' | sort

exit 0
