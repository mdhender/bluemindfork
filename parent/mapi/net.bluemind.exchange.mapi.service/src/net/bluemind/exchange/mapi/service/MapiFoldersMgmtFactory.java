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
package net.bluemind.exchange.mapi.service;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.IFlatHierarchyUids;
import net.bluemind.core.container.persistence.DataSourceRouter;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.exchange.mapi.api.IMapiFoldersMgmt;
import net.bluemind.exchange.mapi.api.IMapiMailbox;
import net.bluemind.exchange.mapi.api.MapiReplica;
import net.bluemind.exchange.mapi.service.internal.MapiFoldersMgmt;
import net.bluemind.exchange.publicfolders.common.PublicFolders;

public class MapiFoldersMgmtFactory implements ServerSideServiceProvider.IServerSideServiceFactory<IMapiFoldersMgmt> {

	private static final Logger logger = LoggerFactory.getLogger(MapiFoldersMgmtFactory.class);

	@Override
	public Class<IMapiFoldersMgmt> factoryClass() {
		return IMapiFoldersMgmt.class;
	}

	private IMapiFoldersMgmt getService(BmContext context, String domain, MapiReplica replica, DataSource storeDs) {
		return new MapiFoldersMgmt(context, domain, replica, storeDs);
	}

	@Override
	public IMapiFoldersMgmt instance(BmContext context, String... params) throws ServerFault {
		if (params == null || params.length < 2) {
			throw new ServerFault("wrong number of instance parameters");
		}
		String domain = params[0];
		String mboxUid = params[1];
		IServiceProvider prov = context.provider();
		IMapiMailbox mapiMboxApi = prov.instance(IMapiMailbox.class, domain, mboxUid);
		MapiReplica replica = mapiMboxApi.get();
		if (replica == null) {
			throw new ServerFault("Replica not found for mailbox " + mboxUid, ErrorCode.NOT_FOUND);
		}
		String hierUid = IFlatHierarchyUids.getIdentifier(mboxUid, domain);
		DataSource storeDs = DataSourceRouter.get(context, hierUid);
		boolean pf = PublicFolders.mailboxGuid(domain).equals(mboxUid);
		if (!pf && context.getDataSource() == storeDs) {
			logger.warn("Directory DS selected for {} @ {}", mboxUid, domain);
		}
		return getService(context, domain, replica, storeDs);
	}

}
