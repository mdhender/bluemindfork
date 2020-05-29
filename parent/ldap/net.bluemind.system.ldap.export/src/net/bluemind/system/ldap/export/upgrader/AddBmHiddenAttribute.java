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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.sql.DataSource;

import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.api.ldap.model.message.ModifyRequestImpl;
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

public class AddBmHiddenAttribute implements Updater {
	private static final Logger logger = LoggerFactory.getLogger(AddBmHiddenAttribute.class);

	@Override
	public Date date() {
		return java.sql.Date.valueOf(LocalDate.of(2020, 4, 28));
	}

	@Override
	public int sequence() {
		return 390;
	}

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

				logger.info("Host: " + directoryServer.value.address() + " BlueMind schema updated.");
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

	private void modifyOlcDbIndex(LdapConnection ldapCon)
			throws LdapInvalidDnException, LdapException, CursorException {
		Entry entry = ldapCon.lookup("cn={4}bluemind,cn=schema,cn=config");

		if (entry != null) {
			ModifyRequestImpl modifyRequest = new ModifyRequestImpl();
			modifyRequest.setName(entry.getDn());
			modifyRequest.replace("olcAttributeTypes",
					"{0}( 1.3.6.1.4.1.39073.2.1 NAME 'bmUid' DESC 'BlueMind UID' EQUALITY caseIgnoreMatch SUBSTR caseIgnoreSubstringsMatch SYNTAX 1.3.6.1.4.1.1466.115.121.1.15{256} SINGLE-VALUE )",
					"{1}( 1.3.6.1.4.1.39073.2.2 NAME 'bmVersion' DESC 'BlueMind changset version' EQUALITY integerMatch SYNTAX 1.3.6.1.4.1.1466.115.121.1.27 SINGLE-VALUE )",
					"{2}( 1.3.6.1.4.1.39073.2.3 NAME 'bmHidden' DESC 'BlueMind hidden status' EQUALITY caseIgnoreMatch SUBSTR caseIgnoreSubstringsMatch SYNTAX 1.3.6.1.4.1.1466.115.121.1.15{256} SINGLE-VALUE )");
			modifyRequest.replace("olcObjectClasses",
					"{0}( 1.3.6.1.4.1.39073.1.1 NAME 'bmGroup' DESC 'BlueMind group' SUP top AUXILIARY MAY ( member $ mail $ bmUid $ bmHidden ) )",
					"{1}( 1.3.6.1.4.1.39073.1.2 NAME 'bmUser' DESC 'BlueMind user' SUP top AUXILIARY MUST ( bmUid ) MAY ( bmHidden ) )",
					"{2}( 1.3.6.1.4.1.39073.1.3 NAME 'bmDomain' DESC 'BlueMind domain' SUP top AUXILIARY MUST ( bmVersion ) )");
			LdapHelper.modifyLdapEntry(ldapCon, modifyRequest);
		} else {
			throw new ServerFault("Unable to find entry DN: cn={4}bluemind,cn=schema,cn=config");
		}
	}

	@Override
	public boolean afterSchemaUpgrade() {
		return true;
	}

}
