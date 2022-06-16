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
import net.bluemind.cli.utils.Tasks;
import net.bluemind.core.api.Regex;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.api.TaskStatus;
import net.bluemind.domain.api.Domain;
import net.bluemind.system.api.CertData;
import net.bluemind.system.api.ISecurityMgmt;
import picocli.CommandLine.Command;
import picocli.CommandLine.IExitCodeGenerator;
import picocli.CommandLine.Option;

@Command(name = "manage-lets-encrypt", description = "Setup or renew Let's Encrypt for the specified domain.\nTo enable globally: specify global.virt domain, or don't specify the domain.\nIf enabled globally, a certificate can ALSO be used per domain (specify --domain=).")
public class LetsEncryptManageCommand implements ICmdLet, Runnable, IExitCodeGenerator {

	private int exitCode = 0;

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("certificate");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return LetsEncryptManageCommand.class;
		}
	}

	private CliContext ctx;
	private CliUtils cliUtils;

	@Option(required = false, names = { "--domain", "-d" }, description = "The domain, default 'global.virt'")
	public String domain;

	@Option(required = false, names = { "--contact",
			"-c" }, description = "The contact email to use for the certificate (default: no-reply@<default-domain>).")
	public String contactEmail;

	@Option(required = false, names = { "--silent", "-s" }, description = "Automatically accept Let's Encrypt Terms.")
	public boolean silent = false;

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

		ISecurityMgmt service = ctx.adminApi().instance(ISecurityMgmt.class);
		if (!silent) {
			ctx.info(String.format("Let's Encrypt conditions: %s", service.getLetsEncryptTos()));
			String s = System.console().readLine("Do you accept Let's Encrypt conditions? y/n: ");
			if (!"y".equalsIgnoreCase(s)) {
				throw new CliException("Let's Encrypt not enabled, because conditions not accepted.");
			}
		}

		try {
			service.approveLetsEncryptTos(domainItem.uid);
			ctx.info(String.format("Let's Encrypt conditions accepted for domain '%s'.", domain));

			CertData certData = CertData.createForLetsEncrypt(domainItem.uid, contactEmail);
			TaskRef tr = service.generateLetsEncrypt(certData);
			TaskStatus status = Tasks.follow(ctx, tr, String.format("Failed to get certificate for domain %s", domain));

			if (status != null && status.state == TaskStatus.State.Success) {
				ctx.info(String.format("Let's Encrypt Certificate generated for domain '%s'.", domain));
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
