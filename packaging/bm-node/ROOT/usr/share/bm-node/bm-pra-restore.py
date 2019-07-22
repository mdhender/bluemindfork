#!/usr/bin/python

import sys, os, time, glob
import json, datetime
import subprocess

def selectBackupGeneration(bmBackupPath):
	backupGenerations = glob.glob(bmBackupPath + "/generation-*.json")
	if len(backupGenerations) == 0:
		sys.stdout.write("No backup generation files found in: " + bmBackupPath + "\n")
		sys.exit(0)
	
	bgs = {}
	for backupGeneration in backupGenerations:
		with open(backupGeneration) as json_data:
			try:
				bg = json.load(json_data)
				bgs[bg['protectionTime']] = bg
			except ValueError as ve:
				sys.stderr.write("Ignoring file: " + backupGeneration + " - " + ve.message + "\n")
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
		d = datetime.datetime.fromtimestamp(t/1000).strftime('%Y-%m-%d %H:%M:%S')
		g = bgs[t]
		
		status = "status: "
		if bgs[t]['withErrors'] == True:
			status += "Ending with errors"
		elif bgs[t]['withWarnings'] == True:
			status += "Ending with warnings"
		else:
			status += "OK"
		
		sys.stdout.write(str(i) + "\t" + d + "\t" + g['blueMind']['displayName'] + "\t" + status + "\n")
		
	sys.stdout.write("Choose backup generation to restore (1-" + str(i) + "): ")
	return sys.stdin.readline()

