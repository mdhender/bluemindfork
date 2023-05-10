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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.freva.asciitable.AsciiTable;
import com.github.freva.asciitable.Column;
import com.github.freva.asciitable.HorizontalAlign;

import net.bluemind.authentication.api.AuthTypes;
import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.utils.CliUtils;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomains;
import net.bluemind.openid.api.OpenIdProperties;
import net.bluemind.system.api.SysConfKeys;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "get-conf", description = "Get domains authentication configuration")
public class GetAuthConfCommand implements ICmdLet, Runnable {
	public static class Reg implements ICmdLetRegistration {
		@Override
		public Optional<String> group() {
			return Optional.of("auth");
		}

		@Override
		public Class<? extends ICmdLet> commandClass() {
			return GetAuthConfCommand.class;
		}
	}

	private static class AuthSettings {
		public final String domainUid;
		public final AuthTypes authType;
		public final Map<String, String> properties;

		public AuthSettings(String domainUid, AuthTypes authType, Map<String, String> properties) {
			this.domainUid = domainUid;
			this.authType = authType;
			this.properties = properties;
		}

		public static AuthSettings internalAuthSettings(String domainUid) {
			return new AuthSettings(domainUid, AuthTypes.INTERNAL, Collections.emptyMap());
		}
	}

	private CliContext ctx;
	private CliUtils cliUtils;

	@Option(names = "--json", required = false, defaultValue = "false", description = {
			"Display authentication configuration using Json format", "Table format otherwise" })
	public Boolean json;

	@Option(required = false, names = {
			"--domain" }, description = "Get authentication configuration from this domain UID or alias")
	public String domain;

	@Override
	public void run() {
		IDomains domainsClient = ctx.adminApi().instance(IDomains.class);
		List<AuthSettings> domainsAuthSettings = Optional.ofNullable(domain).map(cliUtils::getDomainUidByDomain)
				.map(domainsClient::get).map(Arrays::asList).orElseGet(domainsClient::all).stream()
				.filter(d -> !d.value.global).map(this::getDomainsAuthSettings).toList();

		ctx.info(json ? JsonUtils.asString(domainsAuthSettings) : domainsAsTable(domainsAuthSettings));
	}

	private String domainsAsTable(List<AuthSettings> domainsAuthSettings) {
		return AsciiTable.getTable(domainsAuthSettings,
				Arrays.asList(
						new Column().header("Domain UID").dataAlign(HorizontalAlign.LEFT)
								.with(domainAuthSettings -> domainAuthSettings.domainUid),
						new Column().header("Auth type").dataAlign(HorizontalAlign.LEFT)
								.with(domainAuthSettings -> domainAuthSettings.authType.name()),
						new Column().header("Auth properties").dataAlign(HorizontalAlign.LEFT)
								.with(domainAuthSettings -> domainAuthSettings.properties.entrySet().stream()
										.map(e -> e.getKey() + " => " + e.getValue())
										.collect(Collectors.joining("\n")))));
	}

	private AuthSettings getDomainsAuthSettings(ItemValue<Domain> domain) {
		AuthTypes authType = AuthTypes.INTERNAL;
		try {
			authType = AuthTypes.valueOf(domain.value.properties.get(SysConfKeys.auth_type.name()));
		} catch (IllegalArgumentException | NullPointerException e) {
			return AuthSettings.internalAuthSettings(domain.uid);
		}

		Set<String> authTypeProperties = Collections.emptySet();

		switch (authType) {
		case INTERNAL:
			return AuthSettings.internalAuthSettings(domain.uid);
		case CAS:
			authTypeProperties = Set.of(SysConfKeys.cas_url.name());
			break;
		case KERBEROS:
			authTypeProperties = Set.of(SysConfKeys.krb_ad_domain.name(), SysConfKeys.krb_ad_ip.name(),
					SysConfKeys.krb_keytab.name());
			break;
		case OPENID:
			authTypeProperties = Set.of(OpenIdProperties.OPENID_HOST.name(), OpenIdProperties.OPENID_CLIENT_ID.name(),
					OpenIdProperties.OPENID_CLIENT_SECRET.name());
			break;
		}

		domain.value.properties.keySet().retainAll(authTypeProperties);
		return new AuthSettings(domain.uid, authType, domain.value.properties);
	}

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		this.cliUtils = new CliUtils(ctx);
		return this;
	}
}
