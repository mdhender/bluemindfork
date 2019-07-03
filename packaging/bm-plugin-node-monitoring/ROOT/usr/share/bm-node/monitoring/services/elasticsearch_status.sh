#!/bin/sh

curl -s "$1:9200/_cat/nodes?v&h=heap.current,heap.max,heap.percent,file_desc.current,file_desc.max,file_desc.percent" | sed "1d"
