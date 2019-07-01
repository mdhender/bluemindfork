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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */

package net.bluemind.reminder.job;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.mockito.Matchers;

import net.bluemind.addressbook.api.VCard;
import net.bluemind.core.api.Email;
import net.bluemind.core.api.ListResult;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.DirEntryQuery;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomains;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;

public class MockHelper {

	public static IDirectory getMockDirectoryService() {
		IDirectory mock = mock(IDirectory.class);
		DirEntry bde = new DirEntry();
		bde.displayName = "testuser";
		bde.entryUid = "testuser";
		ListResult<ItemValue<DirEntry>> ret = new ListResult<>();
		ret.values = Arrays.asList(ItemValue.create(bde.entryUid, bde));
		doReturn(ret).when(mock).search(Matchers.<DirEntryQuery>any());
		return mock;
	}

	public static IMailboxes getMockMailboxesService() throws ServerFault {
		IMailboxes mock = mock(IMailboxes.class);
		doReturn(getFakeMailbox()).when(mock).getComplete(Matchers.<String>any());
		return mock;
	}

	public static IDomains getMockDomainService() throws ServerFault {
		IDomains mock = mock(IDomains.class);

		List<ItemValue<Domain>> items = new ArrayList<>();
		items.add(getFakeDomain());
		doReturn(items).when(mock).all();
		return mock;
	}

	public static ItemValue<Mailbox> getFakeMailbox() {
		Mailbox mailbox = new Mailbox();
		Email email = new Email();
		email.isDefault = true;
		email.address = "testuser@test.loc";
		mailbox.emails = Arrays.asList(email);
		mailbox.name = "MyUser";

		ItemValue<Mailbox> item = new ItemValue<>();
		item.value = mailbox;
		item.displayName = "mailboxName";
		return item;
	}

	public static ItemValue<Domain> getFakeDomain() {
		ItemValue<Domain> item = new ItemValue<>();
		item.uid = "test.loc";
		return item;
	}

	public static IUser getMockUserService() throws ServerFault {
		IUser mock = mock(IUser.class);
		List<String> userUids = new ArrayList<>();
		userUids.add("testuser");
		doReturn(userUids).when(mock).allUids();
		doReturn(getFakeUser()).when(mock).getComplete(Matchers.<String>any());
		return mock;
	}

	public static ItemValue<User> getFakeUser() {
		User user = new User();
		VCard vcard = new VCard();
		vcard.identification.formatedName.value = "Testuser";
		user.contactInfos = vcard;
		Collection<Email> emails = new ArrayList<>();
		Email mail = new Email();
		mail.address = "testuser@test.loc";
		mail.isDefault = true;
		emails.add(mail);
		user.emails = emails;
		ItemValue<User> item = new ItemValue<>();
		item.value = user;
		return item;
	}

}
