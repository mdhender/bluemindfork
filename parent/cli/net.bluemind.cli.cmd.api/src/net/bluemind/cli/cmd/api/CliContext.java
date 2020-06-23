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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.fusesource.jansi.Ansi;

import com.google.common.base.Suppliers;

import net.bluemind.config.BmIni;
import net.bluemind.config.Token;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.http.ClientSideServiceProvider;
import net.bluemind.network.topology.Topology;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;

public class CliContext {

	private static final CliContext INSTANCE = new CliContext();
	private Supplier<ClientSideServiceProvider> adminServices = Suppliers.memoize(this::loadAdminServices);

	private CliContext() {
	}

	private ClientSideServiceProvider loadAdminServices() {
		String core = Optional.ofNullable(BmIni.value("external-url")).orElse("127.0.0.1");
		ClientSideServiceProvider ret = ClientSideServiceProvider.getProvider("http://" + core + ":8090",
				Token.admin0());
		Topology.getIfAvailable().orElseGet(() -> {
			try {
				List<ItemValue<Server>> servers = ret.instance(IServer.class, "default").allComplete();
				Topology.update(servers);
				return Topology.get();
			} catch (Exception e) { // NOSONAR
				return null;
			}
		});
		return ret;
	}

	public Ansi ansi() {
		return Ansi.ansi();
	}

	public static CliContext get() {
		return INSTANCE;
	}

	public IServiceProvider adminApi() {
		return adminServices.get();
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

	/**
	 * Create a stacktrace in string format
	 * 
	 * @param e {@link java.lang.Exception}
	 * @return String containing the stacktrace
	 */
	public String toStack(Exception e) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		return sw.toString();
	}
}
