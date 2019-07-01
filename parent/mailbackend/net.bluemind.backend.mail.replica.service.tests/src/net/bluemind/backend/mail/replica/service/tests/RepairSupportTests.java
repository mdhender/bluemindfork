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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.junit.BeforeClass;
import org.junit.Test;

import net.bluemind.backend.cyrus.partitions.CyrusPartition;
import net.bluemind.backend.cyrus.replication.testhelper.ExpectCommand;
import net.bluemind.backend.mail.api.MailboxFolder;
import net.bluemind.backend.mail.replica.api.IDbReplicatedMailboxes;
import net.bluemind.backend.mail.replica.indexing.RecordIndexActivator;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.rest.http.ClientSideServiceProvider;
import net.bluemind.core.sessions.Sessions;
import net.bluemind.core.task.api.TaskRef;
import net.bluemind.core.task.api.TaskStatus;
import net.bluemind.core.task.api.TaskStatus.State;
import net.bluemind.core.task.service.TaskUtils;
import net.bluemind.directory.api.IDirEntryMaintenance;
import net.bluemind.directory.api.MaintenanceOperation;
import net.bluemind.imap.FlagsList;
import net.bluemind.imap.IMAPException;
import net.bluemind.imap.mime.MimeTree;

public class RepairSupportTests extends AbstractRollingReplicationTests {

	private String partition;
	private String mboxRoot;
	private ExpectCommand expect;
	private ClientSideServiceProvider provider;

	@BeforeClass
	public static void oneShotBefore() {
		System.setProperty("es.mailspool.count", "1");
	}

	@Override
	public void before() throws Exception {
		super.before();
		CyrusPartition part = CyrusPartition.forServerAndDomain(cyrusReplication.server(), domainUid);
		this.partition = part.name;
		this.mboxRoot = "user." + userUid.replace('.', '^');
		String apiKey = "sid";
		SecurityContext secCtx = new SecurityContext(apiKey, userUid, Collections.emptyList(), Collections.emptyList(),
				domainUid);
		Sessions.get().put(apiKey, secCtx);
		this.provider = ClientSideServiceProvider.getProvider("http://127.0.0.1:8090", apiKey);
		this.expect = new ExpectCommand();
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

		CompletableFuture<Void> onMsg = expect.onNextApplyMailbox(inboxFolder.uid);
		imapAsUser(sc -> {
			int added = sc.append("INBOX", testEml(), new FlagsList());
			assertTrue(added > 0);
			sc.select("INBOX");
			Collection<MimeTree> bs = sc.uidFetchBodyStructure(Arrays.asList(added));
			MimeTree tree = bs.iterator().next();
			System.out.println("Mail " + added + " added:\n" + tree);
			return null;
		});
		onMsg.get(10, TimeUnit.SECONDS);
		System.err.println("Apply mailbox step.");

		IServiceProvider adminProv = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
		IDirEntryMaintenance maintenanceApi = adminProv.instance(IDirEntryMaintenance.class, dom, entry);
		Set<MaintenanceOperation> ops = maintenanceApi.getAvailableOperations();
		System.err.println("Found " + ops.size() + " repair op(s)");
		for (MaintenanceOperation mo : ops) {
			System.err.println("* " + mo);
		}
		System.err.println("Repair starts...");
		TaskRef taskRef = maintenanceApi.repair(ops.stream().map(o -> o.identifier).collect(Collectors.toSet()));
		TaskStatus status = TaskUtils.wait(adminProv, taskRef);
		System.err.println("status " + status);
		assertTrue(status.state == State.Success);
	}

}
