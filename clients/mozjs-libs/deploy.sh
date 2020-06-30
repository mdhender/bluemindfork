#!/bin/bash
rm -rf ../../mozilla/bm-connector-tb/src/chrome/content/core2/client/*.js
rm -rf ../../mozilla/bm-connector-tb-webext/src/content/core2/client/*.js
cp -r net.bluemind.core.api.mozjs/generated/BM/*.js ../../mozilla/bm-connector-tb/src/chrome/content/core2/client/
cp -r net.bluemind.core.api.mozjs/generated/BM/*.js ../../mozilla/bm-connector-tb-webext/src/content/core2/client/
