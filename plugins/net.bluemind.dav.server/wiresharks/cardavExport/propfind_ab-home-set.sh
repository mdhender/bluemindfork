#!/bin/bash

curl -k -v -X PROPFIND \
-H 'Content-Type: text/xml' \
-H 'Depth: 0' \
-d@./propfind_ab-home-set-req.txt \
https://admin:admin@mav-srv.willow.vmw/principals/__uids__/EB397106-443A-47BE-9CA5-558037E137BF/
