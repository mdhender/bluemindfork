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
package net.bluemind.user.service.internal;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

import net.bluemind.core.api.Email;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.hooks.IAclHook;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.rest.BmContext;
import net.bluemind.directory.api.BaseDirEntry.Kind;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomains;
import net.bluemind.group.api.IGroup;
import net.bluemind.group.api.Member;
import net.bluemind.mailbox.api.IMailboxAclUids;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.mailbox.api.Mailbox.Type;
import net.bluemind.mailbox.hook.DefaultMailboxHook;
import net.bluemind.mailbox.identity.api.IdentityDescription;
import net.bluemind.user.api.IInternalUserMailIdentities;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.IUserMailIdentities;
import net.bluemind.user.api.UserMailIdentity;
import net.bluemind.user.service.IInCoreUser;

public class UserMailIdentitiesHook extends DefaultMailboxHook implements IAclHook {

	private static Logger logger = LoggerFactory.getLogger(UserMailIdentitiesHook.class);

	@Override
	public void onAclChanged(BmContext context, ContainerDescriptor container, List<AccessControlEntry> previous,
			List<AccessControlEntry> current) {

		if (!IMailboxAclUids.TYPE.equals(container.type)) {
			return;
		}

		Set<AccessControlEntry> p = previous.stream().filter(ace -> {
			return ace.verb == Verb.All || ace.verb == Verb.Write;
		}).collect(Collectors.toSet());

		Set<AccessControlEntry> c = current.stream().filter(ace -> {
			return ace.verb == Verb.All || ace.verb == Verb.Write;
		}).collect(Collectors.toSet());

		Set<AccessControlEntry> removed = Sets.difference(p, c);

		if (removed.isEmpty()) {
			return;
		}
		try {
			handleChanges(context, container, removed, c);
		} catch (Exception e) {
			logger.error("error during auto subscribe to mailbox", e);
		}
	}

	private void handleChanges(BmContext context, ContainerDescriptor container, Set<AccessControlEntry> removed,
			Set<AccessControlEntry> current) throws ServerFault {

		Set<String> removedUsers = asUsers(context, container.domainUid, removed);
		Set<String> users = asUsers(context, container.domainUid, current);

		removedUsers.removeAll(users);
		removedUsers.remove(container.owner);

		if (removedUsers.isEmpty()) {
			return;
		}

		IInCoreUser usersService = context.su().provider().instance(IInCoreUser.class, container.domainUid);
		for (String userId : removedUsers) {
			usersService.deleteUserIdentitiesForMailbox(userId, container.owner);
		}

	}

	private Set<String> asUsers(BmContext context, String domainUid, Collection<AccessControlEntry> acl)
			throws ServerFault {

		IDirectory dir = context.provider().instance(IDirectory.class, domainUid);
		IGroup groups = context.su().provider().instance(IGroup.class, domainUid);
		Set<DirEntry> entries = new HashSet<>();
		for (AccessControlEntry ace : acl) {
			DirEntry entry = dir.findByEntryUid(ace.subject);
			if (entry != null) {
				entries.add(entry);
			} else {
				logger.warn("did not found entry for {} in domain {}", ace.subject, domainUid);
			}
		}

		Set<String> ret = new HashSet<>(entries.size());
		if (entries.stream().anyMatch((d) -> {
			return d.kind == Kind.DOMAIN;
		})) {
			// public sharing
			ret.addAll(context.provider().instance(IUser.class, domainUid).allUids());
		} else {

			for (DirEntry entry : entries) {
				if (entry.kind == Kind.USER) {
					ret.add(entry.entryUid);
				} else if (entry.kind == Kind.GROUP) {
					for (Member member : groups.getExpandedUserMembers(entry.entryUid)) {
						ret.add(member.uid);
					}
				}
			}
		}
		return ret;
	}

	@Override
	public void onMailboxDeleted(BmContext context, String domainUid, ItemValue<Mailbox> value) throws ServerFault {

		IInCoreUser users = context.su().provider().instance(IInCoreUser.class, domainUid);
		users.deleteUserIdentitiesForMailbox(value.uid);

	}

	@Override
	public void onMailboxCreated(BmContext context, String domainUid, ItemValue<Mailbox> value) throws ServerFault {
		if (value.value.type != Type.user) {
			return;
		}

		if (value.value.defaultEmail() != null) {
			DirEntry dirEntry = context.su().provider().instance(IDirectory.class, domainUid).findByEntryUid(value.uid);
			context.su().provider().instance(IInternalUserMailIdentities.class, domainUid, value.uid)
					.createDefaultIdentity(value, dirEntry);
		}
	}

