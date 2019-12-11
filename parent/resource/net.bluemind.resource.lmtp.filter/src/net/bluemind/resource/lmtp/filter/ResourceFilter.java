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
package net.bluemind.resource.lmtp.filter;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.address.Address;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.calendar.api.ICalendarUids;
import net.bluemind.config.Token;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.IContainerManagement;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.AccessControlEntry;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.http.ClientSideServiceProvider;
import net.bluemind.core.sendmail.ISendmail;
import net.bluemind.core.sendmail.Sendmail;
import net.bluemind.core.sendmail.SendmailHelper;
import net.bluemind.directory.api.BaseDirEntry;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.group.api.IGroup;
import net.bluemind.group.api.Member;
import net.bluemind.imip.parser.IIMIPParser;
import net.bluemind.imip.parser.IMIPInfos;
import net.bluemind.imip.parser.IMIPParserFactory;
import net.bluemind.imip.parser.PureICSRewriter;
import net.bluemind.lmtp.backend.FilterException;
import net.bluemind.lmtp.backend.IMessageFilter;
import net.bluemind.lmtp.backend.LmtpAddress;
import net.bluemind.lmtp.backend.LmtpEnvelope;
import net.bluemind.lmtp.backend.LmtpReply;
import net.bluemind.lmtp.filter.imip.cache.MailboxCache;
import net.bluemind.locator.client.LocatorClient;
import net.bluemind.mailbox.api.Mailbox;

public class ResourceFilter implements IMessageFilter {
	private static final Logger logger = LoggerFactory.getLogger(ResourceFilter.class);
	private ISendmail mailer;

	private String coreUrl;

	public ResourceFilter() {
		this(new Sendmail());
	}

	public ResourceFilter(ISendmail mailer) {
		this.mailer = mailer;
	}

	@Override
	public Message filter(LmtpEnvelope env, Message message, long messageSize) throws FilterException {
		IIMIPParser parser = IMIPParserFactory.create();

		Message pureIcs = new PureICSRewriter().rewrite(message);
		IMIPInfos infos = parser.parse(pureIcs);
		if (infos != null) {
			return null;
		}

		IServiceProvider provider = ClientSideServiceProvider.getProvider(getCoreUrl(), Token.admin0());
		List<LmtpAddress> recipients = env.getRecipients();
		if (recipients != null && !recipients.isEmpty()) {
			for (LmtpAddress recipient : recipients) {
				try {
					String mailbox = getResourceMailbox(provider, recipient);
					if (mailbox != null) {
						redirectMessageToResourceAdmins(provider, recipient.getDomainPart(), mailbox, message);
					}
				} catch (ServerFault e) {
					logger.error("[{}] Error while handling resource filter message",
							message.getHeader().getField("Message-ID"), e);
					throw new FilterException(LmtpReply.TEMPORARY_FAILURE,
							"[" + message.getHeader().getField("Message-ID")
									+ "] Error while handling resource filter message: " + e.getMessage());
				}
			}
		}

		return null;
	}

	private String getCoreUrl() {
		if (coreUrl == null) {
			try {
				LocatorClient lc = new LocatorClient();
				coreUrl = "http://" + lc.locateHost("bm/core", "admin0@global.virt") + ":8090";
			} catch (Exception e) {

			}
		}
		return coreUrl;
	}

	private String getResourceMailbox(IServiceProvider provider, LmtpAddress recipient) {
		String mbox = lmtpRecipientToMailboxName(recipient.getEmailAddress());
		Optional<ItemValue<Mailbox>> mailbox = MailboxCache.get(provider, recipient.getDomainPart(), mbox);

		if (!mailbox.isPresent() || mailbox.get().value.type != Mailbox.Type.resource) {
			return null;
		}
		return mailbox.get().uid;
	}

	private String lmtpRecipientToMailboxName(String lmtpRecipient) {
		if (lmtpRecipient.startsWith("+")) {
			lmtpRecipient = lmtpRecipient.substring(1);
		}

		return lmtpRecipient.split("@")[0];
	}

	private void redirectMessageToResourceAdmins(IServiceProvider provider, String domainUid, String mailbox,
			Message message) {
		message.setTo((Address) null);
		message.setCc((Address) null);
		message.setBcc((Address) null);

		Collection<Address> admins = getResourcesAdmins(provider, domainUid, mailbox);
		if (admins.isEmpty()) {
			message.setSubject("[Unable to deliver mail to resource address] " + message.getSubject());
			message.setTo(message.getFrom().iterator().next());
		} else {
			message.setTo(admins);
		}

		mailer.send(domainUid, message);
	}

	private Collection<Address> getResourcesAdmins(IServiceProvider provider, String domainUid, String mailbox) {
		IContainerManagement containerMgmt = provider.instance(IContainerManagement.class,
				ICalendarUids.TYPE + ":" + mailbox);
		List<AccessControlEntry> acls = containerMgmt.getAccessControlList();

		IGroup groupService = provider.instance(IGroup.class, domainUid);
		IDirectory directoryService = provider.instance(IDirectory.class, domainUid);

		Map<String, Address> adminsUsers = new HashMap<>();
		for (AccessControlEntry acl : acls) {
			if (acl.verb != Verb.Write && acl.verb != Verb.All) {
				continue;
			}

			DirEntry entry = directoryService.findByEntryUid(acl.subject);
			if (entry != null) {
				if (entry.kind == BaseDirEntry.Kind.GROUP) {
					List<Member> users = groupService.getExpandedUserMembers(entry.entryUid);
					for (Member user : users) {
						if (!adminsUsers.containsKey(user.uid)) {
							DirEntry dirEntry = directoryService.findByEntryUid(user.uid);
							adminsUsers.put(user.uid,
									SendmailHelper.formatAddress(dirEntry.displayName, dirEntry.email));
						}
					}
				} else {
					adminsUsers.put(entry.entryUid, SendmailHelper.formatAddress(entry.displayName, entry.email));
				}
			}
		}

		return adminsUsers.values();
	}
}
