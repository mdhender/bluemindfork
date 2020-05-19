/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.system.config;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.NCUtils;
import net.bluemind.node.api.NodeActivator;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.system.api.SystemConf;
import net.bluemind.system.hook.ISystemConfigurationObserver;
import net.bluemind.tag.api.TagDescriptor;

public class NginxExternalUrlConfigurationObserver implements ISystemConfigurationObserver {

	private static Logger logger = LoggerFactory.getLogger(NginxExternalUrlConfigurationObserver.class);

	@Override
	public void onUpdated(BmContext context, SystemConf previous, SystemConf conf) throws ServerFault {
		if ((previous.stringValue("external-url") != null
				&& !previous.stringValue("external-url").equals(conf.stringValue("external-url")))
				|| (previous.stringValue("external-url") == null && conf.stringValue("external-url") != null)) {
			String eu = conf.stringValue("external-url");
			logger.info("System configuration has been updated, external-url changed to {}", eu);
			updateExternalUrl(context.su(), eu);
		}

		String defaultDomain = conf.values.get(SysConfKeys.default_domain.name());
		if ((defaultDomain != null && !defaultDomain.equals(previous.values.get(SysConfKeys.default_domain.name())))
				|| (defaultDomain == null
						&& !Strings.isNullOrEmpty(previous.values.get(SysConfKeys.default_domain.name())))) {
			logger.info("Reload NGinx authentication handler");
			VertxPlatform.eventBus().publish("bm.defaultdomain.changed", (Object) null);
		}
	}

	private void updateExternalUrl(BmContext context, String eu) {
		List<ItemValue<Server>> webservers = getTaggedServers(context, TagDescriptor.bm_nginx.getTag(),
				TagDescriptor.bm_nginx_edge.getTag());

		byte[] serverName = NginxService.serverNameContent(eu);
		byte[] conf = NginxService.externalUrlContent(eu);
		logger.info("Distributing new settings to {} servers", webservers.size());
		for (ItemValue<Server> webmail : webservers) {
			logger.info("Distributing new settings to {}:{}", webmail.value.name, webmail.value.ip);
			INodeClient nc = NodeActivator.get(webmail.value.address());

			nc.writeFile("/etc/nginx/bm-servername.conf", new ByteArrayInputStream(serverName));
			nc.writeFile("/etc/nginx/bm-externalurl.conf", new ByteArrayInputStream(conf));
			reloadHttpd(nc);
		}

	}

	private void reloadHttpd(INodeClient nc) throws ServerFault {
		NCUtils.forget(nc, "service bm-php-fpm reload");
		NCUtils.forget(nc, "service bm-nginx reload");
	}

	List<ItemValue<Server>> getTaggedServers(BmContext context, String... tag) throws ServerFault {
		IServer serverService = context.provider().instance(IServer.class, "default");

		List<ItemValue<Server>> all = serverService.allComplete();
		List<ItemValue<Server>> ret = new ArrayList<>();
		for (ItemValue<Server> server : all) {
			for (int i = 0; i < tag.length; i++) {
				if (server.value.tags.contains(tag[i])) {
					ret.add(server);
				}
			}
		}
		return ret;
	}

}
