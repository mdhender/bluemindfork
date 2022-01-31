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

package net.bluemind.mailbox.service.internal;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import net.bluemind.config.InstallationId;
import net.bluemind.core.api.Email;
import net.bluemind.core.api.fault.ErrorCode;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.Item;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.persistence.ItemStore;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.email.EmailHelper;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.domain.api.DomainSettingsKeys;
import net.bluemind.domain.api.IDomainSettings;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.mailbox.api.Mailbox.Type;
import net.bluemind.mailbox.persistence.MailboxStore;
import net.bluemind.mailbox.service.SplittedShardsMapping;
import net.bluemind.server.api.Assignment;
import net.bluemind.server.api.IServer;
import net.bluemind.server.api.Server;

/**
 * Checks a {@link Mailbox} object to ensure it is in a sane state for CRUD
 * operations.
 *
 */
public class MailboxValidator {

	private final BmContext context;
	private final String domainUid;
	private final MailboxStore mailboxStore;
	private final ItemStore itemStore;

	public MailboxValidator(BmContext context, String domainUid, MailboxStore mailshareStore, ItemStore itemStore) {
		this.context = context;
		this.domainUid = domainUid;
		this.mailboxStore = mailshareStore;
		this.itemStore = itemStore;
	}

	/**
	 * Performs mailbox validation
	 * 
	 * @param mailbox the object to check
	 * @string uid the expected uid of the object to check
	 * @throws ServerFault
	 */
	public void validate(Mailbox mailbox, String uid) throws ServerFault {

		if (mailbox == null) {
			throw new ServerFault("Mailbox is null", ErrorCode.INVALID_PARAMETER);
		}

		if (mailbox.type == null) {
			throw new ServerFault("Mailbox.type must be set for " + uid, ErrorCode.INVALID_PARAMETER);
		}

		if (isNullOrEmpty(mailbox.name)) {
			throw new ServerFault("Mailbox.name must be set for " + uid, ErrorCode.INVALID_PARAMETER);
		}

		validateRouting(mailbox);

		Long itemId = null;
		Item item = null;
		try {
			item = itemStore.get(uid);
		} catch (SQLException e) {
			throw new ServerFault(e);
		}
		if (item != null) {
			itemId = item.id;
		}

		try {
			if (mailboxStore.nameAlreadyUsed(itemId, mailbox)) {
				throw new ServerFault("Mail name: " + mailbox.name + " already used", ErrorCode.ALREADY_EXISTS);
			}
		} catch (SQLException sqle) {
			throw ServerFault.sqlFault(sqle);
		}

		if (!isNullOrEmpty(mailbox.emails)) {
			EmailHelper.validate(mailbox.emails);
			validateEmailsListIntegrity(mailbox.emails);
			try {
				if (mailboxStore.emailAlreadyUsed(itemId, mailbox.emails)) {
					Set<String> existingMails = new HashSet<>();
					for (Email mail : mailbox.emails) {
						if (mailboxStore.emailAlreadyUsed(itemId, Arrays.asList(mail))) {
							existingMails.add(mail.address);
						}
					}
					String asString = existingMails.stream().collect(Collectors.joining(","));
					throw new ServerFault("Following emails of mailbox " + uid + ":" + mailbox.name
							+ " are already in use: " + asString, ErrorCode.ALREADY_EXISTS);
				}
			} catch (SQLException sqle) {
				throw ServerFault.sqlFault(sqle);
			}
		}

		validateDataLocation(mailbox);

		if ((mailbox.type == Mailbox.Type.user || mailbox.type == Mailbox.Type.resource)
				&& mailbox.routing != Routing.none && mailbox.emails.isEmpty()) {
			throw new ServerFault("No email address for mailbox: " + mailbox.name + ", routing: " + mailbox.routing,
					ErrorCode.INVALID_PARAMETER);
		}

		validateMaxQuota(uid, mailbox);

	}

