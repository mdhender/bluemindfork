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
import net.bluemind.core.sendmail.SendmailCredentials;
import net.bluemind.core.sendmail.SendmailHelper;
import net.bluemind.delivery.lmtp.common.LmtpEnvelope;
import net.bluemind.delivery.lmtp.common.ResolvedBox;
import net.bluemind.delivery.lmtp.filters.FilterException;
import net.bluemind.delivery.lmtp.filters.IMessageFilter;
import net.bluemind.directory.api.BaseDirEntry;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.group.api.IGroup;
import net.bluemind.group.api.Member;
import net.bluemind.imip.parser.IIMIPParser;
import net.bluemind.imip.parser.IMIPInfos;
import net.bluemind.imip.parser.IMIPParserFactory;
import net.bluemind.imip.parser.PureICSRewriter;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.network.topology.Topology;

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
	public Message filter(LmtpEnvelope env, Message message) throws FilterException {
		IIMIPParser parser = IMIPParserFactory.create();

		Message pureIcs = new PureICSRewriter().rewrite(message);
		IMIPInfos infos = parser.parse(pureIcs);
		if (infos != null) {
			return null;
		}

		IServiceProvider provider = ClientSideServiceProvider.getProvider(getCoreUrl(), Token.admin0());
		List<ResolvedBox> recipients = env.getRecipients();
		if (recipients != null && !recipients.isEmpty()) {
			for (ResolvedBox recipient : recipients) {
				System.err.println("on " + recipient);
				try {
					String mailbox = getResourceMailbox(provider, recipient);
					if (mailbox != null) {
						redirectMessageToResourceAdmins(provider, recipient.dom.uid, mailbox, message);
					}
				} catch (ServerFault e) {
					logger.error("[{}] Error while handling resource filter message",
							message.getHeader().getField("Message-ID"), e);
					throw new FilterException("[" + message.getHeader().getField("Message-ID")
							+ "] Error while handling resource filter message: " + e.getMessage());
				}
			}
		}

		return null;
	}

	private String getCoreUrl() {
		if (coreUrl == null) {
			coreUrl = "http://" + Topology.get().core().value.address() + ":8090";
		}
		return coreUrl;
	}

	private String getResourceMailbox(IServiceProvider provider, ResolvedBox recipient) {
		Optional<ItemValue<Mailbox>> mailbox = Optional.of(recipient.mbox);

		if (mailbox.get().value.type != Mailbox.Type.resource) {
			return null;
		}
		return mailbox.get().uid;
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

		mailer.send(SendmailCredentials.asAdmin0(), domainUid, message);
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
			if (entry != null && !entry.archived) {
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
