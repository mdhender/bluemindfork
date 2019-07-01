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
import java.util.LinkedList;
import java.util.List;

import net.bluemind.imap.sieve.SieveArg;
import net.bluemind.imap.sieve.SieveCommand;
import net.bluemind.imap.sieve.SieveResponse;
import net.bluemind.imap.sieve.SieveScript;

public class SieveListscripts extends SieveCommand<List<SieveScript>> {

	public SieveListscripts() {
		retVal = new LinkedList<SieveScript>();
	}

	@Override
	protected List<SieveArg> buildCommand() {
		List<SieveArg> args = new ArrayList<SieveArg>(1);
		args.add(new SieveArg("LISTSCRIPTS".getBytes(), false));
		return args;
	}

	@Override
	public void responseReceived(SieveResponse rs) {
		if (commandSucceeded(rs)) {
			for (int i = 0; i < rs.getLines().size(); i++) {
				String line = rs.getLines().get(i);
				boolean active = line.endsWith("ACTIVE");
				int idx = line.lastIndexOf("\"");
				if (idx > 0) {
					String name = line.substring(1, idx);
					retVal.add(new SieveScript(name, active));
				} else {
					logger.warn("receveid from listscripts: '" + line + "'");
				}
			}
		} else {
			reportErrors(rs);
		}
		logger.debug("returning a list of " + retVal.size() + " sieve script(s)");
	}

}
