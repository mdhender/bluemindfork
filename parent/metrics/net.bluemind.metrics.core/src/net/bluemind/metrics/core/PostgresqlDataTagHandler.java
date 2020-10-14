package net.bluemind.metrics.core;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.config.InstallationId;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.metrics.core.tick.TickInputConfigurator;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;

public class PostgresqlDataTagHandler extends TickInputConfigurator {
	private static final Logger logger = LoggerFactory.getLogger(PostgresqlDataTagHandler.class);

	private final String[] telegrafConfigs = {
		"bm-postgres-data.conf",
		"bm-postgres-size-data.conf",
		"bm-postgres-statio-data.conf"
	};

	@Override
	public void onServerTagged(BmContext context, ItemValue<Server> server, String tag) throws ServerFault {
		if (!tag.equals("bm/pgsql-data")) {
			return;
		}
		IServer serverApi = context.provider().instance(IServer.class, InstallationId.getIdentifier());
		for (String telegrafConfig: telegrafConfigs) {
			try {
				TagHelper.jarToFS(getClass(), "/configs/" + telegrafConfig,
						"/etc/telegraf/telegraf.d/" + telegrafConfig, server, serverApi);
			} catch (IOException e) {
				logger.error("Error copying file : {}", telegrafConfig, e);
			}
		}

		TagHelper.reloadTelegraf(server.value.address());
		monitor.ifPresent(mon -> mon.log("Telegraf input for " + tag + " configured on " + server.value.address()));
	}

	@Override
	public void onServerUntagged(BmContext context, ItemValue<Server> server, String tag) throws ServerFault {
		if (!tag.equals("bm/pgsql-data")) {
			return;
		}
		for (String telegrafConfig: telegrafConfigs) {
			TagHelper.deleteRemote(server.value.address(), "/etc/telegraf/telegraf.d/" + telegrafConfig);
		}
		TagHelper.reloadTelegraf(server.value.address());
	}
}