	private void validateMaxQuota(String uid, Mailbox mailbox) {
		Map<String, String> domSettings = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM)
				.instance(IDomainSettings.class, domainUid).get();
		int max = 0;
		if (mailbox.type == Mailbox.Type.user
				&& domSettings.containsKey(DomainSettingsKeys.mailbox_max_user_quota.name())) {
			max = Integer.parseInt(domSettings.get(DomainSettingsKeys.mailbox_max_user_quota.name()));
		} else if (mailbox.type == Mailbox.Type.mailshare
				&& domSettings.containsKey(DomainSettingsKeys.mailbox_max_publicfolder_quota.name())) {
			max = Integer.parseInt(domSettings.get(DomainSettingsKeys.mailbox_max_publicfolder_quota.name()));
		}

		if (max == 0) {
			return;
		}

		if (mailbox.quota == null || mailbox.quota == 0 || mailbox.quota > max) {
			throw new ServerFault(
					String.format("Invalid quota for %s. Quota must be less than %d MiB", uid, (max / 1024)),
					ErrorCode.INVALID_PARAMETER);
		}
	}

	private void validateRouting(Mailbox mailbox) {
		if (mailbox.routing == Routing.external) {
			IDomainSettings domSettingsService = context.provider().instance(IDomainSettings.class, domainUid);
			if (!domSettingsService.get().containsKey(DomainSettingsKeys.mail_routing_relay.name())) {
				throw new ServerFault("Cannot set external routing, relay host is not configured");
			}
		}
	}

	private void validateEmailsListIntegrity(Collection<Email> emails) throws ServerFault {
		Map<String, Boolean> allAliasesByleftPart = new HashMap<>();
		List<String> emailsAddress = new ArrayList<>();
		String defaultEmail = null;

		for (Email email : emails) {
			// Throw an error if more than one default email
			if (email.isDefault && defaultEmail != null) {
				throw new ServerFault("There is more than one default address (at least " + email.address + " and "
						+ defaultEmail + ")", ErrorCode.ALREADY_EXISTS);
			}

			if (email.isDefault) {
				defaultEmail = email.address;
			}

			// Throw an error if email address is duplicate
			if (emailsAddress.contains(email.address)) {
				throw new ServerFault(email.address + " is duplicate", ErrorCode.ALREADY_EXISTS);
			}

			emailsAddress.add(email.address);

			// Throw an error if 2 addresses have same left part and one is set
			// as an all aliases address
			String leftPart = email.address.split("@")[0];
			if (allAliasesByleftPart.keySet().contains(leftPart)
					&& (allAliasesByleftPart.get(leftPart) || email.allAliases)) {
				throw new ServerFault(email.address + " is duplicate", ErrorCode.ALREADY_EXISTS);
			}

			allAliasesByleftPart.put(leftPart, email.allAliases);
		}
	}

	private void validateDataLocation(Mailbox mailbox) throws ServerFault {

		if (!mailbox.routing.managed()) {
			return;
		}

		if (mailbox.dataLocation == null || mailbox.dataLocation.trim().isEmpty()) {
			if (mailbox.type != Type.group) {
				throw new ServerFault("Datalocation must be set", ErrorCode.INVALID_PARAMETER);
			} else {
				// a group can be created without mailshare
				return;
			}

		}

		IServer serverService = context.provider().instance(IServer.class, InstallationId.getIdentifier());
		ItemValue<Server> dataLocation = Optional.ofNullable(serverService.getComplete(mailbox.dataLocation)) //
				.map(SplittedShardsMapping::remap) //
				.orElseThrow(() -> new ServerFault("Datalocation " + mailbox.dataLocation + " must exist",
						ErrorCode.INVALID_PARAMETER));

		boolean assigned = false;
		boolean assignedAsImapServer = false;

		List<Assignment> assignments = serverService.getAssignments(domainUid);

		for (Assignment assignment : assignments) {
			if (!assignment.serverUid.equals(dataLocation.uid)) {
				continue;
			}

			assigned = true;
			if (assignment.tag.equals("mail/imap")) {
				assignedAsImapServer = true;
			}
		}

		if (!assigned || !assignedAsImapServer) {
			throw new ServerFault(
					"Datalocation " + mailbox.dataLocation + " not assigned to: " + domainUid + " as mail/imap",
					ErrorCode.INVALID_PARAMETER);
		}
	}

	private boolean isNullOrEmpty(String s) {
		return (s == null || s.trim().isEmpty());
	}

	private boolean isNullOrEmpty(Collection<Email> c) {
		return (c == null || c.isEmpty());
	}

}
