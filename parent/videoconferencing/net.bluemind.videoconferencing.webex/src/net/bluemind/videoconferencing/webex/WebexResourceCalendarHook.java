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
package net.bluemind.videoconferencing.webex;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

import org.apache.james.mime4j.dom.TextBody;
import org.apache.james.mime4j.dom.address.Mailbox;
import org.apache.james.mime4j.dom.address.MailboxList;
import org.apache.james.mime4j.message.BasicBodyFactory;
import org.apache.james.mime4j.message.BodyPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import net.bluemind.calendar.api.VEventSeries;
import net.bluemind.calendar.helper.ical4j.VEventServiceHelper;
import net.bluemind.calendar.helper.mail.CalendarMail;
import net.bluemind.calendar.hook.ICalendarHook;
import net.bluemind.calendar.hook.VEventMessage;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.IContainerManagement;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.sendmail.ISendmail;
import net.bluemind.core.sendmail.Sendmail;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.domain.api.IDomains;
import net.bluemind.resource.api.IResources;
import net.bluemind.resource.api.ResourceDescriptor;
import net.fortuna.ical4j.model.property.Method;

public class WebexResourceCalendarHook implements ICalendarHook {

	private static final Logger logger = LoggerFactory.getLogger(WebexResourceCalendarHook.class);
	private static final String WEBEX_ICS_MAIL = "webex-ics-mail";

	public WebexResourceCalendarHook() {
	}

	@Override
	public void onEventCreated(VEventMessage message) {
		handleEventInvitations(message);
	}

	@Override
	public void onEventUpdated(VEventMessage message) {
		handleEventInvitations(message);
	}

	private void handleEventInvitations(VEventMessage message) {
		try {
			if (isResourceAttendeeVersion(message.vevent, message.container)) {
				ResourceDescriptor rd = provider().instance(IResources.class, message.container.domainUid)
						.get(message.container.owner);
				ItemValue<ResourceDescriptor> resource = ItemValue.create(message.container.owner, rd);
				boolean isWebex = resource.value.properties.stream()
						.anyMatch(prop -> prop.propertyId.equals("bm-videoconferencing-type")
								&& prop.value.equals(WebexProvider.ID));
				if (isWebex) {
					IContainerManagement containerMgmtService = ServerSideServiceProvider
							.getProvider(SecurityContext.SYSTEM)
							.instance(IContainerManagement.class, resource.uid + "-settings-container");
					Map<String, String> settings = containerMgmtService.getSettings();
					if (settings.containsKey(WEBEX_ICS_MAIL)) {
						String icsMail = settings.get(WEBEX_ICS_MAIL);
						if (!Strings.isNullOrEmpty(icsMail)) {
							ItemValue<VEventSeries> series = ItemValue.create(message.itemUid, message.vevent);
							sendIcs(message.container.domainUid, series, icsMail);
						}
					}
				}
			}
		} catch (ServerFault e) {
			logger.error(e.getMessage(), e);
		}
	}

	private void sendIcs(String domain, ItemValue<VEventSeries> series, String icsMail) {
		String ics = VEventServiceHelper.convertToIcs(series);
		BodyPart icsPart = new BodyPart();
		BasicBodyFactory bodyFactory = new BasicBodyFactory();
		TextBody body = bodyFactory.textBody(ics, StandardCharsets.UTF_8);
		icsPart.setText(body);
		Mailbox from = new Mailbox("noreply", domainDefaultAlias(domain));
		String[] rec = icsMail.split("@");
		Mailbox to = new Mailbox(rec[0], rec[1]);
		BodyPart bodyPart = new BodyPart();
		TextBody bodyText = bodyFactory.textBody(series.value.mainOccurrence().description, StandardCharsets.UTF_8);
		bodyPart.setText(bodyText);
		CalendarMail m = new CalendarMail.CalendarMailBuilder() //
				.from(from) //
				.to(new MailboxList(Arrays.asList(to), true)) //
				.method(Method.REQUEST) //
				.ics(Optional.of(icsPart)) //
				.html(bodyPart) //
				.subject("bluemind").build();
		ISendmail mailer = new Sendmail();
		mailer.send(from, m.getMessage());
	}

	private String domainDefaultAlias(String domainUid) {
		return ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM).instance(IDomains.class)
				.get(domainUid).value.defaultAlias;
	}

	private IServiceProvider provider() {
		return ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
	}

	private boolean isResourceAttendeeVersion(VEventSeries vevent, Container container) {
		if (vevent.master(container.domainUid, container.owner)) {
			return false;
		}

		IDirectory directoryService = provider().instance(IDirectory.class, container.domainUid);
		DirEntry dirEntry = directoryService.findByEntryUid(container.owner);
		return dirEntry != null && dirEntry.kind == DirEntry.Kind.RESOURCE;
	}

	@Override
	public void onEventDeleted(VEventMessage message) {

	}

}