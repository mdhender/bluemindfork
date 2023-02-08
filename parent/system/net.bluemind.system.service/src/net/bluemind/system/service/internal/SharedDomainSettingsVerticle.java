/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
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
package net.bluemind.system.service.internal;

import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Verticle;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.DomainSettingsKeys;
import net.bluemind.domain.api.IDomainSettings;
import net.bluemind.domain.api.IDomains;
import net.bluemind.domain.hook.DomainHookAdapter;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.hornetq.client.MQ.SharedMap;
import net.bluemind.hornetq.client.Shared;
import net.bluemind.lib.vertx.IUniqueVerticleFactory;
import net.bluemind.lib.vertx.IVerticleFactory;

public class SharedDomainSettingsVerticle extends AbstractVerticle {

	public static class Init implements IVerticleFactory, IUniqueVerticleFactory {
		@Override
		public boolean isWorker() {
			return true;
		}

		@Override
		public Verticle newInstance() {
			return new SharedDomainSettingsVerticle();
		}
	}

	public class SharedDomainSettingsDomainHook extends DomainHookAdapter {

		@Override
		public void onSettingsUpdated(BmContext context, ItemValue<Domain> domain, Map<String, String> prev,
				Map<String, String> cur) throws ServerFault {
			String oldHost = Optional.ofNullable(prev.get(DomainSettingsKeys.openid_host.name())).orElse("");
			String newHost = Optional.ofNullable(cur.get(DomainSettingsKeys.openid_host.name())).orElse("");

			String oldClient = Optional.ofNullable(prev.get(DomainSettingsKeys.openid_client_id.name())).orElse("");
			String newClient = Optional.ofNullable(cur.get(DomainSettingsKeys.openid_client_id.name())).orElse("");

			String oldSecret = Optional.ofNullable(prev.get(DomainSettingsKeys.openid_client_secret.name())).orElse("");
			String newSecret = Optional.ofNullable(cur.get(DomainSettingsKeys.openid_client_secret.name())).orElse("");

			String oldRealm = Optional.ofNullable(prev.get(DomainSettingsKeys.openid_client_secret.name())).orElse("");
			String newRealm = Optional.ofNullable(cur.get(DomainSettingsKeys.openid_client_secret.name())).orElse("");

			if (!oldHost.equals(newHost) || !oldClient.equals(newClient) || !oldSecret.equals(newSecret)
					|| !oldRealm.equals(newRealm)) {
				putDomainSettings(domain.uid);
			}

		}
	}

	private static final Logger logger = LoggerFactory.getLogger(SharedSystemConfigurationVerticle.class);

	@Override
	public void start() {
		MQ.init().thenAccept(v -> {
			IServiceProvider sysprov = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
			IDomains domApi = sysprov.instance(IDomains.class);
			domApi.all().stream().filter(d -> !"global.virt".equals(d.uid)).forEach(dom -> {
				putDomainSettings(dom.uid);
				logger.info("SharedDomainSettingsVerticle pre-load domain settings for {}", dom.uid);
			});
		}).exceptionally(t -> {
			logger.warn(t.getMessage());
			return null;
		});
	}

	private void putDomainSettings(String domainUid) {
		IServiceProvider sysprov = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
		IDomainSettings domSettingsApi = sysprov.instance(IDomainSettings.class, domainUid);
		SharedMap<String, Map<String, String>> clusterConf = MQ.sharedMap(Shared.MAP_DOMAIN_SETTINGS);
		clusterConf.put(domainUid, domSettingsApi.get());
	}

}
