#!/bin/bash

test -f /var/spool/bm-elasticsearch/data/es.pid && pkill -F /var/spool/bm-elasticsearch/data/es.pid

# devnull redir is important, otherwise bm-node is stuck reading
ES_PATH_CONF=/usr/share/bm-elasticsearch/config sudo -HEn -g elasticsearch -u elasticsearch \
    /usr/share/bm-elasticsearch/bin/elasticsearch \
    -d -p /var/spool/bm-elasticsearch/data/es.pid >/dev/null 2>&1

echo "ES restarted, waiting 60s for port..."
wait-for-it --strict --timeout=60 --host=$(hostname -i) --port=9300
exit 0
