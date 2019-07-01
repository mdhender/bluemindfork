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
package net.bluemind.system.nginx;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import net.bluemind.config.InstallationId;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.NCUtils;
import net.bluemind.node.api.NodeActivator;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.system.api.SystemConf;
import net.bluemind.system.hook.ISystemConfigurationObserver;
import net.bluemind.system.hook.ISystemHook;

public class NginxSystemHook implements ISystemHook, ISystemConfigurationObserver {

	private static final Logger logger = LoggerFactory.getLogger(NginxSystemHook.class);

	private final Configuration cfg;

	public NginxSystemHook() {
		cfg = new Configuration();
		cfg.setClassForTemplateLoading(getClass(), "/templates");
	}

	@Override
	public void onCertificateUpdate() throws ServerFault {
		forEachNginx(new NginxAction() {

			@Override
			public void run(ItemValue<Server> s) throws ServerFault {
				logger.info("reloading nginx : {}", s.value.address());
				INodeClient nc = NodeActivator.get(s.value.address());
				NCUtils.exec(nc, "service bm-nginx reload");
			}
		});
	}

	private static interface NginxAction {
		void run(ItemValue<Server> s) throws ServerFault;
	}

	private void forEachNginx(NginxAction action) throws ServerFault {
		IServer srvApi = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IServer.class,
				InstallationId.getIdentifier());
		List<ItemValue<Server>> hosts = srvApi.allComplete();
		for (ItemValue<Server> h : hosts) {
			if (h.value == null) {
				logger.warn("no server value for {}", h.uid);
				continue;
			}
			Server s = h.value;
			HashSet<String> allTags = Sets.newHashSet(s.tags);
			if (allTags.contains("bm/nginx")) {
				action.run(h);
			}
		}
	}

	@Override
	public void onUpdated(BmContext context, SystemConf previous, SystemConf conf) throws ServerFault {

		String pwd = conf.values.get(SysConfKeys.sw_password.name());
		String workerConnections = conf.values.get(SysConfKeys.nginx_worker_connections.name());
		String oldWorkerConnections = previous.values.get(SysConfKeys.nginx_worker_connections.name());

		boolean changePwd = pwd != null && !pwd.trim().isEmpty();
		boolean changeWorderConnections = workerConnections != null && !workerConnections.trim().isEmpty()
				&& !workerConnections.equals(oldWorkerConnections);

		if (!changePwd && !changeWorderConnections) {
			return;
		}

		StringWriter sw = new StringWriter();
		if (changeWorderConnections) {
			try {
				Map<String, Object> data = new HashMap<>();
				data.put("worker_connections", workerConnections);
				Template template = cfg.getTemplate("events.ftl");
				template.process(data, sw);
			} catch (TemplateException | IOException e) {
				throw new ServerFault(e);
			}
		}

		forEachNginx(s -> {
			INodeClient nc = NodeActivator.get(s.value.address());

			if (changePwd) {
				logger.info("update htpasswd on {}", s.value.address());
				NCUtils.exec(nc, "/usr/bin/htpasswd -bc /etc/nginx/sw.htpasswd admin '" + pwd + "'");
			}

			if (changeWorderConnections) {
				logger.info("update worker_connections on {}", s.value.address());
				nc.writeFile("/etc/nginx/global.d/events.conf", new ByteArrayInputStream(sw.toString().getBytes()));
				NCUtils.exec(nc, "service bm-nginx reload");
			}

		});

	}

}
