#!/bin/bash

rsyncBackup="/var/backups/bluemind/dp_spool/rsync"

moveData() {
  spool=$1
  newSpool=${spool}"/data"
  meta=${spool}"/meta"

  mkdir -p ${newSpool}
  chown cyrus:mail ${newSpool}

  mkdir -p ${meta}

  pushd ${spool} 2>&1 > /dev/null
  for domain in *; do
    if [ ! -d ${domain} ]; then
      continue
    fi

    case ${domain} in
      meta|data|news)
        continue
        ;;
    esac

    echo "Moving data for cyrus partition: "${spool}"/"${domain}
    pushd ${spool}/${domain} 2>&1 > /dev/null
    
    if [ -d "domain" ]; then
      find domain -type d -exec mkdir -p ${meta}"/"${domain}"/"{} \;
      find domain -type f -name 'cyrus.*' -exec mv {} ${meta}"/"${domain}"/"{} \;
    fi
    popd 2>&1 > /dev/null

    mv ${spool}"/"${domain} ${newSpool}"/"${domain}
  done
  popd 2>&1 > /dev/null

  chown -R cyrus:mail ${meta}
}

hostDirs() {
  for i in ${rsyncBackup}"/"*; do
    hostDir=$i
    if [ ! -d ${hostDir} ]; then
      continue
    fi
    
    spoolBackupDirs ${hostDir}
  done
}

spoolBackupDirs() {
  backupsDir=$1"/mail/imap"
  if [ ! -d ${backupsDir} ]; then
    return
  fi

  for i in ${backupsDir}"/"*; do
    backupDir=$i
    if [ ! -d ${backupDir} ]; then
      continue
    fi

    spoolDir=${backupDir}"/var/spool/cyrus"
    if [ ! -d ${spoolDir} ]; then
      continue
    fi

    moveData ${spoolDir}
  done
}

hostDirs
