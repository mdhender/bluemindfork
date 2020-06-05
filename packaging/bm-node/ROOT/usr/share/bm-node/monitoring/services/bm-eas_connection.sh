#!/bin/sh
$(curl --silent -XOPTIONS http://localhost:8082/Microsoft-Server-ActiveSync/)
result=$?
exit $result
