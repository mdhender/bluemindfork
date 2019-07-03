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
package net.bluemind.system.ldap.importation.hooks;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.authentication.provider.IAuthProvider;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;
import net.bluemind.lib.ldap.LdapConProxy;
import net.bluemind.system.importation.commons.ICoreServices;
import net.bluemind.system.importation.commons.Parameters;
import net.bluemind.system.importation.commons.UuidMapper;
import net.bluemind.system.importation.commons.hooks.ImportLoginValidation;
import net.bluemind.system.importation.commons.managers.UserManager;
import net.bluemind.system.importation.commons.scanner.Scanner;
import net.bluemind.system.ldap.importation.internal.tools.LdapHelper;
import net.bluemind.system.ldap.importation.internal.tools.LdapParameters;
import net.bluemind.system.ldap.importation.internal.tools.LdapUuidMapper;

public class ImportLdapLoginValidation extends ImportLoginValidation {
	private static final Logger logger = LoggerFactory.getLogger(ImportLdapLoginValidation.class);

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.bluemind.system.ldap.commons.ImportLoginValidation#
	 * getDirectoryParameters (net.bluemind.core.container.model.ItemValue)
	 */
	@Override
	protected Parameters getDirectoryParameters(ItemValue<Domain> domain, Map<String, String> domainSettings) {
		return LdapParameters.build(domain.value, domainSettings);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.bluemind.system.ldap.commons.ImportLoginValidation#mustValidLogin
	 * (net.bluemind.authentication.provider.IAuthProvider)
	 */
	@Override
	protected boolean mustValidLogin(IAuthProvider authenticationService) {
		if (!authenticationService.getClass().equals(ImportLdapAuthenticationService.class)) {
			return false;
		}

		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.bluemind.system.ldap.commons.ImportLoginValidation#getDirectoryUser
	 * (net.bluemind.system.ldap.commons.LdapParameters,
	 * net.bluemind.core.container.model.ItemValue)
	 */
	@Override
	protected Optional<UserManager> getDirectoryUser(Parameters ldapParameters, ItemValue<Domain> domain,
			String userLogin) {
		return LdapHelper.getLdapUser((LdapParameters) ldapParameters, domain, userLogin, null, null);
	}

	@Override
	protected void manageUserGroups(ICoreServices coreService, Parameters ldapParameters, UserManager userManager) {
		try (LdapConProxy ldapCon = LdapHelper.connectLdap(ldapParameters)) {
			Scanner.manageUserGroups(ldapCon, coreService, userManager,
					new Function<String, Optional<? extends UuidMapper>>() {
						@Override
						public Optional<? extends UuidMapper> apply(String externalId) {
							return LdapUuidMapper.fromExtId(externalId);
						}
					});
		} catch (Exception e) {
			logger.error(String.format("Unable to import user %s (%s) groups", userManager.user.uid,
					userManager.user.value.login), e);
		}
	}
}
