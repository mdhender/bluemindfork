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
package net.bluemind.group.service.internal;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.addressbook.api.VCard;
import net.bluemind.addressbook.api.VCard.Kind;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.persistance.ItemStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.directory.service.AbstractVCardAdapter;
import net.bluemind.domain.api.Domain;
import net.bluemind.group.api.Group;
import net.bluemind.group.api.Member;
import net.bluemind.group.persistance.GroupStore;

public class GroupVCardAdapter extends AbstractVCardAdapter<Group> {

	private static final Logger logger = LoggerFactory.getLogger(GroupVCardAdapter.class);
	private BmContext context;
	private GroupStore groupStore;
	private ItemStore itemStore;

	public GroupVCardAdapter(DataSource dataSource, SecurityContext securityContext, Container container,
			String domainUid) {
		this.context = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).getContext();
		this.groupStore = new GroupStore(dataSource, container);
		this.itemStore = new ItemStore(dataSource, container, securityContext);
	}

	@Override
	public VCard asVCard(ItemValue<Domain> domain, String uid, Group group) throws ServerFault {
		VCard vcard = new VCard();
		vcard.kind = Kind.group;
		vcard.source = "bm://" + domain.uid + "/groups/" + uid;
		vcard.identification.formatedName.value = group.name;
		vcard.explanatory.note = group.description;

		IDirectory dir = context.provider().instance(IDirectory.class, domain.uid);
		if (!group.hiddenMembers) {

			List<Member> members = getMembers(uid);
			vcard.organizational.member = new ArrayList<>(members.size());
			for (Member member : members) {
				DirEntry entry = dir.findByEntryUid(member.uid);

				if (entry != null && !entry.archived) {
					vcard.organizational.member.add(VCard.Organizational.Member.create("addressbook_" + domain.uid,
							member.uid, entry.displayName, entry.email));
				} else if (entry != null && entry.archived) {
					logger.debug("entry {} is archive, not in group vcard", entry.path);
				} else {
					logger.warn("did not found member {} for group {}@{}", member.uid, uid, domain.uid);
				}
			}
		}

		vcard.communications.emails = getEmails(domain, group.emails);

		return vcard;
	}

	private List<Member> getMembers(String uid) throws ServerFault {
		try {
			Item item = itemStore.get(uid);
			if (item == null) {
				// create
				return Collections.emptyList();
			}
			return groupStore.getMembers(item);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

}
