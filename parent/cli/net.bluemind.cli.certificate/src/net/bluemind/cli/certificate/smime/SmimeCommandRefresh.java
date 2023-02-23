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
package net.bluemind.cli.certificate.smime;

import java.util.Optional;

import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.CliException;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.utils.CliUtils;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;
import net.bluemind.smime.cacerts.api.ISmimeRevocation;
import picocli.CommandLine.Command;
import picocli.CommandLine.IExitCodeGenerator;
import picocli.CommandLine.Option;

@Command(name = "update-smime", description = "Refresh S/MIME certificates revoked clients for a domain")
public class SmimeCommandRefresh implements ICmdLet, Runnable, IExitCodeGenerator {

	private int exitCode = 0;

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("certificate");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return SmimeCommandRefresh.class;
		}
	}

	private CliContext ctx;
	private CliUtils cliUtils;

	@Option(required = true, names = { "--domain" }, description = "The domain, 'global.virt' invalid")
	public String domain;

	@Option(required = false, names = { "--uid" }, description = "S/MIME certificate item uid")
	public String uid;

	@Option(required = false, names = { "--all" }, description = "Refresh all domain S/MIME certificates revocations")
	public boolean all = false;

	@Override
	public void run() {
		if (domain == null || domain.isEmpty()) {
			throw new CliException("Domain is missing.");
		}
		if (!all && uid == null) {
			throw new CliException("Certificate id is missing");
		}

		try {
			ItemValue<Domain> domainItem = cliUtils.getNotGlobalDomain(domain);

			ISmimeRevocation revocationApi = ctx.adminApi().instance(ISmimeRevocation.class, domainItem.uid);
			if (uid != null) {
				revocationApi.refreshRevocations(uid);
				ctx.info("S/MIME certificate '{}' revoked clients updated from domain '{}' ({}).", uid,
						domainItem.value.defaultAlias, domainItem.displayName);
			} else {
				revocationApi.refreshDomainRevocations();
				ctx.info("All S/MIME certificates revoked clients updated from domain '{}' ({}).",
						domainItem.value.defaultAlias, domainItem.displayName);
			}

		} catch (Exception e) {
			throw new CliException(e);
		}
	}

	@Override
	public int getExitCode() {
		return exitCode;
	}

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		this.cliUtils = new CliUtils(ctx);
		return this;
	}

}
