#!/bin/bash
total=0
total=$((total + `dig bluemind.net MX +short | head -1 | wc -l`))
total=$((total + `dig orange.fr MX +short | head -1 | wc -l`))
total=$((total + `dig google.com MX +short | head -1 | wc -l`))

if [ $total -gt 1 ]
then
        exit 0
else
        exit 1
fi

