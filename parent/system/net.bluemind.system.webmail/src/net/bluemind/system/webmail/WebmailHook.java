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
package net.bluemind.system.webmail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.NCUtils;
import net.bluemind.node.client.AHCNodeClientFactory;
import net.bluemind.server.api.Server;
import net.bluemind.server.hook.DefaultServerHook;
import net.bluemind.system.api.ISystemConfiguration;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.system.config.MessageSizeValue;
import net.bluemind.tag.api.TagDescriptor;

public class WebmailHook extends DefaultServerHook {

	private static final Logger logger = LoggerFactory.getLogger(WebmailHook.class);

	private static final int DEFAULT_MESSAGE_SIZE_LIMIT = 200 * 1024 * 1024;

	private AHCNodeClientFactory ncr = new AHCNodeClientFactory();

	@Override
	public void onServerTagged(BmContext context, ItemValue<Server> server, String tag) throws ServerFault {

		if (!isWebmailTag(tag)) {
			return;
		}

		INodeClient nc = ncr.create(server.value.address());

		NCUtils.execNoOut(nc, "/bin/mkdir -p /etc/bm-webmail");

		logger.info("host " + server.value.address() + " tagged as " + tag + ". (Over)writing main & master.cf");

		setPhpFpmMessageSizeLimit(context, nc);

		reloadHttpd(nc);
	}

	private boolean isWebmailTag(String tag) {
		return tag.equals(TagDescriptor.bm_webmail.getTag()) || tag.equals(TagDescriptor.bm_nginx_edge.getTag());
	}

	private void setPhpFpmMessageSizeLimit(BmContext context, INodeClient nc) throws ServerFault {

		ISystemConfiguration sconf = context.provider().instance(ISystemConfiguration.class);
		MessageSizeValue messageSizeLimit = MessageSizeValue.getMessageSizeLimit(SysConfKeys.message_size_limit.name(),
				sconf.getValues());

		if (!messageSizeLimit.isSet()) {
			logger.warn("message_size_limit not defined, default value {}", DEFAULT_MESSAGE_SIZE_LIMIT);
			messageSizeLimit = MessageSizeValue.create(DEFAULT_MESSAGE_SIZE_LIMIT);
		}

		try {
			new WebmailConfigUpdater().updateMessageSize(nc, messageSizeLimit.newValue);
		} catch (Exception e) {
			throw new ServerFault(e);
		}
	}

	private void reloadHttpd(INodeClient nc) throws ServerFault {
		NCUtils.forget(nc, "service bm-php-fpm reload");

		NCUtils.forget(nc, "service bm-nginx reload");
	}

}
