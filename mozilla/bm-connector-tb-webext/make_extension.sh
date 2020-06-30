#!/bin/bash

pushd src
rm -f ../test-mailextension.xpi
zip -r ../test-mailextension.xpi * -x "*~" "*git*"
popd
