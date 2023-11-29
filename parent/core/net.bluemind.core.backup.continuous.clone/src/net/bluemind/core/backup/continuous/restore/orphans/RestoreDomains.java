/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2021
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
package net.bluemind.core.backup.continuous.restore.orphans;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;

import net.bluemind.core.api.auth.AuthDomainProperties;
import net.bluemind.core.backup.continuous.DataElement;
import net.bluemind.core.backup.continuous.restore.orphans.RestoreTopology.PromotingServer;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.core.task.service.TaskUtils;
import net.bluemind.core.task.service.TaskUtils.ExtendedTaskStatus;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.core.utils.JsonUtils.ValueReader;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.DomainSettings;
import net.bluemind.domain.api.IDomainSettings;
import net.bluemind.domain.api.IDomains;
import net.bluemind.domain.api.IInCoreDomains;
import net.bluemind.keycloak.api.IKeycloakAdmin;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.TagDescriptor;

public class RestoreDomains {

	private static final Logger logger = LoggerFactory.getLogger(RestoreDomains.class);
	private static final ValueReader<ItemValue<Domain>> domainReader = JsonUtils
			.reader(new TypeReference<ItemValue<Domain>>() {
			});
	private static final ValueReader<ItemValue<DomainSettings>> settingsReader = JsonUtils
			.reader(new TypeReference<ItemValue<DomainSettings>>() {
			});

	private final IServiceProvider target;
	private final Collection<PromotingServer> servers;
	private final IInCoreDomains domainApi;
	private final IServer topologyApi;

	public RestoreDomains(IServiceProvider target, Collection<PromotingServer> servers) {
		this.target = target;
		this.servers = servers;
		this.domainApi = target.instance(IInCoreDomains.class);
		this.topologyApi = target.instance(IServer.class, "default");
	}

	public Map<String, ItemValue<Domain>> restore(IServerTaskMonitor monitor, List<DataElement> domains) {
		Map<String, ItemValue<Domain>> domainsToHandle = new HashMap<>();
		domains.forEach(domainElement -> {
			try {
				String payload = new String(domainElement.payload);
				switch (domainElement.key.valueClass) {
				case "net.bluemind.domain.api.Domain":
					restoreDomain(monitor, domainsToHandle, payload);
					break;
				case "net.bluemind.domain.api.DomainSettings":
					restoreDomainSettings(payload);
					break;
				default:
					logger.warn("Unhandled {}", domainElement.key.valueClass);
					break;
				}
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		});
		monitor.progress(1, "Dealt with " + domainsToHandle.size() + " domain(s)");
		return domainsToHandle;
	}

	private void restoreDomainSettings(String payload) {
		ItemValue<DomainSettings> set = settingsReader.read(payload);
		IDomainSettings setApi = target.instance(IDomainSettings.class, set.uid);
		setApi.set(set.value.settings);
		logger.info("Set settings of {}", set.uid);
	}

	private void restoreDomain(IServerTaskMonitor monitor, Map<String, ItemValue<Domain>> domainsToHandle,
			String payload) {
		ItemValue<Domain> domain = domainReader.read(payload);
		if (domain.uid.equals("global.virt")) {
			// update it for keycloak properties
			tweakKeycloakProps(target.instance(IDomains.class).get("global.virt"), domain);
			domainApi.restore(domain, false);
			TaskRef taskRef = target.instance(IKeycloakAdmin.class).initForDomain("global.virt");
			ExtendedTaskStatus taskStatus = TaskUtils.wait(target, taskRef);
			if (!taskStatus.state.succeed) {
				logger.warn("Unable to setup keycloak for {}: task ended in status {}", domain, taskStatus.state);
			}
			return;
		}
		ItemValue<Domain> known = domainApi.get(domain.uid);
		if (known != null) {
			logger.info("UPDATE DOMAIN {}", domain);
			domainApi.setAliases(domain.uid, domain.value.aliases);
			tweakKeycloakProps(known, domain);
			domainApi.restore(domain, false);
		} else {
			logger.info("CREATE DOMAIN {}", domain);
			monitor.log("CREATE DOMAIN {}", domain);
			tweakKeycloakProps(null, domain);
			domainApi.restore(domain, true);
			for (PromotingServer iv : servers) {
				for (String tag : iv.clone.value.tags) {
					topologyApi.assign(iv.clone.uid, domain.uid, tag);
				}
				monitor.log("assign " + iv.clone.uid + " to " + domain.uid);
			}
		}
		domainsToHandle.put(domain.uid, domain);
	}

	private void tweakKeycloakProps(ItemValue<Domain> prev, ItemValue<Domain> domain) {
		if (domain.value.properties != null) {
			String oidSec = AuthDomainProperties.OPENID_CLIENT_SECRET.name();
			if (prev != null && prev.value.properties != null && prev.value.properties.containsKey(oidSec)) {
				domain.value.properties.put(oidSec, prev.value.properties.get(oidSec));
			}
			servers.stream().filter(pv -> pv.clone.value.tags.contains(TagDescriptor.bm_keycloak.getTag()))
					.forEach(pv -> {
						for (Entry<String, String> kv : domain.value.properties.entrySet()) {
							String fresh = kv.getValue().replace(pv.leader.value.address(), pv.clone.value.address());
							kv.setValue(fresh);
						}
					});
		}
	}

}
