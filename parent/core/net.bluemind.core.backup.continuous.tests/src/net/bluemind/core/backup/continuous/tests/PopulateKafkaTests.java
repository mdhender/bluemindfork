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

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHits;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.common.io.ByteStreams;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import net.bluemind.authentication.api.IAuthentication;
import net.bluemind.authentication.api.LoginResponse;
import net.bluemind.aws.s3.utils.S3Configuration;
import net.bluemind.backend.cyrus.partitions.CyrusPartition;
import net.bluemind.backend.mail.api.MailboxFolder;
import net.bluemind.backend.mail.replica.api.IDbMailboxRecords;
import net.bluemind.backend.mail.replica.api.IDbMessageBodies;
import net.bluemind.backend.mail.replica.api.IDbReplicatedMailboxes;
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
import net.bluemind.directory.api.DirEntry;
import net.bluemind.directory.api.IDirectory;
import net.bluemind.dockerclient.DockerEnv;
import net.bluemind.domain.api.Domain;
import net.bluemind.domain.api.IDomains;
import net.bluemind.kafka.container.ZkKafkaContainer;
import net.bluemind.lib.elasticsearch.ESearchActivator;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.IMailboxAclUids;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.node.server.BlueMindUnsecureNode;
import net.bluemind.node.server.busmod.SysCommand;
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
import net.bluemind.vertx.testhelper.Deploy;

public class PopulateKafkaTests {

	private ZkKafkaContainer kafka;
	Path marker = Paths.get(CloneDefaults.MARKER_FILE_PATH);
	private String s3;
	private String bucket;
	private Client client;

	@Before
	public void before() throws Exception {
		Deploy.verticles(false, BlueMindUnsecureNode::new).get(5, TimeUnit.SECONDS);
		Deploy.verticles(true, SysCommand::new).get(5, TimeUnit.SECONDS);

		this.kafka = new ZkKafkaContainer();
		kafka.start();
		String ip = kafka.inspectAddress();
		System.setProperty("bm.kafka.bootstrap.servers", ip + ":9093");
		System.setProperty("bm.zk.servers", ip + ":2181");
		System.setProperty("node.local.ipaddr", PopulateHelper.FAKE_CYRUS_IP);
		System.setProperty("imap.local.ipaddr", PopulateHelper.FAKE_CYRUS_IP);
		System.setProperty("cyrus.deactivated", "true");

		DefaultLeader.reset();
		DefaultBackupStore.reset();
		StateContext.setState("reset");

		this.s3 = "http://" + DockerEnv.getIp("bluemind/s3") + ":8000";
		this.bucket = "junit-clone-kafka-" + System.currentTimeMillis();
		S3Configuration s3conf = S3Configuration.withEndpointAndBucket(s3, bucket);
		System.err.println(s3conf.asJson().encodePrettily());
		ISdsBackingStore sds = new S3StoreFactory().create(VertxPlatform.getVertx(), s3conf.asJson(), "unused");

		await().atMost(20, TimeUnit.SECONDS).until(DefaultLeader.leader()::isLeader);

		JdbcTestHelper.getInstance().beforeTest();
		JdbcTestHelper.getInstance().getDataSource().getConnection().createStatement()
				.execute("select setval('t_container_item_id_seq', 10000)");

		VertxPlatform.spawnBlocking(20, TimeUnit.SECONDS);

		populateKafka();

		Server pipo = new Server();
		pipo.tags = Collections.singletonList("mail/imap");
		pipo.ip = PopulateHelper.FAKE_CYRUS_IP;
		pipo.name = "bm-master";

		Server esServer = new Server();
		esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		System.out.println("ES is " + esServer.ip);
		assertNotNull(esServer.ip);
		esServer.tags = Lists.newArrayList("bm/es");

		PopulateHelper.initGlobalVirt(pipo, esServer);
		ElasticsearchTestHelper.getInstance().beforeTest();
		PopulateHelper.addDomainAdmin("admin0", "global.virt", Routing.none);

		client = ESearchActivator.getClient();

		DataSource datasource = JdbcTestHelper.getInstance().getMailboxDataDataSource();
		assertNotNull(datasource);
		ServerSideServiceProvider.mailboxDataSource.put("bm-master", datasource);

		datasource.getConnection().createStatement().execute("select setval('t_container_item_id_seq', 100000)");

		StateContext.setState("core.started");
		StateContext.setState("core.cloning.start");
	}

