/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
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
package net.bluemind.cli.authentication;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.Base64;
import java.util.Optional;

import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.CliException;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.utils.CliUtils;
import net.bluemind.core.api.auth.AuthDomainProperties;
import net.bluemind.core.api.auth.AuthTypes;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomains;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Spec;

@Command(name = "set-conf", description = "Set domain authentication configuration")
public class SetAuthConfCommand implements ICmdLet, Runnable {
	public static class Reg implements ICmdLetRegistration {
		@Override
		public Optional<String> group() {
			return Optional.of("auth");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return SetAuthConfCommand.class;
		}
	}

	@Spec
	private static CommandSpec spec;

	@Option(required = true, names = {
			"--domain" }, description = "Set authentication configuration for this domain UID or alias")
	public String domain;

	@ArgGroup(exclusive = true, multiplicity = "1")
	private Scope scope;

	private static class Scope {
		@ArgGroup(exclusive = false, heading = "Set internal authentication%n")
		AuthInternal internal;
		@ArgGroup(exclusive = false, heading = "Set CAS authentication%n")
		AuthCas cas;
		@ArgGroup(exclusive = false, heading = "Set Kerberos authentication%n")
		AuthKerberos kerberos;
		@ArgGroup(exclusive = false, heading = "Set third-party OpenID authentication server%n")
		AuthOpenId openId;
	}

	private static class AuthInternal {
		@Option(required = true, names = { "--internal" }, description = "Enable internal authentication")
		public Boolean internal;
	}

	private static class AuthCas {
		public URL casUrl;

		@Option(required = true, names = { "--cas-url" }, description = "CAS server http(s) URL ending with /")
		public void setCasUrl(URL url) {
			if (url == null || !url.toString().endsWith("/")) {
				throw new ParameterException(spec.commandLine(),
						"CAS URL should be a valid http(s) url and end with /");
			}

			this.casUrl = url;
		}

		public void enable(ItemValue<Domain> domain) {
			domain.value.properties.put(AuthDomainProperties.AUTH_TYPE.name(), AuthTypes.CAS.name());
			domain.value.properties.put(AuthDomainProperties.CAS_URL.name(), casUrl.toString());
		}

		public static void disable(ItemValue<Domain> domain) {
			domain.value.properties.remove(AuthDomainProperties.AUTH_TYPE.name());
			domain.value.properties.remove(AuthDomainProperties.CAS_URL.name());
		}
	}

	private static class AuthKerberos {
		public String krbAdDomain;

		@Option(required = true, names = { "--krb-ad-domain" }, paramLabel = "AD.DOMAIN.TLD", description = {
				"Active directory kerberos domain", "Upper case only" })
		public void setKrbAdDomain(String krbAdDomain) {
			if (!krbAdDomain.equals(krbAdDomain.toUpperCase())) {
				throw new ParameterException(spec.commandLine(),
						"Active directory kerberos domain must be in upper case");
			}

			this.krbAdDomain = krbAdDomain;
		}

		@Option(required = true, names = { "--krb-ad-ip" }, description = "Active directory server IP or FQDN")
		public String krbAdIp;

		@ArgGroup(exclusive = true, multiplicity = "1")
		public Keytab keytab;

		private static class Keytab {
			@Option(required = false, names = {
					"--krb-keytab" }, description = "Base64 encoded Active directory keytab content")
			public String base64;

			public File file;

			@Option(required = false, names = { "--krb-keytab-file" }, description = "Path to Active directory keytab")
			public void setKeytabFile(File file) {
				if (file == null || !file.exists()) {
					throw new ParameterException(spec.commandLine(), "Keytab file must exists and readable");
				}

				this.file = file;
			}

			private String loadFromFile() {
				try {
					return Base64.getEncoder().encodeToString(Files.readAllBytes(file.toPath()));
				} catch (IOException e) {
					throw new CliException("Could not read keytab file " + file);
				}
			}
		}

		public void enable(ItemValue<Domain> domain) {
			domain.value.properties.put(AuthDomainProperties.AUTH_TYPE.name(), AuthTypes.KERBEROS.name());
			domain.value.properties.put(AuthDomainProperties.KRB_AD_DOMAIN.name(), krbAdDomain);
			domain.value.properties.put(AuthDomainProperties.KRB_AD_IP.name(), krbAdIp);
			domain.value.properties.put(AuthDomainProperties.KRB_KEYTAB.name(),
					Optional.ofNullable(keytab.base64).orElseGet(keytab::loadFromFile));
		}

		public static void disable(ItemValue<Domain> domain) {
			domain.value.properties.remove(AuthDomainProperties.KRB_AD_DOMAIN.name());
			domain.value.properties.remove(AuthDomainProperties.KRB_AD_IP.name());
			domain.value.properties.remove(AuthDomainProperties.KRB_KEYTAB.name());
		}
	}

	private static class AuthOpenId {
		@Option(required = true, names = { "--openid-server-url" }, description = "OpenId third-party server URL")
		public String openIdServerUrl;
		@Option(required = true, names = { "--openid-client-id" }, description = "OpenId client ID")
		public String openIdClientId;
		@Option(required = true, names = { "--openid-client-secret" }, description = "OpenId client secret")
		public String openIdClientSecret;

		public void enable(ItemValue<Domain> domain) {
			domain.value.properties.put(AuthDomainProperties.AUTH_TYPE.name(), AuthTypes.OPENID.name());
			domain.value.properties.put(AuthDomainProperties.OPENID_HOST.name(), openIdServerUrl);
			domain.value.properties.put(AuthDomainProperties.OPENID_CLIENT_ID.name(), openIdClientId);
			domain.value.properties.put(AuthDomainProperties.OPENID_CLIENT_SECRET.name(), openIdClientSecret);
		}

		public static void disable(ItemValue<Domain> domain) {
			domain.value.properties.remove(AuthDomainProperties.OPENID_HOST.name());
			domain.value.properties.remove(AuthDomainProperties.OPENID_CLIENT_ID.name());
			domain.value.properties.remove(AuthDomainProperties.OPENID_CLIENT_SECRET.name());
		}
	}

	private CliContext ctx;
	private CliUtils cliUtils;

	@Override
	public void run() {
		IDomains domainClient = ctx.adminApi().instance(IDomains.class);
		ItemValue<Domain> domainItem = Optional.ofNullable(cliUtils.getDomainUidByDomain(domain))
				.map(uid -> domainClient.get(uid)).filter(d -> !d.value.global)
				.orElseThrow(() -> new CliException("Domain must not be global"));

		Optional.ofNullable(scope.internal).ifPresent(
				i -> domainItem.value.properties.put(AuthDomainProperties.AUTH_TYPE.name(), AuthTypes.INTERNAL.name()));
		Optional.ofNullable(scope.cas).ifPresentOrElse(s -> s.enable(domainItem), () -> AuthCas.disable(domainItem));
		Optional.ofNullable(scope.kerberos).ifPresentOrElse(s -> s.enable(domainItem),
				() -> AuthKerberos.disable(domainItem));
		Optional.ofNullable(scope.openId).ifPresentOrElse(s -> s.enable(domainItem),
				() -> AuthOpenId.disable(domainItem));

		try {
			domainClient.update(domainItem.uid, domainItem.value);
		} catch (ServerFault sf) {
			throw new CliException(sf.getMessage());
		}
	}

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		this.cliUtils = new CliUtils(ctx);
		return this;
	}
}
