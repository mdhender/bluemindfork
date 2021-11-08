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
package net.bluemind.cli.certificate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Collectors;

import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.CliException;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.utils.CliUtils;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;
import net.bluemind.system.api.CertData;
import net.bluemind.system.api.CertData.CertificateDomainEngine;
import net.bluemind.system.api.ISecurityMgmt;
import picocli.CommandLine.Command;
import picocli.CommandLine.IExitCodeGenerator;
import picocli.CommandLine.Option;

@Command(name = "file-cert", description = "Change SSL certificate")
public class CertCommand implements ICmdLet, Runnable, IExitCodeGenerator {

	private int exitCode = 0;

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("certificate");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return CertCommand.class;
		}
	}

	private CliContext ctx;
	private CliUtils cliUtils;

	@Option(required = true, names = { "--ca" }, description = "Certification Authority PEM file.")
	public Path ca;

	@Option(required = true, names = { "--cert" }, description = "Cert PEM file.")
	public Path cert;

	@Option(required = true, names = { "--key" }, description = "Private key PEM file.")
	public Path key;

	@Option(required = false, names = { "--domain" }, description = "The domain, default 'global.virt'")
	public String domain;

	@Override
	public void run() {
		if (!ca.toFile().exists()) {
			ctx.error("CA file is missing.");
			exitCode = 2;
			return;
		}
		if (!cert.toFile().exists()) {
			ctx.error("Cert file is missing.");
			exitCode = 2;
			return;
		}
		if (!key.toFile().exists()) {
			ctx.error("Private key file is missing.");
			exitCode = 2;
			return;
		}

		if (domain == null || domain.isEmpty()) {
			domain = "global.virt";
		}

		ISecurityMgmt secApi = ctx.adminApi().instance(ISecurityMgmt.class);
		try {
			ItemValue<Domain> domainItem = cliUtils.getDomain(domain)
					.orElseThrow(() -> new CliException(String.format("Domain '%s' not found", domain)));

			String caContent = Files.readAllLines(ca).stream().collect(Collectors.joining("\n"));
			String certContent = Files.readAllLines(cert).stream().collect(Collectors.joining("\n"));
			String keyContent = Files.readAllLines(key).stream().collect(Collectors.joining("\n"));
			CertData cd = CertData.createWithDomainUid(CertificateDomainEngine.FILE, caContent, certContent, keyContent,
					domainItem.uid);
			secApi.updateCertificate(cd);
			ctx.info(String.format("Certificate updated for domain '%s'.", domain));
		} catch (IOException e) {
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
