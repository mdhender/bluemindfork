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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */
package net.bluemind.core.backup.continuous.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.LongAdder;

import javax.sql.DataSource;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.github.luben.zstd.ZstdInputStream;
import com.google.common.collect.Lists;
import com.google.common.io.ByteStreams;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import net.bluemind.authentication.api.IAuthentication;
import net.bluemind.authentication.api.LoginResponse;
import net.bluemind.aws.s3.utils.S3Configuration;
import net.bluemind.backend.cyrus.CyrusService;
import net.bluemind.backend.cyrus.replication.testhelper.CyrusReplicationHelper;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.config.InstallationId;
import net.bluemind.core.backup.continuous.DefaultBackupStore;
import net.bluemind.core.backup.continuous.IBackupReader;
import net.bluemind.core.backup.continuous.ILiveBackupStreams;
import net.bluemind.core.backup.continuous.ILiveStream;
import net.bluemind.core.backup.continuous.api.CloneDefaults;
import net.bluemind.core.backup.continuous.api.IBackupStore;
import net.bluemind.core.backup.continuous.api.IBackupStoreFactory;
import net.bluemind.core.backup.continuous.leader.DefaultLeader;
import net.bluemind.core.backup.continuous.restore.CloneState;
import net.bluemind.core.backup.continuous.restore.IClonePhaseObserver;
import net.bluemind.core.backup.continuous.restore.InstallFromBackupTask;
import net.bluemind.core.backup.continuous.restore.SysconfOverride;
import net.bluemind.core.backup.continuous.restore.TopologyMapping;
import net.bluemind.core.backup.continuous.restore.mbox.DefaultSdsStoreLoader;
import net.bluemind.core.backup.continuous.store.TopicNames;
import net.bluemind.core.container.api.IFlatHierarchyUids;
import net.bluemind.core.container.api.IOwnerSubscriptionUids;
import net.bluemind.core.container.model.BaseContainerDescriptor;
import net.bluemind.core.container.model.Container;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.container.persistence.ContainerStore;
import net.bluemind.core.container.persistence.DataSourceRouter;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.BmContext;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.task.service.IServerTaskMonitor;
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.dockerclient.DockerEnv;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomains;
import net.bluemind.kafka.container.ZkKafkaContainer;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.IMailboxAclUids;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.sds.dto.PutRequest;
import net.bluemind.sds.store.ISdsBackingStore;
import net.bluemind.sds.store.s3.S3StoreFactory;
import net.bluemind.server.api.Server;
import net.bluemind.system.api.CloneConfiguration;
import net.bluemind.system.api.CloneConfiguration.Mode;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.system.state.StateContext;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.user.api.IUser;
import net.bluemind.user.api.User;

public class PopulateKafkaTests {

	private String cyrusIp;
	private ZkKafkaContainer kafka;
	Path marker = Paths.get(CloneDefaults.MARKER_FILE_PATH);
	private String s3;
	private String bucket;
	private CyrusReplicationHelper replicationHelper;

