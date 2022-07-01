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
package net.bluemind.core.container.service;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.IOfflineMgmt;
import net.bluemind.core.container.service.internal.OfflineMgmtService;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirectory;

public class OfflineMgmtFactory implements ServerSideServiceProvider.IServerSideServiceFactory<IOfflineMgmt> {

	private static final Logger logger = LoggerFactory.getLogger(OfflineMgmtFactory.class);

	@Override
	public Class<IOfflineMgmt> factoryClass() {
		return IOfflineMgmt.class;
	}

	@Override
	public IOfflineMgmt instance(BmContext context, String... params) throws ServerFault {
		if (params == null || params.length != 2) {
			throw new ServerFault("2 parameters expected, domainUid, ownerUid");
		}
		return new OfflineMgmtService(dataSourceOfDirEntry(context, params[0], params[1]));
	}

	private DataSource dataSourceOfDirEntry(BmContext context, String domainUid, String dirEntryUid) {
		if ("global.virt".equals(domainUid)) {
			return context.getDataSource();
		}

		DirEntry entry = context.su().provider().instance(IDirectory.class, domainUid).findByEntryUid(dirEntryUid);
		// in some test case, the mailbox is created without it's dir entry
		if (entry == null) {
			return context.getDataSource();
		}

		DataSource ds = (entry.dataLocation == null) //
				? context.getDataSource() //
				: context.getMailboxDataSource(entry.dataLocation);
		// ds is null in some tests when a second imap server is added without a
		// matching psql-data
		return (ds != null) ? ds : context.getAllMailboxDataSource().iterator().next();
	}

}
