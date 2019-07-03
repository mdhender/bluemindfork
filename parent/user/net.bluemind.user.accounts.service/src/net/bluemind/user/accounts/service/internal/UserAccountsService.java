/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2017
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
package net.bluemind.user.accounts.service.internal;

import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

import net.bluemind.core.annotationvalidator.AnnotationValidator;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.jdbc.JdbcAbstractStore;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.validator.IValidatorFactory;
import net.bluemind.role.api.BasicRoles;
import net.bluemind.user.accounts.persistence.UserAccountsStore;
import net.bluemind.user.api.IInternalUserExternalAccount;
import net.bluemind.user.api.UserAccount;
import net.bluemind.user.api.UserAccountInfo;

public class UserAccountsService implements IInternalUserExternalAccount {

	private RBACManager rbacManager;
	private final UserAccountsStore store;
	private final Item item;
	private IValidatorFactory<UserAccount> validator;
	private BmContext context;

	public UserAccountsService(BmContext context, Container container, Item item) {
		this.rbacManager = new RBACManager(context).forDomain(container.uid);
		this.store = new UserAccountsStore(context.getDataSource());
		this.item = item;
		validator = new AnnotationValidator.GenericValidatorFactory<>(UserAccount.class);
		this.context = context;
	}

	@Override
	public void create(String systemIdentifier, UserAccount account) throws ServerFault {
		rbacManager.forEntry(item.uid).check(BasicRoles.ROLE_MANAGE_EXTERNAL_ACCOUNTS);
		validator.create(context);

		b64Credentials(account);
		JdbcAbstractStore.doOrFail(() -> {
			store.create(item, systemIdentifier, account);
			return null;
		});
	}

	@Override
	public void update(String systemIdentifier, UserAccount account) throws ServerFault {
		rbacManager.forEntry(item.uid).check(BasicRoles.ROLE_MANAGE_EXTERNAL_ACCOUNTS);
		validator.create(context);

		JdbcAbstractStore.doOrFail(() -> {
			if (null != account.credentials) {
				b64Credentials(account);
			} else {
				account.credentials = store.get(item, systemIdentifier).credentials;
			}
			store.update(item, systemIdentifier, account);
			return null;
		});
	}

	@Override
	public UserAccount get(String systemIdentifier) throws ServerFault {
		rbacManager.forEntry(item.uid).check(BasicRoles.ROLE_MANAGE_EXTERNAL_ACCOUNTS);

		return JdbcAbstractStore.doOrFail(() -> {
			return decorate(store.get(item, systemIdentifier));
		});
	}

	@Override
	public List<UserAccountInfo> getAll() throws ServerFault {
		rbacManager.forEntry(item.uid).check(BasicRoles.ROLE_MANAGE_EXTERNAL_ACCOUNTS);

		return JdbcAbstractStore.doOrFail(() -> {
			return store.getAll(item).stream().map(i -> new UserAccountInfo(decorate(i), i.externalSystemId))
					.collect(Collectors.toList());
		});
	}

	@Override
	public void delete(String systemIdentifier) throws ServerFault {
		rbacManager.forEntry(item.uid).check(BasicRoles.ROLE_MANAGE_EXTERNAL_ACCOUNTS);

		JdbcAbstractStore.doOrFail(() -> {
			store.delete(item, systemIdentifier);
			return null;
		});
	}

	@Override
	public void deleteAll() throws ServerFault {
		rbacManager.forEntry(item.uid).check(BasicRoles.ROLE_MANAGE_EXTERNAL_ACCOUNTS);

		JdbcAbstractStore.doOrFail(() -> {
			store.deleteAll(item);
			return null;
		});
	}

	@Override
	public String getCredentials(String systemIdentifier) throws ServerFault {
		rbacManager.forEntry(item.uid).check(BasicRoles.ROLE_MANAGE_EXTERNAL_ACCOUNTS);

		return JdbcAbstractStore.doOrFail(() -> {
			return deB64Credentials(store.get(item, systemIdentifier).credentials);
		});
	}

	private <T extends UserAccount> T decorate(T account) {
		if (null == account) {
			return null;
		}
		account.credentials = null;
		return account;
	}

	private String deB64Credentials(String credentials) {
		return new String(Base64.getDecoder().decode(credentials));
	}

	private void b64Credentials(UserAccount account) {
		account.credentials = Base64.getEncoder().encodeToString(account.credentials.getBytes());
	}

}
