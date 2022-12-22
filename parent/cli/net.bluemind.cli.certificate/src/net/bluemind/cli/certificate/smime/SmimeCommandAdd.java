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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.CliException;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.utils.CliUtils;
import net.bluemind.core.container.api.Ack;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;
import net.bluemind.smime.cacerts.api.ISmimeCACert;
import net.bluemind.smime.cacerts.api.ISmimeCacertUids;
import net.bluemind.smime.cacerts.api.SmimeCacert;
import picocli.CommandLine.Command;
import picocli.CommandLine.IExitCodeGenerator;
import picocli.CommandLine.Option;

@Command(name = "add-smime", description = "Add S/MIME certificate for a domain")
public class SmimeCommandAdd implements ICmdLet, Runnable, IExitCodeGenerator {

	private int exitCode = 0;

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("certificate");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return SmimeCommandAdd.class;
		}
	}

	private CliContext ctx;
	private CliUtils cliUtils;

	@Option(required = true, names = { "--ca" }, description = "Certification Authority PEM file.")
	public Path ca;

	@Option(required = true, names = { "--domain" }, description = "The domain, 'global.virt' invalid")
	public String domain;

	@Override
	public void run() {
		if (!ca.toFile().exists()) {
			throw new CliException("CA file is missing.");
		}
		if (domain == null || domain.isEmpty()) {
			throw new CliException("Domain is missing.");
		}

		try {
			ItemValue<Domain> domainItem = cliUtils.getDomain(domain)
					.orElseThrow(() -> new CliException(String.format("Domain '%s' not found", domain)));
			ISmimeCACert smimeApi = ctx.adminApi().instance(ISmimeCACert.class,
					ISmimeCacertUids.domainCreatedCerts(domainItem.uid));

			String caContent = Files.readAllLines(ca).stream().collect(Collectors.joining("\n"));
			if (caContent == null || caContent.isEmpty()) {
				throw new CliException("Certificate Authority file must be valid.");
			}

			Ack create = smimeApi.create(UUID.randomUUID().toString(), SmimeCacert.create(caContent));
			if (create.version <= 0) {
				throw new CliException("S/MIME certificate not created for domain '" + domainItem.value.defaultAlias
						+ "' (" + domainItem.displayName + ")");
			}
			ctx.info("S/MIME certificate added for domain '{}' ({}).", domainItem.value.defaultAlias,
					domainItem.displayName);
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
