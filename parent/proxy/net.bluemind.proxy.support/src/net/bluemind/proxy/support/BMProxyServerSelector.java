/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
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
package net.bluemind.proxy.support;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.asynchttpclient.Realm;
import org.asynchttpclient.Realm.AuthScheme;
import org.asynchttpclient.proxy.ProxyServer;
import org.asynchttpclient.proxy.ProxyServerSelector;
import org.asynchttpclient.proxy.ProxyType;
import org.asynchttpclient.uri.Uri;

import net.bluemind.system.api.SysConfKeys;
import net.bluemind.system.api.SystemConf;

public class BMProxyServerSelector implements ProxyServerSelector {
	private final Optional<ProxyServer> proxyServer;

	public BMProxyServerSelector(SystemConf systemConf) {
		proxyServer = initProxyServer(systemConf);
	}

	private Optional<ProxyServer> initProxyServer(SystemConf systemConf) {
		Boolean enabled = systemConf.booleanValue(SysConfKeys.http_proxy_enabled.name());
		if (enabled == null || !enabled) {
			return Optional.empty();
		}

		String hostname = systemConf.values.get(SysConfKeys.http_proxy_hostname.name());
		if (hostname == null || (hostname = hostname.trim()).isEmpty()) {
			return Optional.empty();
		}

		int port;
		try {
			port = Integer.parseInt(systemConf.values.get(SysConfKeys.http_proxy_port.name()));
		} catch (NumberFormatException nfe) {
			port = 3128;
		}

		Realm realm = getRealm(systemConf);

		Set<String> exceptions = Stream
				.concat(Arrays.asList("127.0.0.1", "localhost", "localhost.localdomain").stream(),
						systemConf.stringList(SysConfKeys.http_proxy_exceptions.name()).stream())
				.map(String::trim).filter(exception -> !exception.isEmpty()).collect(Collectors.toSet());

		return Optional.of(new ProxyServer(hostname, port, port, realm,
				exceptions.stream().collect(Collectors.toList()), ProxyType.HTTP));
	}

	private Realm getRealm(SystemConf systemConf) {
		String login = systemConf.values.get(SysConfKeys.http_proxy_login.name());
		if (login != null && (login = login.trim()).isEmpty()) {
			login = null;
		}

		String password = systemConf.values.get(SysConfKeys.http_proxy_password.name());
		if (password != null && (password = password.trim()).isEmpty()) {
			password = null;
		}

		if (login == null || password == null) {
			return null;
		}

		return new Realm.Builder(login, password).setScheme(AuthScheme.BASIC).build();
	}

	@Override
	public ProxyServer select(Uri uri) {
		return proxyServer.orElse(null);
	}
}
