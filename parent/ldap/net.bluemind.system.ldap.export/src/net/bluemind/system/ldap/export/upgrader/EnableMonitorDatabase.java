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
package net.bluemind.system.ldap.export.upgrader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.sql.DataSource;

import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.api.ldap.model.message.AliasDerefMode;
import org.apache.directory.api.ldap.model.message.ModifyRequestImpl;
import org.apache.directory.api.ldap.model.message.ModifyResponse;
import org.apache.directory.api.ldap.model.message.ResultCodeEnum;
import org.apache.directory.api.ldap.model.message.SearchRequestImpl;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;
import net.bluemind.system.ldap.export.LdapHelper;
import net.bluemind.system.ldap.export.hook.LdapServerHook;
import net.bluemind.system.schemaupgrader.IVersionedUpdater;
import net.bluemind.system.schemaupgrader.UpdateAction;
import net.bluemind.system.schemaupgrader.UpdateResult;

public class EnableMonitorDatabase implements IVersionedUpdater {
	private static final Logger logger = LoggerFactory.getLogger(EnableMonitorDatabase.class);

	@Override
	public int major() {
		return 4;
	}

	@Override
	public int buildNumber() {
		return 30931;
	}

	@Override
	public UpdateResult executeUpdate(IServerTaskMonitor monitor, DataSource pool, Set<UpdateAction> handledActions) {
		monitor.begin(2, "Enable LDAP monitor database");

		BmContext context = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).getContext();

		List<ItemValue<Server>> directoryServers = new ArrayList<>();
		try {
			for (ItemValue<Server> server : context.provider().instance(IServer.class, "default").allComplete()) {
				if (server.value.tags.contains(LdapServerHook.LDAPTAG)) {
					directoryServers.add(server);
				}
			}

			monitor.progress(1, String.format("Get hosts tagged as %s", LdapServerHook.LDAPTAG));
		} catch (ServerFault e) {
			monitor.end(false, String.format("Fail to get %s hosts: %s", LdapServerHook.LDAPTAG, e.getMessage()), "");
			return UpdateResult.failed();
		}

		IServerTaskMonitor monitorByServer = monitor.subWork(1);
		return upgradeServers(monitorByServer, context, directoryServers);
	}

	private UpdateResult upgradeServers(IServerTaskMonitor monitorByServer, BmContext context,
			List<ItemValue<Server>> directoryServers) {
		logger.info(String.format("Upgrade LDAP configuration on %s hosts tagged as %s", directoryServers.size(),
				LdapServerHook.LDAPTAG));
		monitorByServer.begin(directoryServers.size(),
				String.format("Upgrade LDAP configuration on %s hosts tagged as %s", directoryServers.size(),
						LdapServerHook.LDAPTAG));

		for (ItemValue<Server> directoryServer : directoryServers) {
			try (LdapConnection ldapCon = LdapHelper.connectConfigDirectory(directoryServer)) {
				enableMonitorModule(ldapCon);
				enableMonitorDatabase(ldapCon);

				logger.info("Host: " + directoryServer.value.address() + " upgraded");
				monitorByServer.progress(1, "Host: " + directoryServer.value.address() + " upgraded");
			} catch (ServerFault | IOException | LdapException | CursorException e) {
				String errorMsg = String.format("Fail to update LDAP configuration on host %s: %s",
						directoryServer.value.address(), e.getMessage());
				logger.error(errorMsg, e);
				monitorByServer.end(false, errorMsg, "");

				return UpdateResult.failed();
			}
		}

		return UpdateResult.ok();
	}

	private void enableMonitorDatabase(LdapConnection ldapCon)
			throws LdapInvalidDnException, LdapException, CursorException {
		SearchRequestImpl sr = new SearchRequestImpl();
		sr.setBase(new Dn("cn=config"));
		sr.setScope(SearchScope.SUBTREE);
		sr.setDerefAliases(AliasDerefMode.NEVER_DEREF_ALIASES);
		sr.setSizeLimit(1);
		sr.setFilter("(&(objectclass=olcDatabaseConfig)(olcDatabase=monitor))");
		if (!ldapCon.search(sr).next()) {
			Entry ldapEntry = new DefaultEntry("olcDatabase=monitor,cn=config", "objectclass: olcDatabaseConfig",
					"olcDatabase: monitor",
					"olcAccess: to dn.subtree=\"cn=monitor\" by dn.base=\"uid=admin,cn=config\" write",
					"olcAccess: to dn.subtree=\"cn=monitor\" by * none");
			LdapHelper.addLdapEntry(ldapCon, ldapEntry);
		}
	}
	
	private void enableMonitorModule(LdapConnection ldapCon) throws LdapInvalidDnException, LdapException {
		ModifyRequestImpl m = new ModifyRequestImpl();
		m.setName(new Dn("cn=module{0},cn=config"));
		m.add("olcModuleLoad", "back_monitor");
		ModifyResponse ret = ldapCon.modify(m);
		if (ret.getLdapResult().getResultCode() != ResultCodeEnum.SUCCESS
				&& ret.getLdapResult().getResultCode() != ResultCodeEnum.ATTRIBUTE_OR_VALUE_EXISTS) {
			throw new ServerFault(ret.getLdapResult().getResultCode().toString());
		}
	}

	@Override
	public boolean afterSchemaUpgrade() {
		return true;
	}

}
