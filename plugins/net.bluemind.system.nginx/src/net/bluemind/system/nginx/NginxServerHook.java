/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
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
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import net.bluemind.server.api.TagDescriptor;
import net.bluemind.server.hook.DefaultServerHook;

public class NginxServerHook extends DefaultServerHook {
	private static Logger logger = LoggerFactory.getLogger(NginxServerHook.class);

	@Override
	public void onServerTagged(BmContext context, ItemValue<Server> server, String tag) throws ServerFault {
		if (TagDescriptor.bm_nginx.getTag().equals(tag)) {
			newNginx(server);
		}

		if (TagDescriptor.bm_nginx_edge.getTag().equals(tag)) {
			newEdge(server);
		}

		if (tag.equals(TagDescriptor.bm_nginx.getTag()) || tag.equals(TagDescriptor.bm_nginx_edge.getTag())) {
			new NginxService().restart(NodeActivator.get(server.value.address()));
		}
	}

	private void newNginx(ItemValue<Server> server) {
		NginxService nginxService = new NginxService();

		ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IServer.class, InstallationId.getIdentifier()).allComplete().stream()
				.filter(s -> s.value.tags.contains("metrics/influxdb")).forEach(metricsInfluxServer -> nginxService
						.updateTickUpstream(server.value.address(), metricsInfluxServer.value.address()));

		nginxService.reloadHttpd(NodeActivator.get(server.value.address()));
	}

	private void newEdge(ItemValue<Server> server) {
		// BM-10505
		INodeClient remote = NodeActivator.get(server.value.address());

		if (!remote.listFiles("/etc/nginx/BM-DONOTCONF").isEmpty()) {
			NCUtils.exec(remote, "rm -f /etc/nginx/BM-DONOTCONF");
			remote.listFiles("/etc/nginx/sites-enabled").stream().filter(fd -> !fd.isDirectory())
					.forEach(fd -> NCUtils.exec(remote, String.format("rm -f %s", fd.getPath())));
		}

		remote.writeFile("/etc/nginx/bm-nginx-role.conf",
				new ByteArrayInputStream("set $bm_nginx_role edge;".getBytes()));

		Optional<ItemValue<Server>> nginxServer = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IServer.class, InstallationId.getIdentifier()).allComplete().stream()
				.filter(s -> s.value.tags.contains(TagDescriptor.bm_nginx.getTag())).findFirst();
		if (nginxServer.isPresent()) {
			remote.writeFile("/etc/nginx/bm-upstream-mainnginx.conf", new ByteArrayInputStream(
					String.format("server %s:443;", nginxServer.get().value.address()).getBytes()));

			remote.writeFile("/etc/nginx/bm-http-auth.conf", new ByteArrayInputStream(
					String.format("auth_http %s:8090/nginx;", nginxServer.get().value.address()).getBytes()));

			// Deploy dynamic Nginx configuration
			INodeClient nginxServerNC = NodeActivator.get(nginxServer.get().value.address());
			remote.writeFile("/etc/nginx/sw.htpasswd", nginxServerNC.openStream("/etc/nginx/sw.htpasswd"));
			remote.writeFile("/etc/nginx/global.d/events.conf",
					nginxServerNC.openStream("/etc/nginx/global.d/events.conf"));

			remote.writeFile("/etc/bm-eas/bm-eas-nginx.conf",
					nginxServerNC.openStream("/etc/bm-eas/bm-eas-nginx.conf"));
		} else {
			throw new ServerFault(String.format("No host tag %s found!", TagDescriptor.bm_nginx.getTag()));
		}

		new NginxService().reloadHttpd(remote);
	}
}
