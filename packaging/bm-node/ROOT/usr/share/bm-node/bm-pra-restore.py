#!/usr/bin/python

import sys
import os
import time
import glob
import json
import subprocess
from os import path
from datetime import datetime


def selectBackupGeneration(bmBackupPath):
    backup_generations = glob.glob(bmBackupPath + "/generation-*.json")
    if len(backup_generations) == 0:
        sys.stdout.write(
            "No backup generation files found in: " + bmBackupPath + "\n"
            )
        sys.exit(0)

    bgs = {}
    for backup_generation in backup_generations:
        with open(backup_generation) as json_data:
            try:
                bg = json.load(json_data)
                bgs[bg["protectionTime"]] = bg
            except ValueError as ve:
                sys.stderr.write(
                    "Ignoring file: " + backup_generation + " - " + ve.message + "\n"
                )
                continue

    ts = bgs.keys()
    ts.sort()

    while True:
        choice = displayMenu(ts, bgs)
        try:
            c = int(choice) - 1
            if c >= 0 and c < len(ts):
                return bgs[ts[c]]
        except:
            pass


def displayMenu(ts, bgs):
    i = 0
    for t in ts:
        i += 1
        d = datetime.fromtimestamp(t / 1000).strftime("%Y-%m-%d %H:%M:%S")
        g = bgs[t]

        status = "status: "
        if bgs[t]["withErrors"] is True:
            status += "Ending with errors"
        elif bgs[t]["withWarnings"] is True:
            status += "Ending with warnings"
        else:
            status += "OK"

        sys.stdout.write(
            "\t".join([
                str(i),
                d,
                g["blueMind"]["displayName"],
                status,
            ])
        )

    print("\nChoose backup generation to restore (1-" + str(i) + "): ")
    return sys.stdin.readline()


def getHostIp(bm_backup_path):
    p = subprocess.Popen(
        ["ip", "addr"], stdout=subprocess.PIPE, stderr=subprocess.STDOUT
    )
    for line in iter(p.stdout.readline, b""):
        if line.find("inet ") != -1:
            ip = line.strip().split(" ")[1].split("/")[0]
            if os.path.isdir(bm_backup_path + "/" + ip):
                return ip

    sys.stderr.write(
        "No BlueMind backup data found for this host in " + bm_backup_path + " !\n"
    )
    sys.exit(0)


def get_path_with_id(backup_path, p_ids):
    gen_id = None
    for generation_id_folder in os.listdir(backup_path):
        if generation_id_folder in p_ids:
            gen_id = generation_id_folder
    return backup_path + gen_id


def getBmConfTagBackupPath(bm_backup_path, generation_to_restore):
    bmCoreTagBackupPath = glob.glob(bm_backup_path + "/*/bm/conf")
    if len(bmCoreTagBackupPath) != 0:
        for part in generation_to_restore["parts"]:
            if part["tag"] == "bm/conf":
                return bmCoreTagBackupPath[0] + "/" + str(part["id"])

    sys.stderr.write(
        "No backup for tag bm/core found, unable to restore BlueMind data\n"
    )
    sys.exit(1)


def getBmEsTagBackupPath(bm_backup_path, generation_to_restore):
    bmEsTagBackupPath = glob.glob(bm_backup_path + "/*/bm/es")
    if len(bmEsTagBackupPath) != 0:
        for part in generation_to_restore["parts"]:
            if part["tag"] == "bm/es":
                return bmEsTagBackupPath[0] + "/" + str(part["id"])

    sys.stderr.write(
        "No backup for tag bm/core found, unable to restore BlueMind data\n"
    )
    sys.exit(1)


def execCmd(logFileName, cmd, env, desc):
    logFile = open(logFileName, "a")
    logFile.write(
        "----------------------\nRunning command: " + str(cmd) + "\n"
        )
    logFile.flush()
    if env is None:
        p = subprocess.Popen(cmd, stdout=logFile, stderr=logFile)
    else:
        p = subprocess.Popen(cmd, stdout=logFile, stderr=logFile, env=env)

    spinner = ["-", "\\", "|", "/"]
    c = 0
    while True:
        sys.stdout.write("\r" + desc + " " + spinner[c % len(spinner)])
        sys.stdout.flush()
        c += 1
        time.sleep(0.5)

        if p.poll() is not None:
            break

    if p.returncode != 0:
        sys.stdout.write("\r" + desc + " FAIL (" + str(p.returncode) + ")\n")
        sys.exit(p.returncode)

    sys.stdout.write("\r" + desc + " OK\n")
    sys.stdout.flush()

    logFile.close()


