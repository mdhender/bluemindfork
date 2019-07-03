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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import net.bluemind.imap.sieve.SieveArg;
import net.bluemind.imap.sieve.SieveCommand;
import net.bluemind.imap.sieve.SieveResponse;
import net.bluemind.utils.FileUtils;

public class SievePutscript extends SieveCommand<Boolean> {

	private String name;
	private byte[] data;

	public SievePutscript(String name, InputStream scriptContent) {
		retVal = false;
		this.name = name;
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			FileUtils.transfer(scriptContent, out, true);
			this.data = out.toByteArray();
			logger.debug("script " + new String(data));
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}

	}

	@Override
	protected List<SieveArg> buildCommand() {
		List<SieveArg> args = new ArrayList<SieveArg>(1);
		args.add(new SieveArg(("PUTSCRIPT \"" + name + "\"").getBytes(), false));
		args.add(new SieveArg(data, true));
		return args;
	}

	@Override
	public void responseReceived(SieveResponse rs) {
		logger.debug("putscript response received.");
		if (commandSucceeded(rs)) {
			retVal = true;
		} else {
			logger.error("error {}, script name: {}, content {}", rs.getMessageResponse(), name, new String(data));
		}
	}

}
