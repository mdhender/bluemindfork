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
package net.bluemind.system.config;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.asynchttpclient.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import net.bluemind.config.InstallationId;
import net.bluemind.core.api.ParametersValidator;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.rest.BmContext;
import net.bluemind.node.api.NodeActivator;
import net.bluemind.proxy.support.AHCWithProxy;
import net.bluemind.server.api.IServer;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.system.api.SystemConf;
import net.bluemind.system.hook.ISystemConfigurationObserver;
import net.bluemind.system.hook.ISystemConfigurationSanitizor;
import net.bluemind.system.hook.ISystemConfigurationValidator;

public class HttpProxyHook
		implements ISystemConfigurationSanitizor, ISystemConfigurationValidator, ISystemConfigurationObserver {
	private static final Logger logger = LoggerFactory.getLogger(HttpProxyHook.class);

	private static final String BO_PING_URL = "https://bo.bluemind.net/bo4/ping";
	private static final String DEFAULT_PORT = "3128";
	private static final String PROXYVARS = "/etc/bm/proxy-vars";

	private static final List<SysConfKeys> proxySysconfKeys = Arrays.asList(SysConfKeys.http_proxy_enabled,
			SysConfKeys.http_proxy_hostname, SysConfKeys.http_proxy_port, SysConfKeys.http_proxy_login,
			SysConfKeys.http_proxy_password, SysConfKeys.http_proxy_exceptions);

	@Override
	public void validate(SystemConf previous, Map<String, String> modifications) throws ServerFault {
		String proxyEnabled = getValue(SysConfKeys.http_proxy_enabled.name(), previous, modifications);
		if (!Boolean.parseBoolean(proxyEnabled)) {
			return;
		}

		String proxyHostname = getValue(SysConfKeys.http_proxy_hostname.name(), previous, modifications);
		if (Strings.isNullOrEmpty(proxyHostname)) {
			throw new ServerFault("Proxy hostname must be defined", ErrorCode.INVALID_PARAMETER);
		}

		String proxyPort = getValue(SysConfKeys.http_proxy_port.name(), previous, modifications);
		try {
			int port = Integer.parseInt(proxyPort);
			if (port < 1 || port > 65535) {
				throw new ServerFault("Proxy port must be an integer between 1 and 65535", ErrorCode.INVALID_PARAMETER);
			}
		} catch (NumberFormatException nfe) {
			throw new ServerFault("Proxy port must be an integer", ErrorCode.INVALID_PARAMETER);
		}

		String proxyLogin = getValue(SysConfKeys.http_proxy_login.name(), previous, modifications);
		String proxyPassword = getValue(SysConfKeys.http_proxy_password.name(), previous, modifications);

		if (!Strings.isNullOrEmpty(proxyLogin) && Strings.isNullOrEmpty(proxyPassword)) {
			throw new ServerFault("Proxy password must be defined for login '" + proxyLogin + "'",
					ErrorCode.INVALID_PARAMETER);
		}

		if (Strings.isNullOrEmpty(proxyLogin) && !Strings.isNullOrEmpty(proxyPassword)) {
			throw new ServerFault("Proxy login must be defined", ErrorCode.INVALID_PARAMETER);
		}

		// Can't use Collectors.toMap as value may be null
		// https://bugs.openjdk.java.net/browse/JDK-8148463
		pingBlueMindBackOffice(previous, proxySysconfKeys.stream().collect(HashMap::new,
				(map, sysConfKey) -> map.put(sysConfKey.name(), getValue(sysConfKey.name(), previous, modifications)),
				HashMap::putAll));
	}

	private void pingBlueMindBackOffice(SystemConf previous, Map<String, String> newHttpConf) {
		if (!newHttpConf.entrySet().stream()
				.anyMatch(entry -> (entry.getValue() == null && previous.stringValue(entry.getKey()) != null)
						|| (entry.getValue() != null
								&& !entry.getValue().equals(previous.stringValue(entry.getKey()))))) {
			// Same new and old proxy configuration
			return;
		}

		// Ensure proxy will be used to contact BO ping URL
		newHttpConf.remove(SysConfKeys.http_proxy_exceptions.name());

		try {
			Response response = AHCWithProxy
					.build(AHCWithProxy.defaultConfig().setRequestTimeout(
							(int) TimeUnit.MILLISECONDS.convert(5, TimeUnit.SECONDS)), SystemConf.create(newHttpConf))
					.prepareGet(BO_PING_URL).execute().get();

			if (response.getStatusCode() != 200) {
				throw new ServerFault(String.format("Unable to contact %s using proxy parameters: %s (%d)", BO_PING_URL,
						response.getStatusText(), response.getStatusCode()), ErrorCode.INVALID_PARAMETER);
			}
		} catch (InterruptedException | ExecutionException e) {
			logger.error("Unable to get {} using proxy {}:{} login:{}, password:{}", BO_PING_URL,
					newHttpConf.get(SysConfKeys.http_proxy_hostname.name()),
					newHttpConf.get(SysConfKeys.http_proxy_port.name()),
					newHttpConf.get(SysConfKeys.http_proxy_login.name()),
					newHttpConf.get(SysConfKeys.http_proxy_password.name()), e);
			throw new ServerFault(String.format("Unable to get %s using proxy parameters", BO_PING_URL),
					ErrorCode.INVALID_PARAMETER);
		}
	}

	private String getValue(String key, SystemConf previous, Map<String, String> modifications) {
		if (modifications.containsKey(key)) {
			return modifications.get(key);
		}

		return previous.stringValue(key);
	}

	@Override
	public void sanitize(SystemConf previous, Map<String, String> modifications) throws ServerFault {
		ParametersValidator.notNull(modifications);
		ParametersValidator.notNull(previous);

		if (Strings.isNullOrEmpty(previous.stringValue(SysConfKeys.http_proxy_enabled.name()))
				&& !modifications.containsKey(SysConfKeys.http_proxy_enabled.name())) {
			modifications.put(SysConfKeys.http_proxy_enabled.name(), Boolean.FALSE.toString());
		} else if (modifications.containsKey(SysConfKeys.http_proxy_enabled.name())) {
			modifications.put(SysConfKeys.http_proxy_enabled.name(),
					Boolean.valueOf(modifications.get(SysConfKeys.http_proxy_enabled.name())).toString());
		}

		if (modifications.containsKey(SysConfKeys.http_proxy_hostname.name())
				&& modifications.get(SysConfKeys.http_proxy_hostname.name()) != null) {
			modifications.put(SysConfKeys.http_proxy_hostname.name(),
					modifications.get(SysConfKeys.http_proxy_hostname.name()).trim());
		}

		if (Strings.isNullOrEmpty(previous.stringValue(SysConfKeys.http_proxy_port.name()))
				&& !modifications.containsKey(SysConfKeys.http_proxy_port.name())) {
			modifications.put(SysConfKeys.http_proxy_port.name(), DEFAULT_PORT);
		} else if (modifications.containsKey(SysConfKeys.http_proxy_port.name())) {
			try {
				Integer.parseInt(modifications.get(SysConfKeys.http_proxy_port.name()));
			} catch (NumberFormatException nfe) {
				modifications.put(SysConfKeys.http_proxy_port.name(), DEFAULT_PORT);
			}
		}

		String login = modifications.get(SysConfKeys.http_proxy_login.name());
		if (login != null) {
			modifications.put(SysConfKeys.http_proxy_login.name(), login.trim());
		}

		String password = modifications.get(SysConfKeys.http_proxy_password.name());
		if (password != null) {
			modifications.put(SysConfKeys.http_proxy_password.name(), password.trim());
		}
	}

	@Override
	public void onUpdated(BmContext context, SystemConf previous, SystemConf conf) throws ServerFault {
		if (!proxySysconfKeys.stream()
				.anyMatch(proxySysconfKey -> (previous.values.get(proxySysconfKey.name()) == null
						&& conf.values.get(proxySysconfKey.name()) != null)
						|| (previous.values.get(proxySysconfKey.name()) != null && !previous.values
								.get(proxySysconfKey.name()).equals(conf.values.get(proxySysconfKey.name()))))) {
			// Same new and old proxy configuration
			return;
		}

		if (!Boolean.valueOf(conf.booleanValue(SysConfKeys.http_proxy_enabled.name()))) {
			context.getServiceProvider().instance(IServer.class, InstallationId.getIdentifier()).allComplete().stream()
					.forEach(server -> {
						try {
							NodeActivator.get(server.value.address()).writeFile(PROXYVARS,
									new ByteArrayInputStream("".getBytes()));
						} catch (ServerFault e) {
							logger.warn("Cannot deactivate proxy on server {}", server.uid, e);
						}
					});
			return;
		}

		StringBuilder content = new StringBuilder("# DO NOT EDIT").append("\n")
				.append("# Setup proxy using bm-cli or AC").append("\n");

		Optional<String> auth = Optional.empty();
		if (!Strings.isNullOrEmpty(conf.stringValue(SysConfKeys.http_proxy_login.name()))
				&& !Strings.isNullOrEmpty(conf.stringValue(SysConfKeys.http_proxy_password.name()))) {
			auth = Optional.of(String.format("%s:%s@", conf.stringValue(SysConfKeys.http_proxy_login.name()),
					conf.stringValue(SysConfKeys.http_proxy_password.name())));
		}

		content.append(String.format("http_proxy=http://%s%s:%s/", auth.orElse(""),
				conf.stringValue(SysConfKeys.http_proxy_hostname.name()),
				conf.stringValue(SysConfKeys.http_proxy_port.name()))).append("\n");
		content.append(String.format("https_proxy=http://%s%s:%s/", auth.orElse(""),
				conf.stringValue(SysConfKeys.http_proxy_hostname.name()),
				conf.stringValue(SysConfKeys.http_proxy_port.name()))).append("\n");
		content.append(String.format("no_proxy=\"%s\"",
				conf.stringValue(SysConfKeys.http_proxy_exceptions.name()).replaceAll("\\*", ""))).append("\n");

		context.getServiceProvider().instance(IServer.class, InstallationId.getIdentifier()).allComplete().stream()
				.forEach(server -> NodeActivator.get(server.value.address()).writeFile(PROXYVARS,
						new ByteArrayInputStream(content.toString().getBytes())));
	}
}
