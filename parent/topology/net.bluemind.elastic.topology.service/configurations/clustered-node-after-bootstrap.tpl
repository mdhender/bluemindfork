# ======================== Elasticsearch Configuration =========================

# ---------------------------------- Cluster -----------------------------------

cluster.name: ${installationId}

# ------------------------------------ Node ------------------------------------
node.name: ${serverIp}
node.roles: ${roles}
#node.attr.rack: r1

# ----------------------------------- Paths ------------------------------------
#
path.data: /var/spool/bm-elasticsearch/data
path.logs: /var/log/bm-elasticsearch
path.repo: ["/var/spool/bm-elasticsearch/repo"]

#disable aut-index-create 
action.auto_create_index: +filebeat*,.watches,.triggered_watches,.watcher-history-*,-* 

# ----------------------------------- Memory -----------------------------------
bootstrap.memory_lock: ${memlock}

# BLUEMIND disable seccomp
# https://www.elastic.co/guide/en/elasticsearch/reference/master/_onerror_and_onoutofmemoryerror_checks.html
bootstrap.system_call_filter: false

# ---------------------------------- Network -----------------------------------
network.host: ${serverIp}

# --------------------------------- Discovery ----------------------------------

#es8
#discovery.type: multi-node

discovery.seed_hosts: ${otherSeedNodes}

# ---------------------------------- Various -----------------------------------

xpack.ml.enabled: false
xpack.monitoring.collection.enabled: false
xpack.security.enabled: false
xpack.watcher.enabled: false
ingest.geoip.downloader.enabled: false
