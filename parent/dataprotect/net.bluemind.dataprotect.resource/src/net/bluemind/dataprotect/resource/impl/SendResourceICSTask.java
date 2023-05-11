/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2023
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
package net.bluemind.dataprotect.resource.impl;

import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.calendar.api.CalendarDescriptor;
import net.bluemind.calendar.api.ICalendarUids;
import net.bluemind.calendar.api.ICalendarsMgmt;
import net.bluemind.calendar.api.IVEvent;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.rest.base.GenericStream;
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
import net.bluemind.resource.api.IResources;
import net.bluemind.resource.api.ResourceDescriptor;

public class SendResourceICSTask extends BlockingServerTask implements IServerTask {

	private static final Logger logger = LoggerFactory.getLogger(SendResourceICSTask.class);

	private final DataProtectGeneration backup;
	private final Restorable item;
	private final RestoreActionExecutor<EmailData> executor;
	private ResourceBundle bundle;

	@SuppressWarnings("unchecked")
	public SendResourceICSTask(DataProtectGeneration backup, Restorable item,
			RestoreActionExecutor<? extends IRestoreActionData> executor) {
		this.backup = backup;
		this.item = item;
		this.executor = (RestoreActionExecutor<EmailData>) executor;
		this.bundle = ResourceBundle.getBundle("OSGI-INF/l10n/RestoreResource", new Locale(ServerSideServiceProvider
				.getProvider(SecurityContext.SYSTEM).getContext().getSecurityContext().getLang()));
	}

	@Override
	public void run(IServerTaskMonitor monitor) throws Exception {
		monitor.begin(1, String.format("Starting restore for uid %s", item.entryUid));
		logger.info("Starting restore for uid {}", item.entryUid);
		SendIcs sendMail = new SendIcs(item, executor, monitor);

		try (BackupDataProvider bdp = new BackupDataProvider(null, SecurityContext.SYSTEM, monitor)) {
			BmContext back = bdp.createContextWithData(backup, item);

			IResources ouBackup = back.provider().instance(IResources.class, item.domainUid);
			ItemValue<ResourceDescriptor> backupOu = ouBackup.getComplete(item.entryUid);

			String resourceCal = ICalendarUids.resourceCalendar(backupOu.uid);
			CalendarDescriptor calendarDescriptor = back.provider().instance(ICalendarsMgmt.class).get(resourceCal);

			IVEvent service = back.provider().instance(IVEvent.class, resourceCal);
			Map<String, String> allIcs = Map.of(calendarDescriptor.name,
					GenericStream.streamToString(service.exportAll()));

			sendMail.sendMessage(allIcs, bundle.getString("send.resource.restore.message"),
					bundle.getString("send.resource.restore.subject"));

			monitor.progress(1, "restored...");
		} catch (Exception e) {
			logger.error("Error while send resource by mail", e);
			monitor.end(false, "finished with errors : " + e.getMessage(), "[]");
			return;
		}

		sendMail.endTask();
	}

}
