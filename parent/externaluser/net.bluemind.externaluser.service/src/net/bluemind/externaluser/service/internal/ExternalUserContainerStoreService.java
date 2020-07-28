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
package net.bluemind.externaluser.service.internal;

import java.util.ArrayList;
import java.util.Arrays;

import javax.sql.DataSource;

import net.bluemind.core.api.Email;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.persistence.IItemValueStore;
import net.bluemind.core.container.persistence.ItemStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.service.DirEntryAndValue;
import net.bluemind.directory.service.DirEntryHandler;
import net.bluemind.directory.service.DirValueStoreService;
import net.bluemind.directory.service.NullMailboxAdapter;
import net.bluemind.domain.api.Domain;
import net.bluemind.externaluser.api.ExternalUser;

public class ExternalUserContainerStoreService extends DirValueStoreService<ExternalUser> {

	public static class ExternalUserDirEntryAdapter implements DirEntryAdapter<ExternalUser> {

		@Override
		public DirEntry asDirEntry(String domainUid, String uid, ExternalUser eu) {
			return DirEntry.create(eu.orgUnitUid, domainUid + "/externaluser/" + uid, DirEntry.Kind.EXTERNALUSER, uid,
					eu.contactInfos.identification.formatedName.value, eu.defaultEmailAddress(), eu.hidden, eu.system,
					eu.archived, eu.dataLocation);
		}
	}

	private ItemStore itemStore;

	public ExternalUserContainerStoreService(BmContext context, ItemValue<Domain> domain,
			Container externalUserContainer) {
		this(context, context.getDataSource(), context.getSecurityContext(), domain, externalUserContainer,
				DirEntry.Kind.EXTERNALUSER, null, new ExternalUserDirEntryAdapter(), new ExternalUserVCardAdapter(),
				new NullMailboxAdapter<>());
	}

	public ExternalUserContainerStoreService(BmContext context, DataSource pool, SecurityContext securityContext,
			ItemValue<Domain> domain, Container container, Kind kind, IItemValueStore<ExternalUser> itemValueStore,
			DirEntryAdapter<ExternalUser> adapter, VCardAdapter<ExternalUser> vcardAdapter,
			MailboxAdapter<ExternalUser> mailboxAdapter) {
		super(context, pool, securityContext, domain, container, kind, itemValueStore, adapter, vcardAdapter,
				mailboxAdapter);
		this.itemStore = new ItemStore(pool, container, context.getSecurityContext());
	}

	@Override
	protected void decorate(Item item, ItemValue<DirEntryAndValue<ExternalUser>> itemValue) throws ServerFault {
		super.decorate(item, itemValue);
		ExternalUser user = new ExternalUser();
		user.contactInfos = itemValue.value.vcard;
		user.emails = new ArrayList<>();
		user.emails.add(Email.create(itemValue.value.entry.email, true));
		user.dataLocation = itemValue.value.entry.dataLocation;
		user.orgUnitUid = itemValue.value.entry.orgUnitUid;
		user.hidden = itemValue.value.entry.hidden;
		itemValue.value.value = user;
	}

	@Override
	protected byte[] getDefaultImage() {
		return DirEntryHandler.EMPTY_PNG;
	}

	public boolean allValid(String[] externalUsersUids) {
		return doOrFail(() -> {
			return itemStore.getMultiple(Arrays.asList(externalUsersUids)).size() == externalUsersUids.length;
		});
	}
}