def restoreBmNode(backup_path, p_ids):
    bm_conf = backup_path + "/bm/conf/"
    bm_node_path = get_path_with_id(bm_conf, p_ids)

    execCmd(
        LOG_FILE,
        ["service", "bm-node", "stop"],
        None,
        "Stopping BlueMind Node service"
    )
    execCmd(
        LOG_FILE,
        [
            "rsync",
            "-avH",
            "--delete",
            bm_node_path + "/var/backups/bluemind/work/conf/etc/bm-node/",
            "/etc/bm-node/",
        ],
        None,
        "Restoring BlueMind Node configuration",
    )


def restoreBmHps(backup_path):
    execCmd(
        LOG_FILE,
        [
            "rsync",
            "-avH",
            "--delete",
            backup_path + "/var/backups/bluemind/work/conf/etc/bm-hps/",
            "/etc/bm-hps/",
        ],
        None,
        "Restoring BlueMind HPS configuration",
    )


def restorePostfixConfig(backup_path):
    execCmd(
        LOG_FILE,
        [
            "rsync",
            "-avH",
            "--delete",
            backup_path + "/var/backups/bluemind/work/conf/etc/postfix/",
            "/etc/postfix/",
        ],
        None,
        "Restoring BlueMind Postfix configuration",
    )


def restoreBmConf(backup_path, p_ids):
    bm_conf = backup_path + "/bm/conf/"
    bm_conf_path = get_path_with_id(bm_conf, p_ids)

    execCmd(
        LOG_FILE,
        [
            "rsync",
            "-avH",
            "--delete",
            bm_conf_path + "/var/backups/bluemind/work/conf/etc/bm/",
            "/etc/bm/",
        ],
        None,
        "Restoring BlueMind common configuration",
    )

    execCmd(
        LOG_FILE,
        ["chown", "root:www-data", "/etc/bm/bm-core.tok"],
        None,
        "Fix owner of bm-core.tok file",
    )

    execCmd(
        LOG_FILE,
        ["chmod", "440", "/etc/bm/bm-core.tok"],
        None,
        "Fix permission of bm-core.tok file",
    )


def restoreCertificate():
    execCmd(
        LOG_FILE,
        [
            "cp",
            "-f",
            "/etc/bm/certs/bm_cert.pem",
            "/etc/ssl/certs/bm_cert.pem"
        ],
        None,
        "Restoring BlueMind certificate file",
    )


def restoreBmHollow(backup_path):
    execCmd(
        LOG_FILE,
        [
            "rsync",
            "-avH",
            "--delete",
            backup_path + "/var/backups/bluemind/work/directory/",
            "/var/spool/bm-hollowed/directory",
        ],
        None,
        "Restoring BlueMind Hollow Directory configuration",
    )


def restoreBmCore(bmCoreTagBackupPath, p_ids):
    execCmd(LOG_FILE, ["bmctl", "stop"], None, "Stopping BlueMind services")
    bmcore_conf_path = bmCoreTagBackupPath + "/bm/conf/"
    bm_conf_path = get_path_with_id(bmcore_conf_path, p_ids)

    bmcore_core_path = bmCoreTagBackupPath + "/bm/core/"
    bm_core_path = get_path_with_id(bmcore_core_path, p_ids)

    restoreBmHps(bm_conf_path)
    restorePostfixConfig(bm_conf_path)
    restoreBmHollow(bm_core_path)
    restoreCertificate()