	@Before
	public void before() throws Exception {
		this.kafka = new ZkKafkaContainer();
		kafka.start();
		String ip = kafka.inspectAddress();
		System.setProperty("bm.kafka.bootstrap.servers", ip + ":9093");
		System.setProperty("bm.zk.servers", ip + ":2181");

		DefaultLeader.reset();

		this.s3 = "http://" + DockerEnv.getIp("bluemind/s3") + ":8000";
		this.bucket = "junit-clone-kafka-" + System.currentTimeMillis();
		S3Configuration s3conf = S3Configuration.withEndpointAndBucket(s3, bucket);
		System.err.println(s3conf.asJson().encodePrettily());
		ISdsBackingStore sds = new S3StoreFactory().create(VertxPlatform.getVertx(), s3conf.asJson());

		int electAttempts = 0;
		do {
			System.err.println("LEADER: " + DefaultLeader.leader().isLeader());
			Thread.sleep(500);
		} while (!DefaultLeader.leader().isLeader() && electAttempts++ < 120);
		System.err.println("leader: " + DefaultLeader.leader());
		assertTrue("Not elected as leader", DefaultLeader.leader().isLeader());

		System.err.println("sds: " + sds);
		try (InputStream in = getClass().getClassLoader().getResourceAsStream("data/kafka/emls.tar.bz2");
				BZip2CompressorInputStream bz2 = new BZip2CompressorInputStream(in);
				TarArchiveInputStream tar = new TarArchiveInputStream(bz2)) {
			TarArchiveEntry ce;
			long s3content = 0;
			while ((ce = tar.getNextTarEntry()) != null) {
				if (!ce.isDirectory()) {
					byte[] eml = ByteStreams.toByteArray(tar);
					try (ZstdInputStream dec = new ZstdInputStream(new ByteArrayInputStream(eml))) {
						eml = ByteStreams.toByteArray(dec);
					}
					String key = ce.getName().replace("./", "");
					Path tmp = Files.createTempFile(key, ".eml");
					Files.write(tmp, eml, StandardOpenOption.CREATE, StandardOpenOption.WRITE,
							StandardOpenOption.TRUNCATE_EXISTING);
					PutRequest pr = PutRequest.of(key, tmp.toFile().getAbsolutePath());
					sds.upload(pr).get(10, TimeUnit.SECONDS);
					Files.delete(tmp);
					s3content += eml.length;
					System.err.println("Uploaded " + key + " to s3 " + eml.length + " bytes");
				}
			}
			System.err.println("Pushed " + s3content + " byte(s) to s3.");
		}

		JdbcTestHelper.getInstance().beforeTest();
		JdbcTestHelper.getInstance().getDataSource().getConnection().createStatement()
				.execute("select setval('t_container_item_id_seq', 10000)");

		Set<String> domains = populateKafka();

		this.cyrusIp = new BmConfIni().get("imap-role");

		Server imapServer = Server.tagged(cyrusIp, "mail/imap");
		imapServer.name = "bm-master";
		ItemValue<Server> withIp = ItemValue.create("bm-master", imapServer);
		CyrusService cs = new CyrusService(withIp);
		cs.reset();

		List<String> parts = new LinkedList<>();
		for (String dom : domains) {
			cs.createPartition(dom);
			parts.add(dom);
		}
		cs.refreshPartitions(parts);
		try {
			cs.reload();
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		System.err.println("partitions: " + parts);

		Server esServer = new Server();
		esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		System.out.println("ES is " + esServer.ip);
		assertNotNull(esServer.ip);
		esServer.tags = Lists.newArrayList("bm/es");

		this.replicationHelper = new CyrusReplicationHelper(cyrusIp, "core");
		String coreIp = CyrusReplicationHelper.getMyIpAddress();
		System.setProperty("sync.core.address", coreIp);
		System.err.println("coreIp for replication set to " + coreIp);

		System.err.println("populate global virt...");
		PopulateHelper.initGlobalVirt(imapServer, esServer);
		ElasticsearchTestHelper.getInstance().beforeTest();
		PopulateHelper.addDomainAdmin("admin0", "global.virt", Routing.none);

		DataSource ds = ServerSideServiceProvider.mailboxDataSource.get(imapServer.ip);
		assertNotNull(ds);
		ServerSideServiceProvider.mailboxDataSource.put("bm-master", ds);

		System.err.println("set item_id seq on " + ds);
		ds.getConnection().createStatement().execute("select setval('t_container_item_id_seq', 100000)");

		VertxPlatform.spawnBlocking(20, TimeUnit.SECONDS);
		StateContext.setState("core.started");
		StateContext.setState("core.cloning.start");
		System.err.println("Waiting for SyncServer...");
		SyncServerHelper.waitFor();
		System.err.println("=======================");
	}

	private Set<String> populateKafka() throws IOException, InterruptedException, ExecutionException, TimeoutException {
		Set<String> domains = new HashSet<>();

		try (InputStream in = getClass().getClassLoader().getResourceAsStream("data/kafka/clone-dump.tar.bz2");
				BZip2CompressorInputStream bz2 = new BZip2CompressorInputStream(in);
				TarArchiveInputStream tar = new TarArchiveInputStream(bz2)) {
			TarArchiveEntry ce;
			String iid = "bluemind-" + UUID.randomUUID().toString();
			int iidLen = iid.length();

			LongAdder storedRecords = new LongAdder();
			List<CompletableFuture<Void>> futures = new ArrayList<>();
			while ((ce = tar.getNextTarEntry()) != null) {
				if (!ce.isDirectory()) {
					iid = ce.getName().replace("./", "").substring(0, iidLen);
					System.setProperty("bm.mcast.id", iid);
					InstallationId.reload();

					String domain = ce.getName().replace("./", "").substring(iidLen + 1).replace(".json", "");
					String domainUid = (domain.equals("__orphans__")) ? null : domain;
					if (domainUid != null) {
						domains.add(domainUid);
					}

					IBackupStoreFactory store = DefaultBackupStore.store();
					byte[] jsonData = ByteStreams.toByteArray(tar);
					JsonArray js = new JsonArray(Buffer.buffer(jsonData));
					TopicNames topicNames = new TopicNames(iid);
					js.forEach(keyValue -> {
						JsonObject key = ((JsonObject) keyValue).getJsonObject("key");
						BaseContainerDescriptor descriptor = new BaseContainerDescriptor();
						descriptor.type = key.getString("type");
						descriptor.domainUid = domainUid;
						descriptor.owner = key.getString("owner");
						descriptor.uid = key.getString("uid");

						JsonObject value = ((JsonObject) keyValue).getJsonObject("value");
						if (key.getString("type").equals("sysconf") && key.getString("owner").equals("system")) {
							JsonObject sysconfMap = value.getJsonObject("value").getJsonObject("values");
							sysconfMap.put(SysConfKeys.sds_s3_endpoint.name(), s3);
							sysconfMap.put(SysConfKeys.sds_s3_bucket.name(), bucket);
							sysconfMap.put(SysConfKeys.sds_s3_access_key.name(), "accessKey1");
							sysconfMap.put(SysConfKeys.sds_s3_secret_key.name(), "verySecretKey1");
							System.err.println("Updated conf is " + value.encodePrettily());
						}
						// System.err.println(descriptor + ":\nkey:" + key.encode() + "\nvalue:" +
						// value.encode());
						String partitionKey = topicNames.forContainer(descriptor).partitionKey(value.getString("uid"));
						IBackupStore<Object> topic = store.forContainer(descriptor);
						CompletableFuture<Void> prom = topic.storeRaw(partitionKey, key.toBuffer().getBytes(),
								value.toBuffer().getBytes());
						futures.add(prom.whenComplete((v, ex) -> {
							if (ex != null) {
								ex.printStackTrace();
							}
							storedRecords.increment();
						}));
					});
				}
			}
			CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0])).get(10, TimeUnit.SECONDS);
			long total = storedRecords.sum();
			System.err.println("We stored " + total + " records in kafka");
			assertTrue(total > 0);
			InstallationId.getIdentifier();
			System.err.println("installation: " + iid);
			IBackupReader store = DefaultBackupStore.reader();
			ILiveBackupStreams streams = store.forInstallation(iid);
			System.err.println("After populating kafka we have " + streams.domains().size() + " domain streams");
			assertNotNull(streams.orphans());
			assertFalse(streams.domains().isEmpty());
			System.setProperty("backup.continuous.store.disabled", "true");
		}
		return domains;
	}

	@Test
	public void populateContainersFromKafkaContent() throws Exception {
		IBackupReader store = DefaultBackupStore.reader();

		ILiveStream anyStream = store.forInstallation(InstallationId.getIdentifier()).orphans();
		Path p = Paths.get("/etc/bm", "clone.state.json");
		new File("/etc/bm").mkdirs();
		CloneState cs = new CloneState(p, anyStream);
		cs.clear().save();

		IClonePhaseObserver obs = new IClonePhaseObserver() {
			boolean started = false;

			@Override
			public void beforeMailboxesPopulate(IServerTaskMonitor mon) {
				mon.log("beforeMailboxes.....");
				if (!started) {
					try {
						replicationHelper.startReplication().get(10, TimeUnit.SECONDS);
						started = true;
					} catch (InterruptedException | ExecutionException | TimeoutException e) {
						fail(e.getMessage());
					}
				}
			}
		};
		ServerSideServiceProvider prov = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);

		doClone(store, obs, prov);

		checkContainersLocations(prov, IMailboxAclUids.TYPE, false, true);
		checkContainersLocations(prov, IFlatHierarchyUids.TYPE, false, false);
		checkContainersLocations(prov, IOwnerSubscriptionUids.TYPE, false, false);
		checkContainersLocations(prov, IMailReplicaUids.REPLICATED_MBOXES, true, false);
		checkContainersLocations(prov, IMailReplicaUids.MAILBOX_RECORDS, true, false);

		IDomains domApi = prov.instance(IDomains.class);
		ItemValue<Domain> domFound = domApi.findByNameOrAliases("devenv.blue");
		assertNotNull(domFound);
		IDirectory dirApi = prov.instance(IDirectory.class, domFound.uid);
		DirEntry found = dirApi.getByEmail("tom@devenv.blue");
		assertNotNull(found);
		System.err.println("Got " + found);
		IUser user = prov.instance(IUser.class, domFound.uid);
		ItemValue<User> asUser = user.getComplete(found.entryUid);
		System.err.println("user: " + asUser);
		LoginResponse sudo = prov.instance(IAuthentication.class).su(asUser.value.defaultEmailAddress());
		assertNotNull(sudo.authUser.roles);
		System.err.println("roles: " + sudo.authUser.roles);
		assertTrue(sudo.authUser.roles.contains("hasMailWebapp"));

		// re-run after clone
		System.err.println("Run cloning process again.....");
		doClone(store, obs, prov);
	}

	private void doClone(IBackupReader store, IClonePhaseObserver obs, ServerSideServiceProvider prov) {
		CloneConfiguration conf = new CloneConfiguration();
		conf.sourceInstallationId = InstallationId.getIdentifier();
		conf.mode = Mode.FORK;

		DefaultSdsStoreLoader sds = new DefaultSdsStoreLoader();
		Collection<String> installs = store.installations();
		System.err.println("installations: " + installs);
		assertTrue(new HashSet<>(installs).contains(conf.sourceInstallationId));

		TopologyMapping topo = new TopologyMapping();
		topo.register("bm-master", cyrusIp);

		try {
			InstallFromBackupTask tsk = new InstallFromBackupTask(conf, store,
					new SysconfOverride(Collections.emptyMap()), topo, sds, prov);
			tsk.registerObserver(obs);
			TestTaskMonitor mon = new TestTaskMonitor();
			tsk.run(mon);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	private void checkContainersLocations(ServerSideServiceProvider prov, String type, boolean noneOnDirExcepted,
			boolean noneOnShardExcepted) throws SQLException {
		BmContext ctx = prov.getContext();
		ContainerStore cs = new ContainerStore(null, ctx.getDataSource(), SecurityContext.SYSTEM);
		List<Container> dirByType = cs.findByType(type);
		ContainerStore mboxCs = new ContainerStore(null, ctx.getMailboxDataSource("bm-master"), SecurityContext.SYSTEM);
		List<Container> shardByType = mboxCs.findByType(type);
		System.err.println("=== CHECKING " + type + " containers: " + dirByType.size() + " avail on dir, "
				+ shardByType.size() + " on shard ===");
		if (noneOnDirExcepted) {
			for (Container cd : dirByType) {
				String loc = DataSourceRouter.location(ctx, cd.uid);
				System.err.println("* " + cd.name + ", o: " + cd.owner + ", u: " + cd.uid + " loc: " + loc);
			}
			assertEquals("Expected no containers of type " + type + " on directory DB", 0, dirByType.size());
		}
		if (noneOnShardExcepted) {
			for (Container cd : shardByType) {
				String loc = DataSourceRouter.location(ctx, cd.uid);
				System.err.println("* " + cd.name + ", o: " + cd.owner + ", u: " + cd.uid + " loc: " + loc);
			}
			assertEquals("Expected no containers of type " + type + " on shard DB", 0, shardByType.size());
		}
//		for (Container cd : Iterables.concat(dirByType, shardByType)) {
//			String loc = DataSourceRouter.location(ctx, cd.uid);
//			System.err.println("* " + cd.name + ", o: " + cd.owner + ", u: " + cd.uid + " loc: " + loc);
//		}
	}

	@After
	public void after() throws Exception {
		try {
			StateContext.setState("core.cloning.end");
			if (replicationHelper != null) {
				replicationHelper.stopReplication().get(10, TimeUnit.SECONDS);
			}
			DefaultLeader.leader().releaseLeadership();

			Thread.sleep(2000);
			Files.deleteIfExists(marker);
			kafka.stop();
			kafka.close();
			System.clearProperty("backup.continuous.store.disabled");
			JdbcTestHelper.getInstance().afterTest();
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

}
