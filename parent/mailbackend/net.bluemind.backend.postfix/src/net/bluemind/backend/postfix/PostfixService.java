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
package net.bluemind.backend.postfix;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.backend.postfix.internal.cf.MainCf;
import net.bluemind.backend.postfix.internal.cf.MasterCf;
import net.bluemind.backend.postfix.internal.cf.RelayPassword;
import net.bluemind.backend.postfix.internal.cf.ShardMainCf;
import net.bluemind.backend.postfix.internal.cf.SmtpdConf;
import net.bluemind.backend.postfix.internal.maps.ServerMaps;
import net.bluemind.backend.postfix.internal.maps.events.EventProducer;
import net.bluemind.config.InstallationId;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.node.api.ExitList;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.NCUtils;
import net.bluemind.node.api.NodeActivator;
import net.bluemind.server.api.CommandStatus;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;
import net.bluemind.system.api.ISystemConfiguration;
import net.bluemind.system.api.SystemConf;

public class PostfixService {
	private static final Logger logger = LoggerFactory.getLogger(PostfixService.class);

	private static final String DEFAULT_MESSAGE_SIZE = "10485760";

	private IServiceProvider provider;

	public PostfixService() {
		this.provider = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
	}

	public void initializeServer(String serverUid, String tag) throws ServerFault {
		IServer serverService = provider.instance(IServer.class, InstallationId.getIdentifier());
		ItemValue<Server> serverItem = serverService.getComplete(serverUid);
		if (serverItem == null) {
			throw new ServerFault("Server " + serverUid + " not found ", ErrorCode.NOT_FOUND);
		}

		MainConfig config = MainConfig.get(provider);

		MasterCf masterCf = new MasterCf(serverService, serverUid);
		masterCf.write();

		MainCf mainCf = new MainCf(serverService, serverUid);
		mainCf.setHostname(getHostname(serverItem.value));
		mainCf.setMyNetworks(config.myNetworks);
		mainCf.setMessageSizeLimit(config.messageSizeLimit);
		mainCf.write();

		SmtpdConf saslConf = new SmtpdConf(serverService, serverUid);
		saslConf.write();

		// Needed to ensure that /etc/aliases.db exist
		CommandStatus st = serverService.submitAndWait(serverUid, "/usr/bin/newaliases");
		if (!st.successful) {
			throw new ServerFault("error during newaliases execution", ErrorCode.FAILURE);
		}

		Optional<ServerMaps> optionalServerMaps = ServerMaps.init(serverItem);
		if (!optionalServerMaps.isPresent()) {
			logger.error("Unable to init postfix map on server: {}", serverItem.uid);
			throw new ServerFault("Unable to init postfix map on server: " + serverItem.uid, ErrorCode.FAILURE);
		}

		ServerMaps serverMaps = optionalServerMaps.get();
		serverMaps.writeFlatMaps();
		serverMaps.enableMaps();

		restartPostfix(serverItem);
	}

	public void initializeShard(ItemValue<Server> server, ItemValue<Server> smtp) {
		IServer serverService = provider.instance(IServer.class, InstallationId.getIdentifier());

		MasterCf masterCf = new MasterCf(serverService, server.uid);
		masterCf.write();

		ShardMainCf mainCf = new ShardMainCf(serverService, server.uid);
		mainCf.setHostname(getHostname(server.value));
		mainCf.setRelayHost(smtp.value.address());
		mainCf.write();

		RelayPassword relayPwd = new RelayPassword(serverService, server.uid);
		relayPwd.setRelayHost(smtp.value.address());
		relayPwd.setHostname(getHostname(smtp.value));
		relayPwd.write();
		relayPwd.enable();

		// Needed to ensure that /etc/aliases.db exist
		CommandStatus st = serverService.submitAndWait(server.uid, "/usr/bin/newaliases");
		if (!st.successful) {
			throw new ServerFault("error during newaliases execution", ErrorCode.FAILURE);
		}

		restartPostfix(server);

		// disable milter
		INodeClient nc = NodeActivator.get(server.value.address());
		nc.executeCommandNoOut("systemctl stop bm-milter");
		nc.executeCommandNoOut("touch /etc/bm/bm-milter.disabled");
	}

	public void restartPostfix(ItemValue<Server> server) {
		logger.info("Restarting postfix on server {}", server.value.address());
		INodeClient nc = NodeActivator.get(server.value.address());

		ExitList result = NCUtils.waitFor(nc, nc.executeCommandNoOut("service postfix restart"));
		if (result.getExitCode() != 0) {
			logger.error("Error during postfix restart {} ", String.join(", ", result));
			throw new ServerFault(String.format("error during postfix restart %s", String.join(", ", result)),
					ErrorCode.FAILURE);
		}
	}

	private String getHostname(Server serverItem) {
		INodeClient nodeClient = NodeActivator.get(serverItem.address());
		return NCUtils.exec(nodeClient, "hostname -f").stream().reduce("", (output, elem) -> {
			return output.concat(elem);
		});
	}

	private static class MainConfig {

		public final String myNetworks;
		public final String messageSizeLimit;

		public MainConfig(String myNetworks, String messageSizeLimit) {
			this.myNetworks = myNetworks;
			this.messageSizeLimit = messageSizeLimit;
		}

		private static MainConfig get(IServiceProvider provider) {
			ISystemConfiguration sysConf = provider.instance(ISystemConfiguration.class);

			SystemConf conf = sysConf.getValues();
			String externalUrl = conf.stringValue("external-url");
			if (externalUrl == null || externalUrl.trim().isEmpty()) {
				throw new ServerFault("invalid external-url value", ErrorCode.INVALID_PARAMETER);
			}

			String mn = conf.stringValue("mynetworks");
			if (mn == null || mn.trim().length() == 0 || "null".equals(mn)) {
				throw new ServerFault("Invalid mynetworks value", ErrorCode.INVALID_PARAMETER);
			}

			String msl = conf.stringValue("message_size_limit");
			if (msl == null || msl.trim().length() == 0 || "null".equals(msl)) {
				msl = DEFAULT_MESSAGE_SIZE;
			}

			return new MainConfig(mn, msl);

		}

	}

	public void reInitializeAllMaps() {
		EventProducer.dirtyMaps();
	}

	public void reloadPostfix(ItemValue<Server> server) {
		logger.info("Reloading postfix on server {}", server.value.address());
		INodeClient nc = NodeActivator.get(server.value.address());

		ExitList result = NCUtils.waitFor(nc, nc.executeCommandNoOut("service postfix reload"));
		if (result.getExitCode() != 0) {
			logger.error("Error during postfix reload {} ", String.join(", ", result));
			throw new ServerFault(String.format("error during postfix reload %s", String.join(", ", result)),
					ErrorCode.FAILURE);
		}
	}

}
