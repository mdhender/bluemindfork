/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2018
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
package net.bluemind.backend.postfix.internal.maps;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.config.InstallationId;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.domain.api.IDomainSettings;
import net.bluemind.domain.api.IDomains;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;

public class PostfixMapUpdater {
	private static final Logger logger = LoggerFactory.getLogger(PostfixMapUpdater.class);

	private final BmContext context;

	public PostfixMapUpdater(BmContext context) {
		this.context = context;
	}

	public void refreshMaps() {
		List<ServerMaps> serversMaps = loadServersMaps();

		serversMaps.forEach(serverMaps -> {
			try {
				serverMaps.writeFlatMaps();
			} catch (ServerFault e) {
				logger.error("Fail to update flat maps on server: {}, address: {}", serverMaps.getServer().uid,
						serverMaps.getServer().value.address());
				throw e;
			}
		});

		serversMaps.forEach(serverMaps -> {
			try {
				serverMaps.enableMaps();
			} catch (ServerFault e) {
				logger.error("Fail to update indexed maps on server: {}, address: {}", serverMaps.getServer().uid,
						serverMaps.getServer().value.address());
				throw e;
			}
		});
	}

	private List<ServerMaps> loadServersMaps() throws ServerFault {
		Map<String, DomainInfo> domainInfoByUid = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDomains.class).all().stream().filter(dIv -> !dIv.value.global)
				.collect(Collectors.toMap(dIv -> dIv.uid, dIv -> DomainInfo.build(dIv, ServerSideServiceProvider
						.getProvider(SecurityContext.SYSTEM).instance(IDomainSettings.class, dIv.uid).get())));

		List<ItemValue<Server>> servers = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IServer.class, InstallationId.getIdentifier()).allComplete();

		List<MapRow> mapRows;
		try {
			mapRows = MapRow.build(context, servers, domainInfoByUid);
		} catch (SQLException e) {
			throw new ServerFault(e);
		}

		List<ServerMaps> serversMaps = new ArrayList<>();
		servers.forEach(server -> {
			ServerMaps.init(servers, server, domainInfoByUid, mapRows)
					.ifPresent(serverMaps -> serversMaps.add(serverMaps));
		});

		return serversMaps;
	}
}
