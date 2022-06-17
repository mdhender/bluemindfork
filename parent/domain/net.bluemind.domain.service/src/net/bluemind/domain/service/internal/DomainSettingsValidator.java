/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.domain.service.internal;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.Regex;
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
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.system.api.ISystemConfiguration;
import net.bluemind.system.api.SysConfKeys;

public class DomainSettingsValidator {
	private final Logger logger = LoggerFactory.getLogger(DomainSettingsValidator.class);

	public void create(BmContext context, Map<String, String> settings, String domainUid) throws ServerFault {
		checkSplitDomain(settings);
		checkDomainMaxUsers(settings);
		checkDomainUrl(context, Optional.empty(),
				Optional.ofNullable(settings.get(DomainSettingsKeys.other_urls.name()))
						.map(eu -> eu.isEmpty() ? null : eu),
				Optional.ofNullable(settings.get(DomainSettingsKeys.external_url.name()))
						.map(eu -> eu.isEmpty() ? null : eu),
				domainUid);
		checkOtherDomainUrls(context,
				Optional.ofNullable(settings.get(DomainSettingsKeys.external_url.name()))
						.map(eu -> eu.isEmpty() ? null : eu),
				Optional.empty(), Optional.ofNullable(settings.get(DomainSettingsKeys.other_urls.name()))
						.map(eu -> eu.isEmpty() ? null : eu),
				domainUid);
		checkDefaultDomain(context, domainUid,
				Optional.ofNullable(settings.get(DomainSettingsKeys.default_domain.name()))
						.map(dd -> dd.isEmpty() ? null : dd));
	}

	public void update(BmContext context, Map<String, String> oldSettings, Map<String, String> newSettings,
			String domainUid) throws ServerFault {
		checkSplitDomain(newSettings);

		if (null != getRelay(oldSettings) && (null == getRelay(newSettings))) {
			List<String> externalUids = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
					.instance(IMailboxes.class, domainUid).byRouting(Routing.external);
			if (!externalUids.isEmpty()) {
				logger.error("{} Routing.external user(s)", externalUids.size());
				throw new ServerFault("Cannot unset split relay. Some mailboxes are still Routing.external",
						ErrorCode.INVALID_PARAMETER);
			}

		}

		checkDomainMaxUsers(newSettings);
		checkDomainUrl(context,
				Optional.ofNullable(newSettings.get(DomainSettingsKeys.other_urls.name()))
						.map(eu -> eu.isEmpty() ? null : eu),
				Optional.ofNullable(oldSettings.get(DomainSettingsKeys.external_url.name()))
						.map(eu -> eu.isEmpty() ? null : eu),
				Optional.ofNullable(newSettings.get(DomainSettingsKeys.external_url.name()))
						.map(eu -> eu.isEmpty() ? null : eu),
				domainUid);
		checkOtherDomainUrls(context,
				Optional.ofNullable(newSettings.get(DomainSettingsKeys.external_url.name()))
						.map(eu -> eu.isEmpty() ? null : eu),
				Optional.ofNullable(oldSettings.get(DomainSettingsKeys.other_urls.name()))
						.map(eu -> eu.isEmpty() ? null : eu),
				Optional.ofNullable(newSettings.get(DomainSettingsKeys.other_urls.name()))
						.map(eu -> eu.isEmpty() ? null : eu),
				domainUid);
		checkDefaultDomain(context, domainUid,
				Optional.ofNullable(newSettings.get(DomainSettingsKeys.default_domain.name()))
						.map(dd -> dd.isEmpty() ? null : dd));
	}

	private void checkSplitDomain(Map<String, String> settings) throws ServerFault {
		if (isForwardUnknownToRelay(settings) && null == getRelay(settings)) {
			throw new ServerFault("Split domain relay hostname cannot be empty", ErrorCode.INVALID_PARAMETER);
		}
	}

	private boolean isForwardUnknownToRelay(Map<String, String> settings) {
		return Boolean.valueOf(settings.get(DomainSettingsKeys.mail_forward_unknown_to_relay.name())).booleanValue();
	}

