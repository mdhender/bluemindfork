/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2023
  *
  * This file is part of Blue Mind. Blue Mind is a messaging and collaborative
  * solution.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of either the GNU Affero General Public License as
  * published by the Free Software Foundation (version 3 of the License)
  * or the CeCILL as published by CeCILL.info (version 2 of the License).
  *
  * There are special exceptions to the terms and conditions of the
  * licenses as they are applied to this program. See LICENSE.txt in
  * the directory of this program distribution.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.central.reverse.proxy.model.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.common.base.Strings;

import net.bluemind.central.reverse.proxy.model.PostfixMapsStorage;
import net.bluemind.central.reverse.proxy.model.common.DirInfo.DirEmail;
import net.bluemind.central.reverse.proxy.model.impl.postfix.Domains;
import net.bluemind.central.reverse.proxy.model.impl.postfix.Domains.DomainAliases;
import net.bluemind.central.reverse.proxy.model.impl.postfix.Domains.DomainSettings;
import net.bluemind.central.reverse.proxy.model.impl.postfix.EmailRecipients;
import net.bluemind.central.reverse.proxy.model.impl.postfix.EmailRecipients.Recipient;
import net.bluemind.central.reverse.proxy.model.impl.postfix.Emails;
import net.bluemind.central.reverse.proxy.model.impl.postfix.Emails.EmailParts;
import net.bluemind.central.reverse.proxy.model.impl.postfix.Emails.EmailUid;
import net.bluemind.central.reverse.proxy.model.impl.postfix.Mailboxes;
import net.bluemind.central.reverse.proxy.model.impl.postfix.Mailboxes.Mailbox;
import net.bluemind.lib.srs.SrsData;
import net.bluemind.lib.srs.SrsHash;

public class HashMapPostfixMapsStorage implements PostfixMapsStorage {
	private String installationUid;
	private final Domains domains = new Domains();
	private final Map<String, String> dataLocationsUidIp = new HashMap<>();
	private final Mailboxes mailboxes = new Mailboxes();
	private final EmailRecipients emailUidRecipients = new EmailRecipients();
	private final Emails emails = new Emails();

	@Override
	public void updateInstallationUid(String installationUid) {
		this.installationUid = installationUid;
	}

	@Override
	public void updateDataLocation(String uid, String ip) {
		dataLocationsUidIp.put(uid, ip);
	}

	@Override
	public void removeDataLocation(String uid) {
		dataLocationsUidIp.remove(uid);
	}

	@Override
	public void updateDomain(String domainUid, Set<String> aliases) {
		domains.updateDomainAliases(domainUid, aliases);
	}

	@Override
	public Collection<String> domainAliases(String domainUid) {
		return Optional.ofNullable(domains.getDomainAliases(domainUid)).map(DomainAliases::aliases).orElse(null);
	}

	@Override
	public boolean domainManaged(String domainAlias) {
		return domains.domainUidFromAlias(domainAlias).isPresent();
	}

	@Override
	public void removeDomain(String domainUid) {
		domains.removeDomain(domainUid);
	}

	@Override
	public void updateDomainSettings(String domainUid, String mailRoutingRelay, boolean mailForwardUnknown) {
		domains.updateDomainSetting(domainUid, mailRoutingRelay, mailForwardUnknown);
	}

	@Override
	public void updateMailbox(String domainUid, String uid, String name, String routing, String dataLocationUid) {
		mailboxes.updateMailbox(domainUid, uid, name, routing, dataLocationUid);
	}

	@Override
	public void removeMailbox(String uid) {
		mailboxes.removeMailbox(uid);
	}

