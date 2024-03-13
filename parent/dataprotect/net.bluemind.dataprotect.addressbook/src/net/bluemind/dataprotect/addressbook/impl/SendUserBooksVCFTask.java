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
package net.bluemind.dataprotect.addressbook.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.addressbook.api.IAddressBookUids;
import net.bluemind.addressbook.api.IVCardService;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.api.ContainerQuery;
import net.bluemind.core.container.api.IContainers;
import net.bluemind.core.container.model.ContainerDescriptor;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.sessions.Sessions;
import net.bluemind.core.task.service.BlockingServerTask;
import net.bluemind.core.task.service.IServerTask;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.dataprotect.api.DataProtectGeneration;
import net.bluemind.dataprotect.api.Restorable;
import net.bluemind.dataprotect.common.email.SendVcf;
import net.bluemind.dataprotect.service.BackupDataProvider;
import net.bluemind.dataprotect.service.action.EmailData;
import net.bluemind.dataprotect.service.action.IRestoreActionData;
import net.bluemind.dataprotect.service.action.RestoreActionExecutor;

public class SendUserBooksVCFTask extends BlockingServerTask implements IServerTask {
	private static final Logger logger = LoggerFactory.getLogger(SendUserBooksVCFTask.class);

	private final DataProtectGeneration backup;
	private final Restorable item;
	private final RestoreActionExecutor<EmailData> executor;
	private final ResourceBundle bundle;

	@SuppressWarnings("unchecked")
	public SendUserBooksVCFTask(DataProtectGeneration backup, Restorable item,
			RestoreActionExecutor<? extends IRestoreActionData> executor) {
		this.backup = backup;
		this.item = item;
		this.executor = (RestoreActionExecutor<EmailData>) executor;
		this.bundle = ResourceBundle.getBundle("OSGI-INF/l10n/RestoreAddressBook", Locale.of(ServerSideServiceProvider
				.getProvider(SecurityContext.SYSTEM).getContext().getSecurityContext().getLang()));
	}

	@Override
	public void run(IServerTaskMonitor monitor) throws Exception {
		monitor.begin(10, String.format("Starting restore for uid %s", item.entryUid));
		logger.info("Starting restore for uid {}", item.entryUid);
		SendVcf sendEmail = new SendVcf(item, executor, monitor);

		SecurityContext backUserContext = as(item.entryUid, item.domainUid);
		try (BackupDataProvider bdp = new BackupDataProvider(null, backUserContext, monitor)) {
			IServiceProvider back = bdp.createContextWithData(backup, item).provider();

			IContainers containersService = back.instance(IContainers.class);
			ContainerQuery cq = ContainerQuery.ownerAndType(backUserContext.getSubject(), IAddressBookUids.TYPE);
			List<ContainerDescriptor> books = containersService.all(cq);

			Map<String, String> allVCards = new HashMap<String, String>(books.size());
			for (ContainerDescriptor b : books) {
				IVCardService service = back.instance(IVCardService.class, b.uid);
				allVCards.put(b.name, service.exportAll());
			}

			sendEmail.sendMessage(allVCards, bundle.getString("send.addressBook.restore.message"),
					bundle.getString("send.addressBook.restore.subject"));

		} catch (Exception e) {
			logger.error("Error while sending user contacts", e);
			monitor.end(false, "finished with errors : " + e.getMessage(), "[]");
			return;
		}

		sendEmail.endTask();
	}

	private static final SecurityContext as(String uid, String domainContainerUid) throws ServerFault {
		SecurityContext userContext = new SecurityContext(UUID.randomUUID().toString(), uid, Arrays.<String>asList(),
				Arrays.<String>asList(), Collections.emptyMap(), domainContainerUid, "en", "RestoreUserBooksTask.as");
		Sessions.get().put(userContext.getSessionId(), userContext);
		return userContext;
	}

}