	private String getRelay(Map<String, String> settings) {
		boolean notNull = settings.get(DomainSettingsKeys.mail_routing_relay.name()) != null;
		boolean empty = true;
		if (notNull) {
			empty = settings.get(DomainSettingsKeys.mail_routing_relay.name()).trim().isEmpty();
		}
		if (!empty) {
			return settings.get(DomainSettingsKeys.mail_routing_relay.name());
		}
		return null;
	}

	private void checkDomainMaxUsers(Map<String, String> settings) throws ServerFault {
		String domainMaxUser = settings.get(DomainSettingsKeys.domain_max_users.name());
		if (domainMaxUser == null || domainMaxUser.isEmpty()) {
			return;
		}

		Integer maxUsers = null;
		try {
			maxUsers = Integer.parseInt(domainMaxUser);
		} catch (NumberFormatException nfe) {
			throw new ServerFault("Invalid maximum number of users. Must be an integer greater than 0.",
					ErrorCode.INVALID_PARAMETER);
		}

		if (maxUsers < 1) {
			throw new ServerFault("Invalid maximum number of users. Must be an integer greater than 0.",
					ErrorCode.INVALID_PARAMETER);
		}
	}

	private void checkDomainUrl(BmContext context, Optional<String> otherUrls, Optional<String> oldDomainUrl,
			Optional<String> domainUrl, String domainUid) {
		if (!oldDomainUrl.map(odu -> isDomainUrlUpdated(odu, domainUrl))
				.orElseGet(() -> isDomainUrlUpdated(null, domainUrl))) {
			return;
		}

		if (!context.getSecurityContext().isDomainGlobal()) {
			throw new ServerFault("Only global admin can update domain external URL", ErrorCode.FORBIDDEN);
		}

		if (!domainUrl.isPresent()) {
			if (otherUrls.isPresent()) {
				throw new ServerFault(
						String.format("Domain %s other URLs must be empty to be able to unset external URL", domainUid),
						ErrorCode.INVALID_PARAMETER);
			}

			return;
		}

		if (!domainUrl.map(Regex.DOMAIN::validate).orElse(false)) {
			throw new ServerFault(String.format("Invalid external URL '%s' for domain '%s'", domainUrl, domainUid),
					ErrorCode.INVALID_PARAMETER);
		}

		ServerSideServiceProvider provider = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);

		if (domainUrl.map(getGlobalUrls(provider)::contains).orElse(false)) {
			throw new ServerFault(
					String.format("External URL '%s' already used as global external URL", domainUrl.orElse(null)),
					ErrorCode.INVALID_PARAMETER);
		}

