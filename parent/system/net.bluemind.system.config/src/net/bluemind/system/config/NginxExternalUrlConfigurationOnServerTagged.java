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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.NCUtils;
import net.bluemind.node.api.NodeActivator;
import net.bluemind.server.api.Server;
import net.bluemind.server.hook.DefaultServerHook;
import net.bluemind.system.api.ISystemConfiguration;
import net.bluemind.tag.api.TagDescriptor;

public class NginxExternalUrlConfigurationOnServerTagged extends DefaultServerHook {

	private static Logger logger = LoggerFactory.getLogger(NginxExternalUrlConfigurationOnServerTagged.class);

	private void reloadHttpd(INodeClient nc) throws ServerFault {
		NCUtils.forget(nc, "service bm-php-fpm reload");
		NCUtils.forget(nc, "service bm-nginx reload");
	}

	@Override
	public void onServerTagged(BmContext context, ItemValue<Server> itemValue, String tag) throws ServerFault {
		if (!tag.equals(TagDescriptor.bm_nginx.getTag()) && !tag.equals(TagDescriptor.bm_nginx_edge.getTag())) {
			return;
		}

		logger.info("Server tagged as {}, deploy external url", tag);
		String url = context.su().provider().instance(ISystemConfiguration.class).getValues()
				.stringValue("external-url");
		if (url != null) {
			INodeClient nc = NodeActivator.get(itemValue.value.address());

			byte[] serverName = NginxService.serverNameContent(url);
			nc.writeFile("/etc/nginx/bm-servername.conf", new ByteArrayInputStream(serverName));

			byte[] conf = NginxService.externalUrlContent(url);
			nc.writeFile("/etc/nginx/bm-externalurl.conf", new ByteArrayInputStream(conf));

			reloadHttpd(nc);
		}
	}
}
