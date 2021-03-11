/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2021
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
package net.bluemind.dataprotect.mailbox.internal;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

import net.bluemind.aws.s3.utils.S3Configuration;
import net.bluemind.backend.cyrus.replication.testhelper.CyrusReplicationHelper;
import net.bluemind.backend.cyrus.replication.testhelper.SyncServerHelper;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.dataprotect.mailbox.internal.MboxRestoreService.Mode;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.imap.ListInfo;
import net.bluemind.imap.ListResult;
import net.bluemind.imap.StoreClient;
import net.bluemind.network.topology.Topology;
import net.bluemind.node.api.INodeClient;
import net.bluemind.node.api.NodeActivator;
import net.bluemind.system.api.ISystemConfiguration;
import net.bluemind.system.api.SysConfKeys;

public class MboxRestoreSdsTests extends AbstractRestoreTests {
	CyrusReplicationHelper cyrusReplication;

	@Override
	protected CyrusReplicationHelper setupReplication(String cyrusIp) {
		// ObjectStoreTestHelper.setup(cyrusService, false);
		System.err.println("Setup replication START");
		cyrusReplication = new CyrusReplicationHelper(cyrusIp);
		cyrusReplication.installReplication();
		System.err.println("Setup replication END");
		JdbcActivator.getInstance().addMailboxDataSource(cyrusReplication.server().uid,
				JdbcTestHelper.getInstance().getMailboxDataDataSource());
		return cyrusReplication;
	}

	@Override
	protected void startReplication() throws Exception {
		MQ.init().get(30, TimeUnit.SECONDS);
		Topology.get();
		SyncServerHelper.waitFor();
		cyrusReplication.startReplication().get(5, TimeUnit.SECONDS);
		disableCyrusArchive(cyrusService.server().value.ip);
	}

	@Before
	public void beforeSdsTest() throws Exception {
		String bucket = "junit-" + System.currentTimeMillis();
		// s3 server does not exists, that's ok
		S3Configuration config = S3Configuration.withEndpointAndBucket("http://127.0.0.1:8000", bucket);
		ImmutableMap<String, String> freshConf = new ImmutableMap.Builder<String, String>() //
				.put(SysConfKeys.archive_kind.name(), "s3") //
				.put(SysConfKeys.sds_s3_access_key.name(), config.getAccessKey()) //
				.put(SysConfKeys.sds_s3_secret_key.name(), config.getSecretKey()) //
				.put(SysConfKeys.sds_s3_endpoint.name(), config.getEndpoint()) //
				.put(SysConfKeys.sds_s3_region.name(), config.getRegion()) //
				.put(SysConfKeys.sds_s3_bucket.name(), config.getBucket()) //
				.build();
		ServerSideServiceProvider prov = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
		ISystemConfiguration sysConfApi = prov.instance(ISystemConfiguration.class);
		sysConfApi.updateMutableValues(freshConf);

		// We don't setup cyrus with sds-proxy, so just override it for now
		disableCyrusArchive(cyrusService.server().value.ip);
		cyrusService.reload();
		backupAll();
	}

	protected void disableCyrusArchive(String cyrusIp) {
		INodeClient nodeClient = NodeActivator.get(cyrusIp);
		try (InputStream in = new ByteArrayInputStream(
				("object_storage_enabled: 0\n" + "archive_enabled: 0\n").getBytes())) {
			nodeClient.writeFile("/etc/cyrus-hsm", in);
		} catch (IOException e) {
			e.printStackTrace(System.err);
			throw new ServerFault(e);
		}
	}

	@After
	public void afterReplication() throws Exception {
		System.err.println("Waiting for last events...");
		Thread.sleep(1000);
		cyrusReplication.stopReplication().get(5, TimeUnit.SECONDS);
		JdbcTestHelper.getInstance().afterTest();
	}

	@Test
	public void restoreSdsUserInSubfolder() throws Exception {
		MboxRestoreService mbr = new MboxRestoreService();
		assertNotNull(mbr);

		TestMonitor monitor = new TestMonitor();
		mbr.restore(latestGen, mbox, testDomain, Mode.Subfolder, monitor);
		assertTrue(monitor.finished);

		try (StoreClient sc = new StoreClient(imapServer.ip, 1143, latd, login)) {
			assertTrue(sc.login());
			boolean found = false;
			ListResult list = sc.listAll();
			for (ListInfo li : list) {
				if (li.getName().startsWith("restored-") && li.isSelectable()) {
					found = true;
					break;
				}
			}
			assertTrue("A restore-xxxx directory should exist in the imap hierarchy", found);
		}
	}

	@Test
	public void restoreSdsUserReplace() throws Exception {
		/* Create a new subfolder, must be deleted by the restore process */
		try (StoreClient sc = new StoreClient(imapServer.ip, 1143, latd, login)) {
			assertTrue(sc.login());
			assertTrue(sc.create("petitchat"));
			assertTrue(sc.create("petitchat/groschien"));
			sc.deleteMailbox(subFolderWithSpace);
			sc.deleteMailbox(subFolder);
		}
		Thread.sleep(2000);
		MboxRestoreService mbr = new MboxRestoreService();
		assertNotNull(mbr);

		TestMonitor monitor = new TestMonitor();
		mbr.restore(latestGen, mbox, testDomain, Mode.Replace, monitor);
		assertTrue(monitor.finished);

		try (StoreClient sc = new StoreClient(imapServer.ip, 1143, latd, login)) {
			assertTrue(sc.login());
			ListResult list = sc.listAll();
			for (ListInfo li : list) {
				if (li.getName().contains("petitchat")) {
					fail("petitchat* should not exist");
				}
				if (li.getName().contains("groschien")) {
					fail("groschien should not exist");
				}
			}
		}
	}

}
