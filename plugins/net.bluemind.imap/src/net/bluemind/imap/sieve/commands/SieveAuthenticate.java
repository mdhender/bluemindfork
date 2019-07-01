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

public class SieveAuthenticate extends SieveCommand<Boolean> {

	private String login;
	private String authname;
	private String password;
	private byte[] encoded;

	public SieveAuthenticate(String login, String authname, String password) {
		this.login = login;
		this.authname = authname;
		this.password = password;
		this.encoded = encodeAuthString(login, authname, password);
		this.retVal = Boolean.FALSE;
	}

	@Override
	public void responseReceived(SieveResponse rs) {
		if (commandSucceeded(rs)) {
			retVal = true;
		}
		if (logger.isDebugEnabled()) {
			logger.debug("response received for login " + login + ", authname  " + authname + " " + password + " => "
					+ retVal);
		}
	}

	@Override
	protected List<SieveArg> buildCommand() {
		List<SieveArg> ret = new ArrayList<SieveArg>(2);

		ret.add(new SieveArg("AUTHENTICATE \"PLAIN\"".getBytes(), false));
		ret.add(new SieveArg(encoded, true));

		return ret;
	}

	private byte[] encodeAuthString(String login, String authname, String password) {
		byte[] log = login.getBytes();
		byte[] auth = authname.getBytes();
		byte[] pass = password.getBytes();
		byte[] data = new byte[log.length + auth.length + pass.length + 2];
		int i = 0;
		for (int j = 0; j < log.length; j++) {
			data[i++] = log[j];
		}
		data[i++] = 0x0;
		for (int j = 0; j < auth.length; j++) {
			data[i++] = auth[j];
		}
		data[i++] = 0x0;

		for (int j = 0; j < pass.length; j++) {
			data[i++] = pass[j];
		}

		byte[] ret = java.util.Base64.getEncoder().encode(data);
		if (logger.isDebugEnabled()) {
			logger.debug("l: " + login + " p: " + password + " encoded auth string: " + ret.toString());
		}

		return ret;
	}

}
