#!/bin/bash
rm -rf ../../mozilla/bm-connector-tb-webext/src/content/core2/client/*.js
cp -r net.bluemind.core.api.mozjs/generated/BM/*.js ../../mozilla/bm-connector-tb-webext/src/content/core2/client/
