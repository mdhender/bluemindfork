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
package net.bluemind.imap.sieve.commands;

import java.util.ArrayList;
import java.util.List;

import net.bluemind.imap.sieve.SieveArg;
import net.bluemind.imap.sieve.SieveCommand;
import net.bluemind.imap.sieve.SieveResponse;

public class SieveDeleteScript extends SieveCommand<Boolean> {

	private String name;

	public SieveDeleteScript(String name) {
		this.name = name;
		retVal = false;
	}

	@Override
	protected List<SieveArg> buildCommand() {
		List<SieveArg> args = new ArrayList<SieveArg>(1);
		args.add(new SieveArg(("DELETESCRIPT \"" + name + "\"").getBytes(), false));
		return args;
	}

	@Override
	public void responseReceived(SieveResponse rs) {
		logger.debug("listscripts response received.");
		if (commandSucceeded(rs)) {
			retVal = true;
		} else {
			logger.error("error " + rs.getMessageResponse());
		}
	}

}
