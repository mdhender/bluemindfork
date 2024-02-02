package net.bluemind.core.auditlogs.client.kafka;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.auditlogs.IAuditLogClient;
import net.bluemind.core.auditlogs.IAuditLogFactory;
import net.bluemind.core.auditlogs.IAuditLogMgmt;
import net.bluemind.core.auditlogs.IItemChangeLogClient;

public class KafkaAuditLogFactory implements IAuditLogFactory {

	private static final Logger logger = LoggerFactory.getLogger(KafkaAuditLogFactory.class);

	private final String bootstrap;

	public KafkaAuditLogFactory() {
		this.bootstrap = kafkaBootstrapServers();
	}

	@Override
	public int priority() {
		if (bootstrap == null) {
			return 0;
		}
		return Integer.MAX_VALUE;
	}

	@Override
	public IAuditLogClient createClient() {
		return new KafkaAuditLogClient(bootstrap, new KafkaAuditLogMngt(bootstrap));
	}

	@Override
	public IItemChangeLogClient createItemChangelogClient() {
		return new KafkaAuditLogItemChangeLog(bootstrap);
	}

	@Override
	public IAuditLogMgmt createManager() {
		return new KafkaAuditLogMngt(bootstrap);
	}

	private String kafkaBootstrapServers() {
		String loadingBootServers = System.getProperty("bm.kafka.bootstrap.servers");
		if (loadingBootServers == null) {
			File local = new File("/etc/bm/kafka.properties");
			if (!local.exists()) {
				local = new File(System.getProperty("user.home") + "/kafka.properties");
			}
			if (local.exists()) {
				Properties tmp = new Properties();
				try (InputStream in = Files.newInputStream(local.toPath())) {
					tmp.load(in);
				} catch (Exception e) {
					logger.warn(e.getMessage());
				}
				loadingBootServers = tmp.getProperty("bootstrap.servers");
			}
		}
		return loadingBootServers;

	}

}
