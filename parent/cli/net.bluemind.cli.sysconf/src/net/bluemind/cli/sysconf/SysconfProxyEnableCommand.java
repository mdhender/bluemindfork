/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2019
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
package net.bluemind.cli.sysconf;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import io.airlift.airline.Command;
import io.airlift.airline.Option;
import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.system.api.ISystemConfiguration;
import net.bluemind.system.api.SysConfKeys;

@Command(name = "proxy-enable", description = "Set HTTP proxy parameters. Used to join bo.bluemind.net (checking"
		+ " subscription validity on upgrade, getting available new versions, hosting kind subscription report...)"
		+ " and synchronise remote calendar")
public class SysconfProxyEnableCommand implements ICmdLet, Runnable {

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("sysconf");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return SysconfProxyEnableCommand.class;
		}
	}

	protected CliContext ctx;

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		return this;
	}

	@Option(name = { "--hostname", "-h" }, required = true, description = "Proxy hostname")
	public String hostname = null;

	@Option(name = { "--port", "-p" }, required = false, description = "Proxy port. Default: 3128")
	public String port = null;

	@Option(name = { "--login" }, required = false, description = "Proxy login. Default: none")
	public String login = null;

	@Option(name = { "--password" }, required = false, description = "Proxy password. Default: none")
	public String password = null;

	@Option(name = {
			"--exceptions" }, required = false, description = "Proxy exceptions. Comma separated list of FQDN/IP/IP range. Default: none")
	public String exceptions = null;

	@Override
	public void run() {
		ISystemConfiguration configurationApi = ctx.adminApi().instance(ISystemConfiguration.class);

		Map<String, String> map = new HashMap<>();
		map.put(SysConfKeys.http_proxy_enabled.name(), Boolean.TRUE.toString());
		map.put(SysConfKeys.http_proxy_hostname.name(), hostname);
		map.put(SysConfKeys.http_proxy_port.name(), port);
		map.put(SysConfKeys.http_proxy_login.name(), login);
		map.put(SysConfKeys.http_proxy_password.name(), password);
		map.put(SysConfKeys.http_proxy_exceptions.name(), exceptions);

		configurationApi.updateMutableValues(map);
	}
}