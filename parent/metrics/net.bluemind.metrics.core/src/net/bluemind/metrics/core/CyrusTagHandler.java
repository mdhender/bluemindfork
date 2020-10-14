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

public class CyrusTagHandler extends TickInputConfigurator {
	private static final Logger logger = LoggerFactory.getLogger(CyrusTagHandler.class);

	@Override
	public void onServerTagged(BmContext context, ItemValue<Server> itemValue, String tag) throws ServerFault {
		if (!tag.equals("mail/imap")) {
			return;
		}
		try {
			TagHelper.jarToFS(getClass(), "/configs/bm-imapd.conf", "/etc/telegraf/telegraf.d/bm-imapd.conf", itemValue,
					context.provider().instance(IServer.class, InstallationId.getIdentifier()));
		} catch (IOException e) {
			logger.error("Error copying file : {}", e.toString());
			return;
		}

		TagHelper.reloadTelegraf(itemValue.value.address());
		monitor.ifPresent(mon -> mon.log("Telegraf input for " + tag + " configured on " + itemValue.value.address()));
	}

	@Override
	public void onServerUntagged(BmContext context, ItemValue<Server> itemValue, String tag) throws ServerFault {
		if (!tag.equals("mail/imap")) {
			return;
		}
		TagHelper.deleteRemote(itemValue.value.address(), "/etc/telegraf/telegraf.d/bm-imapd.conf");
		TagHelper.reloadTelegraf(itemValue.value.address());
	}
}