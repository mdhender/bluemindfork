package net.bluemind.core.backup.continuous.events.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.testcontainers.shaded.org.awaitility.Awaitility;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import io.vertx.core.json.JsonObject;
import net.bluemind.aws.s3.utils.S3Configuration;
import net.bluemind.backend.mail.api.MessageBody;
import net.bluemind.config.InstallationId;
import net.bluemind.core.backup.continuous.DefaultBackupStore;
import net.bluemind.core.backup.continuous.IBackupReader;
import net.bluemind.core.backup.continuous.RecordKey;
import net.bluemind.core.backup.continuous.dto.IndexedMessageBodyDTO;
import net.bluemind.core.backup.continuous.events.MessageBodyESSourceHook;
import net.bluemind.core.backup.continuous.events.MessageBodyHook;
import net.bluemind.core.backup.continuous.leader.DefaultLeader;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.tests.BmTestContext;
import net.bluemind.delivery.lmtp.ApiProv;
import net.bluemind.delivery.lmtp.LmtpMessageHandler;
import net.bluemind.delivery.lmtp.dedup.DuplicateDeliveryDb;
import net.bluemind.dockerclient.DockerEnv;
import net.bluemind.kafka.container.ZkKafkaContainer;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.sds.store.ISdsBackingStore;
import net.bluemind.server.api.Server;
import net.bluemind.system.api.ISystemConfiguration;
import net.bluemind.system.api.SysConfKeys;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class MailMessageBodyHookTests {

	public MessageBodyHook mailMessageBodyHook;
	private BmTestContext context;
	private SecurityContext defaultSecurityContext;
	private ItemValue<Mailbox> mboxUser1;
	private ItemValue<Mailbox> mboxUser2;
	private String emailUser1;
	private String emailUser2;
	private static String domainUid;
	private String user1Uid;
	private String user2Uid;
	private String iid;
	private static SecurityContext secCtxUser1;
	private String s3;
	private String bucket;
	private ISdsBackingStore sds;
	private static ZkKafkaContainer kafka;

	private static DuplicateDeliveryDb dedup = DuplicateDeliveryDb.get();

	@BeforeClass
	public static void beforeClass() throws Exception {
		kafka = new ZkKafkaContainer();
		kafka.start();
		String ip = kafka.inspectAddress();
		System.setProperty("bm.kafka.bootstrap.servers", ip + ":9093");
		System.setProperty("bm.zk.servers", ip + ":2181");
		System.setProperty("node.local.ipaddr", "localhost,127.0.0.1," + PopulateHelper.FAKE_CYRUS_IP);
	}

	@Before
	public void before() throws Exception {

		DefaultLeader.reset();
		DefaultBackupStore.reset();
		domainUid = "test" + System.currentTimeMillis() + ".lab";

		iid = "bluemind-" + UUID.randomUUID().toString();
		System.setProperty("bm.mcast.id", iid);
		InstallationId.reload();

		Awaitility.await().atMost(20, TimeUnit.SECONDS).until(DefaultLeader.leader()::isLeader);

		JdbcTestHelper.getInstance().beforeTest();
		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());

		Server pipo = new Server();
		pipo.ip = PopulateHelper.FAKE_CYRUS_IP;
		pipo.tags = Collections.singletonList("mail/imap");

		Server esServer = new Server();
		esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		esServer.tags = Lists.newArrayList("bm/es");

		VertxPlatform.spawnBlocking(25, TimeUnit.SECONDS);

		PopulateHelper.initGlobalVirt(pipo, esServer);
		ElasticsearchTestHelper.getInstance().beforeTest();
		PopulateHelper.addDomainAdmin("admin0", "global.virt", Routing.none);

		PopulateHelper.addDomain(domainUid, Routing.none);
		user1Uid = PopulateHelper.addUser("user1", domainUid, Routing.internal);
		user2Uid = PopulateHelper.addUser("user2", domainUid, Routing.internal);
		mboxUser1 = systemServiceProvider().instance(IMailboxes.class, domainUid).getComplete(user1Uid);
		mboxUser2 = systemServiceProvider().instance(IMailboxes.class, domainUid).getComplete(user2Uid);
		Assert.assertNotNull(mboxUser1);
		Assert.assertNotNull(mboxUser2);

		emailUser1 = mboxUser1.value.defaultEmail().address;
		emailUser2 = mboxUser2.value.defaultEmail().address;

		defaultSecurityContext = BmTestContext
				.contextWithSession("testUser", "user2", domainUid, SecurityContext.ROLE_SYSTEM).getSecurityContext();
		context = new BmTestContext(defaultSecurityContext);

		secCtxUser1 = new SecurityContext("apikey", user1Uid, Collections.emptyList(), Collections.emptyList(),
				domainUid);

		this.s3 = "http://" + DockerEnv.getIp("bluemind/s3") + ":8000";
		this.bucket = "junit-clone-kafka-" + System.currentTimeMillis();
		S3Configuration s3conf = S3Configuration.withEndpointAndBucket(s3, bucket);
		System.err.println(s3conf.asJson().encodePrettily());
		ImmutableMap<String, String> freshConf = new ImmutableMap.Builder<String, String>() //
				.put(SysConfKeys.archive_kind.name(), "s3") //
				.put(SysConfKeys.sds_s3_access_key.name(), s3conf.getAccessKey()) //
				.put(SysConfKeys.sds_s3_secret_key.name(), s3conf.getSecretKey()) //
				.put(SysConfKeys.sds_s3_endpoint.name(), s3conf.getEndpoint()) //
				.put(SysConfKeys.sds_s3_region.name(), s3conf.getRegion()) //
				.put(SysConfKeys.sds_s3_bucket.name(), s3conf.getBucket()) //
				.build();
		ServerSideServiceProvider prov = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
		ISystemConfiguration sysConfApi = prov.instance(ISystemConfiguration.class);
		sysConfApi.updateMutableValues(freshConf);

//		sds = new S3StoreFactory().create(VertxPlatform.getVertx(), s3conf.asJson());
//		System.err.println("sds: " + sds);
	}

	@After
	public void tearDown() throws Exception {
		JdbcTestHelper.getInstance().afterTest();
		System.clearProperty("bm.mcast.id");
		InstallationId.reload();
	}

	@AfterClass
	public static void afterClass() throws Exception {
		DefaultLeader.leader().releaseLeadership();
		System.clearProperty("bm.kafka.bootstrap.servers");
		System.clearProperty("bm.zk.servers");
		DefaultLeader.reset();
		Thread.sleep(2_000);
		kafka.stop();
		kafka.close();
		System.clearProperty("backup.continuous.store.disabled");
	}

	@Test
	public void testMailMessageBodyHook() throws Exception {
		ApiProv prov = k -> context.getServiceProvider();
		LmtpMessageHandler messageHandler = new LmtpMessageHandler(prov, dedup);
		messageHandler.deliver(emailUser1, emailUser2, eml("emls/test_mail_empty_references.eml"));
		messageHandler.deliver(emailUser1, emailUser1, eml("emls/test_mail_empty_references1.eml"));
		messageHandler.deliver(emailUser2, emailUser1, eml("emls/test_mail_empty_references2.eml"));

		IBackupReader reader = DefaultBackupStore.reader();
		assertNotNull("reader must not be null", reader);

		var streams = reader.forInstallation(iid).domains();
		List<RecordKey> keys = new ArrayList<>();
		streams.get(0).subscribe(de -> {
			keys.add(de.key);
			if (de.key.type.equals((new MessageBodyHook()).type())) {
				assertEquals("CREATE", de.key.operation);
				assertEquals(MessageBody.class.getCanonicalName(), de.key.valueClass);
				var jsonObject = new JsonObject(new String(de.payload));
				var value = (JsonObject) jsonObject.getValue("value");
				assertEquals(true, value.containsKey("guid"));
				assertEquals(true, value.containsKey("subject"));
				assertEquals(true, value.containsKey("smartAttach"));
				assertEquals(true, value.containsKey("size"));
				assertEquals(true, value.containsKey("headers"));
				assertEquals(true, value.containsKey("messageId"));
				assertEquals(true, value.containsKey("structure"));
				assertEquals(true, value.containsKey("size"));
				assertEquals(true, value.containsKey("preview"));
				assertEquals(true, value.containsKey("date"));
			}
			if (de.key.type.equals("mailbox_records")) {
				System.err.println(de.key.uid);
			}
		});
		long countMessageBody = keys.stream().filter(k -> k.type.equals((new MessageBodyHook()).type())).count();
		long countUser1MessageBodies = keys.stream()
				.filter(k -> k.owner.equals(user1Uid) && k.type.equals((new MessageBodyHook()).type())).count();
		long countUser2MessageBodies = keys.stream()
				.filter(k -> k.owner.equals(user2Uid) && k.type.equals((new MessageBodyHook()).type())).count();
		// 3 messages with messageBody type because of 3 sent mails
		assertEquals(3L, countMessageBody);
		assertEquals(2L, countUser1MessageBodies);
		assertEquals(1L, countUser2MessageBodies);

	}

	@Test
	public void testMailMessageBodyLESSourceHook() throws Exception {
		ApiProv prov = k -> context.getServiceProvider();
		LmtpMessageHandler messageHandler = new LmtpMessageHandler(prov, dedup);
		messageHandler.deliver(emailUser1, emailUser2, eml("emls/test_mail_empty_references.eml"));

		IBackupReader reader = DefaultBackupStore.reader();
		assertNotNull("reader must not be null", reader);

		var streams = reader.forInstallation(iid).domains();
		List<String> keys = new ArrayList<>();
		streams.get(0).subscribe(de -> {
			keys.add(de.key.type);
			if (de.key.type.equals((new MessageBodyESSourceHook()).type())) {
				assertEquals("CREATE", de.key.operation);
				assertEquals(IndexedMessageBodyDTO.class.getCanonicalName(), de.key.valueClass);
				var jsonObject = new JsonObject(new String(de.payload));
				var value = (JsonObject) jsonObject.getValue("value");
				var data = (JsonObject) value.getValue("data");
				assertEquals(true, data.containsKey("preview"));
				assertEquals(true, data.containsKey("date"));
				assertEquals(true, data.containsKey("cc"));
				assertEquals(true, data.containsKey("headers"));
				assertEquals(true, data.containsKey("references"));
				assertEquals(true, data.containsKey("subject"));
				assertEquals(true, data.containsKey("messageId"));
				assertEquals(true, data.containsKey("content"));
				assertEquals(true, data.containsKey("subject_kw"));
				assertEquals(true, data.containsKey("with"));
				assertEquals(true, data.containsKey("size"));
				assertEquals(true, data.containsKey("content-type"));
				assertEquals(true, data.containsKey("from"));
				assertEquals(true, data.containsKey("to"));
				assertEquals(true, data.containsKey("has"));
			}
		});
		long messageBodyTypeCount = keys.stream().filter(k -> k.equals((new MessageBodyESSourceHook()).type())).count();
		// 1 message with messageBodyES types because of 1 recipient
		assertEquals(1L, messageBodyTypeCount);

	}

	private InputStream eml(String resPath) {
		return MailMessageBodyHookTests.class.getClassLoader().getResourceAsStream(resPath);
	}

	protected IServiceProvider systemServiceProvider() {
		return ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
	}

}
