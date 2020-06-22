/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2018
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.metrics.core;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import net.bluemind.config.InstallationId;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.metrics.core.tick.TickInputConfigurator;
import net.bluemind.network.utils.NetworkHelper;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;

public class InfluxTagHandler extends TickInputConfigurator {

	private static final Logger logger = LoggerFactory.getLogger(InfluxTagHandler.class);

	@Override
	public void onServerTagged(BmContext context, ItemValue<Server> itemValue, String tag) throws ServerFault {
		if (!tag.equals("metrics/influxdb")) {
			return;
		}
		logger.info("Tagging {}", itemValue.value.address());

		IServer serverApi = context.provider().instance(IServer.class, InstallationId.getIdentifier());
		try {
			TagHelper.jarToFS(InfluxTagHandler.class, "/configs/bm-influxdb.conf",
					"/etc/telegraf/telegraf.d/bm-influxdb.conf", itemValue, serverApi);
			TagHelper.jarToFS(InfluxTagHandler.class, "/configs/bm-kapacitor.conf",
					"/etc/telegraf/telegraf.d/bm-kapacitor.conf", itemValue, serverApi);
		} catch (IOException e) {
			logger.error("Error copying file : {}", e);
			return;
		}
		serverApi.submitAndWait(itemValue.uid, "service influxdb restart");
		new NetworkHelper(itemValue.value.address()).waitForListeningPort(8086, 1, TimeUnit.MINUTES);

		serverApi.submitAndWait(itemValue.uid,
				"/usr/bin/influx -execute 'alter retention policy autogen on telegraf duration 30d;'");
		List<ItemValue<Server>> allServers = serverApi.allComplete();
		for (ItemValue<Server> srvItem : allServers) {
			try {
				Configuration cfg = new Configuration();
				cfg.setTemplateLoader(new ClassTemplateLoader(InfluxTagHandler.class, "/templates/"));
				Template temp = cfg.getTemplate("output.ftl");
				StringWriter out = new StringWriter();
				Map<String, String> map = new HashMap<String, String>();
				map.put("influxdbip", itemValue.value.address() + ":8086");
				map.put("hostAddress", srvItem.value.address());
				map.put("uid", srvItem.uid);
				temp.process(map, out);
				serverApi.writeFile(srvItem.uid, "/etc/telegraf/telegraf.d/output.conf", out.toString().getBytes());
				serverApi.submitAndWait(srvItem.uid, "service telegraf restart");
			} catch (IOException e1) {
				logger.error("Can't open ftl template : {}", e1);
			} catch (TemplateException e2) {
				logger.error("Exception during template processing : {}", e2);
			}
		}
		monitor.ifPresent(mon -> mon.log("TICK for " + tag + " configured on " + itemValue.value.address()));

	}

	@Override
	public void onServerUntagged(BmContext context, ItemValue<Server> itemValue, String tag) throws ServerFault {
		if (!tag.equals("metrics/influxdb")) {
			return;
		}

		logger.info("Untagging {}", itemValue.value.address());

		TagHelper.deleteRemote(itemValue.value.address(), "/etc/telegraf/telegraf.d/bm-influxdb.conf");
		logger.info("Deleted file /etc/telegraf/telegraf.d/bm-influxdb.conf at " + itemValue.value.address());
		TagHelper.deleteRemote(itemValue.value.address(), "/etc/telegraf/telegraf.d/bm-kapacitor.conf");
		logger.info("Deleted file /etc/telegraf/telegraf.d/bm-kapacitor.conf at " + itemValue.value.address());

		IServer serverApi = context.provider().instance(IServer.class, InstallationId.getIdentifier());
		List<ItemValue<Server>> allServers = serverApi.allComplete();
		for (ItemValue<Server> srvItem : allServers) {
			try {
				Configuration cfg = new Configuration();
				cfg.setTemplateLoader(new ClassTemplateLoader(InfluxTagHandler.class, "/templates/"));
				Template temp = cfg.getTemplate("output-influxdb_local.ftl");
				StringWriter out = new StringWriter();
				Map<String, String> map = new HashMap<String, String>();
				map.put("hostAddress", srvItem.value.address());
				map.put("uid", srvItem.uid);
				temp.process(map, out);
				serverApi.writeFile(srvItem.uid, "/etc/telegraf/telegraf.d/output.conf", out.toString().getBytes());
				serverApi.submitAndWait(srvItem.uid, "service telegraf restart");
			} catch (IOException e1) {
				logger.error("Can't open ftl template : {}", e1);
			} catch (TemplateException e2) {
				logger.error("Exception during template processing : {}", e2);
			}
		}
	}
}
