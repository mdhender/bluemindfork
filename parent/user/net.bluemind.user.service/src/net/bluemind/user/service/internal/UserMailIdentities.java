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

import java.sql.SQLException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.ContainerQuery;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.rest.BmContext;
import net.bluemind.domain.api.Domain;
import net.bluemind.mailbox.api.IMailboxAclUids;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.identity.api.IMailboxIdentity;
import net.bluemind.mailbox.identity.api.IdentityDescription;
import net.bluemind.role.api.BasicRoles;
import net.bluemind.user.api.IUserMailIdentities;
import net.bluemind.user.api.UserMailIdentity;
import net.bluemind.user.hook.identity.IUserMailIdentityHook;

public class UserMailIdentities implements IUserMailIdentities {

	private ContainerUserStoreService storeService;
	private String userUid;
	private UserMailIdentityValidator validator;
	private BmContext context;
	private String domainUid;
	private ContainerStore containerStore;
	private RBACManager rbacManager;
	private List<IUserMailIdentityHook> hooks;

	public UserMailIdentities(BmContext context, ItemValue<Domain> domain, Container usersContainer, String userUid,
			List<IUserMailIdentityHook> hooks) {
		this.context = context;
		this.domainUid = domain.uid;

		this.userUid = userUid;
		this.hooks = hooks;
		storeService = new ContainerUserStoreService(context, usersContainer, domain, "global.virt".equals(domainUid));
		validator = new UserMailIdentityValidator(context.su().provider().instance(IMailboxes.class, domainUid),
				domainUid, domain.value.aliases, context.getSecurityContext());

		containerStore = new ContainerStore(context, context.getDataSource(), context.getSecurityContext());

		rbacManager = new RBACManager(context).forDomain(domainUid);

	}

	@Override
	public void create(String id, UserMailIdentity identity) {
		rbacManager.forEntry(userUid).check(BasicRoles.ROLE_MANAGE_USER_MAIL_IDENTITIES);
		validator.validate(identity);

		checkMailboxAclContainer(identity);

		UserMailIdentity userMailIdentity = get(id);
		if (userMailIdentity != null) {
			throw new ServerFault(String.format("Identity id %s of user %s already exists", id, userUid));
		}

		hooks.forEach(hook -> hook.beforeCreate(context, domainUid, id, identity));
		storeService.createIdentity(userUid, id, identity);
	}

	@Override
	public void update(String id, UserMailIdentity identity) {
		rbacManager.forEntry(userUid).check(BasicRoles.ROLE_MANAGE_USER_MAIL_IDENTITIES);
		validator.validate(identity);

		checkMailboxAclContainer(identity);
		UserMailIdentity previousIdentity = get(id);
		hooks.forEach(hook -> hook.beforeUpdate(context, domainUid, id, identity, previousIdentity));
		storeService.updateIdentity(userUid, id, identity);
	}

	private void checkMailboxAclContainer(UserMailIdentity identity) {
		if (identity.mailboxUid == null) {
			return;
		}
		// FIXME not sure it is necessary
		try {
			Container mailboxAclContainer = containerStore.get(IMailboxAclUids.uidForMailbox(identity.mailboxUid));

			if (mailboxAclContainer != null) {
				rbacManager.forContainer(mailboxAclContainer).check(Verb.Write.name(),
						BasicRoles.ROLE_MANAGE_USER_MAIL_IDENTITIES);
			} else {
				throw new ServerFault(
						"Mailbox container ACL not found " + IMailboxAclUids.uidForMailbox(identity.mailboxUid));
			}

		} catch (SQLException e) {
			ServerFault.sqlFault(e);
		}
	}

	@Override
	public void delete(String id) {
		rbacManager.forEntry(userUid).check(BasicRoles.ROLE_MANAGE_USER_MAIL_IDENTITIES);
		UserMailIdentity identity = get(id);
		hooks.forEach(hook -> hook.beforeDelete(context, domainUid, id, identity));
		storeService.deleteIdentity(userUid, id);
	}

	@Override
	public UserMailIdentity get(String id) {
		rbacManager.forEntry(userUid).check(BasicRoles.ROLE_SELF, BasicRoles.ROLE_MANAGE_USER_MAIL_IDENTITIES);

		return storeService.getIdentity(userUid, id);
	}

	@Override
	public List<IdentityDescription> getIdentities() {
		rbacManager.forEntry(userUid).check(BasicRoles.ROLE_SELF, BasicRoles.ROLE_MANAGE_USER_MAIL_IDENTITIES);
		return storeService.getIdentities(userUid);
	}

	@Override
	public List<IdentityDescription> getAvailableIdentities() {

		rbacManager.forEntry(userUid).check(BasicRoles.ROLE_SELF, BasicRoles.ROLE_MANAGE_USER_MAIL_IDENTITIES);

		IContainers containers = null;
		if (context.getSecurityContext().getSubject().equals(userUid)) {
			containers = context.provider().instance(IContainers.class);
		} else {
			containers = context.su(userUid, domainUid).provider().instance(IContainers.class);
		}
		// FIXME userUid != context subject

		ContainerQuery query = ContainerQuery.type(IMailboxAclUids.TYPE);
		query.verb = Arrays.asList(Verb.SendOnBehalf, Verb.Write, Verb.All);
		List<ContainerDescriptor> descriptors = containers.all(query);

		List<IdentityDescription> descrs = new LinkedList<>();
		for (ContainerDescriptor descriptor : descriptors) {
			if (descriptor.domainUid.equals(domainUid)) {
				String mboxUid = descriptor.uid.substring(IMailboxAclUids.MAILBOX_ACL_PREFIX.length());
				IMailboxIdentity mboxIdentity = context.su().provider().instance(IMailboxIdentity.class, domainUid,
						mboxUid);
				descrs.addAll(mboxIdentity.getPossibleIdentities());
			}
		}
		return descrs;
	}

	@Override
	public void setDefault(String id) {
		rbacManager.forEntry(userUid).check(BasicRoles.ROLE_MANAGE_USER_MAIL_IDENTITIES);
		storeService.setDefaultIdentify(userUid, id);
	}
}