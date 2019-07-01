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

/**
 * cyrus 2.3.X only :'(
 * 
 * 
 */
public class SieveUnauthenticate extends SieveCommand<Boolean> {

	@Override
	protected List<SieveArg> buildCommand() {
		List<SieveArg> args = new ArrayList<SieveArg>(1);
		args.add(new SieveArg("UNAUTHENTICATE".getBytes(), false));
		return args;
	}

	@Override
	public void responseReceived(SieveResponse rs) {
		if (!commandSucceeded(rs)) {
			logger.error("error " + rs.getMessageResponse());

		}
	}

}
