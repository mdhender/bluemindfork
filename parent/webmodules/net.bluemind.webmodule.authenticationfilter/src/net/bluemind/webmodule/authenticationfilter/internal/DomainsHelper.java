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
package net.bluemind.webmodule.authenticationfilter.internal;

import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.http.HttpServerRequest;
import net.bluemind.core.api.auth.AuthDomainProperties;
import net.bluemind.core.api.auth.AuthTypes;
import net.bluemind.domain.api.DomainSettingsKeys;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.hornetq.client.MQ.SharedMap;
import net.bluemind.hornetq.client.Shared;
import net.bluemind.system.api.SysConfKeys;

public class DomainsHelper {
	private static final Logger logger = LoggerFactory.getLogger(DomainsHelper.class);

	/**
	 * Get domain UID from request host
	 * 
	 * Search from request host in domain external URL or other URLs, including
	 * global URLs
	 * 
	 * @param request
	 * @return domain UID matching request host if found, emtpy otherwise
	 */
	public static String getDomainUid(HttpServerRequest request) {
		SharedMap<String, Map<String, String>> all = MQ.sharedMap(Shared.MAP_DOMAIN_SETTINGS);

		// Look for host in domains external_url
		Iterator<String> it = all.keys().iterator();
		while (it.hasNext()) {
			String domainUid = it.next();
			Map<String, String> values = all.get(domainUid);
			String extUrl = values.get(DomainSettingsKeys.external_url.name());
			if (request.host().equalsIgnoreCase(extUrl)) {
				return domainUid;
			}
		}

		// Look for host in domains other_urls
		it = all.keys().iterator();
		while (it.hasNext()) {
			String domainUid = it.next();
			Map<String, String> values = all.get(domainUid);
			String otherUrls = values.get(DomainSettingsKeys.other_urls.name());
			if (otherUrls != null) {
				StringTokenizer tokenizer = new StringTokenizer(otherUrls.trim(), " ");
				while (tokenizer.hasMoreElements()) {
					if (request.host().equalsIgnoreCase(tokenizer.nextToken())) {
						return domainUid;
					}
				}
			}
		}

		// Look for a CAS domain without external_url (no matter the request host)
		it = all.keys().iterator();
		while (it.hasNext()) {
			String domainUid = it.next();
			Map<String, String> values = all.get(domainUid);
			String authType = values.get(AuthDomainProperties.AUTH_TYPE.name());
			if (AuthTypes.CAS.name().equals(authType)) {
				String extUrl = values.get(DomainSettingsKeys.external_url.name());
				if (extUrl == null || extUrl.trim().isEmpty()) {
					return domainUid;
				}
			}
		}

		// Look for host in global external_url
		SharedMap<String, String> sysconf = MQ.sharedMap(Shared.MAP_SYSCONF);
		if (request.host().equalsIgnoreCase(sysconf.get(SysConfKeys.external_url.name()))) {
			return "global.virt";
		}

		// Look for host in global other_urls
		String otherUrls = sysconf.get(SysConfKeys.other_urls.name());
		if (otherUrls != null) {
			StringTokenizer tokenizer = new StringTokenizer(otherUrls.trim(), " ");
			while (tokenizer.hasMoreElements()) {
				if (request.host().equalsIgnoreCase(tokenizer.nextToken())) {
					return "global.virt";
				}
			}
		}

		if (logger.isWarnEnabled()) {
			logger.warn("No BlueMind domain found for request: {}", request.absoluteURI());
		}
		return "global.virt";
	}
}
