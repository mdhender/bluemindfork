/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2021
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
package net.bluemind.cli.certificate;

import java.util.Optional;

import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.CliException;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.utils.CliUtils;
import net.bluemind.core.api.Regex;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;
import net.bluemind.system.api.ICertificateSecurityMgmt;
import picocli.CommandLine.Command;
import picocli.CommandLine.IExitCodeGenerator;
import picocli.CommandLine.Option;

@Command(name = "renew-lets-encrypt", description = "Renew Let's Encrypt SSL Certificate for a domain")
public class LetsEncryptRenewCommand implements ICmdLet, Runnable, IExitCodeGenerator {

	private int exitCode = 0;

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("certificate");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return LetsEncryptRenewCommand.class;
		}
	}

	private CliContext ctx;
	private CliUtils cliUtils;

	@Option(required = false, names = { "--domain", "-d" }, description = "The domain, default 'global.virt'")
	public String domain;

	@Option(required = false, names = { "--externalUrl", "-u" }, description = "The domain External URL.")
	public String externalUrl;

	@Option(required = false, names = { "--contact",
			"-c" }, description = "The contact email to use for the certificate (default: the one used for the previous generation).")
	public String contactEmail;

	@Override
	public void run() {
		if (contactEmail != null && !Regex.EMAIL.validate(contactEmail)) {
			throw new CliException("Invalid contact email format");
		}
		if (domain == null || domain.isEmpty()) {
			domain = "global.virt";
		}

		ItemValue<Domain> domainItem = cliUtils.getDomain(domain)
				.orElseThrow(() -> new CliException(String.format("Domain '%s' not found", domain)));

		try {
			ctx.adminApi().instance(ICertificateSecurityMgmt.class).renewLetsEncryptCertificate(domainItem.uid,
					externalUrl, contactEmail);
			ctx.info(String.format("Let's Encrypt Certificate updated for domain '%s'.", domain));
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
