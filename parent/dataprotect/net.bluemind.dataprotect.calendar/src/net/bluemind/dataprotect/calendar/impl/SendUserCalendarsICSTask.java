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
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.dataprotect.calendar.impl;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.james.mime4j.dom.BinaryBody;
import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.TextBody;
import org.apache.james.mime4j.dom.address.Mailbox;
import org.apache.james.mime4j.message.BasicBodyFactory;
import org.apache.james.mime4j.message.BodyPart;
import org.apache.james.mime4j.message.MessageImpl;
import org.apache.james.mime4j.message.MultipartImpl;
import org.apache.james.mime4j.util.MimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.calendar.api.ICalendarUids;
import net.bluemind.calendar.api.IVEvent;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.ContainerQuery;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.rest.base.GenericStream;
import net.bluemind.core.sendmail.Sendmail;
import net.bluemind.core.sendmail.SendmailHelper;
import net.bluemind.core.sessions.Sessions;
import net.bluemind.core.task.service.BlockingServerTask;
import net.bluemind.core.task.service.IServerTask;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.dataprotect.api.DataProtectGeneration;
import net.bluemind.dataprotect.api.Restorable;
import net.bluemind.dataprotect.service.BackupDataProvider;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;

public class SendUserCalendarsICSTask extends BlockingServerTask implements IServerTask {
	private static final Logger logger = LoggerFactory.getLogger(SendUserCalendarsICSTask.class);

	private final DataProtectGeneration backup;
	private final Restorable item;

	public SendUserCalendarsICSTask(DataProtectGeneration backup, Restorable item) {
		this.backup = backup;
		this.item = item;
	}

	public static final SecurityContext as(String uid, String domainContainerUid) throws ServerFault {
		SecurityContext userContext = new SecurityContext(UUID.randomUUID().toString(), uid, Arrays.<String>asList(),
				Arrays.<String>asList(), Collections.emptyMap(), domainContainerUid, "en",
				"SendUserCalendarsICSTask.as");
		Sessions.get().put(userContext.getSessionId(), userContext);
		return userContext;
	}

	@Override
	public void run(IServerTaskMonitor monitor) throws Exception {
		monitor.begin(10, String.format("Starting restore for uid %s", item.entryUid));

		SecurityContext backUserContext = as(item.entryUid, item.domainUid);
		try (BackupDataProvider bdp = new BackupDataProvider(null, backUserContext, monitor)) {
			IServiceProvider back = bdp.createContextWithData(backup, item).provider();

			IContainers containersService = back.instance(IContainers.class);
			ContainerQuery cq = ContainerQuery.ownerAndType(backUserContext.getSubject(), ICalendarUids.TYPE);
			List<ContainerDescriptor> cals = containersService.all(cq);

			Map<String, String> allIcs = new HashMap<>(cals.size());
			for (ContainerDescriptor cal : cals) {
				IVEvent service = back.instance(IVEvent.class, cal.uid);
				allIcs.put(cal.name, GenericStream.streamToString(service.exportAll()));
			}

			IUser userService = ServerSideServiceProvider.getProvider(as(item.liveEntryUid(), item.domainUid))
					.instance(IUser.class, item.domainUid);
			ItemValue<User> user = userService.getComplete(item.liveEntryUid());

			Mailbox sender = SendmailHelper.formatAddress(String.format("no-reply@%s", item.domainUid),
					String.format("no-reply@%s", item.domainUid));
			Mailbox to = SendmailHelper.formatAddress(user.value.contactInfos.identification.formatedName.value,
					user.value.defaultEmail().address);
			try (Message m = getMessage(sender, to, allIcs)) {
				Sendmail mailer = new Sendmail();
				mailer.send(sender, m);
			}
		} catch (Exception e) {
			logger.error("Error while sending user calendars", e);
			monitor.end(false, "finished with errors : " + e.getMessage(), "[]");
			return;
		}

		monitor.end(true, "finished.", "[]");
	}

	private Message getMessage(Mailbox sender, Mailbox to, Map<String, String> allIcs) {
		MessageImpl mi = new MessageImpl();
		MultipartImpl mp = new MultipartImpl("mixed");
		BasicBodyFactory bbf = new BasicBodyFactory();
		TextBody tb = bbf.textBody("Une sauvegarde de vos calendriers est attachée à ce message",
				StandardCharsets.UTF_8);
		BodyPart textPart = new BodyPart();
		textPart.setBody(tb, "text/plain");
		textPart.setContentTransferEncoding(MimeUtil.ENC_QUOTED_PRINTABLE);
		mp.addBodyPart(textPart);

		for (String cal : allIcs.keySet()) {
			BodyPart icsPart = new BodyPart();
			BinaryBody ib = bbf.binaryBody(allIcs.get(cal).getBytes());
			icsPart.setBody(ib, "text/calendar");
			icsPart.setContentDisposition("attachment", cal + ".ics");
			icsPart.setContentTransferEncoding(MimeUtil.ENC_BASE64);
			mp.addBodyPart(icsPart);
		}

		mi.setMultipart(mp);
		mi.setSubject("Mes calendriers");
		mi.setFrom(sender);
		mi.setTo(to);
		mi.setDate(new Date());
		return mi;
	}

}
