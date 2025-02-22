/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2017
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
package net.bluemind.backend.mail.replica.service.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import net.bluemind.backend.mail.api.MailboxFolder;
import net.bluemind.backend.mail.replica.api.IDbReplicatedMailboxes;
import net.bluemind.backend.mail.replica.indexing.RecordIndexActivator;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.rest.http.ClientSideServiceProvider;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.api.TaskStatus;
import net.bluemind.core.task.api.TaskStatus.State;
import net.bluemind.core.task.service.TaskUtils;
import net.bluemind.directory.api.IDirEntryMaintenance;
import net.bluemind.directory.api.MaintenanceOperation;
import net.bluemind.directory.api.RepairConfig;
import net.bluemind.imap.FlagsList;
import net.bluemind.imap.IMAPException;
import net.bluemind.imap.mime.MimeTree;

public class RepairSupportTests extends AbstractRollingReplicationTests {

	private ClientSideServiceProvider provider;

	@BeforeEach
	@Override
	public void before(TestInfo testInfo) throws Exception {
		super.before(testInfo);
		this.provider = ClientSideServiceProvider.getProvider("http://127.0.0.1:8090", apiKey);
		RecordIndexActivator.reload();
	}

	@Test
	public void repairSubtree() throws IMAPException, InterruptedException, ExecutionException, TimeoutException {
		repairDomUid(domainUid, userUid);
	}

	@Test
	public void testRepairDomain() throws InterruptedException, ExecutionException, TimeoutException {
		repairDomUid(domainUid, domainUid);
	}

	private void repairDomUid(String dom, String entry)
			throws InterruptedException, ExecutionException, TimeoutException {
		System.err.println("Test starts..........................");
		IDbReplicatedMailboxes repApi = provider.instance(IDbReplicatedMailboxes.class, partition, mboxRoot);
		ItemValue<MailboxFolder> inboxFolder = repApi.byName("INBOX");
		assertNotNull(inboxFolder);

		imapAsUser(sc -> {
			int added = sc.append("INBOX", testEml(), new FlagsList());
			assertTrue(added > 0);
			sc.select("INBOX");
			Collection<MimeTree> bs = sc.uidFetchBodyStructure(Arrays.asList(added));
			MimeTree tree = bs.iterator().next();
			System.out.println("Mail " + added + " added:\n" + tree);
			return null;
		});
		System.err.println("Apply mailbox step.");

		IServiceProvider adminProv = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
		IDirEntryMaintenance maintenanceApi = adminProv.instance(IDirEntryMaintenance.class, dom, entry);
		List<MaintenanceOperation> ops = maintenanceApi.getAvailableOperations();
		System.err.println("Found " + ops.size() + " repair op(s)");
		for (MaintenanceOperation mo : ops) {
			System.err.println("* " + mo);
		}
		System.err.println("Repair starts...");
		RepairConfig config = RepairConfig.create(ops.stream().map(o -> o.identifier).collect(Collectors.toSet()),
				false, true, true);
		TaskRef taskRef = maintenanceApi.repair(config);
		TaskStatus status = TaskUtils.wait(adminProv, taskRef);
		System.err.println("status " + status);
		assertEquals(State.Success, status.state, "State should be success");
	}

}
