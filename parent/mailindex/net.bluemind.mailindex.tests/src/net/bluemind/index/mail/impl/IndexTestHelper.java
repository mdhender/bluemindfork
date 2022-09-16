/* BEGIN LICENSE
  * Copyright © Blue Mind SAS, 2012-2018
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
package net.bluemind.index.mail.impl;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import net.bluemind.backend.cyrus.CyrusService;
import net.bluemind.backend.cyrus.replication.testhelper.CyrusReplicationHelper;
import net.bluemind.backend.mail.replica.indexing.RecordIndexActivator;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.server.api.Server;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class IndexTestHelper {

	public static class TestDomainOptions {
		public int userCount;

		private TestDomainOptions(int users) {
			this.userCount = users;
		}

		public static TestDomainOptions justUsers(int i) {
			return new TestDomainOptions(i);
		}
	}

	public static class Builder {

		private List<String> domains;
		private boolean replication;
		private TestDomainOptions domainOptions;

		public Builder withDomains(String... domains) {
			this.domains = Arrays.asList(domains);
			return this;
		}

		public Builder withDomainOptions(TestDomainOptions tdo) {
			this.domainOptions = tdo;
			return this;
		}

		public Builder enableCyrusReplication() {
			this.replication = true;
			return this;
		}

		public IndexTestHelper build() {
			BmConfIni ini = new BmConfIni();

			Server esServer = new Server();
			esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
			esServer.tags = Lists.newArrayList("bm/es");

			Server dbServer = new Server();
			dbServer.ip = ini.get("host");
			dbServer.tags = Lists.newArrayList("bm/pgsql", "bm/pgsql-data");

			Server imapServer = new Server();
			imapServer.ip = ini.get("imap-role");
			imapServer.tags = Lists.newArrayList("mail/imap");

			Optional<CyrusReplicationHelper> repl = Optional.empty();
			if (replication) {
				repl = Optional.of(new CyrusReplicationHelper(imapServer.ip));
			}

			return new IndexTestHelper(ImmutableList.copyOf(domains), repl, dbServer, imapServer, esServer,
					domainOptions);
		}
	}

	public static Builder builder() {
		return new Builder();
	}

	public final List<String> domains;
	private Optional<CyrusReplicationHelper> replication;
	public final Server dbServer;
	public final Server imapServer;
	public final Server esServer;
	private TestDomainOptions toProvision;

	private IndexTestHelper(List<String> domains, Optional<CyrusReplicationHelper> repl, Server dbServer,
			Server imapServer, Server esServer, TestDomainOptions provOpts) {
		this.domains = domains;
		this.replication = repl;
		this.dbServer = dbServer;
		this.imapServer = imapServer;
		this.esServer = esServer;
		this.toProvision = provOpts;
	}

	public void beforeTest() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();
		JdbcTestHelper.getInstance().getDbSchemaService().initialize();

		ItemValue<Server> cyrusServer = ItemValue.create("localhost", imapServer);
		CyrusService cyrusService = new CyrusService(cyrusServer);
		cyrusService.reset();

		PopulateHelper.initGlobalVirt(dbServer, esServer, imapServer);
		System.err.println("Deploying with es: " + esServer.ip + ", imap: " + imapServer.ip);

		PopulateHelper.addDomainAdmin("admin0", "global.virt", Routing.none);
		ElasticsearchTestHelper.getInstance().beforeTest(25);

		for (String domUid : domains) {
			PopulateHelper.addDomain(domUid, Routing.none);
		}

		replication.ifPresent(helper -> helper.installReplication());

		final CompletableFuture<Void> spawn = new CompletableFuture<Void>();
		VertxPlatform.spawnVerticles(ar -> {
			if (ar.succeeded()) {
				spawn.complete(ar.result());
			} else {
				spawn.completeExceptionally(ar.cause());
			}
		});

		spawn.thenCompose(depDone -> {
			System.err.println("Reloading index support...");
			RecordIndexActivator.reload();
			System.err.println("Starting replication if needed...");
			return replication.map(helper -> helper.startReplication()).orElse(CompletableFuture.completedFuture(null));
		}).thenApply(replicationStarted -> {
			for (String domUid : domains) {
				for (int i = 0; i < toProvision.userCount; i++) {
					String loginAndUid = String.format("user%02d", i);
					PopulateHelper.addUser(loginAndUid, domUid, Routing.internal);
					System.err.println("User " + loginAndUid + " provisionned.");
				}
			}
			return "yeah";
		}).get(1, TimeUnit.MINUTES);

		// ensure every throttled event has finished...
		Thread.sleep(2000);
	}

	public void afterTest() throws Exception {
		replication.map(helper -> helper.stopReplication()).orElse(CompletableFuture.completedFuture(null)).get(30,
				TimeUnit.SECONDS);
		JdbcTestHelper.getInstance().afterTest();
	}

}
