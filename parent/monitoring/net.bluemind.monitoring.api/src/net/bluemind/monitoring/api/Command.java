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
package net.bluemind.monitoring.api;

import java.util.List;

import net.bluemind.core.api.BMApi;

/**
 * A command is used to execute Shell scripts and contains the results of its
 * execution (raw and in a list).
 */
@BMApi(version = "3")
public class Command {

	/**
	 * The Shell script to be executed.
	 */
	public String commandLine;

	/**
	 * The raw (non-formatted) result of the command's execution.
	 */
	public String rawData;

	/**
	 * Formatted data in a list.
	 */
	public List<FetchedData> dataList;

	public Command() {
	}

	/**
	 * Creates a new {@link Command} with a given command line.
	 * 
	 * @param commandLine the command line to be executed
	 */
	public Command(String commandLine) {
		this.commandLine = commandLine;
		this.rawData = new String();
	}

	/**
	 * Be careful, {@link #dataList} can be modified unlike {@link #rawData}!
	 * 
	 * @return <code>true</code> if the formatted data list is empty or
	 *         <code>null</code>, <code>false</code> otherwise
	 */
	public boolean hasDataList() {
		return (this.dataList != null && !this.dataList.isEmpty());
	}
}
