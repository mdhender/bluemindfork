/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.imap.driver.mailapi;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.authentication.api.AuthUser;
import net.bluemind.authentication.api.IAuthentication;
import net.bluemind.authentication.api.LoginResponse;
import net.bluemind.authentication.api.LoginResponse.Status;
import net.bluemind.authentication.mgmt.api.ISessionsMgmt;
import net.bluemind.authentication.mgmt.api.SessionUpdate;
import net.bluemind.config.BmIni;
import net.bluemind.config.Token;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.hornetq.client.MQ.SharedMap;
import net.bluemind.hornetq.client.Shared;
import net.bluemind.imap.endpoint.driver.MailboxConnection;
import net.bluemind.imap.endpoint.driver.MailboxDriver;
import net.bluemind.serviceprovider.SPResolver;
import net.bluemind.system.api.SysConfKeys;

public class MailApiDriver implements MailboxDriver {

	private static final Logger logger = LoggerFactory.getLogger(MailApiDriver.class);

	private final IAuthentication anonAuthApi;
	private final String coreUrl;
	private final SharedMap<String, String> sharedMap = MQ.sharedMap(Shared.MAP_SYSCONF);
	private final int maxLiteralSize = Integer
			.parseInt(Optional.ofNullable(sharedMap.get(SysConfKeys.message_size_limit.name())).orElse("20000000"));

	public MailApiDriver() {
		String host = Optional.ofNullable(BmIni.value("external-url")).orElse("127.0.0.1");
		this.coreUrl = "http://" + host + ":8090";
		IServiceProvider prov = SPResolver.get().resolve(null);
		this.anonAuthApi = prov.instance(IAuthentication.class);
	}

	@Override
	public MailboxConnection open(String ak, String sk, String remoteIp) {
		LoginResponse login;
		try {
			login = anonAuthApi.login(ak, sk, "imap-endpoint");
		} catch (Exception sf) {
			logger.error("Connection failed: {}", sf.getMessage());
			return null;
		}
		if (login.status != Status.Ok) {
			logger.warn("Got status {} from {}", login.status, coreUrl);
			return null;
		}
		IServiceProvider userProv = SPResolver.get().resolve(login.authKey);
		AuthUser current = userProv.instance(IAuthentication.class).getCurrentUser();
		boolean authWithExistingToken = login.authKey.equals(sk);
		if (!authWithExistingToken) {
			ISessionsMgmt sessMgmt = userProv.instance(ISessionsMgmt.class);
			sessMgmt.updateCurrent(SessionUpdate.forIp(remoteIp));
		}
		logger.info("[{}] logged-in.", current.value.defaultEmail());
		return new MailApiConnection(userProv, SPResolver.get().resolve(Token.admin0()), current,
				!authWithExistingToken, sharedMap);
	}

	@Override
	public int maxLiteralSize() {
		return maxLiteralSize;
	}

}
