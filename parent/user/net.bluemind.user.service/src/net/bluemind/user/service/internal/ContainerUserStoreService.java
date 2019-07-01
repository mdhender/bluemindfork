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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.persistance.ContainerStore;
import net.bluemind.core.rest.BmContext;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.service.DirEntryAndValue;
import net.bluemind.directory.service.DirValueStoreService;
import net.bluemind.domain.api.Domain;
import net.bluemind.mailbox.identity.api.IdentityDescription;
import net.bluemind.role.persistence.RoleStore;
import net.bluemind.user.api.User;
import net.bluemind.user.api.UserMailIdentity;
import net.bluemind.user.persistance.UserMailIdentityStore;
import net.bluemind.user.persistance.UserSettingsStore;
import net.bluemind.user.persistance.UserStore;
import net.bluemind.user.persistance.UserSubscriptionStore;

public class ContainerUserStoreService extends DirValueStoreService<User> {

	public static class UserDirEntryAdapter implements DirEntryAdapter<User> {

		@Override
		public DirEntry asDirEntry(String domainUid, String uid, User user) {
			return DirEntry.create(user.orgUnitUid, domainUid + "/users/" + uid, DirEntry.Kind.USER, uid,
					getSummary(user), user.defaultEmail() != null ? user.defaultEmail().address : null, user.hidden,
					user.system, user.archived, user.dataLocation, user.accountType);
		}

	}

	/**
	 * @param user
	 * @return
	 */
	private static String getSummary(User user) {
		if (user.contactInfos != null && user.contactInfos.identification.formatedName.value != null) {
			return user.contactInfos.identification.formatedName.value;
		} else {
			return user.login;
		}
	}

	private UserMailIdentityStore identityStore;

	private UserSettingsStore userSettingsStore;

	private UserStore userStore;

	private ContainerStore containerStore;

	private UserSubscriptionStore userSubscriptionStore;

	public ContainerUserStoreService(BmContext context, Container container, ItemValue<Domain> domain) {
		this(context, container, domain, domain.uid.equals("global.virt"));
	}

	public ContainerUserStoreService(BmContext context, Container container, ItemValue<Domain> domain,
			boolean globalVirt) {
		super(context, context.getDataSource(), context.getSecurityContext(), domain, container, "user",
				DirEntry.Kind.USER, new UserStore(context.getDataSource(), container), new UserDirEntryAdapter(),
				new UserVCardAdapter(), UserMailboxAdapter.create(globalVirt));
		identityStore = new UserMailIdentityStore(context.getDataSource(), container);

		userStore = new UserStore(context.getDataSource(), container);

		userSettingsStore = new UserSettingsStore(context.getDataSource(), container);

		roleStore = new RoleStore(context.getDataSource(), container);

		containerStore = new ContainerStore(context, context.getDataSource(), context.getSecurityContext());

		userSubscriptionStore = new UserSubscriptionStore(context.getSecurityContext(), context.getDataSource(),
				container);
	}

	@Override
	protected void decorate(Item item, ItemValue<DirEntryAndValue<User>> value) throws ServerFault {
		super.decorate(item, value);
		User user = value.value.value;
		user.contactInfos = value.value.vcard;
		if (value.value.mailbox != null) {
			user.routing = value.value.mailbox.routing;
			user.quota = value.value.mailbox.quota;
			user.emails = value.value.mailbox.emails;
			user.dataLocation = value.value.mailbox.dataLocation;
			user.accountType = value.value.entry.accountType;
			user.orgUnitUid = value.value.entry.orgUnitUid;
		}

	}

	@Override
	protected void deleteValue(Item item) throws ServerFault, SQLException {
		roleStore.set(item, new HashSet<String>());
		identityStore.delete(item);
		userSettingsStore.delete(item);
		userSubscriptionStore.unsubscribeAll(item.uid);
		super.deleteValue(item);
	}

