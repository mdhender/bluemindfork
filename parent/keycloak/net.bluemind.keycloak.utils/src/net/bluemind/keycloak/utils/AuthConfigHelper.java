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
package net.bluemind.keycloak.utils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import com.google.common.base.Strings;

import net.bluemind.core.api.auth.AuthDomainProperties;
import net.bluemind.core.api.auth.AuthTypes;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.DomainSettingsKeys;
import net.bluemind.domain.api.IDomainSettings;
import net.bluemind.domain.api.IDomains;

public class AuthConfigHelper {

	private AuthConfigHelper() {

	}

	public static void checkDomain(BmContext context, Domain domain, boolean create) {

		if (create) {
			checkCas(context, domain, Collections.emptyMap());
		} else {
			String domainUid = getDomainUid(context, domain);
			Map<String, String> settings = ServerSideServiceProvider.getProvider(context.getSecurityContext())
					.instance(IDomainSettings.class, domainUid).get();
			checkKerberos(context, domain, settings);
			checkCas(context, domain, settings);
			checkExternal(domain, settings);
		}

	}

	public static void checkSettings(BmContext context, String domainUid, Map<String, String> settings) {
		Domain domain = ServerSideServiceProvider.getProvider(context.getSecurityContext()).instance(IDomains.class)
				.get(domainUid).value;
		checkKerberos(context, domain, settings);
		checkCas(context, domain, settings);
		checkExternal(domain, settings);
	}

	private static void checkKerberos(BmContext context, Domain domain, Map<String, String> settings) {
		// If no kerb let go
		String authType = domain.properties == null ? null
				: domain.properties.get(AuthDomainProperties.AUTH_TYPE.name());
		if (!AuthTypes.KERBEROS.name().equals(authType)) {
			return;
		}

		// external url mandatory if another Kerberos domain without external url exists
		if (settings != null && settings.get(DomainSettingsKeys.external_url.name()) == null) {
			IDomains domainService = ServerSideServiceProvider.getProvider(context.getSecurityContext())
					.instance(IDomains.class);

			Optional<ItemValue<Domain>> kerberosDomain = domainService.all().stream()
					.filter(d -> !d.value.name.equals(domain.name)
							&& AuthTypes.KERBEROS.name()
									.equals(d.value.properties.get(AuthDomainProperties.AUTH_TYPE.name()))
							&& !domainHasExternalUrl(d.uid))
					.findFirst();

			if (kerberosDomain.isPresent()) {
				throw new ServerFault(
						"External Url is mandatory to enable Kerberos. Only one domain can have kerberos enabled without an external url, which is the case for "
								+ kerberosDomain.get().value.defaultAlias,
						ErrorCode.INVALID_AUTH_PARAMETER);
			}
		}

		// kerb params mandatory
		if (domain.properties.get(AuthDomainProperties.KRB_AD_DOMAIN.name()) == null) {
			throw new ServerFault("AD Domain is mandatory for kerberos configuration",
					ErrorCode.INVALID_AUTH_PARAMETER);
		}
		if (domain.properties.get(AuthDomainProperties.KRB_AD_IP.name()) == null) {
			throw new ServerFault("AD IP adress is mandatory for kerberos configuration",
					ErrorCode.INVALID_AUTH_PARAMETER);
		}
		if (domain.properties.get(AuthDomainProperties.KRB_KEYTAB.name()) == null) {
			throw new ServerFault("Keytab file is mandatory for kerberos configuration",
					ErrorCode.INVALID_AUTH_PARAMETER);
		}
	}

