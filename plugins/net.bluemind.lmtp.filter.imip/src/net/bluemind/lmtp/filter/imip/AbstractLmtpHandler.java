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
package net.bluemind.lmtp.filter.imip;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;

import net.bluemind.calendar.api.ICalendarUids;
import net.bluemind.calendar.api.VEvent;
import net.bluemind.calendar.api.VEventOccurrence;
import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.config.Token;
import net.bluemind.core.api.Email;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.ContainerQuery;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.model.acl.Verb;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.http.ClientSideServiceProvider;
import net.bluemind.icalendar.api.ICalendarElement;
import net.bluemind.lmtp.backend.LmtpAddress;
import net.bluemind.locator.client.LocatorClient;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.resource.api.IResources;
import net.bluemind.resource.api.ResourceDescriptor;
import net.bluemind.todolist.api.ITodoList;
import net.bluemind.todolist.api.ITodoUids;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;

public abstract class AbstractLmtpHandler {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(AbstractLmtpHandler.class);

	public AbstractLmtpHandler() {

	}

	protected IServiceProvider provider() {
		return ClientSideServiceProvider.getProvider(getCoreUrl(), Token.admin0());
	}

	/**
	 * @param recipient
	 * @param calUid
	 * @param sender
	 * @return
	 * @throws ServerFault
	 */
	protected boolean checkInvitationRight(LmtpAddress recipient, String calUid, ItemValue<User> sender)
			throws ServerFault {
		IContainers cm = provider().instance(IContainers.class);
		ContainerDescriptor container = cm.getForUser(recipient.getDomainPart(), sender.uid, calUid);
		return container.verbs.stream().anyMatch(v -> v.can(Verb.Invitation));
	}

	protected String getCalendarUid(ItemValue<Mailbox> recipientMailbox) throws ServerFault {
		if (recipientMailbox.value.type == Mailbox.Type.resource) {
			return ICalendarUids.TYPE + ":" + recipientMailbox.uid;
		}

		List<ContainerDescriptor> calendars = provider().instance(IContainers.class)
				.all(ContainerQuery.ownerAndType(recipientMailbox.uid, ICalendarUids.TYPE));

		ContainerDescriptor cal = null;
		for (ContainerDescriptor cont : calendars) {
			if (cont.defaultContainer) {
				cal = cont;
			}
		}

		if (cal == null) {
			throw new ServerFault("Unable to find default calendar container for: " + recipientMailbox.uid);
		}

		return cal.uid;
	}

	/**
	 * @param authKey
	 * @return
	 * @throws ServerFault
	 */
	protected ITodoList getTodoListService(ItemValue<User> user) throws ServerFault {
		return provider().instance(ITodoList.class, ITodoUids.defaultUserTodoList(user.uid));
	}

	/**
	 * @param authKey
	 * @return
	 * @throws ServerFault
	 */
	protected IMailboxes getMailboxService(LmtpAddress recipient) throws ServerFault {
		return provider().instance(IMailboxes.class, recipient.getDomainPart());
	}

	/**
	 * @param authKey
	 * @param domainUid
	 * @param userUid
	 * @return
	 * @throws ServerFault
	 */
	protected ItemValue<User> getUserFromUid(String domainUid, String userUid) throws ServerFault {
		return provider().instance(IUser.class, domainUid).getComplete(userUid);
	}

	protected ResourceDescriptor getResourceFromUid(String domainUid, String resourceUid) throws ServerFault {
		return provider().instance(IResources.class, domainUid).get(resourceUid);
	}

	/**
	 * @param authKey
	 * @param domainUid
	 * @param entryUid
	 * @return
	 * @throws ServerFault
	 */
	protected Collection<Email> getAllEmails(String domainUid, String entryUid) throws ServerFault {
		return provider().instance(IMailboxes.class, domainUid).getComplete(entryUid).value.emails;
	}

	/**
	 * @return
	 */
	private String getCoreUrl() {
		LocatorClient lc = new LocatorClient();
		return "http://" + lc.locateHost("bm/core", "admin0@global.virt") + ":8090";
	}

	@FunctionalInterface
	interface EventExceptionHandler {
		void handle(String itemUid, VEventOccurrence eventException) throws ServerFault;
	}

	protected VEventSeries fromList(List<ICalendarElement> elements, String icsUid) {
		List<ICalendarElement> mutableList = new ArrayList<>(elements);
		VEvent master = null;
		for (Iterator<ICalendarElement> iter = mutableList.iterator(); iter.hasNext();) {
			VEvent next = (VEvent) iter.next();
			if (!(next instanceof VEventOccurrence)) {
				master = next;
				iter.remove();
			}
		}
		VEventSeries series = new VEventSeries();
		series.main = null != master ? master : null;
		series.occurrences = mutableList.stream().map(v -> (VEventOccurrence) v).collect(Collectors.toList());
		series.icsUid = icsUid;
		return series;
	}

}
