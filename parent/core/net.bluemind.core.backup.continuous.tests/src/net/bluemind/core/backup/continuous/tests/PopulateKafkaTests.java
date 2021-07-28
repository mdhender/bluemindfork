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

public class PopulateKafkaTests {

//	private String cyrusIp;
//	private ZkKafkaContainer kafka;
//	Path marker = Paths.get("/etc/bm/continuous.clone");
//	private String s3;
//	private String bucket;
//	private CyrusReplicationHelper replicationHelper;
//
//	@Before
//	public void before() throws Exception {
//		this.kafka = new ZkKafkaContainer();
//		kafka.start();
//		String ip = kafka.inspectAddress();
//		System.setProperty("bm.kafka.bootstrap.servers", ip + ":9093");
//		System.setProperty("bm.zk.servers", ip + ":2181");
//
//		this.s3 = "http://" + DockerEnv.getIp("bluemind/s3") + ":8000";
//		this.bucket = "junit-clone-kafka-" + System.currentTimeMillis();
//		S3Configuration s3conf = S3Configuration.withEndpointAndBucket(s3, bucket);
//		System.err.println(s3conf.asJson().encodePrettily());
//		ISdsBackingStore sds = new S3StoreFactory().create(VertxPlatform.getVertx(), s3conf.asJson());
//
//		System.err.println("sds: " + sds);
//		try (InputStream in = getClass().getClassLoader().getResourceAsStream("data/kafka/emls.tar.bz2");
//				BZip2CompressorInputStream bz2 = new BZip2CompressorInputStream(in);
//				TarArchiveInputStream tar = new TarArchiveInputStream(bz2)) {
//			TarArchiveEntry ce;
//			long s3content = 0;
//			while ((ce = tar.getNextTarEntry()) != null) {
//				if (!ce.isDirectory()) {
//					byte[] eml = ByteStreams.toByteArray(tar);
//					try (ZstdInputStream dec = new ZstdInputStream(new ByteArrayInputStream(eml))) {
//						eml = ByteStreams.toByteArray(dec);
//					}
//
//					Path tmp = Files.createTempFile(ce.getName(), ".eml");
//					Files.write(tmp, eml, StandardOpenOption.CREATE, StandardOpenOption.WRITE,
//							StandardOpenOption.TRUNCATE_EXISTING);
//					PutRequest pr = PutRequest.of(ce.getName(), tmp.toFile().getAbsolutePath());
//					sds.upload(pr).get(10, TimeUnit.SECONDS);
//					Files.delete(tmp);
//					s3content += eml.length;
//					System.err.println("Uploaded " + ce.getName() + " to s3 " + eml.length + " bytes");
//				}
//			}
//			System.err.println("Pushed " + s3content + " byte(s) to s3.");
//		}
//
//		JdbcTestHelper.getInstance().beforeTest();
//
//		Set<String> domains = populateKafka();
//
//		this.cyrusIp = new BmConfIni().get("imap-role");
//
//		Server imapServer = Server.tagged(cyrusIp, "mail/imap");
//		imapServer.name = "bm-master";
//		ItemValue<Server> withIp = ItemValue.create("bm-master", imapServer);
//		CyrusService cs = new CyrusService(withIp);
//		cs.reset();
//
//		List<String> parts = new LinkedList<>();
//		for (String dom : domains) {
//			cs.createPartition(dom);
//			parts.add(dom);
//		}
//		cs.refreshPartitions(parts);
//		try {
//			cs.reload();
//		} catch (Exception e) {
//			e.printStackTrace();
//			fail(e.getMessage());
//		}
//		System.err.println("partitions: " + parts);
//
//		Server esServer = new Server();
//		esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
//		System.out.println("ES is " + esServer.ip);
//		assertNotNull(esServer.ip);
//		esServer.tags = Lists.newArrayList("bm/es");
//
//		this.replicationHelper = new CyrusReplicationHelper(cyrusIp, "core");
//		String coreIp = CyrusReplicationHelper.getMyIpAddress();
//		System.setProperty("sync.core.address", coreIp);
//		System.err.println("coreIp for replication set to " + coreIp);
//
//		System.err.println("populate global virt...");
//		PopulateHelper.initGlobalVirt(imapServer, esServer);
//		ElasticsearchTestHelper.getInstance().beforeTest();
//		PopulateHelper.addDomainAdmin("admin0", "global.virt", Routing.none);
//
//		DataSource ds = ServerSideServiceProvider.mailboxDataSource.get(imapServer.ip);
//		assertNotNull(ds);
//		ServerSideServiceProvider.mailboxDataSource.put("bm-master", ds);
//
//		VertxPlatform.spawnBlocking(20, TimeUnit.SECONDS);
//		System.err.println("Waiting for SyncServer...");
//		SyncServerHelper.waitFor();
//		System.err.println("=======================");
//	}
//
//	private Set<String> populateKafka() throws IOException {
//		Set<String> domains = new HashSet<>();
//
//		try (InputStream in = getClass().getClassLoader().getResourceAsStream("data/kafka/clone-dump.tar.bz2");
//				BZip2CompressorInputStream bz2 = new BZip2CompressorInputStream(in);
//				TarArchiveInputStream tar = new TarArchiveInputStream(bz2)) {
//			TarArchiveEntry ce;
//			String iid = "bluemind-" + UUID.randomUUID().toString();
//			int iidLen = iid.length();
//
//			while ((ce = tar.getNextTarEntry()) != null) {
//				if (!ce.isDirectory()) {
//					byte[] jsonData = ByteStreams.toByteArray(tar);
//					iid = ce.getName().substring(0, iidLen);
//					System.setProperty("bm.mcast.id", iid);
//					InstallationId.reload();
//					String typeAndDomain = ce.getName().substring(iidLen + 1);
//					JsonObject js = new JsonObject(Buffer.buffer(jsonData));
//					String type;
//					String domainUid;
//					if (typeAndDomain.endsWith("___orphans__.json")) {
//						String justType = typeAndDomain.replace("___orphans__.json", "");
//						type = justType;
//						domainUid = null;
//
//					} else {
//						int lastUnderscore = typeAndDomain.lastIndexOf('_');
//						domainUid = typeAndDomain.substring(lastUnderscore + 1,
//								typeAndDomain.length() - ".json".length());
//						type = typeAndDomain.substring(0, lastUnderscore);
//						System.err.println("dom: " + domainUid + ", type: " + type);
//					}
//					IBackupStoreFactory store = DefaultBackupStore.get();
//
//					BaseContainerDescriptor bcd = new BaseContainerDescriptor();
//					bcd.type = type;
//					bcd.domainUid = domainUid;
//					if (domainUid != null) {
//						domains.add(domainUid);
//					}
//					for (String ownerContItemId : js.fieldNames()) {
//						String[] splitted = ownerContItemId.split("/");
//						String[] idAndValueClass = splitted[3].split("#");
//						bcd.owner = splitted[1];
//						bcd.uid = splitted[2];
//						RecordKey key = new RecordKey(splitted[0], splitted[1], splitted[2],
//								Integer.valueOf(idAndValueClass[0]), idAndValueClass[1]);
//						IBackupStore<Object> topic = store.forContainer(bcd);
//						JsonObject sub = js.getJsonObject(ownerContItemId);
//						if (ownerContItemId.startsWith("system/sysconf/")) {
//							JsonObject sysconfMap = sub.getJsonObject("value").getJsonObject("values");
//							sysconfMap.put(SysConfKeys.sds_s3_endpoint.name(), s3);
//							sysconfMap.put(SysConfKeys.sds_s3_bucket.name(), bucket);
//							sysconfMap.put(SysConfKeys.sds_s3_access_key.name(), "accessKey1");
//							sysconfMap.put(SysConfKeys.sds_s3_secret_key.name(), "verySecretKey1");
//							System.err.println("Updated conf is " + sub.encodePrettily());
//						}
//						topic.storeRaw(key, sub.toBuffer().getBytes());
//					}
//
//				}
//			}
//			InstallationId.getIdentifier();
//			System.err.println("installation: " + iid);
//			IBackupStoreFactory store = DefaultBackupStore.get();
//			ILiveBackupStreams streams = store.forInstallation(iid);
//			System.err.println("After populating kafka we have " + streams.listAvailable().size());
//			assertFalse(streams.listAvailable().isEmpty());
//			DefaultBackupStore.disabled = true;
//
//		}
//		return domains;
//	}
//
//	@Test
//	public void populateContainersFromKafkaContent() throws Exception {
//
//		TopologyMapping topo = new TopologyMapping();
//		topo.register("bm-master", cyrusIp);
//		DefaultSdsStoreLoader sds = new DefaultSdsStoreLoader();
//		IBackupStoreFactory store = DefaultBackupStore.get();
//		Collection<String> installs = store.installations();
//		assertEquals(1, installs.size());
//		ServerSideServiceProvider prov = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
//
//		String iid = installs.iterator().next();
//		List<ILiveStream> streams = store.forInstallation(iid).listAvailable();
//		Path p = Paths.get("/etc/bm", "clone.state.json");
//		CloneState cs = new CloneState(p, streams.get(0));
//		cs.clear().save();
//		ILiveStream instStream = streams.stream().filter(ls -> ls.type().equals("installation")).findAny()
//				.orElseThrow(Exception::new);
//		IResumeToken token = instStream.subscribe(de -> {
//		});
//		cs.record(instStream.fullName(), token).save();
//
//		IClonePhaseObserver obs = new IClonePhaseObserver() {
//			boolean started = false;
//
//			@Override
//			public void beforeMailboxesPopulate(IServerTaskMonitor mon) {
//				mon.log("beforeMailboxes.....");
//				if (!started) {
//					try {
//						replicationHelper.startReplication().get(10, TimeUnit.SECONDS);
//						started = true;
//					} catch (InterruptedException | ExecutionException | TimeoutException e) {
//						fail(e.getMessage());
//					}
//				}
//			}
//		};
//
//		try {
//			InstallFromBackupTask tsk = new InstallFromBackupTask(iid, store, topo, sds, prov);
//			tsk.registerObserver(obs);
//			TestTaskMonitor mon = new TestTaskMonitor();
//			tsk.run(mon);
//		} catch (Exception e) {
//			e.printStackTrace();
//			fail(e.getMessage());
//		}
//		checkContainersLocations(prov, IMailboxAclUids.TYPE, false, true);
//		checkContainersLocations(prov, IFlatHierarchyUids.TYPE, false, false);
//		checkContainersLocations(prov, IOwnerSubscriptionUids.TYPE, false, false);
//		checkContainersLocations(prov, IMailReplicaUids.REPLICATED_MBOXES, true, false);
//		checkContainersLocations(prov, IMailReplicaUids.MAILBOX_RECORDS, true, false);
//
//	}
//
//	private void checkContainersLocations(ServerSideServiceProvider prov, String type, boolean noneOnDirExcepted,
//			boolean noneOnShardExcepted) throws SQLException {
//		BmContext ctx = prov.getContext();
//		ContainerStore cs = new ContainerStore(null, ctx.getDataSource(), SecurityContext.SYSTEM);
//		List<Container> dirByType = cs.findByType(type);
//		ContainerStore mboxCs = new ContainerStore(null, ctx.getMailboxDataSource("bm-master"), SecurityContext.SYSTEM);
//		List<Container> shardByType = mboxCs.findByType(type);
//		System.err.println("=== CHECKING " + type + " containers: " + dirByType.size() + " avail on dir, "
//				+ shardByType.size() + " on shard ===");
//		if (noneOnDirExcepted) {
//			for (Container cd : dirByType) {
//				String loc = DataSourceRouter.location(ctx, cd.uid);
//				System.err.println("* " + cd.name + ", o: " + cd.owner + ", u: " + cd.uid + " loc: " + loc);
//			}
//			assertEquals("Expected no containers of type " + type + " on directory DB", 0, dirByType.size());
//		}
//		if (noneOnShardExcepted) {
//			for (Container cd : shardByType) {
//				String loc = DataSourceRouter.location(ctx, cd.uid);
//				System.err.println("* " + cd.name + ", o: " + cd.owner + ", u: " + cd.uid + " loc: " + loc);
//			}
//			assertEquals("Expected no containers of type " + type + " on shard DB", 0, shardByType.size());
//		}
////		for (Container cd : Iterables.concat(dirByType, shardByType)) {
////			String loc = DataSourceRouter.location(ctx, cd.uid);
////			System.err.println("* " + cd.name + ", o: " + cd.owner + ", u: " + cd.uid + " loc: " + loc);
////		}
//	}
//
//	@After
//	public void after() throws Exception {
//		replicationHelper.stopReplication().get(10, TimeUnit.SECONDS);
//		Thread.sleep(2000);
//		Files.deleteIfExists(marker);
//		kafka.stop();
//		DefaultBackupStore.disabled = false;
//		JdbcTestHelper.getInstance().afterTest();
//	}

}
