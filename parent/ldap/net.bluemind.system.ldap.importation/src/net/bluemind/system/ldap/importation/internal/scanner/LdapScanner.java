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
package net.bluemind.system.ldap.importation.internal.scanner;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.name.Dn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.container.model.ItemValue;
import net.bluemind.domain.api.Domain;
import net.bluemind.lib.ldap.LdapConProxy;
import net.bluemind.system.importation.commons.ICoreServices;
import net.bluemind.system.importation.commons.Parameters;
import net.bluemind.system.importation.commons.UuidMapper;
import net.bluemind.system.importation.commons.enhancer.IScannerEnhancer;
import net.bluemind.system.importation.commons.managers.GroupManager;
import net.bluemind.system.importation.commons.managers.UserManager;
import net.bluemind.system.importation.commons.scanner.ImportLogger;
import net.bluemind.system.importation.commons.scanner.Scanner;
import net.bluemind.system.importation.search.PagedSearchResult;
import net.bluemind.system.ldap.importation.Activator;
import net.bluemind.system.ldap.importation.internal.tools.GroupManagerImpl;
import net.bluemind.system.ldap.importation.internal.tools.LdapHelper;
import net.bluemind.system.ldap.importation.internal.tools.LdapParameters;
import net.bluemind.system.ldap.importation.internal.tools.LdapUuidMapper;
import net.bluemind.system.ldap.importation.internal.tools.UserManagerImpl;
import net.bluemind.system.ldap.importation.search.LdapSearch;

public abstract class LdapScanner extends Scanner {
	private static final Logger logger = LoggerFactory.getLogger(LdapScanner.class);

	protected LdapParameters ldapParameters;

	public LdapScanner(ImportLogger importLogger, LdapParameters ldapParameters, ItemValue<Domain> domain) {
		super(importLogger, domain);

		this.ldapParameters = ldapParameters;
		logger.info("Import LDAP directory using scanner: " + this.getClass().getSimpleName());
	}

	public LdapScanner(ImportLogger importLogger, ICoreServices coreService, LdapParameters ldapParameters,
			ItemValue<Domain> domain) {
		super(importLogger, coreService, domain);

		this.ldapParameters = ldapParameters;
		logger.info("Import LDAP directory using scanner: " + this.getClass().getSimpleName());
	}

	@Override
	protected Optional<UuidMapper> getUuidMapperFromExtId(String externalId) {
		return LdapUuidMapper.fromExtId(externalId);
	}

	protected abstract LdapSearch getLdapSearch();

	@Override
	protected void getRelayMailboxGroupDn() {
	}

	@Override
	protected String getKind() {
		return "LDAP";
	}

	@Override
	protected Parameters getParameter() {
		return ldapParameters;
	}

	@Override
	protected LdapConProxy getConnection() {
		return LdapHelper.connectLdap(ldapParameters);
	}

	@Override
	protected Set<UuidMapper> uuidMapperFromExtIds(List<String> externalIds) {
		return LdapUuidMapper.fromExtIdList(externalIds);
	}

	@Override
	protected PagedSearchResult allUsersFromDirectory() throws LdapException {
		return getLdapSearch().findAllUsers(ldapCon);
	}

	@Override
	protected Optional<UuidMapper> getUuidMapperFromEntry(Entry entry) {
		if (entry.containsAttribute(ldapParameters.ldapDirectory.extIdAttribute)) {
			return Optional.of(LdapUuidMapper.fromEntry(ldapParameters.ldapDirectory.extIdAttribute, entry));
		}

		return Optional.empty();
	}

	@Override
	protected PagedSearchResult allGroupsFromDirectory() throws LdapException {
		return getLdapSearch().findAllGroups(ldapCon);
	}

	@Override
	protected PagedSearchResult usersDnByLastModification(Optional<String> lastUpdate) throws LdapException {
		return getLdapSearch().findUsersDnByLastModification(ldapCon, lastUpdate);
	}

	@Override
	protected PagedSearchResult groupsDnByLastModification(Optional<String> lastUpdate) throws LdapException {
		return getLdapSearch().findGroupsDnByLastModification(ldapCon, lastUpdate);
	}

	@Override
	protected Optional<UserManager> getUserManager(Entry entry) {
		return UserManagerImpl.build(ldapParameters, domain, entry, splitGroupMembers);
	}

	@Override
	protected Optional<GroupManager> getGroupManager(Entry entry) {
		return GroupManagerImpl.build(ldapParameters, domain, entry);
	}

	@Override
	protected PagedSearchResult getUserFromDn(Dn userDn) throws LdapException {
		return getLdapSearch().getUserUUID(ldapCon, userDn);
	}

	@Override
	protected PagedSearchResult getGroupFromDn(Dn groupDn) throws LdapException {
		return getLdapSearch().getGroupUUID(ldapCon, groupDn);
	}

	@Override
	protected boolean doNotImportUser(Entry entry) {
		return false;
	}

	@Override
	protected boolean doNotImportGroup(Entry entry) {
		return false;
	}

	@Override
	protected Optional<Dn> getMemberDnFromLogin(String userLogin) {
		return Optional.empty();
	}

	@Override
	protected String getGroupMembersAttributeName() {
		return GroupManagerImpl.LDAP_MEMBER;
	}

	@Override
	protected List<IScannerEnhancer> getScannerEnhancerHooks() {
		return Activator.getScannerEnhancerHooks();
	}
}
