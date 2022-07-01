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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;

import net.bluemind.core.backup.continuous.DataElement;
import net.bluemind.core.backup.continuous.restore.orphans.RestoreTopology.PromotingServer;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.core.utils.JsonUtils.ValueReader;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.DomainSettings;
import net.bluemind.domain.api.IDomainSettings;
import net.bluemind.domain.api.IInCoreDomains;
import net.bluemind.server.api.IServer;

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
			String payload = new String(domainElement.payload);
			switch (domainElement.key.valueClass) {
			case "net.bluemind.domain.api.Domain":
				restoreDomain(monitor, domainsToHandle, payload);
				break;
			case "net.bluemind.domain.api.DomainSettings":
				restoreDomainSettings(payload);
				break;
			default:
				System.err.println("Unhandled " + domainElement.key.valueClass);
				break;
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
			return;
		}
		ItemValue<Domain> known = domainApi.get(domain.uid);
		if (known != null) {
			logger.info("UPDATE DOMAIN {}", domain);
			domainApi.setAliases(domain.uid, domain.value.aliases);
			domainApi.restore(domain, false);
		} else {
			logger.info("CREATE DOMAIN {}", domain);
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

}