def restoreMailImap(bmcyrusBackupPath, p_ids, mailImapPath):
    spool_cyrus_path = "/var/spool/cyrus/"
    lib_cyrus_path = "/var/lib/cyrus/"
    bm_conf_cyrus_path = bmcyrusBackupPath + "/bm/conf/"
    bm_cyrus_path = get_path_with_id(bm_conf_cyrus_path, p_ids)

    restoreCertificate()

    for f in ["cyrus.conf", "imapd.conf", "cyrus-admins", "cyrus-partitions"]:
        execCmd(
            LOG_FILE,
            [
                "cp",
                "-f",
                bm_cyrus_path + "/var/backups/bluemind/work/conf/etc/" + f,
                "/etc/" + f,
            ],
            None,
            "Restoring " + f + " file",
        )

    execCmd(
        LOG_FILE,
        ["rsync", "-avH", mailImapPath + lib_cyrus_path, lib_cyrus_path],
        None,
        "Restoring Cyrus lib files",
    )

    execCmd(
        LOG_FILE,
        [
            "rsync",
            "-avH",
            "--delete",
            mailImapPath + spool_cyrus_path,
            spool_cyrus_path,
        ],
        None,
        "Restoring Cyrus spool files",
    )

    execCmd(
        LOG_FILE,
        ["chown", "-R", "cyrus:mail", lib_cyrus_path, spool_cyrus_path],
        None,
        "Fixing Cyrus files rights",
    )

    for f in ["/etc/bm/bm-cyrus-imapd.disabled", "/etc/bm/bm-lmtpd.disabled"]:
        if path.exists(f):
            execCmd(
                LOG_FILE,
                [
                    "rm",
                    "-f",
                    f,
                ],
                None,
                "Deleting" + f + " file",
            )


def restoreBmEs(bmEsBackupPath, p_ids, bmEsPath):
    elasticsearch_yml = "/usr/share/bm-elasticsearch/config/elasticsearch.yml"
    backup_es_conf = "/var/backups/bluemind/work/conf/"
    bm_conf_es_path = bmEsBackupPath + "/bm/conf/"

    bm_es_path = get_path_with_id(bm_conf_es_path, p_ids)
    es_yml_backuped = bm_es_path + backup_es_conf + elasticsearch_yml

    execCmd(
        LOG_FILE,
        [
            "cp",
            "-f",
            es_yml_backuped,
            "/usr/share/bm-elasticsearch/config/elasticsearch.yml",
        ],
        None,
        "Restoring ES configuration",
    )

    execCmd(
        LOG_FILE,
        ["rm", "-Rf", "/var/spool/bm-elasticsearch/repo"],
        None,
        "Cleanup ES repo",
    )

    execCmd(
        LOG_FILE,
        [
            "rsync",
            "-r",
            bmEsTagBackupPath + "/var/spool/bm-elasticsearch/",
            "/var/spool/bm-elasticsearch",
        ],
        None,
        "Copy backup to ES repo",
    )

    execCmd(
        LOG_FILE,
        [
            "chown",
            "-R",
            "elasticsearch:elasticsearch",
            "/var/spool/bm-elasticsearch/repo",
        ],
        None,
        "chown ES repo",
    )

    execCmd(
        LOG_FILE, ["service", "bm-elasticsearch", "restart"], None, "Restarting Elasticsearch",
    )

    execCmd(
        LOG_FILE, ['bash', '-c', 'while [[ "$(curl -s -o /dev/null -w \'\'%{http_code}\'\' localhost:9200/_cluster/health?pretty=true)" != "200" ]]; do sleep 5; done'], None, "Waiting for ES cluster is up",
    )

    execCmd(
        LOG_FILE,
        [
            "curl",
            "-X",
            "POST",
            "localhost:9200/_snapshot/bm-elasticsearch/snapshot-es/_restore",
        ],
        None,
        "Restoring ES index files",
    )


