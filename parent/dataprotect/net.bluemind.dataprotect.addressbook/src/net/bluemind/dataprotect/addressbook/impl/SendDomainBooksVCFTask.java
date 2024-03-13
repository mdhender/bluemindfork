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
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *
  * See LICENSE.txt
  * END LICENSE
  */
package net.bluemind.dataprotect.addressbook.impl;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.addressbook.api.AddressBookDescriptor;
import net.bluemind.addressbook.api.IAddressBook;
import net.bluemind.addressbook.api.IAddressBooksMgmt;
import net.bluemind.addressbook.api.IVCardService;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
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

public class SendDomainBooksVCFTask extends BlockingServerTask implements IServerTask {
	private static final Logger logger = LoggerFactory.getLogger(SendDomainBooksVCFTask.class);

	private final DataProtectGeneration backup;
	private final Restorable item;
	private final RestoreActionExecutor<EmailData> executor;
	private final ResourceBundle bundle;

	@SuppressWarnings("unchecked")
	public SendDomainBooksVCFTask(DataProtectGeneration backup, Restorable item,
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

		try (BackupDataProvider bdp = new BackupDataProvider(null, SecurityContext.SYSTEM, monitor)) {
			BmContext back = bdp.createContextWithData(backup, item);

			AddressBookDescriptor backDomainAddressBook = back.provider().instance(IAddressBooksMgmt.class)
					.get(item.entryUid);

			List<String> vCardUids = back.provider().instance(IAddressBook.class, item.entryUid).allUids();
			IVCardService service = back.provider().instance(IVCardService.class, item.entryUid);
			Map<String, String> allVCards = Map.of(backDomainAddressBook.name, service.exportCards(vCardUids));

			sendEmail.sendMessage(allVCards, bundle.getString("send.addressBook.restore.message"),
					bundle.getString("send.addressBook.restore.subject"));

		} catch (Exception e) {
			logger.error("Error while sending user contacts", e);
			monitor.end(false, "finished with errors : " + e.getMessage(), "[]");
			return;
		}

		sendEmail.endTask();

	}

}
