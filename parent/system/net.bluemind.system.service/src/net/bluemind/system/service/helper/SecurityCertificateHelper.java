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
package net.bluemind.system.service.helper;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.Map;
import java.util.Optional;

import org.elasticsearch.common.Strings;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.DomainSettingsKeys;
import net.bluemind.domain.api.IDomainSettings;
import net.bluemind.domain.api.IDomains;
import net.bluemind.domain.service.DomainNotFoundException;
import net.bluemind.domain.service.internal.IInCoreDomainSettings;
import net.bluemind.system.api.ISystemConfiguration;
import net.bluemind.system.api.SysConfKeys;

public class SecurityCertificateHelper {

	private final BmContext context;

	public SecurityCertificateHelper() {
		this.context = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).getContext();
	}

	public SecurityCertificateHelper(BmContext context) {
		this.context = context;
	}

	public IServiceProvider getSuProvider() {
		return context.provider();
	}

	public BmContext getContext() {
		return context;
	}

	public ItemValue<Domain> checkDomain(String domainUid) {
		if (Strings.isNullOrEmpty(domainUid)) {
			throw new ServerFault("Domain uid is mandatory");
		}

		ItemValue<Domain> domain = getDomainService().get(domainUid);
		if (domain == null || domain.value == null) {
			throw new DomainNotFoundException(domainUid);
		}

		return domain;
	}

	public IDomains getDomainService() {
		return context.provider().instance(IDomains.class);
	}

	public IInCoreDomainSettings getDomainSettingsService(String domainUid) {
		return context.provider().instance(IInCoreDomainSettings.class, domainUid);
	}

	public ISystemConfiguration getGlobalSettingsService() {
		return getSuProvider().instance(ISystemConfiguration.class);
	}

	public String getExternalUrl(String domainUid) {
		if (isGlobalVirtDomain(domainUid)) {
			return Optional
					.of(getSuProvider().instance(ISystemConfiguration.class).getValues().values
							.get(SysConfKeys.external_url.name()))
					.orElseThrow(() -> new ServerFault("External URL missing for global.virt domain"));
		} else {
			return getDomainSettingsService(domainUid).getExternalUrl().orElseThrow(
					() -> new ServerFault(String.format("External URL missing for domain '%s'", domainUid)));
		}
	}

	public Optional<String> getOtherUrls(String domainUid) {
		if (isGlobalVirtDomain(domainUid)) {
			return Optional.of(getSuProvider().instance(ISystemConfiguration.class).getValues().values
					.get(SysConfKeys.other_urls.name()));
		} else {
			return Optional
					.ofNullable(context.su().getServiceProvider().instance(IDomainSettings.class, domainUid).get()
							.get(DomainSettingsKeys.other_urls.name()))
					.map(url -> url == null || url.isEmpty() ? null : url);
		}
	}

	public String getDefaultDomain(String domainUid) {
		if (isGlobalVirtDomain(domainUid)) {
			return Optional
					.ofNullable(getSuProvider().instance(ISystemConfiguration.class).getValues().values
							.get(SysConfKeys.default_domain.name()))
					.orElseThrow(() -> new ServerFault("Unknown default domain for global.virt domain"));

		} else {
			return getDomainSettingsService(domainUid).getDefaultDomain().orElseThrow(
					() -> new ServerFault(String.format("Unknown default domain for domain '%s'", domainUid)));
		}
	}

	public String getSslCertifEngine(String domainUid) {
		if (isGlobalVirtDomain(domainUid)) {
			return getSuProvider().instance(ISystemConfiguration.class).getValues().values
					.getOrDefault(SysConfKeys.ssl_certif_engine.name(), "");
		} else {
			return getDomainSettingsService(domainUid).get().getOrDefault(DomainSettingsKeys.ssl_certif_engine.name(),
					"");
		}
	}

	public Proxy configureProxySession() {
		Map<String, String> sysConfMap = getSuProvider().instance(ISystemConfiguration.class).getValues().values;

		String proxyEnabled = sysConfMap.get(SysConfKeys.http_proxy_enabled.name());
		if (Strings.isNullOrEmpty(proxyEnabled) || !proxyEnabled.equals("true")) {
			return null;
		}

		return new Proxy(Proxy.Type.HTTP, new InetSocketAddress(sysConfMap.get(SysConfKeys.http_proxy_hostname.name()),
				Integer.valueOf(sysConfMap.get(SysConfKeys.http_proxy_port.name()))));
	}

	public static boolean isGlobalVirtDomain(String domainUid) {
		return domainUid.equals("global.virt");
	}

}