def restoreBmPgsql(bmPgsqlPath):
    execCmd(
        LOG_FILE,
        [
            "rsync",
            "-avH",
            "--delete",
            bmPgsqlPath + "/var/backups/bluemind/work/pgsql/configuration/14/",
            "/etc/postgresql/14/",
        ],
        None,
        "Restoring PostgreSQL configuration",
    )

    execCmd(
        LOG_FILE,
        ["chown", "-R", "postgres:postgres", "/etc/postgresql/14/"],
        None,
        "Fixing PostgreSQL configuration rights",
    )

    execCmd(
        LOG_FILE, ["service", "postgresql", "stop"], None, "Stopping PostGreSQL"
    )

    execCmd(
        LOG_FILE, ["service", "telegraf", "stop"], None, "Stopping Telegraf"
    )

    execCmd(
        LOG_FILE, ["service", "postgresql", "start"], None, "Starting PostGreSQL"
    )

    execCmd(
        LOG_FILE,
        ["su", "-c", "dropdb bj", "postgres"],
        None,
        "Dropping BlueMind database",
    )

    execCmd(
        LOG_FILE,
        ["su", "-c", "createdb bj --encoding=utf-8", "postgres"],
        None,
        "Creating empty BlueMind database",
    )

    execCmd(
        TMP_BJ_RESTORE,
        [
            "/usr/bin/pg_restore",
            "-l",
            bmPgsqlPath + "/var/backups/bluemind/work/pgsql/dump.sql",
        ],
        {"PGPASSWORD": "bj"},
        "Convert SQL DUMP to SQL File",
    )

    execCmd(
        LOG_FILE,
        [
            "/usr/bin/pg_restore",
            "-U",
            "bj",
            "-h",
            "localhost",
            "-d",
            "bj",
            "-L",
            TMP_BJ_RESTORE,
            bmPgsqlPath + "/var/backups/bluemind/work/pgsql/dump.sql",
        ],
        {"PGPASSWORD": "bj"},
        "Restaure DB bj",
    )

    p = subprocess.Popen(
        "pw=$(echo \"select configuration -> 'sw_password' as sw_password from t_systemconf;\" | PGPASSWORD=bj psql -t -U bj -h 127.0.0.1 bj); echo $pw",
        shell=True,
        stdout=subprocess.PIPE,
    )
    password = p.stdout.read()
    password = password.decode("utf-8").rstrip()
    execCmd(
        LOG_FILE,
        ["htpasswd", "-b", "-c", "/etc/nginx/sw.htpasswd", "admin", password],
        None,
        "Reset BlueMind SW password",
    )

    execCmd(
        LOG_FILE, ["service", "telegraf", "start"], None, "Starting Telegraf"
    )


def restoreBmPgsqlData(bmPgsqlPath):
    execCmd(
        LOG_FILE,
        [
            "rsync",
            "-avH",
            "--delete",
            bmPgsqlPath + "/var/backups/bluemind/work/pgsql-data/configuration/14/",
            "/etc/postgresql/14/",
        ],
        None,
        "Restoring PostgreSQL configuration",
    )

    execCmd(
        LOG_FILE,
        ["chown", "-R", "postgres:postgres", "/etc/postgresql/14/"],
        None,
        "Fixing PostgreSQL configuration rights",
    )

    execCmd(
        LOG_FILE, ["service", "postgresql", "stop"], None, "Stopping PostGreSQL"
    )

    execCmd(
        LOG_FILE, ["service", "telegraf", "stop"], None, "Stopping Telegraf"
    )

    execCmd(
        LOG_FILE, ["service", "postgresql", "start"], None, "Starting PostGreSQL"
    )

    execCmd(
        LOG_FILE,
        ["su", "-c", "dropdb bj-data", "postgres"],
        None,
        "Dropping BlueMind database bj-data",
    )

    execCmd(
        LOG_FILE,
        ["su", "-c", "createdb bj-data --encoding=utf-8", "postgres"],
        None,
        "Creating empty BlueMind database bj-data",
    )

    execCmd(
        TMP_BJDATA_RESTORE,
        [
            "/usr/bin/pg_restore",
            "-l",
            bmPgsqlPath + "/var/backups/bluemind/work/pgsql-data/dump.sql",
        ],
        {"PGPASSWORD": "bj"},
        "Convert SQL DUMP to SQL File",
    )

    execCmd(
        LOG_FILE,
        [
            "/usr/bin/pg_restore",
            "-U",
            "bj",
            "-h",
            "localhost",
            "-d",
            "bj-data",
            "-L",
            TMP_BJDATA_RESTORE,
            bmPgsqlPath + "/var/backups/bluemind/work/pgsql-data/dump.sql",
        ],
        {"PGPASSWORD": "bj"},
        "Restaure DB bj-data",
    )

    execCmd(
        LOG_FILE, ["service", "telegraf", "start"], None, "Stopping Telegraf"
    )


def restoreMailArchive(mailArchivePath):
    if os.path.exists(mailArchivePath + "/var/spool/bm-hsm"):
        execCmd(
            LOG_FILE,
            [
                "rsync",
                "-avH",
                "--delete",
                mailArchivePath + "/var/spool/bm-hsm/",
                "/var/spool/bm-hsm/",
            ],
            None,
            "Restoring BlueMind HSM directory",
        )


