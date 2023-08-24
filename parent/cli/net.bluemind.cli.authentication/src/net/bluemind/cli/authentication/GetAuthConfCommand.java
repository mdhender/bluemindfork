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

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.freva.asciitable.AsciiTable;
import com.github.freva.asciitable.Column;
import com.github.freva.asciitable.HorizontalAlign;

import net.bluemind.cli.cmd.api.CliContext;
import net.bluemind.cli.cmd.api.ICmdLet;
import net.bluemind.cli.cmd.api.ICmdLetRegistration;
import net.bluemind.cli.utils.CliUtils;
import net.bluemind.core.api.auth.AuthDomainProperties;
import net.bluemind.core.api.auth.AuthTypes;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.utils.JsonUtils;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.DomainSettingsKeys;
import net.bluemind.domain.api.IDomainSettings;
import net.bluemind.domain.api.IDomains;
import net.bluemind.keycloak.api.BluemindProviderComponent;
import net.bluemind.keycloak.api.IKeycloakAdmin;
import net.bluemind.keycloak.api.IKeycloakBluemindProviderAdmin;
import net.bluemind.keycloak.api.IKeycloakClientAdmin;
import net.bluemind.keycloak.api.IKeycloakUids;
import net.bluemind.keycloak.api.OidcClient;
import net.bluemind.keycloak.api.Realm;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "get-conf", description = "Get domain authentication configurations")
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
		public final String authType;
		public final Map<String, String> properties;

		public AuthSettings(String domainUid, String authType, Map<String, String> properties) {
			this.domainUid = domainUid;
			this.authType = authType;
			this.properties = properties;
		}

		public static AuthSettings invalid(String domainUid, String errorMessage) {
			return new AuthSettings(domainUid, "INVALID", Map.of("Error", errorMessage));
		}

		public static AuthSettings internal(boolean jsonOutput, String domainUid, Realm realm,
				BluemindProviderComponent component, OidcClient client) {
			return new AuthSettings(domainUid, AuthTypes.INTERNAL.name(),
					realmProperties(jsonOutput, realm, component, client));
		}

		public static AuthSettings kerberos(boolean jsonOutput, String domainUid, Realm realm) {
			return new AuthSettings(domainUid, AuthTypes.KERBEROS.name(),
					realmProperties(jsonOutput, realm, null, null));
		}

		private static Map<String, String> realmProperties(boolean jsonOutput, Realm realm,
				BluemindProviderComponent component, OidcClient client) {
			Map<String, String> properties = new LinkedHashMap<>();
			if (realm == null) {
				properties.put("realmStatus", "realm not found!");
			} else {
				properties.put("id", realm.id);
				properties.put("realm", realm.realm);
				properties.put("enabled", Boolean.toString(realm.enabled));
				properties.put("accessCodeLifespanLogin", //
						jsonOutput ? String.valueOf(realm.accessCodeLifespanLogin)
								: secondsToString(realm.accessCodeLifespanLogin));
				properties.put("accessTokenLifespan", jsonOutput ? String.valueOf(realm.accessTokenLifespan)
						: secondsToString(realm.accessTokenLifespan));
				properties.put("ssoSessionMaxLifespan", jsonOutput ? String.valueOf(realm.ssoSessionMaxLifespan)
						: secondsToString(realm.ssoSessionMaxLifespan));
				properties.put("ssoSessionIdleTimeout", jsonOutput ? String.valueOf(realm.ssoSessionIdleTimeout)
						: secondsToString(realm.ssoSessionIdleTimeout));
			}

			if (component == null) {
				properties.put("componentStatus", "Keycloak BlueMind component not found!");
			} else {
				properties.put("componentName", component.name);
				properties.put("componentBmUrl", component.bmUrl);
			}

			if (client == null) {
				properties.put("clientStatus", "Keycloak BlueMind client not found!");
			} else {
				properties.put("clientId", client.id);
				properties.put("clientSecret", client.secret);
			}

			return properties;
		}

		private static String secondsToString(long seconds) {
			Duration duration = Duration.ofSeconds(seconds);

			String durationAsString = "";
			if (duration.toDaysPart() > 0) {
				durationAsString += duration.toDaysPart() + " days ";
			}

			if (duration.toHoursPart() > 0) {
				durationAsString += duration.toHoursPart() + " hours ";
			}

			if (duration.toMinutesPart() > 0) {
				durationAsString += duration.toMinutesPart() + " minutes";
			}

			return durationAsString;
		}
	}

	private CliContext ctx;
	private CliUtils cliUtils;

	@Option(names = "--json", required = false, defaultValue = "false", description = {
			"Display authentication configuration using Json format", "Table format otherwise" })
	public boolean json;

	@Option(required = false, names = {
			"--domain" }, description = "Get authentication configuration from this domain UID or alias")
	public String domain;

	@Override
	public void run() {
		IDomains domainsClient = ctx.adminApi().instance(IDomains.class);
		List<AuthSettings> domainsAuthSettings = Optional.ofNullable(domain) //
				.map(cliUtils::getDomainUidByDomain).map(domainsClient::get).map(Arrays::asList) //
				.orElseGet(domainsClient::all).stream() //
				.filter(d -> !d.value.global).map(this::getDomainsAuthSettings).toList();

		ctx.info(json ? JsonUtils.asString(domainsAuthSettings) : domainsAsTable(domainsAuthSettings));
	}

	private String domainsAsTable(List<AuthSettings> domainsAuthSettings) {
		return AsciiTable.getTable(domainsAuthSettings,
				Arrays.asList(
						new Column().header("Domain UID").dataAlign(HorizontalAlign.LEFT)
								.with(domainAuthSettings -> domainAuthSettings.domainUid),
						new Column().header("Auth type").dataAlign(HorizontalAlign.LEFT)
								.with(domainAuthSettings -> domainAuthSettings.authType),
						new Column().header("Auth properties").dataAlign(HorizontalAlign.LEFT)
								.with(domainAuthSettings -> domainAuthSettings.properties.entrySet().stream()
										.map(e -> e.getKey() + ": " + e.getValue())
										.collect(Collectors.joining("\n")))));
	}

	private AuthSettings getDomainsAuthSettings(ItemValue<Domain> domain) {
		AuthTypes authType = AuthTypes.INTERNAL;
		try {
			authType = AuthTypes.valueOf(domain.value.properties.get(AuthDomainProperties.AUTH_TYPE.name()));
		} catch (IllegalArgumentException | NullPointerException e) {
			return AuthSettings.invalid(domain.uid, "Null or invalid AUTH_TYPE propery: '"
					+ domain.value.properties.get(AuthDomainProperties.AUTH_TYPE.name()) + "'");
		}

		Set<String> authTypeProperties = Collections.emptySet();
		switch (authType) {
		case INTERNAL:
			return internal(domain);
		case CAS:
			authTypeProperties = Set.of(AuthDomainProperties.CAS_URL.name());
			break;
		case KERBEROS:
			return kerberos(domain);
		case OPENID:
			authTypeProperties = Set.of(AuthDomainProperties.OPENID_HOST.name(),
					AuthDomainProperties.OPENID_CLIENT_ID.name(), AuthDomainProperties.OPENID_CLIENT_SECRET.name());
			break;
		}

		domain.value.properties.keySet().retainAll(authTypeProperties);
		return new AuthSettings(domain.uid, authType.name(), domain.value.properties);
	}

	private String getKeycloakDomainId(ItemValue<Domain> domain) {
		return ctx.adminApi().instance(IDomainSettings.class, domain.uid).get()
				.get(DomainSettingsKeys.external_url.name()) != null ? domain.uid : "global.virt";
	}

	private AuthSettings internal(ItemValue<Domain> domain) {
		String keycloakDomainId = getKeycloakDomainId(domain);

		Realm realm = ctx.adminApi().instance(IKeycloakAdmin.class, domain.uid).getRealm(keycloakDomainId);
		BluemindProviderComponent component = ctx.adminApi()
				.instance(IKeycloakBluemindProviderAdmin.class, keycloakDomainId)
				.getBluemindProvider(IKeycloakUids.bmProviderId(keycloakDomainId));
		OidcClient client = ctx.adminApi().instance(IKeycloakClientAdmin.class, keycloakDomainId)
				.getOidcClient(IKeycloakUids.clientId(keycloakDomainId));

		return AuthSettings.internal(json, domain.uid, realm, component, client);
	}

	private AuthSettings kerberos(ItemValue<Domain> domain) {
		String keycloakDomainId = getKeycloakDomainId(domain);

		Realm realm = ctx.adminApi().instance(IKeycloakAdmin.class, domain.uid).getRealm(keycloakDomainId);
		return AuthSettings.kerberos(json, domain.uid, realm);
	}

	@Override
	public Runnable forContext(CliContext ctx) {
		this.ctx = ctx;
		this.cliUtils = new CliUtils(ctx);
		return this;
	}
}
