package net.bluemind.metrics.core;

import java.util.Optional;
import java.util.Set;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.config.InstallationId;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.server.api.CommandStatus;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;
import net.bluemind.system.schemaupgrader.IVersionedUpdater;
import net.bluemind.system.schemaupgrader.UpdateAction;
import net.bluemind.system.schemaupgrader.UpdateResult;

/*
 * Drop metrics when changing data format from dropwizard to influx
 * to avoid conflicts
 */

public class UpgradeDataFormat implements IVersionedUpdater {
	private static final Logger logger = LoggerFactory.getLogger(UpgradeDataFormat.class);
	private static final String REMOVE_OLD_ALERTS = "kapacitor delete tasks bm-core-hprof bm-eas-hprof bm-elasticsearch-hprof "
			+ "bm-hps-hprof bm-ips-hprof bm-lmtpd-hprof bm-locator-hprof bm-milter-hprof bm-node-hprof bm-tika-hprof "
			+ "bm-webserver-hprof bm-xmpp-hprof bm-ysnp-hprof imap-connections memcached-evictions postfix-queue-size";

	@Override
	public UpdateResult executeUpdate(IServerTaskMonitor monitor, DataSource pool, Set<UpdateAction> handledActions) {
		ServerSideServiceProvider prov = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);

		IServer serversApi = prov.instance(IServer.class, InstallationId.getIdentifier());

		Optional<ItemValue<Server>> influx = serversApi.allComplete().stream()
				.filter(iv -> iv.value.tags.contains("metrics/influxdb")).findFirst();
		if (!influx.isPresent()) {
			logger.error("No metrics server detected");
		} else {
			ItemValue<Server> influxSrv = influx.get();
			CommandStatus status = serversApi.submitAndWait(influxSrv.uid,
					"/usr/bin/influx -database 'telegraf' -execute 'drop series from /.*/'");
			logger.info("Dropped metrics from older format {}", status.output);
			status = serversApi.submitAndWait(influxSrv.uid, REMOVE_OLD_ALERTS);
			logger.info("Dropped old alerts {}", status.output);
		}
		return UpdateResult.ok();
	}

	@Override
	public int major() {
		return 4;
	}

	@Override
	public int buildNumber() {
		return 32705;
	}

	@Override
	public boolean afterSchemaUpgrade() {
		return true;
	}
}
