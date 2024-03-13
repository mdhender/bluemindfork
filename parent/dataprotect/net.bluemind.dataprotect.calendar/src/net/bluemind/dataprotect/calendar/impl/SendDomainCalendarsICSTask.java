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

import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.calendar.api.CalendarDescriptor;
import net.bluemind.calendar.api.ICalendarsMgmt;
import net.bluemind.calendar.api.IVEvent;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.rest.base.GenericStream;
import net.bluemind.core.sessions.Sessions;
import net.bluemind.core.task.service.BlockingServerTask;
import net.bluemind.core.task.service.IServerTask;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.dataprotect.api.DataProtectGeneration;
import net.bluemind.dataprotect.api.Restorable;
import net.bluemind.dataprotect.common.email.SendIcs;
import net.bluemind.dataprotect.service.BackupDataProvider;
import net.bluemind.dataprotect.service.action.EmailData;
import net.bluemind.dataprotect.service.action.IRestoreActionData;
import net.bluemind.dataprotect.service.action.RestoreActionExecutor;

public class SendDomainCalendarsICSTask extends BlockingServerTask implements IServerTask {
	private static final Logger logger = LoggerFactory.getLogger(SendDomainCalendarsICSTask.class);

	private final DataProtectGeneration backup;
	private final Restorable item;
	private final RestoreActionExecutor<EmailData> executor;
	private final ResourceBundle bundle;

	@SuppressWarnings("unchecked")
	public SendDomainCalendarsICSTask(DataProtectGeneration backup, Restorable item,
			RestoreActionExecutor<? extends IRestoreActionData> executor) {
		this.backup = backup;
		this.item = item;
		this.executor = (RestoreActionExecutor<EmailData>) executor;
		this.bundle = ResourceBundle.getBundle("OSGI-INF/l10n/RestoreCalendar", Locale.of(ServerSideServiceProvider
				.getProvider(SecurityContext.SYSTEM).getContext().getSecurityContext().getLang()));
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
		logger.info("Starting restore for uid {}", item.entryUid);
		SendIcs sendMail = new SendIcs(item, executor, monitor);

		try (BackupDataProvider bdp = new BackupDataProvider(null, SecurityContext.SYSTEM, monitor)) {
			BmContext back = bdp.createContextWithData(backup, item);

			CalendarDescriptor backDomainCalendar = back.provider().instance(ICalendarsMgmt.class).get(item.entryUid);

			IVEvent service = back.provider().instance(IVEvent.class, item.entryUid);
			Map<String, String> allIcs = Map.of(backDomainCalendar.name,
					GenericStream.streamToString(service.exportAll()));

			sendMail.sendMessage(allIcs, bundle.getString("send.calendar.restore.message"),
					bundle.getString("send.calendar.restore.subject"));

		} catch (Exception e) {
			logger.error("Error while sending domain calendars", e);
			monitor.end(false, "finished with errors : " + e.getMessage(), "[]");
			return;
		}

		sendMail.endTask();
	}

}
