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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.NCUtils;
import net.bluemind.node.api.NodeActivator;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;
import net.bluemind.system.api.GlobalSettingsKeys;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.system.api.SystemConf;
import net.bluemind.system.config.NginxConfig.NginxConfigBuilder;
import net.bluemind.system.hook.ISystemConfigurationObserver;
import net.bluemind.tag.api.TagDescriptor;

public class ConfigHook implements ISystemConfigurationObserver {

	private static Logger logger = LoggerFactory.getLogger(ConfigHook.class);

	List<INginxConfigUpdater> searchUpdaters() throws ServerFault {
		List<INginxConfigUpdater> updaters = new ArrayList<>();
		IExtensionPoint point = Platform.getExtensionRegistry().getExtensionPoint("net.bluemind.nginx.update");
		if (point == null) {
			throw new ServerFault("point net.bluemind.nginx.update not found");
		}
		IExtension[] extensions = point.getExtensions();

		for (IExtension ie : extensions) {
			for (IConfigurationElement e : ie.getConfigurationElements()) {
				if (e.getName().equals("updater")) {
					INginxConfigUpdater updater = null;
					try {
						updater = (INginxConfigUpdater) e.createExecutableExtension("impl");
					} catch (CoreException e1) {
						throw new ServerFault(e1);
					}
					updaters.add(updater);
				}
			}
		}

		return updaters;
	}

	@Override
	public void onUpdated(BmContext context, SystemConf previous, SystemConf conf) throws ServerFault {
		logger.info("System configuration has been updated");
		MessageSizeValue messageSizeLimit = MessageSizeValue.getMessageSizeLimit(SysConfKeys.message_size_limit.name(),
				previous, conf);

		MessageSizeValue dataSizeLimit = MessageSizeValue
				.getMessageSizeLimit(GlobalSettingsKeys.filehosting_max_filesize.name(), previous, conf);

		if (valueNotChanged(messageSizeLimit) && valueNotChanged(dataSizeLimit)) {
			logger.debug("Message size limit has not changed or is not set");
			return;
		}
		logger.info("Message size limit has changed to {}", messageSizeLimit.newValue);

		List<INginxConfigUpdater> updaters = searchUpdaters();

		logger.info("Found {} nginx config updaters", updaters.size());

		List<ItemValue<Server>> webmails = getTaggedServers(context, TagDescriptor.bm_webmail.getTag());

		NginxConfig config = NginxConfigBuilder.init("messageSizeLimit", "" + messageSizeLimit.newValue) //
				.add("dataSizeLimit", "" + dataSizeLimit.newValue).build();

		logger.info("Distributing new settings to {} servers", webmails.size());
		for (ItemValue<Server> webmail : webmails) {
			logger.info("Distributing new settings to {}:{}", webmail.value.name, webmail.value.ip);
			INodeClient nc = NodeActivator.get(webmail.value.address());

			for (INginxConfigUpdater updater : updaters) {
				try {
					updater.update(nc, config);
				} catch (Exception e) {
					logger.warn("Cannot update nginx config on server {}:{}", webmail.value.address(), e.getMessage());
					throw new ServerFault(e);
				}
			}

			reloadHttpd(nc);
		}

	}

	private boolean valueNotChanged(MessageSizeValue messageSizeLimit) {
		return !messageSizeLimit.isSet() || !messageSizeLimit.hasChanged();
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
