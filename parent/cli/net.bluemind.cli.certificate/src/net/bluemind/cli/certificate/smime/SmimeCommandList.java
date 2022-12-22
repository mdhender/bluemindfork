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

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Optional;

import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.CliException;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.utils.CliUtils;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;
import net.bluemind.smime.cacerts.api.ISmimeCACert;
import net.bluemind.smime.cacerts.api.ISmimeCacertUids;
import net.bluemind.smime.cacerts.api.SmimeCacert;
import net.bluemind.utils.CertificateUtils;
import picocli.CommandLine.Command;
import picocli.CommandLine.IExitCodeGenerator;
import picocli.CommandLine.Option;

@Command(name = "list-smime", description = "List S/MIME certificates for a domain")
public class SmimeCommandList implements ICmdLet, Runnable, IExitCodeGenerator {

	private int exitCode = 0;

	public static class Reg implements ICmdLetRegistration {

		@Override
		public Optional<String> group() {
			return Optional.of("certificate");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return SmimeCommandList.class;
		}
	}

	private CliContext ctx;
	private CliUtils cliUtils;

	@Option(required = true, names = { "--domain" }, description = "The domain, 'global.virt' invalid")
	public String domain;

	@Override
	public void run() {
		if (domain == null || domain.isEmpty()) {
			throw new CliException("Domain is missing.");
		}

		ItemValue<Domain> domainItem = cliUtils.getDomain(domain)
				.orElseThrow(() -> new CliException(String.format("Domain '%s' not found", domain)));
		ISmimeCACert smimeApi = ctx.adminApi().instance(ISmimeCACert.class,
				ISmimeCacertUids.domainCreatedCerts(domainItem.uid));

		List<ItemValue<SmimeCacert>> all = smimeApi.all();
		if (all.isEmpty()) {
			ctx.info("There is no S/MIME certificate for domain '{}' ({}).", domainItem.value.defaultAlias,
					domainItem.displayName);
			return;
		}
		ctx.info("S/MIME Certificates for domain '{}' ({}).", domainItem.value.defaultAlias, domainItem.displayName);
		display(all);
	}

	private void display(List<ItemValue<SmimeCacert>> certList) {
		// { "UID", "Principal Issuer", "Principal Subject" };
		for (ItemValue<SmimeCacert> item : certList) {
			X509Certificate cert = getCertificate(item);
			String[][] asTable = new String[3][2];
			asTable[0][0] = "UID";
			asTable[0][1] = item.uid;
			asTable[1][0] = "Principal Issuer";
			asTable[1][1] = cert.getIssuerX500Principal().getName();
			asTable[2][0] = "Principal Subject";
			asTable[2][1] = cert.getSubjectX500Principal().getName();
			ctx.info(cliUtils.getAsciiTable(null, asTable));
		}
	}

	private X509Certificate getCertificate(ItemValue<SmimeCacert> i) {
		try {
			return (X509Certificate) CertificateUtils.generateX509Certificate(i.value.cert.getBytes());
		} catch (CertificateException e) {
			ctx.error("Cannot read certificate {} because: {}", i.uid, e.getMessage());
		}
		return null;
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
