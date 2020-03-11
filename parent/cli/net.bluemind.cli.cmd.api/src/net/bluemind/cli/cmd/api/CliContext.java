/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2018
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.cli.cmd.api;

import java.util.Optional;

import org.fusesource.jansi.Ansi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.config.Token;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.http.ClientSideServiceProvider;
import net.bluemind.locator.client.LocatorClient;

public class CliContext {

	private static final CliContext INSTANCE = new CliContext();
	private ClientSideServiceProvider adminServices;
	private static final Logger logger = LoggerFactory.getLogger(CliContext.class);

	private CliContext() {
		try {
			LocatorClient lc = new LocatorClient();
			String host = Optional.ofNullable(lc.locateHost("bm/core", "admin0@global.virt")).orElse("127.0.0.1");
			this.adminServices = ClientSideServiceProvider.getProvider("http://" + host + ":8090", Token.admin0());
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	public Ansi ansi() {
		return Ansi.ansi();
	}

	public static CliContext get() {
		return INSTANCE;
	}

	public IServiceProvider adminApi() {
		return adminServices;
	}

	/**
	 * Prints a red message (and avoids sonar error)
	 * 
	 * @param msg
	 */
	public void error(String msg) {
		System.err.println(ansi().fgRed().a(msg).reset()); // NOSONAR
	}

	/**
	 * Prints a yellow message (and avoids sonar error)
	 * 
	 * @param msg
	 */
	public void warn(String msg) {
		System.out.println(ansi().fgYellow().a(msg).reset()); // NOSONAR
	}

	/**
	 * Use this to avoid sonar errors about logger usage
	 * 
	 * @param msg
	 */
	public void info(String msg) {
		System.out.println(msg); // NOSONAR
	}

	public void progress(int total, int current) {
		System.out.println(ansi().fgGreen()
				.a(String.format("Global progress %d/%d (%s%%)", current, total, current * 100 / total)).reset());
	}
}
