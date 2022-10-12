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
package net.bluemind.mailbox.service;

import java.util.List;
import java.util.function.Consumer;

import jakarta.ws.rs.PathParam;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.directory.api.ReservedIds;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.rules.MailFilterRule;
import net.bluemind.scheduledjob.api.JobExitStatus;
import net.bluemind.scheduledjob.scheduler.IScheduledJobRunId;
import net.bluemind.scheduledjob.scheduler.IScheduler;

public interface IInCoreMailboxes extends IMailboxes {

	void checkAndRepairTask(String mailboxUid, IServerTaskMonitor monitor) throws ServerFault;

	void checkAndRepairTask(String mailboxUid, IServerTaskMonitor monitor, boolean repair) throws ServerFault;

	void checkAndRepairAllTask(IServerTaskMonitor monitor) throws ServerFault;

	void checkAvailabilty(Mailbox mailbox) throws ServerFault;

	List<MailFilterRule> getMailboxRules(@PathParam("mailboxUid") String mailboxUid) throws ServerFault;

	/**
	 * Enable/disable out of office sieve script
	 * 
	 * @param sched
	 * @param rid
	 * @return
	 *         <li>JobExitStatus.SUCCESS if all out of office was set
	 *         successfully</li>
	 *         <li>JobExitStatus.FAILURE if all out of office was set</li> with
	 *         error
	 *         <li>JobExitStatus.COMPLETE_WITH_WARNING if out of office was set with
	 *         some errors</li>
	 * @throws ServerFault
	 */
	JobExitStatus refreshOutOfOffice(IScheduler sched, IScheduledJobRunId rid) throws ServerFault;

	void sanitize(Mailbox mailbox) throws ServerFault;

	void validate(String uid, Mailbox mailbox) throws ServerFault;

	void created(String uid, Mailbox mailbox, Consumer<ReservedIds> reservedIdsConsumer) throws ServerFault;

	void updated(String uid, Mailbox previous, Mailbox mailbox, Consumer<ReservedIds> reservedIdsConsumer)
			throws ServerFault;

	void deleted(String uid, Mailbox mailbox) throws ServerFault;

	void deleteEmailByAlias(String alias) throws ServerFault;
}
