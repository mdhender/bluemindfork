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
package net.bluemind.system.ldap.importation.upg;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.sql.DataSource;

import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.cursor.SearchCursor;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.AliasDerefMode;
import org.apache.directory.api.ldap.model.message.MessageTypeEnum;
import org.apache.directory.api.ldap.model.message.Response;
import org.apache.directory.api.ldap.model.message.SearchRequest;
import org.apache.directory.api.ldap.model.message.SearchRequestImpl;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomains;
import net.bluemind.group.api.IGroup;
import net.bluemind.lib.ldap.LdapConProxy;
import net.bluemind.system.importation.search.LdapSearchCursor;
import net.bluemind.system.ldap.importation.api.LdapConstants;
import net.bluemind.system.ldap.importation.internal.tools.LdapHelper;
import net.bluemind.system.ldap.importation.internal.tools.LdapParameters;
import net.bluemind.system.ldap.importation.search.LdapGroupSearchFilter;
import net.bluemind.system.ldap.importation.search.LdapUserSearchFilter;
import net.bluemind.system.schemaupgrader.IVersionedUpdater;
import net.bluemind.system.schemaupgrader.UpdateAction;
import net.bluemind.system.schemaupgrader.UpdateResult;
import net.bluemind.user.api.IUser;

public class UpgradeExtId implements IVersionedUpdater {
	private static final Logger logger = LoggerFactory.getLogger(UpgradeExtId.class);
	private DataSource pool;

	private enum Kind {
		user, group
	}

	@Override
	public int major() {
		return 4;
	}

	@Override
	public int buildNumber() {
		return 22257;
	}

	@Override
	public UpdateResult executeUpdate(IServerTaskMonitor monitor, DataSource pool, Set<UpdateAction> handledActions) {
		this.pool = pool;

		monitor.begin(2, "Upgrade user and group external ID");

		BmContext context = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).getContext();

		Map<ItemValue<Domain>, LdapParameters> domains = new HashMap<>();
		try {
			for (ItemValue<Domain> domain : context.provider().instance(IDomains.class, "default").all()) {
				if (domain.value.global) {
					continue;
				}

				LdapParameters ldapParameters = LdapParameters.build(domain.value,
						Collections.<String, String>emptyMap());
				if (!ldapParameters.enabled) {
					continue;
				}
				domains.put(domain, ldapParameters);
			}
		} catch (ServerFault e) {
			monitor.progress(1, "Fail to get domains UIDs: " + e.getMessage());
			return UpdateResult.failed();
		}
		monitor.progress(1, "Get domains UID with LDAP enabled");

		IServerTaskMonitor domainsMonitor = monitor.subWork(1);
		domainsMonitor.begin(domains.size(), "Upgrade mailbox ACLs for domains");
		try {
			upgradeDomainsExtIds(context, domainsMonitor, domains);
		} catch (ServerFault sf) {
			logger.error(sf.getMessage(), sf);
			return UpdateResult.failed();
		}

		return UpdateResult.ok();
	}

	private void upgradeDomainsExtIds(BmContext context, IServerTaskMonitor domainsMonitor,
			Map<ItemValue<Domain>, LdapParameters> domains) {
		for (ItemValue<Domain> domain : domains.keySet()) {
			IServerTaskMonitor domainMonitor = domainsMonitor.subWork(1);
			domainMonitor.begin(2, "Upgrade users and groups external IDs for domain: " + domain.uid);

			IUser userApi = null;
			List<String> uids = null;
			try {
				userApi = context.provider().instance(IUser.class, domain.uid);
				uids = userApi.allUids();
			} catch (ServerFault e) {
				logger.error("Fail to get users for domain: " + domain.uid, e);
				throw new ServerFault(e);
			}

			if (uids.size() == 0) {
				domainMonitor.progress(1, "No user for domain: " + domain.uid);
			} else {
				IServerTaskMonitor usersMonitor = domainMonitor.subWork(1);
				usersMonitor.begin(uids.size(), "Upgrade users external IDs for domain: " + domain.uid);

				findAndUpgradeExtIds(usersMonitor, domain, domains.get(domain), Kind.user, uids);
			}

			IGroup groupApi = null;
			uids = null;
			try {
				groupApi = context.provider().instance(IGroup.class, domain.uid);
				uids = groupApi.allUids();
			} catch (ServerFault e) {
				logger.error("Fail to get groups for domain: " + domain.uid, e);
				throw new ServerFault(e);
			}

			if (uids.size() == 0) {
				domainMonitor.progress(1, "No group for domain: " + domain.uid);
			} else {
				IServerTaskMonitor groupsMonitor = domainMonitor.subWork(1);
				groupsMonitor.begin(uids.size(), "Upgrade groups external IDs for domain: " + domain.uid);

				findAndUpgradeExtIds(groupsMonitor, domain, domains.get(domain), Kind.group, uids);
			}
		}
	}

	private void findAndUpgradeExtIds(IServerTaskMonitor usersMonitor, ItemValue<Domain> domain,
			LdapParameters ldapParameters, Kind uuidKind, List<String> uuids) {
		try (LdapConProxy ldapCon = LdapHelper.connectLdap(ldapParameters)) {
			for (String uuid : uuids) {
				usersMonitor.progress(1, "Check external ID for uid: " + uuid);

				String ldapSearchFilter = null;
				if (uuidKind == Kind.user) {
					ldapSearchFilter = new LdapUserSearchFilter().getSearchFilter(ldapParameters, Optional.empty(),
							null, uuid);
				} else {
					ldapSearchFilter = new LdapGroupSearchFilter().getSearchFilter(ldapParameters, Optional.empty(),
							uuid, null);
				}
				SearchCursor searchCursor = getEntryFromUuid(ldapCon, ldapParameters, ldapSearchFilter);

				while (searchCursor.next()) {
					Response response = searchCursor.get();

					if (response.getType() != MessageTypeEnum.SEARCH_RESULT_ENTRY) {
						continue;
					}

					updateExtId(uuid);
				}
			}
		} catch (ServerFault e) {
			throw e;
		} catch (IOException | LdapException | CursorException | SQLException e) {
			throw new ServerFault(e);
		}
	}

	private void updateExtId(String uuid) throws SQLException {
		logger.info("Set external ID for uid {}", uuid);
		String sql = "UPDATE t_container_item SET external_id=? WHERE uid=?";

		try (Connection conn = pool.getConnection(); PreparedStatement st = conn.prepareStatement(sql)) {
			st.setString(1, LdapConstants.EXTID_PREFIX + uuid);
			st.setString(2, uuid);
			st.executeUpdate();
		}
	}

	private SearchCursor getEntryFromUuid(LdapConProxy ldapCon, LdapParameters ldapParameters, String ldapSearchFilter)
			throws LdapException {
		SearchRequest searchRequest = new SearchRequestImpl();
		searchRequest.setBase(ldapParameters.ldapDirectory.baseDn);
		searchRequest.setFilter(ldapSearchFilter);
		searchRequest.setScope(SearchScope.SUBTREE);
		searchRequest.setDerefAliases(AliasDerefMode.NEVER_DEREF_ALIASES);
		searchRequest.setSizeLimit(0);

		return new LdapSearchCursor(ldapCon.search(searchRequest));
	}

	@Override
	public boolean afterSchemaUpgrade() {
		return true;
	}

}