	@Override
	public boolean mailboxManaged(String mailboxOrEmail) {
		Mailbox um = mailboxes.findAnyMailboxByName(mailboxOrEmail).orElse(null);

		if (um != null) {
			if (um.routing().equals("none")) {
				return false;
			}

			if (um.routing().equals("internal")) {
				return true;
			}
		}

		// Perhaps mailbox is from a domain that support split domain ?
		EmailParts emailParts = EmailParts.fromEmail(mailboxOrEmail).orElse(null);
		if (emailParts == null) {
			return false;
		}

		String domainUid = domains.domainUidFromAlias(emailParts.domain()).orElse(null);
		if (domainUid == null) {
			return false;
		}

		DomainSettings domainSettings = domains.getDomainSettings(domainUid);
		if (domainSettings == null || Strings.isNullOrEmpty(domainSettings.mailRoutingRelay())) {
			return false;
		}

		if (domainSettings.mailForwardUnknown()) {
			// ... yes and forward unknown is enabled
			return true;
		}

		DomainAliases da = domains.getDomainAliases(domainUid);
		if (da == null) {
			return false;
		}

		// ... yes and email mailbox routing is external.
		return emails.getEmail(emailParts, da.aliasOnly()).map(email -> mailboxes.getMailboxByUid(email.uid()))
				.filter(m -> m.routing().equals("external")).isPresent();
	}

	@Override
	public String mailboxRelay(String mailboxName) {
		Mailbox m = mailboxes.findAnyMailboxByName(mailboxName).orElse(null);

		if (m == null) {
			return unknownMailboxRelay(mailboxName);
		}

		return Optional.ofNullable(mailboxRelay(m)).map(Strings::nullToEmpty).orElse(null);
	}

	private String unknownMailboxRelay(String email) {
		// Perhaps mailbox is from a domain that support split domain ?
		EmailParts emailParts = EmailParts.fromEmail(email).orElse(null);
		if (emailParts == null) {
			return null;
		}

		String domainUid = domains.domainUidFromAlias(emailParts.domain()).orElse(null);
		if (domainUid == null) {
			return null;
		}

		if (emailParts.domain().equals(domainUid)) {
			return null;
		}

		DomainSettings domainSettings = domains.getDomainSettings(domainUid);
		if (domainSettings == null || Strings.isNullOrEmpty(domainSettings.mailRoutingRelay())) {
			return null;
		}

		DomainAliases da = domains.getDomainAliases(domainUid);
		if (da == null) {
			return null;
		}

		Optional<EmailUid> emailUid = emails.getEmail(emailParts, da);

		if (domainSettings.mailForwardUnknown() && emailUid.isEmpty()) {
			// Forward unknown is enabled and email is unknown
			return "smtp:" + domainSettings.mailRoutingRelay() + ":25";
		}

		// Email is known, ensure routing is external
		return emailUid.map(EmailUid::uid).map(mailboxes::getMailboxByUid)
				.filter(mailbox -> mailbox.routing().equals("external"))
				.map(mailbox -> "smtp:" + domainSettings.mailRoutingRelay() + ":25").orElse(null);
	}

	private String mailboxRelay(Mailbox uidMailbox) {
		if (uidMailbox.routing() == null || !uidMailbox.routing().equals("internal")) {
			return null;
		}

		return Optional.ofNullable(uidMailbox.dataLocationUid()).map(Strings::emptyToNull).map(dataLocationsUidIp::get)
				.map(Strings::emptyToNull).map(ip -> "lmtp:" + ip + ":2400").orElse(null);
	}

	@Override
	public void addRecipient(String groupUid, String recipientType, String recipientUid) {
		emailUidRecipients.addEmailRecipient(groupUid, recipientType, recipientUid);
	}

	@Override
	public void removeRecipient(String groupUid, String recipientType, String recipientUid) {
		emailUidRecipients.removeEmailRecipient(groupUid, recipientType, recipientUid);
	}