	protected void deleteValues() throws ServerFault {
		throw new ServerFault("Should not be called !");
		// try {
		// roleStore.deleteAll();
		// identityStore.deleteAll();
		// userSettingsStore.deleteAll();
		// } catch (SQLException e) {
		// throw ServerFault.sqlFault(e);
		// }
		// super.deleteValues();
	}

	public void createIdentity(String uid, String id, UserMailIdentity identity) throws ServerFault {
		doOrFail(() -> {

			Item item = itemStore.get(uid);
			if (item == null) {
				throw new ServerFault("user " + uid + " doesnt exists", ErrorCode.NOT_FOUND);
			}

			identityStore.create(item, id, identity);
			return null;
		});
	}

	public void updateIdentity(String uid, String id, UserMailIdentity identity) throws ServerFault {
		doOrFail(() -> {
			Item item = itemStore.get(uid);
			if (item == null) {
				throw new ServerFault("user " + uid + " doesnt exists", ErrorCode.NOT_FOUND);
			}

			identityStore.update(item, id, identity);
			return null;
		});
	}

	public void deleteIdentity(String uid, String id) throws ServerFault {
		doOrFail(() -> {

			Item item = itemStore.get(uid);
			if (item == null) {
				throw new ServerFault("user " + uid + " doesnt exists", ErrorCode.NOT_FOUND);
			}

			identityStore.delete(item, id);
			return null;
		});

	}

	public UserMailIdentity getIdentity(String uid, String id) throws ServerFault {
		try {
			Item item = itemStore.get(uid);
			if (item == null) {
				throw new ServerFault("user " + uid + " doesnt exists", ErrorCode.NOT_FOUND);
			}

			return identityStore.get(item, id);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	public List<IdentityDescription> getIdentities(String uid) throws ServerFault {
		try {
			Item item = itemStore.get(uid);
			if (item == null) {
				throw new ServerFault("user " + uid + " doesnt exists", ErrorCode.NOT_FOUND);
			}

			return identityStore.getDescriptions(item);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	public void setPassword(String uid, String password) throws ServerFault {
		doOrFail(() -> {
			userStore.setPassword(getItemStore().get(uid), password);
			return null;
		});
	}

	public Set<String> getItemsWithRoles(List<String> roles) throws ServerFault {
		try {
			return roleStore.getItemsWithRoles(roles);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}

	}

	public void setDefaultIdentify(String uid, String id) throws ServerFault {
		doOrFail(() -> {
			Item item = itemStore.get(uid);
			if (item == null) {
				throw new ServerFault("user " + uid + " doesnt exists", ErrorCode.NOT_FOUND);
			}

			identityStore.setDefault(item, id);
			return null;
		});

	}

	public String findByLogin(String login) throws ServerFault {
		try {
			return userStore.byLogin(login);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	public boolean allValid(String[] usersUids) throws ServerFault {
		try {
			return userStore.areValid(usersUids);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	@Override
	public List<String> allUids() throws ServerFault {
		try {
			return userStore.allUids();
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	@Deprecated
	public void create(String uid, String displayName, User value) throws ServerFault {
		create(uid, value);
	}

	public void deleteMailboxIdentities(String userUid, String mailboxUid) throws ServerFault {
		doOrFail(() -> {
			Item item = getItemStore().getForUpdate(userUid);
			if (item == null) {
				throw new ServerFault("user " + userUid + " doesnt exists", ErrorCode.NOT_FOUND);
			}
			identityStore.deleteMailboxIdentities(item, mailboxUid);
			return null;
		});
	}

	public void deleteMailboxIdentities(String mailboxUid) throws ServerFault {
		doOrFail(() -> {
			identityStore.deleteMailboxIdentities(mailboxUid);
			return null;
		});
	}

	@Override
	protected byte[] getDefaultImage() {
		return UserDefaultImage.DEFAULT_INDIV_ICON;
	}

}
