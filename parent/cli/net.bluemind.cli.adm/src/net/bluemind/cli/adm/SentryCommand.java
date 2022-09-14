/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2022
  *
  * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License)
  * or the CeCILL as published by CeCILL.info (version 2 of the License).
  *
  * There are special exceptions to the terms and conditions of the
  * licenses as they are applied to this program. See LICENSE.txt in
  * the directory of this program distribution.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.cli.adm;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import io.sentry.Sentry;
import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.system.api.ISystemConfiguration;
import net.bluemind.system.api.SysConfKeys;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "sentry", description = "sentry setup")
public class SentryCommand implements ICmdLet, Runnable {
	public static class Reg implements ICmdLetRegistration {
		@Override
		public Optional<String> group() {
			return Optional.of("maintenance");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return SentryCommand.class;
		}
	}

	@Option(names = "--dsn", description = "Sets a new sentry DSN", required = false)
	public String dsn = null;

	@Option(names = "--web-dsn", description = "Sets a new sentry-web DSN", required = false)
	public String webdsn = null;

	@Option(names = "--test", description = "Test sending events")
	public boolean testMode = false;

	private CliContext ctx;

	@Override
	public void run() {
		if (dsn != null && !dsn.isBlank()) {
			ISystemConfiguration configurationApi = ctx.adminApi().instance(ISystemConfiguration.class);
			Map<String, String> map = new HashMap<>();
			ctx.info("Set new sentry endpoint: " + dsn);
			map.put(SysConfKeys.sentry_endpoint.name(), dsn);
			configurationApi.updateMutableValues(map);
		}
		if (webdsn != null && !webdsn.isBlank()) {
			ISystemConfiguration configurationApi = ctx.adminApi().instance(ISystemConfiguration.class);
			Map<String, String> map = new HashMap<>();
			ctx.info("Set new sentry web endpoint: " + webdsn);
			map.put(SysConfKeys.sentry_web_endpoint.name(), webdsn);
			configurationApi.updateMutableValues(map);
		}
		if (testMode) {
			Sentry.init();
			ctx.info("Sending a sentry test message");
			Sentry.captureMessage("Testing " + System.currentTimeMillis());
			ctx.info("Sending a sentry test exception");
			Sentry.captureException(new Exception("Testing " + System.currentTimeMillis()));
			Sentry.flush(5000);
		}

	}

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		return this;
	}
}