def getHostIp(bmBackupPath):
	p =	subprocess.Popen(['ip', 'addr'], stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
	for line in iter(p.stdout.readline, b''):		
		if line.find('inet ') != -1:
			ip = line.strip().split(' ')[1].split('/')[0]
			if os.path.isdir(bmBackupPath + "/" + ip):
				return ip
	
	sys.stderr.write("No BlueMind backup data found for this host in " + bmBackupPath + " !\n")
	sys.exit(0)

def getBmCoreTagBackupPath(bmBackupPath, generationToRestore):
	bmCoreTagBackupPath = glob.glob(bmBackupPath + "/*/bm/core")
	if len(bmCoreTagBackupPath) != 0:
		for part in generationToRestore['parts']:
			if part['tag'] == 'bm/core':
				return bmCoreTagBackupPath[0] + "/" + str(part['id'])
		
	sys.stderr.write("No backup for tag bm/core found, unable to restore BlueMind data\n")
	sys.exit(1)
	
def execCmd(logFileName, cmd, env, desc):
	logFile = open(logFileName, 'a')
	logFile.write('----------------------\nRunning command: ' + str(cmd) + "\n")
	logFile.flush()
	if env is None:
	    p = subprocess.Popen(cmd, stdout=logFile, stderr=logFile)
        else:
	    p = subprocess.Popen(cmd, stdout=logFile, stderr=logFile, env=env)
	
	spinner = [ '-', '\\', '|', '/' ]
	c = 0
	while True:
		sys.stdout.write("\r" + desc + " " + spinner[c % len(spinner)])
		sys.stdout.flush()
		c += 1
		time.sleep(0.5)
		
		if p.poll() is not None:
			break;
	
	if p.returncode != 0:
		sys.stdout.write("\r" + desc + " FAIL (" + str(p.returncode) + ")\n")
		sys.exit(p.returncode)
	
	sys.stdout.write("\r" + desc + " OK\n")
	sys.stdout.flush()

	logFile.close()

def restoreCommonConfiguration(bmCorePath):
	execCmd(LOG_FILE,\
	['bmctl', 'stop'],\
	None,\
	"Stopping BlueMind services")

	execCmd(LOG_FILE,\
	['service', 'bm-node', 'stop'],\
	None,\
	"Stopping BlueMind Node service")

	execCmd(LOG_FILE, \
	['rsync',\
	 '-avH',\
	 '--delete',\
	 bmCoreTagBackupPath + '/var/backups/bluemind/conf/etc/bm/',
	 '/etc/bm/'],\
	None,\
	"Restoring BlueMind common configuration")
	
	execCmd(LOG_FILE, \
	['rsync',\
	 '-avH',\
	 '--delete',\
	 bmCoreTagBackupPath + '/var/backups/bluemind/conf/etc/bm-node/',
	 '/etc/bm-node/'],\
	None,\
	"Restoring BlueMind Node configuration")
	
	execCmd(LOG_FILE,\
        ['chown', 'root:www-data', '/etc/bm/bm-core.tok'],\
	None,\
	"Fix owner of bm-core.tok file")
	
	execCmd(LOG_FILE,\
        ['chmod', '440', '/etc/bm/bm-core.tok'],\
	None,\
	"Fix permission of bm-core.tok file")
	
	execCmd(LOG_FILE,\
	['cp', '-f', '/etc/bm/certs/bm_cert.pem', '/etc/ssl/certs/bm_cert.pem'],\
	None,\
	"Restoring BlueMind certificate file")

def restoreMailImap(bmCorePath, mailImapPath):
	for f in ['cyrus.conf', 'imapd.conf', 'cyrus-admins', 'cyrus-partitions']:
		execCmd(LOG_FILE,\
		['cp', '-f', bmCorePath + '/var/backups/bluemind/conf/etc/' + f, '/etc/' + f],\
		None,\
		"Restoring " + f + " file")
		
	execCmd(LOG_FILE, \
	['rsync',\
	 '-avH',\
	 mailImapPath + '/var/lib/cyrus/',
	 '/var/lib/cyrus/'],\
	None,\
	"Restoring Cyrus lib files")
	
	execCmd(LOG_FILE, \
	['rsync',\
	 '-avH',\
	 '--delete',\
	 mailImapPath + '/var/spool/cyrus/',
	 '/var/spool/cyrus/'],\
	None,\
	"Restoring Cyrus spool files")
	
	execCmd(LOG_FILE, \
	['chown',\
	 '-R',\
	 'cyrus:mail',\
	 '/var/lib/cyrus',
	 '/var/spool/cyrus/'],\
	None,\
	"Fixing Cyrus files rights")

def restoreBmEs(bmCorePath, bmEsPath):
	execCmd(LOG_FILE,\
	['cp', '-f', bmCorePath + '/var/backups/bluemind/conf/usr/share/bm-elasticsearch/config/elasticsearch.yml',\
	 '/usr/share/bm-elasticsearch/config/elasticsearch.yml'],\
	None,\
	"Restoring ES configuration")

	execCmd(LOG_FILE, \
	['rm',\
	 '-Rf',\
	 '/var/spool/bm-elasticsearch/repo'],\
	None,\
	"Cleanup ES repo")

	execCmd(LOG_FILE, \
	['rsync',\
	 '-r',\
	 bmCoreTagBackupPath + '/var/backups/bluemind/work/elasticsearch',\
	 '/var/spool/bm-elasticsearch'],\
	None,\
	"Copy backup to ES repo")

	execCmd(LOG_FILE, \
	['chown',\
	 '-R',\
	 'elasticsearch:elasticsearch',\
	 '/var/spool/bm-elasticsearch/repo'],\
	None,\
	"chown ES repo")
	
	execCmd(LOG_FILE, \
	['curl',\
	 '-X',\
	 'POST',\
	 'localhost:9200/_snapshot/bm-elasticsearch/snapshot-es/_restore'],\
	None,\
	"Restoring ES index files")

def restoreBmCore(bmCorePath):
	execCmd(LOG_FILE, \
	['rsync',\
	 '-avH',\
	 '--delete',\
	 bmCoreTagBackupPath + '/var/backups/bluemind/conf/etc/bm-hps/',
	 '/etc/bm-hps/'],\
	None,\
	"Restoring BlueMind HPS configuration")
	
	execCmd(LOG_FILE, \
	['rsync',\
	 '-avH',\
	 '--delete',\
	 bmCoreTagBackupPath + '/var/backups/bluemind/conf/etc/postfix/',
	 '/etc/postfix/'],\
	None,\
	"Restoring BlueMind Postfix configuration")

def restoreBmPgsql(bmPgsqlPath):
	execCmd(LOG_FILE, \
	['rsync',\
	 '-avH',\
	 '--delete',\
	 bmPgsqlPath + '/var/backups/bluemind/work/pgsql/configuration/11/',
	 '/etc/postgresql/11/'],\
	None,\
	"Restoring PostgreSQL configuration")
	
	execCmd(LOG_FILE, \
	['chown',\
	 '-R',\
	 'postgres:postgres',\
	 '/etc/postgresql/11/'],\
	None,\
	"Fixing PostgreSQL configuration rights")
	
	execCmd(LOG_FILE, \
	['service', 'postgresql',\
	 'restart'],\
	None,\
	"Restarting PostGreSQL")

	execCmd(LOG_FILE, \
	['su',\
	 '-c',\
	 'dropdb bj',\
	 'postgres'],\
	None,\
	"Dropping BlueMind database")

	execCmd(LOG_FILE, \
	['su',\
	 '-c',\
	 'createdb bj',\
	 'postgres'],\
	None,\
	"Creating empty BlueMind database")
	
	execCmd(LOG_FILE, \
	['psql', '-U', 'bj', '-h', '127.0.0.1', '-f',\
	 bmPgsqlPath + '/var/backups/bluemind/work/pgsql/dump.sql' ],\
	{ 'PGPASSWORD': 'bj' },\
	"Loading BlueMind database dump")
	
	p = subprocess.Popen("pw=$(echo \"select configuration -> 'sw_password' as sw_password from t_systemconf;\" | PGPASSWORD=bj psql -t -U bj -h 127.0.0.1 bj); echo $pw", shell=True, stdout=subprocess.PIPE)
	password = p.stdout.read()
	password = password.decode('utf-8').rstrip()
	execCmd(LOG_FILE, \
	['htpasswd', '-b', '-c', '/etc/nginx/sw.htpasswd', 'admin', password ],\
	None,\
	"Reset BlueMind SW password")

def restoreMailArchive(mailArchivePath):	
	if os.path.exists(mailArchivePath + '/var/spool/bm-hsm'):
		execCmd(LOG_FILE, \
		['rsync',\
		 '-avH',\
		 '--delete',\
		 mailArchivePath + '/var/spool/bm-hsm/',
		 '/var/spool/bm-hsm/'],\
		None,\
		"Restoring BlueMind HSM directory")

def restoreFilehostingData(filehostingDataPath):
	if os.path.exists(filehostingDataPath + '/var/spool/bm-filehosting'):
		execCmd(LOG_FILE, \
		['rsync',\
		 '-avH',\
		 '--delete',\
		 filehostingDataPath + '/var/spool/bm-filehosting/',
		 '/var/spool/bm-filehosting/'],\
		None,\
		"Restoring BlueMind filehosting directory")

LOG_FILE=sys.argv[0] + ".log"
f = open(LOG_FILE, 'w')
f.write("Starting PRA\n")
f.close()

BM_BACKUP_DIR="/var/backups/bluemind"
BM_BACKUP_RSYNC_ROOT=BM_BACKUP_DIR + "/dp_spool/rsync"

if not os.path.isdir(BM_BACKUP_DIR):
	sys.stderr.write(BM_BACKUP_DIR + " must exists!\n")
	sys.exit(1)

BM_BACKUP_HOST_ROOT=BM_BACKUP_RSYNC_ROOT + "/" + getHostIp(BM_BACKUP_RSYNC_ROOT)

generationToRestore = selectBackupGeneration(BM_BACKUP_DIR)

bmCoreTagBackupPath = getBmCoreTagBackupPath(BM_BACKUP_RSYNC_ROOT, generationToRestore)

restoreCommonConfiguration(bmCoreTagBackupPath)

for part in generationToRestore['parts']:
	tag = part['tag']
	pId = part['id']
	
	tagPath = BM_BACKUP_HOST_ROOT + "/" + tag + "/" + str(pId)
	
	if not os.path.isdir(tagPath):
		sys.stdout.write("This server is not tagget as " + tag + "\n")
		continue
	
	if tag == 'mail/imap':
		restoreMailImap(bmCoreTagBackupPath, tagPath)
	elif tag == 'bm/es':
		restoreBmEs(bmCoreTagBackupPath, tagPath)
	elif tag == 'bm/core':
		restoreBmCore(tagPath)
	elif tag == 'bm/pgsql':
		restoreBmPgsql(tagPath)
	elif tag == 'mail/archive':
		restoreMailArchive(tagPath)
	elif tag == 'filehosting/data':
		restoreFilehostingData(tagPath)
	else:
		print tagPath

execCmd(LOG_FILE,\
		['service', 'bm-node', 'start'],\
		None,\
		"Starting BlueMind Node service")

execCmd(LOG_FILE,\
		['bmctl', 'start'],\
		None,\
		"Starting BlueMind services")