	private Set<String> populateKafka() throws IOException, InterruptedException, ExecutionException, TimeoutException {
		Set<String> domains = new HashSet<>();

		try (InputStream in = getClass().getClassLoader()
				.getResourceAsStream("data/kafka/clone-dump-without-archive-kind-cyrus.tar.bz2");
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
		CloneState cs = new CloneState(p, anyStream);
		cs.clear().save();

		ServerSideServiceProvider prov = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);

		doClone(store, prov);

		checkContainersLocations(prov, IMailboxAclUids.TYPE, false, true);
		checkContainersLocations(prov, IFlatHierarchyUids.TYPE, false, false);
		checkContainersLocations(prov, IOwnerSubscriptionUids.TYPE, false, false);
		checkContainersLocations(prov, IMailReplicaUids.REPLICATED_MBOXES, true, false);
		checkContainersLocations(prov, IMailReplicaUids.MAILBOX_RECORDS, true, false);

		IDomains domApi = prov.instance(IDomains.class);
		ItemValue<Domain> domFound = domApi.findByNameOrAliases("devenv.blue");
		assertNotNull(domFound);
		IDirectory dirApi = prov.instance(IDirectory.class, domFound.uid);
		DirEntry found = dirApi.getByEmail("sylvain@devenv.blue");
		assertNotNull(found);
		System.err.println("Got " + found);
		IUser user = prov.instance(IUser.class, domFound.uid);
		ItemValue<User> asUser = user.getComplete(found.entryUid);
		System.err.println("user: " + asUser);
		LoginResponse sudo = prov.instance(IAuthentication.class).su(asUser.value.defaultEmailAddress());
		assertNotNull(sudo.authUser.roles);
		System.err.println("roles: " + sudo.authUser.roles);

//		assertTrue(sudo.authUser.roles.contains("hasMailWebapp"));

		IMailboxes mailboxesApi = prov.instance(IMailboxes.class, domFound.uid);
		ItemValue<Mailbox> mailboxSylvain = mailboxesApi.byName("sylvain");
		assertNotNull(mailboxSylvain);
		ItemValue<Mailbox> mailboxTom = mailboxesApi.byName("tom");
		assertNotNull(mailboxTom);
		ItemValue<Mailbox> mailboxDavid = mailboxesApi.byName("david");
		assertNotNull(mailboxDavid);

		CyrusPartition partition = CyrusPartition.forServerAndDomain(mailboxSylvain.value.dataLocation, domFound.uid);

		IDbReplicatedMailboxes apiReplicatedMailboxesSylvain = prov.instance(IDbReplicatedMailboxes.class,
				partition(domFound.uid), mboxRoot(mailboxSylvain));
		long sylvainFolderCount = apiReplicatedMailboxesSylvain.all().stream().count();
		assertEquals(163L, sylvainFolderCount);
		ItemValue<MailboxFolder> folderOne = apiReplicatedMailboxesSylvain.byName("003_Guatemala");
		IDbMailboxRecords apiMailboxRecordsOneSylvain = prov.instance(IDbMailboxRecords.class, folderOne.uid);
		int mailboxRecordsOneSylvain = apiMailboxRecordsOneSylvain.all().size();
		assertEquals(1, mailboxRecordsOneSylvain);
		ItemValue<MailboxFolder> folderTwo = apiReplicatedMailboxesSylvain.byName("003_Guatemala/004_Duvel");
		IDbMailboxRecords apiMailboxRecordsTwoSylvain = prov.instance(IDbMailboxRecords.class, folderTwo.uid);
		int mailboxRecordsTwoSylvain = apiMailboxRecordsTwoSylvain.all().size();
		assertEquals(5, mailboxRecordsTwoSylvain);

		IDbReplicatedMailboxes apiReplicatedMailboxesTom = prov.instance(IDbReplicatedMailboxes.class,
				partition(domFound.uid), mboxRoot(mailboxTom));
		long tomFolderCount = apiReplicatedMailboxesTom.all().stream().count();
		assertEquals(163L, tomFolderCount);

		IDbReplicatedMailboxes apiReplicatedMailboxesDavid = prov.instance(IDbReplicatedMailboxes.class,
				partition(domFound.uid), mboxRoot(mailboxDavid));
		long davidFolderCount = apiReplicatedMailboxesDavid.all().stream().count();
		assertEquals(163L, davidFolderCount);

		IDbMessageBodies apiMessageBodies = prov.instance(IDbMessageBodies.class, partition.name);

		assertTrue(apiMessageBodies.exists("e9c74bbfafe6a04d0bc3e58d5dbe6049d7d3ec35"));
		assertTrue(apiMessageBodies.exists("020f7775f925298ebd488fbe07ca0dc5e690e90d"));
		assertTrue(apiMessageBodies.exists("2cb215cb90a7f3e0bd11adc413a621f1f03ea1de"));
		assertTrue(apiMessageBodies.exists("6b907e9ecbd738b7e316edde5e8da984549ea9bc"));
		assertTrue(apiMessageBodies.exists("6dd42c28c8ba84aa595e2e1967975ebb6c3550d1"));
		assertTrue(apiMessageBodies.exists("bb7c9582c29aba68dd5a2dbecbf643c70f6ec60e"));
		assertTrue(apiMessageBodies.exists("f91381b5399c1e8672b8914c59167cac528af139"));
		assertTrue(apiMessageBodies.exists("c3e3506432ffb5d3932766b31ddba3275086e48f"));

		GetAliasesResponse responseAliases = client.admin().indices()
				.prepareGetAliases("mailspool_alias_cli-created-a1f319cb-806c-330b-8c15-48810870cfcf").execute()
				.actionGet();

		BoolQueryBuilder singleIdQuery = QueryBuilders.boolQuery()//
				.must(QueryBuilders.termQuery("_id", "e9c74bbfafe6a04d0bc3e58d5dbe6049d7d3ec35"));
		String index = responseAliases.getAliases().keysIt().next();
		SearchResponse resp = client.prepareSearch(index).setQuery(singleIdQuery).execute().actionGet();
		SearchHits hitsResponse = resp.getHits();
		long totalHits = hitsResponse.getTotalHits().value;
		assertEquals(1L, totalHits);
		GetResponse response = client
				.prepareGet("mailspool_pending_read_alias", "eml", "e9c74bbfafe6a04d0bc3e58d5dbe6049d7d3ec35").get();

		Map<String, Object> map = response.getSource();
		assertTrue(map.containsKey("subject"));
		assertEquals("[Lys] News from Wylis Manderly with Caraxes", map.get("subject"));

		SearchResponse respTotal = client.prepareSearch(index).execute().actionGet();
		SearchHits hitsTotalResponse = respTotal.getHits();
		assertEquals(1996L, hitsTotalResponse.getTotalHits().value);

		// re-run after clone
		System.err.println("Run cloning process again.....");
		doClone(store, prov);
	}