	private static void checkCas(BmContext context, Domain domain, Map<String, String> settings) {
		String authType = domain.properties == null ? null
				: domain.properties.get(AuthDomainProperties.AUTH_TYPE.name());
		boolean isCas = AuthTypes.CAS.name().equals(authType);
		boolean hasExternalUrl = settings != null && settings.containsKey(DomainSettingsKeys.external_url.name());

		IDomains domainService = ServerSideServiceProvider.getProvider(context.getSecurityContext())
				.instance(IDomains.class);

		// if no external URL, all other CAS domain should have one
		if (!hasExternalUrl) {
			Optional<ItemValue<Domain>> casDomainWithoutExternalUrl = domainService.all().stream()
					.filter(d -> !"global.virt".equals(d.value.name) && !d.value.name.equals(domain.name)
							&& AuthTypes.CAS.name()
									.equals(d.value.properties.get(AuthDomainProperties.AUTH_TYPE.name()))
							&& !domainHasExternalUrl(d.uid))
					.findFirst();

			if (casDomainWithoutExternalUrl.isPresent()) {
				throw new ServerFault(
						"Operation is forbidden, because of the presence of a CAS domain without an external_url ("
								+ casDomainWithoutExternalUrl.get().value.defaultAlias + ")",
						ErrorCode.INVALID_AUTH_PARAMETER);
			}

		}

		// if CAS and no external_url is set, all other domains should have one
		if (isCas && !hasExternalUrl) {
			Optional<ItemValue<Domain>> otherDomainWithoutExternalUrl = domainService.all().stream()
					.filter(d -> !"global.virt".equals(d.value.name) && !d.value.name.equals(domain.name)
							&& !domainHasExternalUrl(d.uid))
					.findFirst();

			if (otherDomainWithoutExternalUrl.isPresent()) {
				throw new ServerFault(
						"Operation is forbidden, because of the presence of a domain without an external_url ("
								+ otherDomainWithoutExternalUrl.get().value.defaultAlias + ")",
						ErrorCode.INVALID_AUTH_PARAMETER);
			}

		}

		if (isCas) {
			// cas url mandatory and ending with /
			String casUrl = domain.properties.get(AuthDomainProperties.CAS_URL.name());
			if (casUrl == null || casUrl.trim().isEmpty()) {
				throw new ServerFault("CAS server URL is mandatory for CAS configuration",
						ErrorCode.INVALID_AUTH_PARAMETER);
			}
			try {
				new URL(casUrl);
			} catch (MalformedURLException e) {
				throw new ServerFault("CAS server URL must be a valid http URL ending with a '/'",
						ErrorCode.INVALID_AUTH_PARAMETER);
			}
			if (!casUrl.startsWith("http") || !casUrl.endsWith("/")) {
				throw new ServerFault("CAS server URL must be a valid http URL ending with a '/'",
						ErrorCode.INVALID_AUTH_PARAMETER);
			}
		}
	}

	private static void checkExternal(Domain domain, Map<String, String> settings) {
		String authType = domain.properties == null ? null
				: domain.properties.get(AuthDomainProperties.AUTH_TYPE.name());
		if (!AuthTypes.OPENID.name().equals(authType)) {
			return;
		}

		if (settings == null || settings.get(DomainSettingsKeys.external_url.name()) == null
				|| settings.get(DomainSettingsKeys.external_url.name()).trim().isEmpty()) {
			throw new ServerFault("External_url is mandatory for a domain with external authentication",
					ErrorCode.INVALID_AUTH_PARAMETER);
		}

		// external auth params mandatory
		if (domain.properties.get(AuthDomainProperties.OPENID_HOST.name()) == null) {
			throw new ServerFault("OpenId configuration URL is mandatory for external authentication configuration",
					ErrorCode.INVALID_AUTH_PARAMETER);
		}
		if (domain.properties.get(AuthDomainProperties.OPENID_CLIENT_ID.name()) == null) {
			throw new ServerFault("Client ID is mandatory for external authentication configuration",
					ErrorCode.INVALID_AUTH_PARAMETER);
		}
		if (domain.properties.get(AuthDomainProperties.OPENID_CLIENT_SECRET.name()) == null) {
			throw new ServerFault("Client secret is mandatory for external authentication configuration",
					ErrorCode.INVALID_AUTH_PARAMETER);
		}
	}

	private static String getDomainUid(BmContext context, Domain domain) {
		ItemValue<Domain> ret = ServerSideServiceProvider.getProvider(context.getSecurityContext())
				.instance(IDomains.class).findByNameOrAliases(domain.name);
		if (ret == null) {
			return null;
		}

		return ret.uid;
	}

	private static boolean domainHasExternalUrl(String domainUid) {
		String externalUrl = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDomainSettings.class, domainUid).get().get(DomainSettingsKeys.external_url.name());
		return !Strings.isNullOrEmpty(externalUrl);
	}
}
