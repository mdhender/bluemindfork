/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2016
  *
  * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
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
package net.bluemind.monitoring.handler.system;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.monitoring.api.Command;
import net.bluemind.monitoring.api.FetchedData;
import net.bluemind.monitoring.api.ServerInformation;
import net.bluemind.monitoring.api.Status;
import net.bluemind.monitoring.service.util.CommandExecutor;
import net.bluemind.monitoring.service.util.Service;
import net.bluemind.server.api.Server;

public final class Dns extends Service {

	private static Dns instance;

	private Dns() {
		super("system", "dns");
		this.endpoints.add("responses");
	}

	public static Dns getInstance() {
		if (Dns.instance == null) {
			Dns.instance = new Dns();
		}

		return Dns.instance;
	}

	/**
	 * Checks if the DNS could be resolved
	 * 
	 * @return
	 * @throws ServerFault
	 */
	@Override
	public ServerInformation getServerInfo(Server server, String endpoint) {
		switch (endpoint) {
		case "responses":
			return responses(server);
		}
		return null;
	}

	private ServerInformation responses(Server server) {
		ServerInformation dnsInfo = new ServerInformation(server, SystemHandler.BASE, "dns", "responses");
		int nbAnswers = 0;

		Command dnsCommand = new Command(SystemHandler.SCRIPTS_FOLDER + "dns_check.sh");

		try {
			int exitCode = CommandExecutor.execCmdOnServer(dnsInfo.server, dnsCommand);

			dnsInfo.commands.add(dnsCommand);

			if (exitCode == 0) {
				dnsInfo.setStatus(Status.OK);
				dnsInfo.addMessage("DNS server resolves correctly domain names");
			} else {
				dnsInfo.setStatus(Status.KO);
				dnsInfo.addMessage("DNS server does not resolve some domain name");
			}

			dnsInfo.addData(new FetchedData("ResponseCount", String.valueOf(nbAnswers)));
		} catch (Exception e) {
			logger.error("Error retrieving server {} DNS status", server.address());
			dnsInfo.addMessage(String.format("Unable to fetch DNS status for server %s", server.address()));
			dnsInfo.setStatus(Status.UNKNOWN);
		}

		return dnsInfo;
	}

}