	private void doClone(IBackupReader store, ServerSideServiceProvider prov) {
		CloneConfiguration conf = new CloneConfiguration();
		conf.sourceInstallationId = InstallationId.getIdentifier();
		conf.mode = Mode.FORK;

		DefaultSdsStoreLoader sds = new DefaultSdsStoreLoader();
		Collection<String> installs = store.installations();
		System.err.println("installations: " + installs);
		assertTrue(new HashSet<>(installs).contains(conf.sourceInstallationId));

		TopologyMapping topo = new TopologyMapping();
		topo.register("bm-master", PopulateHelper.FAKE_CYRUS_IP);

		try {
			InstallFromBackupTask tsk = new InstallFromBackupTask(conf, store,
					new SysconfOverride(Collections.emptyMap()), topo, sds, prov);
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
	}

	@After
	public void after() throws Exception {
		try {
			StateContext.setState("core.cloning.end");
			DefaultLeader.leader().releaseLeadership();
			System.clearProperty("bm.kafka.bootstrap.servers");
			System.clearProperty("bm.zk.servers");
			DefaultLeader.reset();
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

	private String mboxRoot(ItemValue<Mailbox> mbox) {
		if (mbox.value.type.sharedNs) {
			return mbox.value.name.replace(".", "^");
		}
		return "user." + mbox.value.name.replace(".", "^");
	}

	private String partition(String domainUid) {
		return domainUid.replace(".", "_");
	}

}
