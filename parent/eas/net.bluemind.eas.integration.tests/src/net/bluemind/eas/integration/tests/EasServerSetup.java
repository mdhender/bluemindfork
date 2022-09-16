/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2019
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
package net.bluemind.eas.integration.tests;

import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.common.collect.Lists;

import net.bluemind.backend.cyrus.CyrusService;
import net.bluemind.backend.cyrus.replication.testhelper.CyrusReplicationHelper;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.device.api.Device;
import net.bluemind.device.api.IDevice;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.network.topology.Topology;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.role.api.BasicRoles;
import net.bluemind.server.api.Server;
import net.bluemind.system.state.StateContext;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.vertx.testhelper.Deploy;

public class EasServerSetup {

	private static final EasServerSetup INST = new EasServerSetup();

	public static EasServerSetup get() {
		return INST;
	}

	private String cyrusIp;
	private String domainUid;
	private String userUid;
	private CyrusReplicationHelper cyrusReplication;
	private Set<String> locatorVerticles;
	private Device device;

	private EasServerSetup() {

	}

	public Device device() {
		return device;
	}

	public String loginAtDomain() {
		return userUid + "@" + domainUid;
	}

	public String password() {
		return userUid;
	}

	public void beforeTest() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();
		JdbcTestHelper.getInstance().getDbSchemaService().initialize();

		BmConfIni ini = new BmConfIni();

		this.cyrusIp = ini.get("imap-role");
		Server imapServer = new Server();
		imapServer.ip = cyrusIp;
		imapServer.tags = Lists.newArrayList("mail/imap");

		ItemValue<Server> cyrusServer = ItemValue.create("localhost", imapServer);
		CyrusService cyrusService = new CyrusService(cyrusServer);
		cyrusService.reset();

		PopulateHelper.initGlobalVirt(imapServer);
		PopulateHelper.addDomainAdmin("admin0", "global.virt", Routing.none);

		String unique = System.currentTimeMillis() + "";
		this.domainUid = "test" + unique + ".lab";
		this.userUid = "user" + unique;

		// ensure the partition is created correctly before restarting cyrus
		PopulateHelper.addDomain(domainUid, Routing.none);

		System.err.println("Setup replication START");
		this.cyrusReplication = new CyrusReplicationHelper(cyrusIp);
		cyrusReplication.installReplication();
		System.err.println("Setup replication END");

		JdbcActivator.getInstance().addMailboxDataSource(cyrusReplication.server().uid,
				JdbcTestHelper.getInstance().getMailboxDataDataSource());

		CountDownLatch cdl = new CountDownLatch(1);
		VertxPlatform.spawnVerticles(ar -> {
			cdl.countDown();
		});
		boolean beforeTimeout = cdl.await(30, TimeUnit.SECONDS);
		if (!beforeTimeout) {
			throw new TimeoutException("Vertx spaw was too slow");
		}
		SyncServerHelper.waitFor();

		MQ.init().get(30, TimeUnit.SECONDS);
		Topology.get();

		cyrusReplication.startReplication().get(5, TimeUnit.SECONDS);

		System.err.println("Start populate user " + userUid);
		PopulateHelper.addUser(userUid, domainUid, Routing.internal, BasicRoles.ROLE_EAS);

		ServerSideServiceProvider prov = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
		IDevice devApi = prov.instance(IDevice.class, userUid);
		Device dev = new Device();
		dev.hasPartnership = true;
		dev.identifier = "junit-" + userUid;
		dev.type = "junit-phone";
		dev.isWipe = false;
		dev.owner = userUid;
		devApi.create("junit-" + userUid, dev);

		this.device = devApi.byIdentifier("junit-" + userUid).value;

		StateContext.setState("core.started");
		Thread.sleep(200);
		StateContext.setState("core.upgrade.start");
		Thread.sleep(200);
		StateContext.setState("core.upgrade.end");

		Thread.sleep(2000);

		System.err.println("Test setup is complete, dev: " + device);
	}

	public void afterTest() throws Exception {
		System.out.println("Waiting for last events (remove this sleep ?)...");
		Thread.sleep(1000);
		cyrusReplication.stopReplication().get(5, TimeUnit.SECONDS);
		Deploy.afterTest(locatorVerticles);
		JdbcTestHelper.getInstance().afterTest();
	}

}
