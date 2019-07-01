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
package net.bluemind.directory.service.internal;

import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.persistance.ContainerStore;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider.IServerSideServiceFactory;
import net.bluemind.directory.api.BaseDirEntry.AccountType;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.service.DirEventProducer;
import net.bluemind.directory.service.IInCoreDirectory;
import net.bluemind.lib.vertx.VertxPlatform;

public class InCoreDirectory implements IInCoreDirectory {

	public static class Factory implements IServerSideServiceFactory<IInCoreDirectory> {

		@Override
		public Class<IInCoreDirectory> factoryClass() {
			return IInCoreDirectory.class;
		}

		@Override
		public IInCoreDirectory instance(BmContext context, String... params) throws ServerFault {
			String domainUid = params[0];
			ContainerStore containerStore = new ContainerStore(context, context.getDataSource(),
					context.getSecurityContext());

			Container dirContainer = null;
			try {
				dirContainer = containerStore.get(domainUid);
			} catch (SQLException e) {
				throw ServerFault.sqlFault(e);
			}
			if (dirContainer == null) {
				throw new ServerFault("container " + domainUid + " not found");
			}
			return new InCoreDirectory(context.su(), dirContainer, domainUid);
		}

	}

	private static final Logger logger = LoggerFactory.getLogger(InCoreDirectory.class);
	private BmContext context;
	private DirEntryStoreService itemStore;
	private String domainUid;

	public InCoreDirectory(BmContext context, Container dirContainer, String domainUid) {
		this.context = context;
		this.domainUid = domainUid;
		this.itemStore = new DirEntryStoreService(this.context, dirContainer, domainUid);
	}

	@Override
	public void create(String path, DirEntry entry) throws ServerFault {
		itemStore.create(path, entry.displayName, entry);
		logger.debug("direntry {}:{} created", domainUid, path);
		new DirEventProducer(domainUid, VertxPlatform.eventBus()).changed(entry.entryUid, itemStore.getVersion());
	}

	@Override
	public void update(String path, DirEntry entry) throws ServerFault {
		itemStore.update(path, entry.displayName, entry);
		logger.debug("direntry {}:{} updated", domainUid, path);
		new DirEventProducer(domainUid, VertxPlatform.eventBus()).changed(entry.entryUid, itemStore.getVersion());
	}

	@Override
	public void delete(String path) throws ServerFault {
		itemStore.delete(path);
		logger.debug("direntry {}:{} deleted", domainUid, path);
		new DirEventProducer(domainUid, VertxPlatform.eventBus()).deleted(path, itemStore.getVersion());
	}

	@Override
	public void updateAccountType(String uid, AccountType accountType) throws ServerFault {
		itemStore.updateAccountType(uid, accountType);
		new DirEventProducer(domainUid, VertxPlatform.eventBus()).changed(uid, itemStore.getVersion());
	}
}