def restoreFilehostingData(filehostingDataPath):
    if os.path.exists(filehostingDataPath + "/var/spool/bm-filehosting"):
        execCmd(
            LOG_FILE,
            [
                "rsync",
                "-avH",
                "--delete",
                filehostingDataPath + "/var/spool/bm-filehosting/",
                "/var/spool/bm-filehosting/",
            ],
            None,
            "Restoring BlueMind filehosting directory",
        )


now = datetime.now()
dt_string = now.strftime("%d-%m-%Y-%H-%M-%S")
LOG_FILE = dt_string + sys.argv[0] + ".log"
f = open(LOG_FILE, "w")
f.write("Starting PRA\n")
f.close()

BM_BACKUP_DIR = "/var/backups/bluemind"
BM_BACKUP_RSYNC_ROOT = BM_BACKUP_DIR + "/dp_spool/rsync"

TMP_BJ_RESTORE = "/tmp/restore-bj.list"
TMP_BJDATA_RESTORE = "/tmp/restore-bjdata.list"

if not os.path.isdir(BM_BACKUP_DIR):
    sys.stderr.write(BM_BACKUP_DIR + " must exists!\n")
    sys.exit(1)

BM_BACKUP_HOST_ROOT = BM_BACKUP_RSYNC_ROOT + "/" + getHostIp(BM_BACKUP_RSYNC_ROOT)

generationToRestore = selectBackupGeneration(BM_BACKUP_DIR)

bmCoreTagBackupPath = getBmConfTagBackupPath(BM_BACKUP_RSYNC_ROOT, generationToRestore)
bmEsTagBackupPath = getBmEsTagBackupPath(BM_BACKUP_RSYNC_ROOT, generationToRestore)

ids = []
for part in generationToRestore["parts"]:
    ids.append(str(part["id"]))


for part in generationToRestore["parts"]:
    tag = part["tag"]
    pId = part["id"]

    tagPath = BM_BACKUP_HOST_ROOT + "/" + tag + "/" + str(pId)

    if os.path.isdir(tagPath):
        sys.stdout.write("This server is tagged as " + tag + "\n")
    else:
        continue

    if tag == "mail/imap":
        sys.stdout.write("\n**** RESTORE CYRUS SERVICE ****\n")
        restoreMailImap(BM_BACKUP_HOST_ROOT, ids, tagPath)
    elif tag == "bm/pgsql-data":
        sys.stdout.write("\n**** RESTORE BJ-DATA DATABASE ****\n")
        restoreBmPgsqlData(tagPath)
    elif tag == "bm/es":
        sys.stdout.write("\n**** RESTORE ELASTICSEARCH SERVICE ****\n")
        restoreBmEs(BM_BACKUP_HOST_ROOT, ids, tagPath)
    elif tag == "bm/core":
        sys.stdout.write("\n**** RESTORE CORE SERVICE ****\n")
        restoreBmCore(BM_BACKUP_HOST_ROOT, ids)
    elif tag == "bm/pgsql":
        sys.stdout.write("\n**** RESTORE BJ DATABASE ****\n")
        restoreBmPgsql(tagPath)
    elif tag == "mail/archive":
        sys.stdout.write("\n**** RESTORE HSM ****\n")
        restoreMailArchive(tagPath)
    elif tag == "filehosting/data":
        sys.stdout.write("\n**** RESTORE FILEHOSTING SERVICE ****\n")
        restoreFilehostingData(tagPath)
    elif tag == "bm/conf":
        sys.stdout.write("\n**** RESTORE COMMON CONFIGURATION ****\n")
        restoreBmNode(BM_BACKUP_HOST_ROOT, ids)
        restoreBmConf(BM_BACKUP_HOST_ROOT, ids)

sys.stdout.write("\n**** STARTING BLUEMIND SERVICES ****\n")
execCmd(
    LOG_FILE,
    [
        "service",
        "bm-node",
        "start"
    ],
    None,
    "Starting BlueMind Node service"
)

execCmd(LOG_FILE, ["bmctl", "start"], None, "Starting BlueMind services")
