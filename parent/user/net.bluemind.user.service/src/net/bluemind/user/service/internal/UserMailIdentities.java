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

import net.bluemind.authentication.api.incore.IInCoreAuthentication;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.ContainerQuery;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.domain.api.Domain;
import net.bluemind.mailbox.api.IMailboxAclUids;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.identity.api.IMailboxIdentity;
import net.bluemind.mailbox.identity.api.IdentityDescription;
import net.bluemind.mailbox.identity.api.SignatureFormat;
import net.bluemind.role.api.BasicRoles;
import net.bluemind.user.api.IInternalUserMailIdentities;
import net.bluemind.user.api.IUserMailIdentities;
import net.bluemind.user.api.UserMailIdentity;
import net.bluemind.user.hook.identity.IUserMailIdentityHook;

public class UserMailIdentities implements IUserMailIdentities, IInternalUserMailIdentities {

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
		create(id, identity, true);
	}

	private void create(String id, UserMailIdentity identity, boolean validate) {
		rbacManager.forEntry(userUid).check(BasicRoles.ROLE_MANAGE_USER_MAIL_IDENTITIES);
		if (validate) {
			validator.validate(identity);
			checkMailboxAclContainer(identity);
		}

		UserMailIdentity userMailIdentity = get(id);
		if (userMailIdentity != null) {
			throw new ServerFault(String.format("Identity id %s of user %s already exists", id, userUid));
		}
		hooks.forEach(hook -> hook.beforeCreate(context, domainUid, id, identity));
		storeService.createIdentity(userUid, id, identity);

		hooks.forEach(hook -> hook.onIdentityCreated(context, domainUid, userUid, id, identity));
	}

	@Override
	public void update(String id, UserMailIdentity identity) {
		update(id, identity, true);
	}

	private void update(String id, UserMailIdentity identity, boolean validate) {
		rbacManager.forEntry(userUid).check(BasicRoles.ROLE_MANAGE_USER_MAIL_IDENTITIES);
		if (validate) {
			validator.validate(identity);
			checkMailboxAclContainer(identity);
		}

		UserMailIdentity previousIdentity = get(id);
		hooks.forEach(hook -> hook.beforeUpdate(context, domainUid, id, identity, previousIdentity));
		storeService.updateIdentity(userUid, id, identity);
		hooks.forEach(hook -> hook.onIdentityUpdated(context, domainUid, userUid, id, identity, previousIdentity));
	}

	private void checkMailboxAclContainer(UserMailIdentity identity) {
		if (identity.mailboxUid == null) {
			return;
		}

		try {
			Container mailboxAclContainer = containerStore.get(IMailboxAclUids.uidForMailbox(identity.mailboxUid));

			if (mailboxAclContainer != null) {
				rbacManager.forContainer(mailboxAclContainer).check(Verb.SendAs.name(), Verb.SendOnBehalf.name(),
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
		if (identity == null) {
			throw new ServerFault("Identity " + id + " not found", ErrorCode.NOT_FOUND);
		}
		validator.beforeDelete(identity);
		hooks.forEach(hook -> hook.beforeDelete(context, domainUid, id, identity));
		storeService.deleteIdentity(userUid, id);
		hooks.forEach(hook -> hook.onIdentityDeleted(context, domainUid, userUid, id, identity));
	}

	@Override
	public UserMailIdentity get(String id) {
		rbacManager.forEntry(userUid).check(BasicRoles.ROLE_SELF, BasicRoles.ROLE_MANAGE_USER_MAIL_IDENTITIES);

		return storeService.getIdentity(userUid, id);
	}

	private List<String> getMailboxAclUids() {
		IContainers containers = null;
		if (context.getSecurityContext().getSubject().equals(userUid)) {
			containers = context.provider().instance(IContainers.class);
		} else {
			IInCoreAuthentication coreAuth = context.su().provider().instance(IInCoreAuthentication.class);
			SecurityContext fullUserCtx = coreAuth.buildContext(domainUid, userUid);
			containers = ServerSideServiceProvider.getProvider(fullUserCtx).instance(IContainers.class);
		}

		ContainerQuery query = ContainerQuery.type(IMailboxAclUids.TYPE);
		query.verb = Arrays.asList(Verb.SendOnBehalf, Verb.SendAs);
		List<ContainerDescriptor> descriptors = containers.all(query);

		return descriptors.stream().filter(d -> d.domainUid.equals(domainUid))
				.map(d -> d.uid.substring(IMailboxAclUids.MAILBOX_ACL_PREFIX.length())).toList();
	}

	@Override
	public List<IdentityDescription> getIdentities() {
		rbacManager.forEntry(userUid).check(BasicRoles.ROLE_SELF, BasicRoles.ROLE_MANAGE_USER_MAIL_IDENTITIES);

		List<String> mboxUids = getMailboxAclUids();

		return storeService.getIdentities(userUid).stream().filter(i -> i.mbox == null || mboxUids.contains(i.mbox))
				.toList();
	}

	@Override
	public List<IdentityDescription> getAvailableIdentities() {
		rbacManager.forEntry(userUid).check(BasicRoles.ROLE_SELF, BasicRoles.ROLE_MANAGE_USER_MAIL_IDENTITIES);

		List<String> mailboxUids = getMailboxAclUids();
		List<IdentityDescription> descrs = new LinkedList<>();
		for (String mboxUid : mailboxUids) {
			IMailboxIdentity mboxIdentity = context.su().provider().instance(IMailboxIdentity.class, domainUid,
					mboxUid);
			descrs.addAll(mboxIdentity.getPossibleIdentities());
		}
		return descrs;
	}

	@Override
	public void setDefault(String id) {
		rbacManager.forEntry(userUid).check(BasicRoles.ROLE_MANAGE_USER_MAIL_IDENTITIES);
		storeService.setDefaultIdentify(userUid, id);
		hooks.forEach(hook -> hook.onIdentityDefault(context, domainUid, userUid, id));
	}

	@Override
	public void createDefaultIdentity(ItemValue<Mailbox> mailboxItem, DirEntry dirEntry) throws ServerFault {
		UserMailIdentity umi = new UserMailIdentity();
		umi.mailboxUid = mailboxItem.uid;
		umi.email = mailboxItem.value.defaultEmail().address;
		umi.format = SignatureFormat.HTML;
		umi.signature = "";
		umi.sentFolder = "Sent";
		umi.displayname = dirEntry.displayName;
		umi.name = umi.displayname;
		umi.isDefault = true;

		create("default", umi);
		setDefault("default");
	}

	@Override
	public void restore(ItemValue<UserMailIdentity> item, boolean isCreate) {
		if (isCreate) {
			create(item.uid, item.value, false);
		} else {
			update(item.uid, item.value, false);
		}
	}
}
