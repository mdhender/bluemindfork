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

public class PhpfpmTagHandler extends TickInputConfigurator {
	private static final Logger logger = LoggerFactory.getLogger(PhpfpmTagHandler.class);

	@Override
	public void onServerTagged(BmContext context, ItemValue<Server> itemValue, String tag) throws ServerFault {
		if (!tag.equals("bm/webmail")) {
			return;
		}
		try {
			TagHelper.jarToFS(getClass(), "/configs/bm-phpfpm.conf", "/etc/telegraf/telegraf.d/bm-phpfpm.conf",
					itemValue, context.provider().instance(IServer.class, InstallationId.getIdentifier()));
		} catch (IOException e) {
			logger.error("Error copying file : {}", e.toString());
		}
		TagHelper.reloadTelegraf(itemValue.value.address());
		monitor.ifPresent(mon -> mon.log("Telegraf input for " + tag + " configured on " + itemValue.value.address()));
	}

	@Override
	public void onServerUntagged(BmContext context, ItemValue<Server> itemValue, String tag) throws ServerFault {
		if (!tag.equals("bm/webmail")) {
			return;
		}
		TagHelper.deleteRemote(itemValue.value.address(), "/etc/telegraf/telegraf.d/bm-phpfpm.conf");
		logger.info("Deleted file /etc/telegraf/telegraf.d/bm-phpfpm.conf at {}", itemValue.value.address());
		TagHelper.reloadTelegraf(itemValue.value.address());
	}
}
