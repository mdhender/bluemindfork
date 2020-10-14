package net.bluemind.metrics.core;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import net.bluemind.config.InstallationId;
import net.bluemind.config.Token;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.metrics.core.tick.TickInputConfigurator;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;

public class OpenldapTagHandler extends TickInputConfigurator {
	private static final Logger logger = LoggerFactory.getLogger(OpenldapTagHandler.class);
	public static final String LDAPTAG = "directory/bm-master";

	@Override
	public void onServerTagged(BmContext context, ItemValue<Server> itemValue, String tag) throws ServerFault {
		if (!tag.equals(LDAPTAG)) {
			return;
		}

		IServer serverApi = context.provider().instance(IServer.class, InstallationId.getIdentifier());

		String passwd = Token.admin0();

		Configuration cfg = new Configuration();
		cfg.setTemplateLoader(new ClassTemplateLoader(InfluxTagHandler.class, "/templates/"));
		try {
			Template temp = cfg.getTemplate("bm-openldap.ftl");
			StringWriter out = new StringWriter();
			Map<String, String> map = new HashMap<String, String>();
			map.put("password", passwd);
			temp.process(map, out);
			serverApi.writeFile(itemValue.uid, "/etc/telegraf/telegraf.d/bm-openldap.conf", out.toString().getBytes());
		} catch (IOException e1) {
			logger.error("Can't open ftl template : {}", e1.toString());
			return;
		} catch (TemplateException e2) {
			logger.error("Exception during template processing : {}", e2.toString());
			return;
		}
		TagHelper.reloadTelegraf(itemValue.value.address());
		monitor.ifPresent(mon -> mon.log("Telegraf input for " + tag + " configured on " + itemValue.value.address()));
	}

	@Override
	public void onServerUntagged(BmContext context, ItemValue<Server> itemValue, String tag) throws ServerFault {
		if (!tag.equals(LDAPTAG)) {
			return;
		}
		TagHelper.deleteRemote(itemValue.value.address(), "/etc/telegraf/telegraf.d/bm-openldap.conf");
		logger.info("Deleted file /etc/telegraf/telegraf.d/bm-openldap.conf at {}", itemValue.value.address());
		TagHelper.reloadTelegraf(itemValue.value.address());
	}
}
