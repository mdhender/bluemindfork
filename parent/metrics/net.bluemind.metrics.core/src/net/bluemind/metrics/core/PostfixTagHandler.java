package net.bluemind.metrics.core;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.config.InstallationId;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.metrics.core.tick.TickInputConfigurator;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.NCUtils;
import net.bluemind.node.api.NodeActivator;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;

public class PostfixTagHandler extends TickInputConfigurator {
	private static final Logger logger = LoggerFactory.getLogger(PostfixTagHandler.class);
	private static final String UNIX_ACL[] = {
			"usermod -a -G postdrop telegraf",
			"chgrp -R postdrop /var/spool/postfix/active",
			"chgrp -R postdrop /var/spool/postfix/hold",
			"chgrp -R postdrop /var/spool/postfix/incoming",
			"chgrp -R postdrop /var/spool/postfix/deferred",
			"chmod -R g+rXs /var/spool/postfix/active",
			"chmod -R g+rXs /var/spool/postfix/hold",
			"chmod -R g+rXs /var/spool/postfix/incoming",
			"chmod -R g+rXs /var/spool/postfix/deferred",
			"chmod g+r /var/spool/postfix/maildrop" };

	@Override
	public void onServerTagged(BmContext context, ItemValue<Server> itemValue, String tag) throws ServerFault {
		if (!tag.equals("mail/smtp")) {
			return;
		}

		INodeClient nodeClient = NodeActivator.get(itemValue.value.address());
		for (String str : UNIX_ACL) {
			NCUtils.execNoOut(nodeClient, str);
		}
		logger.info("Added postfix monitoring necessary rights at " + itemValue.value.address());

		try {
			TagHelper.jarToFS(getClass(), "/configs/bm-postfix.conf", "/etc/telegraf/telegraf.d/bm-postfix.conf",
					itemValue, context.provider().instance(IServer.class, InstallationId.getIdentifier()));
		} catch (IOException e) {
			logger.error("Error copying file : {}", e);
			return;
		}
		TagHelper.reloadTelegraf(itemValue.value.address());
		monitor.ifPresent(mon -> mon.log("Telegraf input for " + tag + " configured on " + itemValue.value.address()));
	}

	@Override
	public void onServerUntagged(BmContext context, ItemValue<Server> itemValue, String tag) throws ServerFault {
		if (!tag.equals("mail/smtp")) {
			return;
		}
		TagHelper.deleteRemote(itemValue.value.address(), "/etc/telegraf/telegraf.d/bm-postfix.conf");
		TagHelper.reloadTelegraf(itemValue.value.address());
	}
}
