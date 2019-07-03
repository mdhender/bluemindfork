package net.bluemind.monitoring.api;
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

import java.util.ArrayList;
import java.util.List;

import net.bluemind.core.api.BMApi;
import net.bluemind.server.api.Server;

/**
 * A server information is the most specific level of information (PluginInfo >
 * ServiceInfo > MethodInfo > ServerInfo) and belongs to a MethodInfo.
 */
@BMApi(version = "3")
public class ServerInformation {

	public String plugin;

	public String service;

	public String endpoint;

	/**
	 * The server to which belongs the information.
	 */
	public Server server;

	/**
	 * Status of the information.
	 */
	public Status status;

	/**
	 * List of messages given to the administrator.
	 */
	public List<String> messages;

	/**
	 * List of data given to the administrator.
	 */
	public List<FetchedData> dataList;

	/**
	 * Executed Linux commands to fetch and fill the information. Is it really
	 * useful to keep the commands in the Server Information ?
	 */
	public List<Command> commands;

	public ServerInformation() {
	}

	public ServerInformation(Server server, String plugin, String service, String endpoint) {
		this.status = Status.UNKNOWN;
		this.commands = new ArrayList<Command>();
		this.plugin = plugin;
		this.service = service;
		this.endpoint = endpoint;
		this.server = server;
	}

	/**
	 * Adds a new message to the list of messages. Prefer this method over
	 * getMessages().add(...) because this method verifies that the messages field
	 * is initialized.
	 * 
	 * @param message the message to be inserted
	 */
	public void addMessage(String message) {
		if (this.messages == null) {
			this.messages = new ArrayList<String>();
		}

		if (message != null && !message.equals("")) {
			this.messages.add(message);
		}
	}

	/**
	 * Adds a new data to the list of data. Prefer this method over
	 * this.dataList.add(stuff) ) because this method verifies that the dataList
	 * field is initialized.
	 * 
	 * @param data the data to be inserted
	 */
	public void addData(FetchedData data) {
		if (this.dataList == null) {
			this.dataList = new ArrayList<FetchedData>();
		}

		if (data != null) {
			this.dataList.add(data);
		}
	}

	/**
	 * Sets a new status to the current server information if the given status has a
	 * greater value than the current status. Use it to make sure the status of the
	 * server info will never be decreased.
	 * 
	 * @param status the new status
	 */
	public void setStatus(Status status) {
		this.status = (this.status.getValue() < status.getValue() ? status : this.status);
	}

	/**
	 * @return <code>true</code> if every command belonging to the server
	 *         information has raw data, <code>false</code> otherwise
	 */
	public boolean hasData() {
		boolean data = true;

		for (Command c : this.commands) {
			data &= c.hasDataList();
		}

		return data;
	}

}