		Map<String, String> otherDomainsUrls = getOtherDomainsUrls(provider, domainUid);
		if (domainUrl.map(otherDomainsUrls::containsKey).orElse(false)) {
			throw new ServerFault(
					String.format("External URL '%s' already used as external URL of domain '%s'",
							domainUrl.orElse(null), otherDomainsUrls.get(domainUrl.orElse(null))),
					ErrorCode.INVALID_PARAMETER);
		}
	}

	private void checkOtherDomainUrls(BmContext context, Optional<String> externalUrl, Optional<String> oldOtherUrls,
			Optional<String> otherUrls, String domainUid) {
		if (!oldOtherUrls.map(odu -> isDomainUrlUpdated(odu, otherUrls))
				.orElseGet(() -> isDomainUrlUpdated(null, otherUrls))) {
			return;
		}

		if (!context.getSecurityContext().isDomainGlobal()) {
			throw new ServerFault("Only global admin can update domain other URL", ErrorCode.FORBIDDEN);
		}

		if (!otherUrls.isPresent()) {
			return;
		}

		if (otherUrls.isPresent() && !externalUrl.isPresent()) {
			throw new ServerFault(
					String.format("Domain %s other URLs must not be set if external URL is not set", domainUid),
					ErrorCode.INVALID_PARAMETER);
		}

		Set<String> domainUrlsSet = new HashSet<>(
				otherUrls.map(u -> Arrays.asList(u.split(" "))).orElseGet(Collections::emptyList));

		domainUrlsSet.stream().filter(url -> !Regex.DOMAIN.validate(url)).findFirst().ifPresent(iu -> {
			throw new ServerFault(String.format("Invalid external URL '%s' for domain '%s'", iu, domainUid),
					ErrorCode.INVALID_PARAMETER);
		});

		ServerSideServiceProvider provider = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
		Set<String> globalUrls = getGlobalUrls(provider);
		Map<String, String> otherDomainsUrls = getOtherDomainsUrls(provider, domainUid);

		domainUrlsSet.forEach(domainUrl -> {
			if (globalUrls.contains(domainUrl)) {
				throw new ServerFault(
						String.format("External URL '%s' already used as global external URL", otherUrls.orElse(null)),
						ErrorCode.INVALID_PARAMETER);
			}

			if (otherDomainsUrls.containsKey(domainUrl)) {
				throw new ServerFault(String.format("External URL '%s' already used as external URL of domain '%s'",
						domainUrl, otherDomainsUrls.get(domainUrl)), ErrorCode.INVALID_PARAMETER);
			}
		});
	}

	private Set<String> getGlobalUrls(ServerSideServiceProvider provider) {
		Set<String> urls = new HashSet<>();

		Optional.ofNullable(
				provider.instance(ISystemConfiguration.class).getValues().values.get(SysConfKeys.external_url.name()))
				.map(urls::add);

		Optional.ofNullable(
				provider.instance(ISystemConfiguration.class).getValues().values.get(SysConfKeys.other_urls.name()))
				.map(u -> Arrays.asList(u.split(" "))).orElseGet(Collections::emptyList).forEach(urls::add);

		return urls;
	}

	private Map<String, String> getOtherDomainsUrls(ServerSideServiceProvider provider, String domainUid) {
		Map<String, String> urls = new HashMap<>();

		provider.instance(IDomains.class).all().stream().filter(d -> !d.uid.equals(domainUid)).forEach(d -> {
			Optional.ofNullable(provider.instance(IDomainSettings.class, d.uid).get()
					.getOrDefault(DomainSettingsKeys.external_url.name(), null))
					.map(u -> urls.put(u, d.value.defaultAlias));
			Optional.ofNullable(provider.instance(IDomainSettings.class, d.uid).get()
					.getOrDefault(DomainSettingsKeys.other_urls.name(), null)).map(u -> Arrays.asList(u.split(" ")))
					.orElseGet(Collections::emptyList).forEach(u -> urls.put(u, d.value.defaultAlias));
		});

		return urls;
	}

	private boolean isDomainUrlUpdated(String oldDomainUrl, Optional<String> domainUrl) {
		if (oldDomainUrl == null && !domainUrl.isPresent()) {
			return false;
		}

		if (oldDomainUrl != null && domainUrl.isPresent()) {
			return domainUrl.map(du -> !du.equals(oldDomainUrl)).orElse(true);
		}

		return true;
	}

	private void checkDefaultDomain(BmContext context, String domainUid, Optional<String> defaultDomain) {
		if (!defaultDomain.isPresent()) {
			return;
		}

		ItemValue<Domain> domain = defaultDomain.map(dd -> getDomainItemValue(context, dd))
				.orElseThrow(() -> new ServerFault(
						String.format("default domain '%s' not found",
								defaultDomain == null ? null : defaultDomain.orElse(null)),
						ErrorCode.INVALID_PARAMETER));

		if (!domainUid.equals(domain.uid)) {
			throw new ServerFault(
					String.format("Default domain '%s' is not an alias of domain uid '%s'", defaultDomain, domainUid),
					ErrorCode.INVALID_PARAMETER);
		}
	}

	public ItemValue<Domain> getDomainItemValue(BmContext context, String domain) {
		try {
			return ServerSideServiceProvider.getProvider(context).instance(IDomains.class).findByNameOrAliases(domain);
		} catch (Exception e) {
			logger.error("unable to retrieve domain uid: {}", domain, e);
		}

		return null;
	}
}
