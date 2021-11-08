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
package net.bluemind.domain.settings.config;

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.domain.api.DomainSettingsKeys;
import net.bluemind.domain.api.IDomainSettings;
import net.bluemind.domain.api.IDomainUids;
import net.bluemind.domain.api.IDomains;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.hornetq.client.Producer;
import net.bluemind.network.topology.Topology;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.NodeActivator;
import net.bluemind.server.api.Server;
import net.bluemind.system.nginx.NginxService;

public class DomainSettingsConfigFileUpdate extends AbstractVerticle {
	private static final Logger logger = LoggerFactory.getLogger(DomainSettingsConfigFileUpdate.class);

	public static boolean suspended = false;

	static final String BM_EXTERNAL_URL_FILEPATH = "/etc/bm/domains-settings";
	private static final String NEW_LINE_SEPARATOR = "\n";
	private static final String NEW_DATA_SEPARATOR = ":";

	@Override
	public void start() {
		vertx.eventBus().consumer("domainsettings.config.file.update", this::domainSettingsEvent);
	}

	private void domainSettingsEvent(Message<JsonObject> event) {
		if (suspended) {
			logger.warn("Domain settings config file creation is suspended");
			return;
		}

		Boolean externalUrlUpdated = event.body().getBoolean("externalUrlUpdated");

		// get domain uid list excluding global.virt domain
		IDomains domainService = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IDomains.class);
		List<String> domainUidsList = domainService.all().stream().filter(i -> !i.uid.equals(IDomainUids.GLOBAL_VIRT))
				.map(i -> i.uid).collect(Collectors.toList());

		StringBuilder infoByDomain = new StringBuilder();

		for (String domainUid : domainUidsList) {
			IDomainSettings domainSettings = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
					.instance(IDomainSettings.class, domainUid);

			Map<String, String> settings = domainSettings.get();
			String externalUrl = settings.get(DomainSettingsKeys.external_url.name());
			String defaultDomain = settings.get(DomainSettingsKeys.default_domain.name());

			if (Strings.isNullOrEmpty(externalUrl) && Strings.isNullOrEmpty(defaultDomain)) {
				continue;
			}

			infoByDomain.append(domainUid).append(NEW_DATA_SEPARATOR).append(externalUrl == null ? "" : externalUrl)
					.append(NEW_DATA_SEPARATOR).append(defaultDomain == null ? "" : defaultDomain)
					.append(NEW_LINE_SEPARATOR);
		}

		try {
			writeAndPropagate(externalUrlUpdated, infoByDomain);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

	}

	private void writeAndPropagate(Boolean externalUrlUpdated, StringBuilder infoByDomain) {
		Map<Server, INodeClient> serverList = createServersNodeClientMap();

		serverList.entrySet().forEach(s -> {
			writeDomainSettingsFile(infoByDomain, s);
		});

		if (externalUrlUpdated) {
			new NginxService().restart();
		}

		MQ.init(() -> {
			Producer prod = MQ.getProducer("end.domain.settings.file.updated");
			if (prod != null) {
				JsonObject infos = new JsonObject();
				infos.put("filepath", BM_EXTERNAL_URL_FILEPATH);
				prod.send(infos);
				logger.debug("Message sent on 'end.domain.settings.file.updated'");
			} else {
				logger.error("Message cannot be sent on 'end.domain.settings.file.updated'");
			}
		});

	}

	private Map<Server, INodeClient> createServersNodeClientMap() {
		return Topology.getIfAvailable()
				.map(t -> t.nodes().stream()
						.collect(Collectors.toMap(s -> s.value, s -> NodeActivator.get(s.value.address()))))
				.orElseGet(Collections::emptyMap);
	}

	private static void writeDomainSettingsFile(StringBuilder infoByDomain, Entry<Server, INodeClient> server) {

		server.getValue().writeFile(BM_EXTERNAL_URL_FILEPATH,
				new ByteArrayInputStream(infoByDomain.toString().getBytes()));
		logger.info("Domain settings configuration file {} has been updated on {}:{}", BM_EXTERNAL_URL_FILEPATH,
				server.getKey().ip, server.getKey().name);
	}

}
