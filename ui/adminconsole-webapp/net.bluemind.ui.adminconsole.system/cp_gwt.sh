#!/bin/bash
mvn clean install
find . -iname net.*.jar | xargs -I {} scp {} ubuntu-bmcore2:/tmp/
