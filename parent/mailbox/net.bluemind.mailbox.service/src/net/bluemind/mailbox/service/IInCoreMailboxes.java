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

import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.api.report.DiagnosticReport;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;

public interface IInCoreMailboxes extends IMailboxes {

	public void checkAndRepairTask(String mailboxUid, IServerTaskMonitor monitor) throws ServerFault;

	public void checkAndRepairTask(String mailboxUid, DiagnosticReport report, IServerTaskMonitor monitor,
			boolean repair) throws ServerFault;

	public void checkAndRepairAllTask(IServerTaskMonitor monitor) throws ServerFault;

	public void checkAvailabilty(Mailbox mailbox) throws ServerFault;

	public void refreshOutOfOffice() throws ServerFault;

	public void sanitize(Mailbox mailbox) throws ServerFault;

	public void validate(String uid, Mailbox mailbox) throws ServerFault;

	public void created(String uid, Mailbox mailbox) throws ServerFault;

	public void updated(String uid, Mailbox previous, Mailbox mailbox) throws ServerFault;

	public void deleted(String uid, Mailbox mailbox) throws ServerFault;

	public void deleteEmailByAlias(String alias) throws ServerFault;
}