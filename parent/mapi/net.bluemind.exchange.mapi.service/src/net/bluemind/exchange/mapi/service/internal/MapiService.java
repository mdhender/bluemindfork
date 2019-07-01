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
package net.bluemind.exchange.mapi.service.internal;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import net.bluemind.addressbook.api.IAddressBookUids;
import net.bluemind.addressbook.api.IAddressBook;
import net.bluemind.addressbook.api.VCardInfo;
import net.bluemind.addressbook.api.VCardQuery;
import net.bluemind.core.api.ListResult;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.persistance.ContainerStore;
import net.bluemind.core.container.persistance.ItemStore;
import net.bluemind.core.rest.BmContext;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.DirEntryQuery;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.exchange.mapi.api.IMapi;

public class MapiService implements IMapi {

	private static final Logger logger = LoggerFactory.getLogger(MapiService.class);

	private BmContext context;

	private String domainUid;

	private final ItemStore directoryItemStore;

	private final IDirectory directoryService;

	public MapiService(BmContext context, String domainUid) throws ServerFault {
		this.context = context;
		this.domainUid = domainUid;
		try {
			ContainerStore cs = new ContainerStore(context, context.getDataSource(), context.getSecurityContext());
			Container directoryContainer = cs.get(domainUid);
			directoryItemStore = new ItemStore(context.getDataSource(), directoryContainer,
					context.getSecurityContext());
			directoryService = context.provider().instance(IDirectory.class, directoryContainer.uid);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}

	}

	@Override
	public List<Long> searchGAL(String query) throws ServerFault {
		IAddressBook addrApi = context.provider().instance(IAddressBook.class,
				IAddressBookUids.userVCards(domainUid));
		ListResult<ItemValue<VCardInfo>> search = addrApi.search(VCardQuery.create(query));
		List<String> uids = search.values.stream().map(val -> val.uid).collect(Collectors.toList());

		DirEntryQuery dirQuery = DirEntryQuery.entries(uids.toArray(new String[0]));
		IDirectory dirApi = context.provider().instance(IDirectory.class, domainUid);
		ListResult<ItemValue<DirEntry>> found = dirApi.search(dirQuery);
		logger.info("GAL '{}' => {} result(s)", query, found.total);
		return found.values.stream().filter(iv -> !Strings.isNullOrEmpty(iv.value.email)).map(iv -> iv.internalId)
				.collect(Collectors.toList());
	}

	@Override
	public List<ItemValue<DirEntry>> getGALContent(List<Long> galItems) throws ServerFault {
		try {
			List<String> itemUids = directoryItemStore.getMultipleById(galItems).stream().map(i -> i.uid)
					.collect(Collectors.toList());
			return directoryService.search(DirEntryQuery.entries(itemUids)).values;
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

}
