#!/bin/bash

rm -f ./prov.cli

for i in `seq 100 150`; do
    echo "user quickcreate --random user${i}@devenv.blue" >> ./prov.cli
done

time bm-cli ./prov.cli
