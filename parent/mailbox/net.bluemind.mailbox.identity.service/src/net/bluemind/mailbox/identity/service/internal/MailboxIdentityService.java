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
package net.bluemind.mailbox.identity.service.internal;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import net.bluemind.core.api.Email;
import net.bluemind.core.api.ParametersValidator;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.container.service.internal.RBACManager;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.sanitizer.Sanitizer;
import net.bluemind.core.validator.Validator;
import net.bluemind.domain.api.Domain;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.identity.api.IMailboxIdentity;
import net.bluemind.mailbox.identity.api.Identity;
import net.bluemind.mailbox.identity.api.IdentityDescription;
import net.bluemind.mailbox.identity.persistence.MailboxIdentityStore;
import net.bluemind.role.api.BasicRoles;

public class MailboxIdentityService implements IMailboxIdentity {

	private Item mboxItem;
	private MailboxIdentityStore identityStore;
	private IdentityValidator validator;
	private Mailbox mboxValue;
	private Domain domain;
	private Sanitizer sanitizer;
	private Validator extValidator;
	private RBACManager rbacManager;

	public MailboxIdentityService(BmContext context, Container mboxesContainer, Container boxContainer, Item mboxItem,
			Mailbox mboxValue, Domain domain) throws ServerFault {
		this.mboxItem = mboxItem;
		this.mboxValue = mboxValue;
		this.domain = domain;
		this.identityStore = new MailboxIdentityStore(context.getDataSource());
		this.validator = new IdentityValidator(mboxValue, domain.aliases, domain.name);
		rbacManager = new RBACManager(context).forContainer(boxContainer);
		sanitizer = new Sanitizer(context);
		extValidator = new Validator(context);
	}

	@Override
	public void create(String id, Identity identity) throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_MANAGE_MAILBOX_IDENTITIES);
		sanitizer.create(identity);

		ParametersValidator.notNullAndNotEmpty(id);
		validator.validate(identity);
		extValidator.create(identity);

		Identity existing = get(id);
		if (existing != null) {
			throw new ServerFault(String.format("Identity id %s of mbox %s already exists", id, mboxItem.uid));
		}

		try {
			identityStore.create(mboxItem, id, identity);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	@Override
	public void update(String id, Identity identity) throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_MANAGE_MAILBOX_IDENTITIES);
		Identity previousValue = null;
		try {
			previousValue = identityStore.get(mboxItem, id);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}

		if (previousValue == null) {
			throw new ServerFault("identity " + id + " doesnt exists", ErrorCode.NOT_FOUND);
		}

		sanitizer.update(previousValue, identity);

		ParametersValidator.notNullAndNotEmpty(id);
		validator.validate(identity);

		try {
			identityStore.update(mboxItem, id, identity);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	@Override
	public void delete(String id) throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_MANAGE_MAILBOX_IDENTITIES);
		try {
			Identity previousValue = identityStore.get(mboxItem, id);
			if (previousValue == null) {
				throw new ServerFault("identity " + id + " doesnt exists", ErrorCode.NOT_FOUND);
			}
			identityStore.delete(mboxItem, id);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	@Override
	public Identity get(String id) throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_MANAGE_MAILBOX_IDENTITIES, Verb.Read.name());
		try {
			return identityStore.get(mboxItem, id);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	@Override
	public List<IdentityDescription> getIdentities() throws ServerFault {
		rbacManager.check(BasicRoles.ROLE_MANAGE_MAILBOX_IDENTITIES, Verb.Read.name());
		try {
			final ItemValue<Mailbox> mboxItemValue = ItemValue.create(this.mboxItem, this.mboxValue);
			return identityStore.getDescriptions(mboxItemValue);
		} catch (SQLException e) {
			throw ServerFault.sqlFault(e);
		}
	}

	@Override
	public List<IdentityDescription> getPossibleIdentities() throws ServerFault {
		// getIdentiies call checkReadAccess
		List<IdentityDescription> ids = getIdentities();

		if (mboxValue.type == Mailbox.Type.group) {
			// BM-8324 group does not have implicit identities
			return ids;
		}

		List<IdentityDescription> ret = new ArrayList<>(ids);
		for (Email email : mboxValue.emails) {
			if (email.allAliases) {
				String adr = email.address;
				if (adr.contains("@")) {
					adr = adr.split("@")[0];
				}
				for (String alias : domain.aliases) {
					addIfNotPresent(adr + "@" + alias, ret);
				}

				addIfNotPresent(adr + "@" + domain.name, ret);
			} else {
				addIfNotPresent(email.address, ret);
			}

		}

		return ret;
	}

	private void addIfNotPresent(String address, List<IdentityDescription> ret) {
		for (IdentityDescription d : ret) {
			if (d.email.equals(address)) {
				return;
			}
		}

		// add only if not in ret list
		IdentityDescription id = new IdentityDescription();
		id.email = address;
		id.mbox = mboxItem.uid;
		id.name = mboxValue.name;
		id.id = null;
		ret.add(id);
	}
}
