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
import java.util.Iterator;
import java.util.Map;

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
	public static void checkDomain(BmContext context, Domain domain, boolean create) {
		String domainUid = getDomainUid(context, domain);

		Map<String, String> settings = null;
		if (domainUid != null) {
			try {
				settings = ServerSideServiceProvider.getProvider(context.getSecurityContext())
						.instance(IDomainSettings.class, domainUid).get();
			} catch (Throwable t) {
			}
		}
		if (create) {
			checkCas(context, domain, settings);
		} else {
			checkKerberos(context, domain, settings);
			checkCas(context, domain, settings);
			checkExternal(context, domain, settings);
		}

	}

	public static void checkSettings(BmContext context, String domainUid, Map<String, String> settings) {
		Domain domain = ServerSideServiceProvider.getProvider(context.getSecurityContext()).instance(IDomains.class)
				.get(domainUid).value;
		checkKerberos(context, domain, settings);
		checkCas(context, domain, settings);
		checkExternal(context, domain, settings);
	}

	private static void checkKerberos(BmContext context, Domain domain, Map<String, String> settings) {
		IDomains domainService = ServerSideServiceProvider.getProvider(context.getSecurityContext())
				.instance(IDomains.class);

		// If no kerb let go
		String authType = domain.properties == null ? null
				: domain.properties.get(AuthDomainProperties.AUTH_TYPE.name());
		if (!AuthTypes.KERBEROS.name().equals(authType)) {
			return;
		}

		// external url mandatory if another kerb domain without external url exists
		String extUrl = settings.get(DomainSettingsKeys.external_url.name());
		if (extUrl == null) {
			domainService.all().forEach(d -> {
				Domain currDomain = d.value;
				if (!currDomain.name.equals(domain.name)
						&& AuthTypes.KERBEROS.name()
								.equals(currDomain.properties.get(AuthDomainProperties.AUTH_TYPE.name()))
						&& getExternalUrl(context, currDomain.name) == null) {
					throw new ServerFault(
							"External Url is mandatory to enable Kerberos. Only one domain can have kerberos enabled without an external url, which is the case for "
									+ currDomain.defaultAlias + ".",
							ErrorCode.INVALID_PARAMETER);
				}
			});
		}

		// kerb params mandatory
		if (domain.properties.get(AuthDomainProperties.KRB_AD_DOMAIN.name()) == null) {
			throw new ServerFault("AD Domain is mandatory for kerberos configuration", ErrorCode.INVALID_PARAMETER);
		}
		if (domain.properties.get(AuthDomainProperties.KRB_AD_IP.name()) == null) {
			throw new ServerFault("AD IP adress is mandatory for kerberos configuration", ErrorCode.INVALID_PARAMETER);
		}
		if (domain.properties.get(AuthDomainProperties.KRB_KEYTAB.name()) == null) {
			throw new ServerFault("Keytab file is mandatory for kerberos configuration", ErrorCode.INVALID_PARAMETER);
		}
	}

	private static void checkCas(BmContext context, Domain domain, Map<String, String> settings) {
		String domainUid = getDomainUid(context, domain);
		IDomains domainService = ServerSideServiceProvider.getProvider(context.getSecurityContext())
				.instance(IDomains.class);

		String casDomain = null;
		String secondDomain = null;
		int nbCasWithoutExtUrl = 0;
		int nbTotWithoutExtUrl = 0;
		Iterator<ItemValue<Domain>> it = domainService.all().iterator();
		while (it.hasNext()) {
			ItemValue<Domain> dom = it.next();
			if (!dom.uid.equals("global.virt")) {
				Map<String, String> currSettings;
				Map<String, String> currProperties;
				if (domain.defaultAlias.equals(dom.value.defaultAlias)) {
					currSettings = settings;
					currProperties = domain.properties;
				} else {
					currSettings = ServerSideServiceProvider.getProvider(context.getSecurityContext())
							.instance(IDomainSettings.class, dom.uid).get();
					currProperties = dom.value.properties;
				}

				String externalUrl = currSettings == null ? null
						: currSettings.get(DomainSettingsKeys.external_url.name());
				if (externalUrl == null || externalUrl.trim().isEmpty()) {
					nbTotWithoutExtUrl++;
					if (currProperties != null
							&& AuthTypes.CAS.name().equals(currProperties.get(AuthDomainProperties.AUTH_TYPE.name()))) {
						if (domainUid == null) {
							throw new ServerFault(
									"Domain creation is forbidden, because of the presence of a CAS domain without an external_url ("
											+ dom.value.defaultAlias
											+ "). Set an external_url to that domain to enable back domain creation.",
									ErrorCode.INVALID_PARAMETER);
						}
						nbCasWithoutExtUrl++;
						casDomain = dom.value.defaultAlias;
					}
					if (!dom.value.defaultAlias.equals(casDomain)) {
						secondDomain = dom.value.defaultAlias;
					}
				}
			}
		}

		if (nbCasWithoutExtUrl > 0 && nbTotWithoutExtUrl > 1) {
			if (domain.defaultAlias.equals(casDomain)) {
				throw new ServerFault(
						"Can't have a CAS domain without an external_url, because a domain without an external_url already exists ("
								+ secondDomain + ")",
						ErrorCode.INVALID_PARAMETER);
			} else {
				throw new ServerFault(
						"Can't have a domain without an external_url, because a CAS domain without an external_url already exists ("
								+ casDomain + ")",
						ErrorCode.INVALID_PARAMETER);
			}
		}

		// cas url mandatory and ending with /
		if (domain.properties != null
				&& AuthTypes.CAS.name().equals(domain.properties.get(AuthDomainProperties.AUTH_TYPE.name()))) {
			String casUrl = domain.properties.get(AuthDomainProperties.CAS_URL.name());
			if (casUrl == null || casUrl.trim().isEmpty()) {
				throw new ServerFault("CAS server URL is mandatory for CAS configuration", ErrorCode.INVALID_PARAMETER);
			}
			try {
				new URL(casUrl);
			} catch (MalformedURLException e) {
				throw new ServerFault("CAS server URL must be a valid http URL ending with a '/'",
						ErrorCode.INVALID_PARAMETER);
			}
			if (!casUrl.startsWith("http") || !casUrl.endsWith("/")) {
				throw new ServerFault("CAS server URL must be a valid http URL ending with a '/'",
						ErrorCode.INVALID_PARAMETER);
			}
		}
	}

	private static void checkExternal(BmContext context, Domain domain, Map<String, String> settings) {
		if (domain.properties != null
				&& AuthTypes.OPENID.name().equals(domain.properties.get(AuthDomainProperties.AUTH_TYPE.name()))) {
			if (settings == null || settings.get(DomainSettingsKeys.external_url.name()) == null
					|| settings.get(DomainSettingsKeys.external_url.name()).trim().isEmpty()) {
				throw new ServerFault("External_url is mandatory for a domain with external authentication",
						ErrorCode.INVALID_PARAMETER);
			}

			// external auth params mandatory
			if (domain.properties.get(AuthDomainProperties.OPENID_HOST.name()) == null) {
				throw new ServerFault("OpenId configuration URL is mandatory for external authentication configuration",
						ErrorCode.INVALID_PARAMETER);
			}
			if (domain.properties.get(AuthDomainProperties.OPENID_CLIENT_ID.name()) == null) {
				throw new ServerFault("Client ID is mandatory for external authentication configuration",
						ErrorCode.INVALID_PARAMETER);
			}
			if (domain.properties.get(AuthDomainProperties.OPENID_CLIENT_SECRET.name()) == null) {
				throw new ServerFault("Client secret is mandatory for external authentication configuration",
						ErrorCode.INVALID_PARAMETER);
			}
		}
	}

	private static String getDomainUid(BmContext context, Domain domain) {
		String res = null;
		Iterator<ItemValue<Domain>> it = ServerSideServiceProvider.getProvider(context.getSecurityContext())
				.instance(IDomains.class).all().iterator();
		while (it.hasNext() && res == null) {
			ItemValue<Domain> d = it.next();
			if (domain.name.equals(d.value.name)) {
				res = d.uid;
			}
		}
		return res;
	}

	private static String getExternalUrl(BmContext context, String domainUid) {
		return ServerSideServiceProvider
				.getProvider(context != null ? context.getSecurityContext() : SecurityContext.SYSTEM)
				.instance(IDomainSettings.class, domainUid).get().get(DomainSettingsKeys.external_url.name());
	}
}
