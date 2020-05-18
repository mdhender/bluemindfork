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

package net.bluemind.dataprotect.mailbox.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.service.IServerTask;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.dataprotect.api.DataProtectGeneration;
import net.bluemind.dataprotect.api.Restorable;
import net.bluemind.dataprotect.mailbox.internal.MboxRestoreService.Mode;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomains;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;

public class RestoreBoxTask implements IServerTask {

	private static final Logger logger = LoggerFactory.getLogger(RestoreBoxTask.class);
	private final DataProtectGeneration dpg;
	private final Restorable box;
	private final Mode mode;
	private final IServiceProvider sp;

	public RestoreBoxTask(DataProtectGeneration dpg, Restorable box, Mode m) {
		if (dpg == null) {
			throw new NullPointerException("DataProtectGeneration can't be null");
		}
		this.dpg = dpg;
		this.box = box;
		this.mode = m;
		this.sp = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
	}

	@Override
	public void run(IServerTaskMonitor monitor) throws Exception {
		MboxRestoreService mrs = new MboxRestoreService();
		ItemValue<Mailbox> mbox = mbox();
		ItemValue<Domain> domain = domain();
		mrs.restore(dpg, mbox, domain, mode, monitor);
	}

	/**
	 * @return
	 * @throws ServerFault
	 */
	private ItemValue<Mailbox> mbox() throws ServerFault {
		logger.info("Should find box {}@{}", box.liveEntryUid(), box.domainUid);
		IMailboxes mboxApi = sp.instance(IMailboxes.class, box.domainUid);
		return mboxApi.getComplete(box.liveEntryUid());
	}

	private ItemValue<Domain> domain() throws ServerFault {
		logger.info("Shoud find domain {}", box.domainUid);
		IDomains domApi = sp.instance(IDomains.class);
		return domApi.get(box.domainUid);
	}

}