	@Override
	public void onMailboxUpdated(BmContext context, String domainUid, ItemValue<Mailbox> previousValue,
			ItemValue<Mailbox> value) throws ServerFault {
		if (value.value.type != Type.user) {
			return;
		}

		if (value.value.routing == Routing.none) {
			return;
		}

		IInternalUserMailIdentities userMailIdentities = context.su().provider()
				.instance(IInternalUserMailIdentities.class, domainUid, value.uid);

		DirEntry dirEntry = context.su().provider().instance(IDirectory.class, domainUid).findByEntryUid(value.uid);
		boolean needToAddDefaultEmail = value.value.defaultEmail() != null;
		for (IdentityDescription identityDesc : userMailIdentities.getIdentities()) {
			if (identityDesc.isDefault) {
				needToAddDefaultEmail = false;
			}
			if (null != identityDesc.mbox) {
				if (identityDesc.isDefault) {
					UserMailIdentity identity = userMailIdentities.get(identityDesc.id);
					identity.displayname = dirEntry.displayName;
					identity.name = identity.displayname;
					verifyDefaultIdentityIntegrity(identityDesc.id, userMailIdentities, identity,
							value.value.defaultEmail(), domainUid, context, value, previousValue.value.defaultEmail());
				} else {
					verifyIdentityIntegrity(identityDesc, userMailIdentities, value, domainUid, context);
				}
			}
		}

		if (needToAddDefaultEmail) {
			userMailIdentities.createDefaultIdentity(value, dirEntry);
		}

	}

	public void verifyIdentityIntegrity(IdentityDescription identity, IUserMailIdentities userMailIdentities,
			ItemValue<Mailbox> mbox, String domainUid, BmContext context) throws ServerFault {
		ItemValue<Mailbox> referencedMailbox = getReferencedMailbox(identity.mbox, mbox, domainUid, context);
		sanitizeIdentityIntegrity(identity.email, referencedMailbox, context, domainUid, () -> {
			logger.info("Referenced mailbox:email {}:{} does not exist anymore. Deleting identity", identity.mbox,
					identity.email);
			userMailIdentities.delete(identity.id);
		});
	}

	public void verifyDefaultIdentityIntegrity(String id, IUserMailIdentities userMailIdentities,
			UserMailIdentity identity, Email defaultEmail, String domainUid, BmContext context, ItemValue<Mailbox> mbox,
			Email previousDefaultEmail) throws ServerFault {
		if (identityFollowsDefaultEmail(identity, mbox, previousDefaultEmail)) {
			// identity follows default email
			identity.email = mbox.value.defaultEmail().address;
			userMailIdentities.update(id, identity);
		} else {
			if (defaultEmail != null) {
				// default identity check, use default email if configured email
				// does not exist
				ItemValue<Mailbox> referencedMailbox = getReferencedMailbox(identity.mailboxUid, mbox, domainUid,
						context);
				sanitizeIdentityIntegrity(identity.email, referencedMailbox, context, domainUid, () -> {
					if (null != defaultEmail && StringUtils.isNotBlank(defaultEmail.address)) {
						logger.info(
								"Referenced mailbox:email {}:{} does not exist anymore. Setting default identity to {}",
								referencedMailbox.uid, identity.email, defaultEmail.address);
						identity.email = defaultEmail.address;
						userMailIdentities.update(id, identity);
					}
				});
			} else {
				// default identity check fallback (deletes identity if email is
				// not present)
				verifyIdentityIntegrity(toDescription(id, identity), userMailIdentities, mbox, domainUid, context);
			}
		}
	}

	private IdentityDescription toDescription(String id, UserMailIdentity identity) {
		IdentityDescription desc = new IdentityDescription();
		desc.id = id;
		desc.displayname = identity.displayname;
		desc.email = identity.email;
		desc.isDefault = identity.isDefault;
		desc.mbox = identity.mailboxUid;
		desc.name = identity.name;
		return desc;
	}

	private boolean identityFollowsDefaultEmail(UserMailIdentity identity, ItemValue<Mailbox> mbox,
			Email previousDefaultEmail) {
		return null != previousDefaultEmail && identity.email.equals(previousDefaultEmail.address)
				&& null != mbox.value.defaultEmail()
				&& !previousDefaultEmail.address.equals(mbox.value.defaultEmail().address);
	}

	private ItemValue<Mailbox> getReferencedMailbox(String identityBox, ItemValue<Mailbox> userMbox, String domainUid,
			BmContext context) {
		ItemValue<Mailbox> referencedMailbox = null;
		if (!identityBox.equals(userMbox.uid)) {
			referencedMailbox = context.provider().instance(IMailboxes.class, domainUid).getComplete(identityBox);
		} else {
			referencedMailbox = userMbox;
		}
		return referencedMailbox;
	}

	private void sanitizeIdentityIntegrity(String identityEmail, ItemValue<Mailbox> referencedMailbox,
			BmContext context, String domainUid, Runnable sanitizer) {
		boolean addressFound = false;
		if (null != referencedMailbox) {
			ItemValue<Domain> domain = context.su().provider().instance(IDomains.class).get(domainUid);

			String[] smail = identityEmail.split("@");
			for (Email email : referencedMailbox.value.emails) {
				if (email.allAliases) {
					if (smail[0].equals(email.address.split("@")[0])
							&& (smail[1].equals(domain.uid) || domain.value.aliases.contains(smail[1]))) {
						addressFound = true;
						break;
					}
				} else {
					if (email.address.equals(identityEmail)) {
						addressFound = true;
						break;
					}
				}
			}
		}
		if (!addressFound) {
			sanitizer.run();
		}
	}

}
