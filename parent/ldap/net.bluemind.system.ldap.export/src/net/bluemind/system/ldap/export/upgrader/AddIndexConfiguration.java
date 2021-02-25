/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2018
  *
  * This file is part of BlueMind. BlueMind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License).
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
import org.apache.directory.api.ldap.model.cursor.SearchCursor;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.AliasDerefMode;
import org.apache.directory.api.ldap.model.message.MessageTypeEnum;
import org.apache.directory.api.ldap.model.message.ModifyRequestImpl;
import org.apache.directory.api.ldap.model.message.Response;
import org.apache.directory.api.ldap.model.message.SearchRequestImpl;
import org.apache.directory.api.ldap.model.message.SearchResultEntry;
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
import net.bluemind.system.schemaupgrader.UpdateAction;
import net.bluemind.system.schemaupgrader.UpdateResult;
import net.bluemind.system.schemaupgrader.Updater;

public class AddIndexConfiguration implements Updater {
	private static final Logger logger = LoggerFactory.getLogger(AddIndexConfiguration.class);

	@Override
	public UpdateResult executeUpdate(IServerTaskMonitor monitor, DataSource pool, Set<UpdateAction> handledActions) {

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

		for (ItemValue<Server> directoryServer : directoryServers) {
			try (LdapConnection ldapCon = LdapHelper.connectConfigDirectory(directoryServer)) {
				modifyOlcDbIndex(ldapCon);

				logger.info("Host: {} updating index configuration.", directoryServer.value.address());
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

	private void modifyOlcDbIndex(LdapConnection ldapCon) throws LdapException, CursorException {
		SearchRequestImpl sr = new SearchRequestImpl();
		sr.setBase(new Dn("cn=config"));
		sr.setScope(SearchScope.ONELEVEL);
		sr.setDerefAliases(AliasDerefMode.NEVER_DEREF_ALIASES);
		sr.setSizeLimit(1);
		sr.setFilter("(&(olcDatabase=bdb)(olcSuffix=dc=local))");
		SearchCursor searchCursor = ldapCon.search(sr);
		while (searchCursor.next()) {
			Response response = searchCursor.get();
			if (response.getType() != MessageTypeEnum.SEARCH_RESULT_ENTRY) {
				continue;
			}

			Entry entry = ((SearchResultEntry) response).getEntry();

			ModifyRequestImpl modifyRequest = new ModifyRequestImpl();
			modifyRequest.setName(entry.getDn());
			modifyRequest.remove("olcDbIndex");
			modifyRequest.add("olcDbIndex", "uid eq,pres,sub");
			modifyRequest.add("olcDbIndex", "bmUid eq,pres,sub");
			modifyRequest.add("olcDbIndex", "member eq,pres");
			modifyRequest.add("olcDbIndex", "memberOf eq,pres");
			modifyRequest.add("olcDbIndex", "memberUid eq,pres");
			modifyRequest.add("olcDbIndex", "displayName eq,pres,sub");
			modifyRequest.add("olcDbIndex", "uidNumber eq,pres");
			modifyRequest.add("olcDbIndex", "gidNumber eq,pres");
			modifyRequest.add("olcDbIndex", "loginShell eq,pres");
			modifyRequest.add("olcDbIndex", "ou eq,pres,sub");
			modifyRequest.add("olcDbIndex", "cn eq,pres,sub");
			modifyRequest.add("olcDbIndex", "mail eq,pres,sub");
			modifyRequest.add("olcDbIndex", "surname eq,pres,sub");
			modifyRequest.add("olcDbIndex", "givenname eq,pres,sub");
			LdapHelper.modifyLdapEntry(ldapCon, modifyRequest);
		}
	}

	@Override
	public boolean afterSchemaUpgrade() {
		return true;
	}
}
