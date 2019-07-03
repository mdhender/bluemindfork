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
package net.bluemind.system.importation.commons.hooks;

import java.util.Map;
import java.util.Optional;

import net.bluemind.authentication.provider.IAuthProvider;
import net.bluemind.authentication.provider.ILoginValidationListener;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomainSettings;
import net.bluemind.domain.api.IDomains;
import net.bluemind.system.importation.commons.CoreServices;
import net.bluemind.system.importation.commons.ICoreServices;
import net.bluemind.system.importation.commons.Parameters;
import net.bluemind.system.importation.commons.managers.UserManager;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;

/**
 * @author Anthony Prades <anthony.prades@blue-mind.net>
 *
 */
public abstract class ImportLoginValidation implements ILoginValidationListener {
	@Override
	public void onValidLogin(IAuthProvider authenticationService, String userLogin, String domainUid, String password) {
		if (!mustValidLogin(authenticationService)) {
			return;
		}

		ItemValue<Domain> domain = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDomains.class).get(domainUid);
		if (domain == null) {
			throw new ServerFault(String.format("Domain uid %s not found", domainUid));
		}

		Map<String, String> domainSettings = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDomainSettings.class, domain.uid).get();
		Parameters directoryParameters = getDirectoryParameters(domain, domainSettings);

		if (getBMUser(userLogin, domain.uid) != null) {
			return;
		}

		Optional<UserManager> optionalUserManager = getDirectoryUser(directoryParameters, domain, userLogin);
		if (!optionalUserManager.isPresent()) {
			throw new ServerFault(String.format("Can't find user: %s@%s in directory server", userLogin, domainUid));
		}

		UserManager userManager = optionalUserManager.get();
		if (userManager.create) {
			ICoreServices coreService = CoreServices.build(domainUid);
			coreService.createUser(userManager.user);
			userManager.getUpdatedMailFilter().ifPresent(mf -> coreService.setMailboxFilter(userManager.user.uid, mf));

			manageUserGroups(coreService, directoryParameters, userManager);
		}
	}

	protected abstract void manageUserGroups(ICoreServices build, Parameters ldapParameters, UserManager userManager);

	/**
	 * @return
	 */
	protected abstract Parameters getDirectoryParameters(ItemValue<Domain> domain, Map<String, String> domainSettings);

	/**
	 * @param authenticationService
	 * @return
	 */
	protected abstract boolean mustValidLogin(IAuthProvider authenticationService);

	/**
	 * @param adParameters
	 * @param domain
	 * @param userLogin
	 * @param user
	 * @return
	 * @throws ServerFault
	 */
	protected abstract Optional<UserManager> getDirectoryUser(Parameters adParameters, ItemValue<Domain> domain,
			String userLogin);

	ItemValue<User> getBMUser(String userLogin, String domainUid) {
		return ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IUser.class, domainUid)
				.byLogin(userLogin);
	}
}