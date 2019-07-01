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
package net.bluemind.monitoring.handler.services;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.monitoring.api.Command;
import net.bluemind.monitoring.api.FetchedData;
import net.bluemind.monitoring.api.ServerInformation;
import net.bluemind.monitoring.api.Status;
import net.bluemind.monitoring.service.util.CommandExecutor;
import net.bluemind.server.api.Server;

public class Postfix extends AbstractService {

	private static final Logger logger = LoggerFactory.getLogger(Postfix.class);

	public Postfix() {
		super("postfix", Arrays.asList("mail/smtp", "mail/smtp-edge"));
		this.endpoints.add("queue");
		this.endpoints.add("relay");
	}

	public static ServerInformation checkQueue(Server server) {

		ServerInformation info = new ServerInformation(server, ServicesHandler.BASE, "postfix", "queue");
		Command c = new Command(ServicesHandler.SCRIPTS_FOLDER + "postfix_queue.sh");

		try {
			CommandExecutor.execCmdOnServer(server, c);
			info.commands.add(c);
			int queueSize = Integer.parseInt(c.dataList.get(0).data);
			info.addData(new FetchedData("QueuedMessages", c.rawData));
			if (queueSize >= 200) {
				if (queueSize >= 1000) {
					info.setStatus(Status.KO);
				} else {
					info.setStatus(Status.WARNING);
				}
			} else {
				info.setStatus(Status.OK);
			}
			info.addMessage("There are " + queueSize + " messages waiting to be sent");
		} catch (Exception e) {
			logger.error("Unable to retrieve the amount of messages in the queue : " + c.rawData, e);
			info.setStatus(Status.KO);
			info.addMessage("Unable to retrieve the amount of messages in the queue");
			
		}

		return info;

	}

	public static ServerInformation checkRelay(Server server) {
		ServerInformation info = new ServerInformation(server, "services", "postfix", "relay");
		Command c = new Command(ServicesHandler.BASE + "check_postfix_relay.sh");

		// TODO
		info.commands.add(c);

		return info;
	}

	@Override
	public ServerInformation getSpecificServerInfo(Server server, String method) {
		switch (method) {
		case "queue":
			return Postfix.checkQueue(server);
		case "relay":
			return Postfix.checkRelay(server);
		}
		return null;
	}

}
