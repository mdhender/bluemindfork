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

import java.util.HashMap;
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
import net.bluemind.domain.api.IDomainSettings;
import net.bluemind.domain.api.IDomains;
import net.bluemind.domain.hook.DomainHookAdapter;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.hornetq.client.MQ.SharedMap;
import net.bluemind.hornetq.client.Shared;
import net.bluemind.lib.vertx.IUniqueVerticleFactory;
import net.bluemind.lib.vertx.IVerticleFactory;
import net.bluemind.openid.api.OpenIdProperties;

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
		public void onUpdated(BmContext context, ItemValue<Domain> previousValue, ItemValue<Domain> domain)
				throws ServerFault {
			String oldHost = Optional.ofNullable(previousValue.value.properties.get(OpenIdProperties.OPENID_HOST.name()))
					.orElse("");
			String newHost = Optional.ofNullable(domain.value.properties.get(OpenIdProperties.OPENID_HOST.name())).orElse("");

			String oldClient = Optional
					.ofNullable(previousValue.value.properties.get(OpenIdProperties.OPENID_CLIENT_ID.name())).orElse("");
			String newClient = Optional.ofNullable(domain.value.properties.get(OpenIdProperties.OPENID_CLIENT_ID.name()))
					.orElse("");

			String oldSecret = Optional
					.ofNullable(previousValue.value.properties.get(OpenIdProperties.OPENID_CLIENT_SECRET.name())).orElse("");
			String newSecret = Optional.ofNullable(domain.value.properties.get(OpenIdProperties.OPENID_CLIENT_SECRET.name()))
					.orElse("");

			String oldRealm = Optional.ofNullable(previousValue.value.properties.get(OpenIdProperties.OPENID_REALM.name()))
					.orElse("");
			String newRealm = Optional.ofNullable(domain.value.properties.get(OpenIdProperties.OPENID_REALM.name()))
					.orElse("");

			if (!oldHost.equals(newHost) || !oldClient.equals(newClient) || !oldSecret.equals(newSecret)
					|| !oldRealm.equals(newRealm)) {
				putDomainSettingsAndProperties(domain);
			}
		}
	}

	private static final Logger logger = LoggerFactory.getLogger(SharedDomainSettingsVerticle.class);

	@Override
	public void start() {
		MQ.init().thenAccept(v -> {
			IServiceProvider sysprov = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
			IDomains domApi = sysprov.instance(IDomains.class);
			domApi.all().stream().filter(d -> !"global.virt".equals(d.uid)).forEach(dom -> {
				putDomainSettingsAndProperties(dom);
				logger.info("SharedDomainPropertiesVerticle pre-load domain properties for {}", dom.uid);
			});
		}).exceptionally(t -> {
			logger.warn(t.getMessage());
			return null;
		});
	}

	private void putDomainSettingsAndProperties(ItemValue<Domain> domain) {

		Map<String, String> infos = new HashMap<>();
		infos.putAll(domain.value.properties);

		IServiceProvider sysprov = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
		IDomainSettings domSettingsApi = sysprov.instance(IDomainSettings.class, domain.uid);
		infos.putAll(domSettingsApi.get());

		SharedMap<String, Map<String, String>> clusterConf = MQ.sharedMap(Shared.MAP_DOMAIN_SETTINGS);
		clusterConf.put(domain.uid, infos);
	}

}
