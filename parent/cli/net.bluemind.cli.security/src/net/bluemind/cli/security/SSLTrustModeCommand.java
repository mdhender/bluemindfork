/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
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
package net.bluemind.cli.security;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.system.api.ISystemConfiguration;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.system.api.SystemConf;
import picocli.CommandLine.Command;
import picocli.CommandLine.IExitCodeGenerator;
import picocli.CommandLine.Option;

@Command(name = "ssl-trust-mode", description = "Sets trust policy (TRUST ALL) on TLS connections initiated from BlueMind")
public class SSLTrustModeCommand implements ICmdLet, Runnable, IExitCodeGenerator {

	private int exitCode = 0;

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("security");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return SSLTrustModeCommand.class;
		}
	}

	private CliContext ctx;

	@Option(required = true, names = { "--module" }, description = "Module name, ALL for all modules")
	public String module;

	@Option(required = false, names = { "--disable" }, description = "Disable Trust all mode.")
	public boolean disable;

	@Override
	public void run() {
		ISystemConfiguration conf = ctx.adminApi().instance(ISystemConfiguration.class);
		Set<String> current = new HashSet<>(conf.getValues().stringList(SysConfKeys.tls_trust_allcertificates.name()));
		Set<String> newValue = new HashSet<>();

		if (!disable) {
			if (module.equals("ALL")) {
				newValue.add("ALL");
			} else {
				newValue.add(module);
				current.stream().filter(entry -> !entry.equals("ALL")).forEach(newValue::add);
			}
		} else {
			if (!module.equals("ALL")) {
				current.stream().filter(entry -> !entry.equals("ALL") && !entry.equals(module)).forEach(newValue::add);
			}
		}

		conf.updateMutableValues(Map.of(SysConfKeys.tls_trust_allcertificates.name(),
				String.join(SystemConf.systemConfSeparator, newValue)));

	}

	@Override
	public int getExitCode() {
		return exitCode;
	}

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		return this;
	}

}