	@Override
	public Collection<String> aliasToMailboxes(String alias) {
		EmailParts aliasParts = EmailParts.fromEmail(alias).orElse(null);
		if (aliasParts == null) {
			return new ArrayList<>();
		}

		String domainUid = domains.domainUidFromAlias(aliasParts.domain()).orElse(null);
		if (domainUid == null) {
			return new ArrayList<>();
		}

		DomainAliases da = domains.getDomainAliases(domainUid);
		if (da == null) {
			return new ArrayList<>();
		}

		Collection<String> emailMailboxes = emails.getEmail(aliasParts, da)
				.flatMap(emailUid -> aliasToMailboxes(aliasParts, emailUid.uid())).orElseGet(ArrayList::new);
		if (!emailMailboxes.isEmpty()) {
			return emailMailboxes;
		}

		emailMailboxes = getMailbox(aliasParts).flatMap(mu -> mailboxName(aliasParts, mu)).orElseGet(ArrayList::new);

		if (!emailMailboxes.isEmpty()) {
			return emailMailboxes;
		}

		if (Optional.of(domainUid).map(domains::getDomainSettings).filter(ds -> ds.mailForwardUnknown())
				.map(DomainSettings::mailRoutingRelay).isPresent()) {
			return Arrays.asList(alias);
		}

		return new ArrayList<>();
	}

	private Optional<Mailbox> getMailbox(EmailParts emailParts) {
		return mailboxes.findAnyMailboxByName(emailParts.email())
				.filter(mailbox -> mailbox.routing().equals("external"));
	}

	private Optional<Collection<String>> aliasToMailboxes(EmailParts queryEmailParts, String aliasUid) {
		Set<Recipient> recipients = emailUidRecipients.getRecipients(aliasUid);
		if (recipients != null) {
			// Email has recipients (group)
			return Optional.of(recipientsToMailboxes(queryEmailParts, recipients));
		}

		Mailbox mailboxUid = mailboxes.getMailboxByUid(aliasUid);
		return (mailboxUid == null) ? Optional.empty() : mailboxName(queryEmailParts, mailboxUid);
	}

	private Collection<String> recipientsToMailboxes(EmailParts queryEmailParts, Set<Recipient> recipients) {
		return recipients.stream().map(recipient -> recipientToMailbox(queryEmailParts, recipient))
				.flatMap(Collection::stream).toList();
	}

	private Collection<String> recipientToMailbox(EmailParts queryEmailParts, Recipient recipient) {
		if (recipient.type().equalsIgnoreCase("group")) {
			if (!emailUidRecipients.hasRecipients(recipient.uid())) {
				return new ArrayList<>();
			}

			return emails.getEmailByUid(recipient.uid()).map(emailUid -> emailUid.email().getEmail())
					.map(Arrays::asList).orElseGet(ArrayList::new);
		}

		return Optional.ofNullable(mailboxes.getMailboxByUid(recipient.uid()))
				.flatMap(um -> mailboxName(queryEmailParts, um)).orElseGet(ArrayList::new);
	}

	private Optional<Collection<String>> mailboxName(EmailParts queryEmailParts, Mailbox mailbox) {
		switch (mailbox.routing()) {
		case "internal":
			return Optional.of(new ArrayList<>(Arrays.asList(mailbox.name())));

		case "external":
			String domainUid = domains.domainUidFromAlias(queryEmailParts.domain()).orElse(null);
			if (domainUid == null) {
				return Optional.empty();
			}

			DomainAliases da = domains.getDomainAliases(domainUid);
			if (da == null) {
				return Optional.empty();
			}

			return emails.getEmailByUid(mailbox.uid()).map(emailUid -> emailUid.email().getEmail()).map(Arrays::asList);

		default:
			return Optional.empty();
		}
	}

	@Override
	public void updateEmails(String uid, Collection<DirEmail> dirEmails) {
		emails.update(uid, dirEmails);
	}

	@Override
	public void removeUid(String uid) {
		emails.remove(uid);
		emailUidRecipients.remove(uid);
		removeMailbox(uid);
	}

	@Override
	public String srsRecipient(String recipient) {
		if (Strings.isNullOrEmpty(recipient)) {
			return null;
		}

		EmailParts emailParts = EmailParts.fromEmail(recipient).filter(ep -> domainManaged(ep.domain())).orElse(null);
		if (emailParts == null) {
			return null;
		}

		return SrsHash.build(installationUid).flatMap(srsHash -> SrsData.fromLeftPart(srsHash, emailParts.left()))
				.map(SrsData::originalEmail).orElse(null);
	}
}
