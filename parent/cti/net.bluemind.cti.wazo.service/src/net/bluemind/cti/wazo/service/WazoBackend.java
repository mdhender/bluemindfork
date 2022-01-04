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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.cti.wazo.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.cti.api.Status.PhoneState;
import net.bluemind.cti.backend.ICTIBackend;
import net.bluemind.cti.wazo.api.client.WazoAuthentifiedApiClient;
import net.bluemind.cti.wazo.api.client.WazoClickToCallClient;
import net.bluemind.cti.wazo.api.client.WazoUsersClient;
import net.bluemind.cti.wazo.api.client.exception.WazoApiException;
import net.bluemind.domain.api.DomainSettingsKeys;
import net.bluemind.domain.api.IDomainSettings;
import net.bluemind.user.api.IInternalUserExternalAccount;
import net.bluemind.user.api.User;
import net.bluemind.user.api.UserAccount;
import net.bluemind.user.api.UserAccountInfo;

public class WazoBackend implements ICTIBackend {

	private static final String SYSTEM_IDENTIFIER = "Wazo";
	Logger logger = LoggerFactory.getLogger(WazoBackend.class);

	@Override
	public void forward(String domain, ItemValue<User> caller, String imSetPhonePresence) throws ServerFault {
	}

	@Override
	public void dnd(String domain, ItemValue<User> caller, boolean dndEnabled) throws ServerFault {
	}

	@Override
	public void dial(String domain, ItemValue<User> caller, String number) throws ServerFault {

		IInternalUserExternalAccount accountService = getAccountService(domain, caller.uid);

		accountService.getAll().stream().filter(a -> a.externalSystemId.equals(WazoBackend.SYSTEM_IDENTIFIER))
				.findFirst().ifPresent(u -> {
					u.credentials = accountService.getCredentials(WazoBackend.SYSTEM_IDENTIFIER);
					new WazoClickToCallClient(domain, u).dial(number);
				});
	}

	@Override
	public List<String> users(String domain, ItemValue<User> caller) throws ServerFault {

		IInternalUserExternalAccount accountService = getAccountService(domain, caller.uid);

		List<String> users = new ArrayList<String>();
		accountService.getAll().stream().filter(a -> a.externalSystemId.equals(WazoBackend.SYSTEM_IDENTIFIER))
				.findFirst().ifPresent(u -> {
					u.credentials = accountService.getCredentials(WazoBackend.SYSTEM_IDENTIFIER);
					users.addAll(new WazoUsersClient(domain, u).getUsers());
				});

		return users;
	}

	@Override
	public PhoneState getPhoneState(String domain, ItemValue<User> caller) throws ServerFault {
		return null;
	}

	@Override
	public boolean supports(String domain, String uid) {

		IInternalUserExternalAccount accountService = getAccountService(domain, uid);

		if (!isWazoCtiImplementation(domain)) {
			return false;
		}

		return accountService.getAll().stream()
				.filter(a -> a.externalSystemId.equals(WazoBackend.SYSTEM_IDENTIFIER)
						&& !Strings.isNullOrEmpty(accountService.getCredentials(WazoBackend.SYSTEM_IDENTIFIER)))
				.findFirst().isPresent();
	}

	private IInternalUserExternalAccount getAccountService(String domainUid, String userUid) {
		return ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IInternalUserExternalAccount.class, domainUid, userUid);
	}

	private boolean isWazoCtiImplementation(String domainUid) {
		return Optional.ofNullable(ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDomainSettings.class, domainUid).get().get(DomainSettingsKeys.cti_implementation.name()))
				.equals(Optional.of(SYSTEM_IDENTIFIER));
	}

	public boolean testConnection(String domain, UserAccount account) {
		try {
			String token = new WazoAuthentifiedApiClient(domain,
					new UserAccountInfo(account, WazoBackend.SYSTEM_IDENTIFIER)).getToken();
			return !Strings.isNullOrEmpty(token);
		} catch (WazoApiException e) {
			logger.info("Connection test failed for account {}: {}", account.login, e.getMessage());
			return false;
		}
	}

}
