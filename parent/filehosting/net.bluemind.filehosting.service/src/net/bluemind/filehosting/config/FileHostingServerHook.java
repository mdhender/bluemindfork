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
package net.bluemind.filehosting.config;

import java.util.Map;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.domain.api.Domain;
import net.bluemind.filehosting.service.internal.FileHostingRolesVerifier;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.NCUtils;
import net.bluemind.node.client.AHCNodeClientFactory;
import net.bluemind.server.api.Server;
import net.bluemind.server.api.TagDescriptor;
import net.bluemind.server.hook.DefaultServerHook;
import net.bluemind.system.api.GlobalSettingsKeys;
import net.bluemind.system.api.IGlobalSettings;

public class FileHostingServerHook extends DefaultServerHook {

	private AHCNodeClientFactory ncr = new AHCNodeClientFactory();

	@Override
	public void onServerTagged(BmContext context, ItemValue<Server> server, String tag) throws ServerFault {
		if (tag.endsWith("filehosting/data")) {
			FileHostingRolesVerifier.reset();
		}

		if (!isWebmailTag(tag)) {
			return;
		}

		INodeClient nc = ncr.create(server.value.address());

		NCUtils.execNoOut(nc, "/bin/mkdir -p /etc/bm-webmail");

		NginxFileHostingConf webmailFilehostingConf = new NginxFileHostingConf(nc);

		IGlobalSettings settingsService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IGlobalSettings.class);
		Map<String, String> map = settingsService.get();

		String defaultValue = String.valueOf(100);
		String maxDataSize = map.getOrDefault(GlobalSettingsKeys.filehosting_max_filesize, defaultValue);

		webmailFilehostingConf.setMessageSizeLimit(Integer.parseInt(maxDataSize));
		webmailFilehostingConf.write();

		reloadHttpd(nc);

	}

	@Override
	public void onServerUntagged(BmContext context, ItemValue<Server> itemValue, String tag) throws ServerFault {
		if (tag.endsWith("filehosting/data")) {
			FileHostingRolesVerifier.reset();
		}
	}

	@Override
	public void onServerUnassigned(BmContext context, ItemValue<Server> itemValue, ItemValue<Domain> domain, String tag)
			throws ServerFault {
		if (tag.endsWith("filehosting/data")) {
			FileHostingRolesVerifier.reset();
		}
	}

	private boolean isWebmailTag(String tag) {
		return tag.equals(TagDescriptor.bm_webmail.getTag()) || tag.equals(TagDescriptor.bm_nginx_edge.getTag());
	}

	private void reloadHttpd(INodeClient nc) throws ServerFault {
		NCUtils.forget(nc, "service bm-nginx reload");
	}

}
