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
package net.bluemind.system.ldap.importation.internal.tools;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapInvalidAttributeValueException;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;
import net.bluemind.system.importation.commons.Parameters;
import net.bluemind.system.importation.commons.enhancer.IEntityEnhancer;
import net.bluemind.system.importation.commons.managers.GroupManager;
import net.bluemind.system.importation.commons.scanner.IImportLogger;
import net.bluemind.system.ldap.importation.Activator;
import net.bluemind.system.ldap.importation.api.LdapConstants;

public class GroupManagerImpl extends GroupManager {
	public static final String LDAP_NAME = "cn";
	private static final String LDAP_DESCRIPTION = "description";
	private static final String[] LDAP_MAIL = { "mail", "mailLocalAddress", "gosaMailAlternateAddress" };

	private final LdapParameters ldapParameters;

	private GroupManagerImpl(LdapParameters ldapParameters, ItemValue<Domain> domain, Entry entry) {
		super(domain, entry);
		this.ldapParameters = ldapParameters;
	}

	public static Optional<GroupManager> build(LdapParameters ldapParameters, ItemValue<Domain> domain, Entry entry) {
		return Optional.of(new GroupManagerImpl(ldapParameters, domain, entry));
	}

	@Override
	public String getExternalId(IImportLogger importLogger) {
		return group.externalId = LdapConstants.EXTID_PREFIX
				+ LdapHelper.checkMandatoryAttribute(importLogger, entry, ldapParameters.ldapDirectory.extIdAttribute);
	}

	@Override
	protected void setNameFromDefaultAttribute(IImportLogger importLogger) {
		try {
			group.value.name = LdapHelper.checkMandatoryAttribute(importLogger, entry, LDAP_NAME);
		} catch (ServerFault sf) {
			HashMap<String, String> messages = new HashMap<>(2);
			messages.put("en",
					String.format("Unable to manage group: %s, missing attribute: %s", entry.getDn(), LDAP_NAME));
			messages.put("fr", String.format("Impossible de gérer le groupe: %s, attribut manquant: %s", entry.getDn(),
					LDAP_NAME));

			importLogger.error(messages);

			throw sf;
		}
	}

	@Override
	protected void manageInfos() throws LdapInvalidAttributeValueException {
		if (entry.get(LDAP_DESCRIPTION) != null) {
			group.value.description = entry.get(LDAP_DESCRIPTION).getString();
		}
	}

	@Override
	protected List<String> getEmails() {
		return getAttributesValues(entry, LDAP_MAIL);
	}

	@Override
	protected List<IEntityEnhancer> getEntityEnhancerHooks() {
		return Activator.getEntityEnhancerHooks();
	}

	@Override
	protected Parameters getDirectoryParameters() {
		return ldapParameters;
	}
}
