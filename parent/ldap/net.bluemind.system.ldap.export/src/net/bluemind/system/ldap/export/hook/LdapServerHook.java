/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
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
package net.bluemind.system.ldap.export.hook;

import java.io.IOException;

import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.service.BlockingServerTask;
import net.bluemind.core.task.service.ITasksManager;
import net.bluemind.domain.api.Domain;
import net.bluemind.server.api.Server;
import net.bluemind.server.hook.DefaultServerHook;
import net.bluemind.system.ldap.export.LdapHelper;
import net.bluemind.system.ldap.export.conf.SlapdConfig;
import net.bluemind.system.ldap.export.objects.DirectoryRoot;
import net.bluemind.system.ldap.export.objects.DomainDirectoryRoot;
import net.bluemind.system.ldap.export.services.LdapExportService;

public class LdapServerHook extends DefaultServerHook {
	private static final Logger logger = LoggerFactory.getLogger(LdapServerHook.class);
	public static final String LDAPTAG = "directory/bm-master";

	@Override
	public void onServerTagged(BmContext context, ItemValue<Server> server, String tag) throws ServerFault {
		if (!isLdapTag(tag)) {
			return;
		}

		SlapdConfig.build(server).init();
		createEntry(server, new DirectoryRoot().getLdapEntry());
	}

	private void createEntry(ItemValue<Server> server, Entry... entries) {
		try (LdapConnection ldapCon = LdapHelper.connectDirectory(server)) {
			for (Entry entry : entries) {
				LdapHelper.addLdapEntry(ldapCon, entry);
			}
		} catch (IOException e) {
			// close can throw IOException, do nothing with it
			logger.error(e.getMessage(), e);
		}
	}

	private boolean isLdapTag(String tag) {
		return LDAPTAG.equals(tag);
	}

	@Override
	public void onServerAssigned(BmContext context, ItemValue<Server> server, ItemValue<Domain> domain, String tag)
			throws ServerFault {
		if (!isLdapTag(tag)) {
			return;
		}

		initDomainLdapTree(context, server, domain, tag);
	}

	public TaskRef initDomainLdapTree(BmContext context, ItemValue<Server> server, ItemValue<Domain> domain,
			String tag) {
		return context.provider().instance(ITasksManager.class).run(m -> BlockingServerTask.run(m, monitor -> {
			monitor.begin(1, "Init LDAP domain tree");

			logger.info("Processing {} tags for {}", tag, server.value.address());
			LdapExportService.build(context, server, domain).sync();
		}));
	}

	@Override
	public void onServerUnassigned(BmContext context, ItemValue<Server> server, ItemValue<Domain> domain, String tag)
			throws ServerFault {
		if (!isLdapTag(tag)) {
			return;
		}

		try (LdapConnection ldapCon = LdapHelper.connectDirectory(server)) {
			LdapHelper.deleteTree(ldapCon, new DomainDirectoryRoot(domain).getDn());
		} catch (IOException e) {
			// close can throw IOException, do nothing with it
			logger.error(e.getMessage(), e);
		}
	}
}
