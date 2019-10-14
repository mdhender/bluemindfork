#!/bin/bash

DST_PATH=${dstPath}

ETC_PATH=${DST_PATH}/etc
if [ -e ${ETC_PATH} ]; then
    rm -rf ${ETC_PATH}
fi
mkdir -p ${ETC_PATH}

if [ -e /etc/bm ]; then
  echo "/etc/bm"
  cp -r /etc/bm ${ETC_PATH}
fi

if [ -e /etc/imapd.conf ]; then
  echo "/etc/imapd.conf"
  cp -r /etc/imapd.conf ${ETC_PATH}/imapd.conf
fi

if [ -e /etc/cyrus.conf ]; then
  echo "/etc/cyrus.conf"
  cp -r /etc/cyrus.conf ${ETC_PATH}/cyrus.conf
fi

if [ -e /etc/cyrus-partitions ]; then
  echo "/etc/cyrus-partitions"
  cp -r /etc/cyrus-partitions ${ETC_PATH}/cyrus-partitions
fi

if [ -e /etc/cyrus-admins ]; then
  echo "/etc/cyrus-admins"
  cp -r /etc/cyrus-admins ${ETC_PATH}/cyrus-admins
fi

if [ -e /etc/postfix ]; then
  echo "/etc/postfix"
  cp -r /etc/postfix ${ETC_PATH}
fi

if [ -e /etc/bm-hps ]; then
  echo "/etc/bm-hps"
  cp -r /etc/bm-hps ${ETC_PATH}
fi

if [ -e /etc/bm-node ]; then
  echo "/etc/bm-node"
  cp -r /etc/bm-node ${ETC_PATH}
fi

if [ -e /usr/share/bm-elasticsearch/config/elasticsearch.yml ]; then
  ES_PATH=${DST_PATH}/usr/share/bm-elasticsearch/config
  mkdir -p ${ES_PATH}
  
  echo "/usr/share/bm-elasticsearch/config/elasticsearch.yml"
  cp -r /usr/share/bm-elasticsearch/config/elasticsearch.yml ${ES_PATH}/elasticsearch.yml
fi
