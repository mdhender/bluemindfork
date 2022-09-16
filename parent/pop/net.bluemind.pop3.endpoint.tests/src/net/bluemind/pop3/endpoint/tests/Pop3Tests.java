/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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
package net.bluemind.pop3.endpoint.tests;

import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.awaitility.Awaitility;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.io.ByteStreams;
import com.typesafe.config.Config;

import freemarker.template.Configuration;
import freemarker.template.Template;
import io.vertx.core.buffer.Buffer;
import net.bluemind.backend.cyrus.CyrusService;
import net.bluemind.backend.cyrus.partitions.CyrusPartition;
import net.bluemind.backend.cyrus.replication.testhelper.CyrusReplicationHelper;
import net.bluemind.backend.cyrus.replication.testhelper.SyncServerHelper;
import net.bluemind.backend.mail.api.IMailboxFolders;
import net.bluemind.backend.mail.api.IMailboxItems;
import net.bluemind.backend.mail.api.MailboxFolder;
import net.bluemind.backend.mail.api.MailboxItem;
import net.bluemind.backend.mail.api.MessageBody;
import net.bluemind.backend.mail.api.MessageBody.Part;
import net.bluemind.backend.mail.replica.api.IDbMailboxRecords;
import net.bluemind.backend.mail.replica.api.IDbReplicatedMailboxes;
import net.bluemind.core.container.api.Count;
import net.bluemind.core.container.api.IdRange;
import net.bluemind.core.container.model.ItemFlag;
import net.bluemind.core.container.model.ItemFlagFilter;
import net.bluemind.core.container.model.ItemIdentifier;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.elasticsearch.ElasticsearchTestHelper;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.rest.vertx.VertxStream;
import net.bluemind.core.sessions.Sessions;
import net.bluemind.hornetq.client.MQ;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.network.topology.Topology;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.pop3.endpoint.Pop3Config;
import net.bluemind.server.api.Server;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class Pop3Tests {
	private String domainUid;
	private String user1Login;
	private String cyrusIp;
	private CyrusReplicationHelper cyrusReplication;
	private CyrusPartition partition;
	protected String mboxRoot;
	private SecurityContext secCtxUser1;
	protected String apiKey;
	protected IdRange allocations;
	List<ItemValue<MailboxItem>> createdMails;

	private static final Logger logger = LoggerFactory.getLogger(Pop3Tests.class);

	@Before
	public void before() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		BmConfIni ini = new BmConfIni();

		Server esServer = new Server();
		esServer.ip = ElasticsearchTestHelper.getInstance().getHost();
		Assert.assertNotNull(esServer.ip);
		esServer.tags = Lists.newArrayList("bm/es");

		this.cyrusIp = ini.get("imap-role");
		Server imapServer = new Server();
		imapServer.ip = cyrusIp;
		imapServer.tags = Lists.newArrayList("mail/imap");

		PopulateHelper.initGlobalVirt(esServer, imapServer);
		ElasticsearchTestHelper.getInstance().beforeTest();
		PopulateHelper.addDomainAdmin("admin0", "global.virt", Routing.none);

		String unique = "" + System.currentTimeMillis();

		domainUid = "pop3dom-" + unique + ".lab";
		user1Login = "user" + unique;

		PopulateHelper.addDomain(domainUid, Routing.internal);

		// ensure the partition is created correctly before restarting cyrus
		new CyrusService(cyrusIp).createPartition(domainUid);
		new CyrusService(cyrusIp).refreshPartitions(Arrays.asList(domainUid));
		new CyrusService(cyrusIp).reload();

		this.cyrusReplication = new CyrusReplicationHelper(cyrusIp);
		cyrusReplication.installReplication();

		JdbcActivator.getInstance().addMailboxDataSource(cyrusReplication.server().uid,
				JdbcTestHelper.getInstance().getMailboxDataDataSource());
		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);

		MQ.init().get(30, TimeUnit.SECONDS);
		Topology.get();

		this.partition = CyrusPartition.forServerAndDomain(Topology.get().any("mail/imap"), domainUid);

		SyncServerHelper.waitFor();

		cyrusReplication.startReplication().get(5, TimeUnit.SECONDS);

		logger.info("Start populate user " + user1Login);
		PopulateHelper.addUser(user1Login, domainUid, Routing.internal);

		this.apiKey = "sid";
		this.secCtxUser1 = new SecurityContext(apiKey, user1Login, Collections.emptyList(), Collections.emptyList(),
				domainUid);

		Sessions.get().put(apiKey, secCtxUser1);
		this.mboxRoot = "user." + user1Login.replace('.', '^');

		// Wait for INBOX
		for (int i = 0; i < 30; i++) {
			ItemValue<MailboxFolder> inbox = provider().instance(IDbReplicatedMailboxes.class, partition.name, mboxRoot)
					.byName("INBOX");
			if (inbox != null) {
				break;
			} else {
				Thread.sleep(100);
			}
		}
		ItemValue<MailboxFolder> inbox = provider().instance(IMailboxFolders.class, partition.name, mboxRoot)
				.byName("INBOX");
		assertNotNull("Unable to retrieve INBOX", inbox);

		this.createdMails = Stream.of(createEmail(user1Login, "INBOX", testEml01()),
				createEmail(user1Login, "INBOX", testEml01()), createEmail(user1Login, "INBOX", testEml01()),
				createEmail(user1Login, "INBOX", testEml01()), createEmail(user1Login, "INBOX", testEml02()))
				.collect(Collectors.toList());
	}

	@After
	public void after() throws Exception {
		Thread.sleep(3000);

		cyrusReplication.stopReplication().get(5, TimeUnit.SECONDS);
		JdbcTestHelper.getInstance().afterTest();
	}

	protected IServiceProvider provider() {
		return ServerSideServiceProvider.getProvider(secCtxUser1);
	}

	protected IServiceProvider providerSystem() {
		return ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
	}

	protected InputStream testEml01() {
		return withRandomMessageId("with_inlines_01.ftl");
	}

	protected InputStream testEml02() {
		return withRandomMessageId("with_inlines_02.ftl");
	}

	private static InputStream withRandomMessageId(String tplName) {
		Configuration fmCfg = new Configuration(Configuration.VERSION_2_3_30);
		fmCfg.setClassForTemplateLoading(Pop3Tests.class, "/data");
		fmCfg.setTagSyntax(Configuration.AUTO_DETECT_TAG_SYNTAX);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.US_ASCII);
		try {
			Template tpl = fmCfg.getTemplate(tplName, "US-ASCII");
			tpl.process(ImmutableMap.of("randomUUID", UUID.randomUUID().toString()), writer);
			writer.close();
			return new ByteArrayInputStream(out.toByteArray());
		} catch (Exception e) {
			logger.warn(e.getMessage());
		}
		return null;
	}

	@Test
	public void testDeleCommand() throws Exception {
		ItemValue<MailboxFolder> inbox = provider().instance(IMailboxFolders.class, partition.name, mboxRoot)
				.byName("INBOX");
		Assert.assertNotNull(inbox);

		try (Socket sock = new Socket()) {
			ConcurrentLinkedDeque<String> queue = rawSocket(sock);

			OutputStream out = sock.getOutputStream();
			out.write(("USER " + user1Login + "@" + domainUid + "\r\n").getBytes());
			out.write(("PASS " + user1Login + "\r\n").getBytes());
			out.flush();

			Awaitility.await().atMost(4, TimeUnit.SECONDS)
					.until(() -> (testConditionForQueue(queue, 1, "^\\+OK POP3 ready$")));

			Awaitility.await().atMost(4, TimeUnit.SECONDS).until(() -> (testConditionForQueue(queue, 2, "^\\+OK$")));

			out.write(("DELE 1\r\n").getBytes());
			out.flush();

			Awaitility.await().atMost(4, TimeUnit.SECONDS)
					.until(() -> (testConditionForQueue(queue, 1, "^\\+OK message 1 deleted$")));

			IDbMailboxRecords recApi = provider().instance(IDbMailboxRecords.class, inbox.uid);
			Count countBeforeDeletion = recApi.count(ItemFlagFilter.create().mustNot(ItemFlag.Deleted));

			Assert.assertEquals(countBeforeDeletion.total, createdMails.size());
			out.write(("QUIT\r\n").getBytes());
			out.flush();

			Thread.sleep(1000);
			Count countAfterDeletion = recApi.count(ItemFlagFilter.create().mustNot(ItemFlag.Deleted));
			Assert.assertEquals(countAfterDeletion.total, createdMails.size() - 1);
			queue.clear();

		}
	}

	@Test
	public void testDeleCommandWithNoArguments() throws Exception {
		ItemValue<MailboxFolder> inbox = provider().instance(IMailboxFolders.class, partition.name, mboxRoot)
				.byName("INBOX");
		Assert.assertNotNull(inbox);

		try (Socket sock = new Socket()) {
			ConcurrentLinkedDeque<String> queue = rawSocket(sock);

			OutputStream out = sock.getOutputStream();
			out.write(("USER " + user1Login + "@" + domainUid + "\r\n").getBytes());
			out.write(("PASS " + user1Login + "\r\n").getBytes());
			out.flush();

			Awaitility.await().atMost(4, TimeUnit.SECONDS)
					.until(() -> (testConditionForQueue(queue, 1, "^\\+OK POP3 ready$")));

			Awaitility.await().atMost(4, TimeUnit.SECONDS).until(() -> (testConditionForQueue(queue, 2, "^\\+OK$")));

			out.write(("DELE\r\n").getBytes());
			out.flush();

			Awaitility.await().atMost(4, TimeUnit.SECONDS)
					.until(() -> (testConditionForQueue(queue, 1, "-ERR no such message")));
			queue.clear();

		}
	}

	@Test
	public void testDeleCommandWithIdOutOfRange() throws Exception {
		ItemValue<MailboxFolder> inbox = provider().instance(IMailboxFolders.class, partition.name, mboxRoot)
				.byName("INBOX");
		Assert.assertNotNull(inbox);

		try (Socket sock = new Socket()) {
			ConcurrentLinkedDeque<String> queue = rawSocket(sock);

			OutputStream out = sock.getOutputStream();
			out.write(("USER " + user1Login + "@" + domainUid + "\r\n").getBytes());
			out.write(("PASS " + user1Login + "\r\n").getBytes());
			out.flush();

			Awaitility.await().atMost(4, TimeUnit.SECONDS)
					.until(() -> (testConditionForQueue(queue, 1, "^\\+OK POP3 ready$")));

			Awaitility.await().atMost(4, TimeUnit.SECONDS).until(() -> (testConditionForQueue(queue, 2, "^\\+OK$")));

			out.write(("DELE 100\r\n").getBytes());
			out.flush();

			Awaitility.await().atMost(4, TimeUnit.SECONDS)
					.until(() -> (testConditionForQueue(queue, 1, "^-ERR no such message$")));
			queue.clear();

		}
	}

	@Test
	public void testDeleCommandWithINotAnInteger() throws Exception {
		ItemValue<MailboxFolder> inbox = provider().instance(IMailboxFolders.class, partition.name, mboxRoot)
				.byName("INBOX");
		Assert.assertNotNull(inbox);

		try (Socket sock = new Socket()) {
			ConcurrentLinkedDeque<String> queue = rawSocket(sock);

			OutputStream out = sock.getOutputStream();
			out.write(("USER " + user1Login + "@" + domainUid + "\r\n").getBytes());
			out.write(("PASS " + user1Login + "\r\n").getBytes());
			out.flush();

			Awaitility.await().atMost(4, TimeUnit.SECONDS)
					.until(() -> (testConditionForQueue(queue, 1, "^\\+OK POP3 ready$")));

			Awaitility.await().atMost(4, TimeUnit.SECONDS).until(() -> (testConditionForQueue(queue, 2, "^\\+OK$")));

			out.write(("DELE test\r\n").getBytes());
			out.flush();

			Awaitility.await().atMost(4, TimeUnit.SECONDS)
					.until(() -> (testConditionForQueue(queue, 1, "^-ERR no such message$")));
			queue.clear();

		}
	}

	@Test
	public void testRsetCommand() throws Exception {
		ItemValue<MailboxFolder> inbox = provider().instance(IMailboxFolders.class, partition.name, mboxRoot)
				.byName("INBOX");
		Assert.assertNotNull(inbox);

		try (Socket sock = new Socket()) {
			ConcurrentLinkedDeque<String> queue = rawSocket(sock);

			OutputStream out = sock.getOutputStream();
			out.write(("USER " + user1Login + "@" + domainUid + "\r\n").getBytes());
			out.write(("PASS " + user1Login + "\r\n").getBytes());
			out.flush();

			Awaitility.await().atMost(4, TimeUnit.SECONDS)
					.until(() -> (testConditionForQueue(queue, 1, "^\\+OK POP3 ready$")));

			Awaitility.await().atMost(4, TimeUnit.SECONDS).until(() -> (testConditionForQueue(queue, 2, "^\\+OK$")));

			out.write(("RSET 1\r\n").getBytes());
			out.flush();

			Awaitility.await().atMost(4, TimeUnit.SECONDS).until(() -> (testConditionForQueue(queue, 3, "^\\+OK$")));
			queue.clear();
		}
	}

	@Test
	public void testRsetCommandWithoutArgument() throws Exception {
		ItemValue<MailboxFolder> inbox = provider().instance(IMailboxFolders.class, partition.name, mboxRoot)
				.byName("INBOX");
		Assert.assertNotNull(inbox);

		try (Socket sock = new Socket()) {
			ConcurrentLinkedDeque<String> queue = rawSocket(sock);

			OutputStream out = sock.getOutputStream();
			out.write(("USER " + user1Login + "@" + domainUid + "\r\n").getBytes());
			out.write(("PASS " + user1Login + "\r\n").getBytes());
			out.flush();

			Awaitility.await().atMost(4, TimeUnit.SECONDS)
					.until(() -> (testConditionForQueue(queue, 1, "^\\+OK POP3 ready$")));

			Awaitility.await().atMost(4, TimeUnit.SECONDS).until(() -> (testConditionForQueue(queue, 2, "^\\+OK$")));

			out.write(("RSET\r\n").getBytes());
			out.flush();

			Awaitility.await().atMost(4, TimeUnit.SECONDS)
					.until(() -> (testConditionForQueue(queue, 1, "^-ERR invalid command$")));
			queue.clear();
		}
	}

	public ItemValue<MailboxItem> createEmail(String userUid, String folderName, InputStream is) throws IOException {
		IServiceProvider prov = provider();
		IMailboxFolders mboxesApi = prov.instance(IMailboxFolders.class, domainUid, "user." + userUid);
		ItemValue<MailboxFolder> inbox = mboxesApi.byName(folderName);

		IMailboxItems recApi = prov.instance(IMailboxItems.class, inbox.uid);
		MailboxItem item = createMailboxItemFromResource(recApi, is);
		ItemIdentifier created = recApi.create(item);
		recApi.removePart(item.body.structure.address);
		return recApi.getCompleteById(created.id);
	}

	private MailboxItem createMailboxItemFromResource(IMailboxItems recApi, InputStream is) throws IOException {
		byte[] eml;
		eml = ByteStreams.toByteArray(is);
		eml = new String(eml).replace("\r", "").replace("\n", "\r\n").getBytes();

		String partId = recApi.uploadPart(VertxStream.stream(Buffer.buffer(eml)));
		MailboxItem mi = new MailboxItem();
		MessageBody mb = new MessageBody();
		Part part = new Part();
		part.mime = "message/rfc822";
		part.address = partId;
		mb.structure = part;
		mi.body = mb;
		return mi;
	}

	private ConcurrentLinkedDeque<String> rawSocket(Socket sock) throws IOException {

		Config conf = Pop3Config.get();
		int port = conf.getInt("pop3.port");

		sock.connect(new InetSocketAddress("127.0.0.1", port));
		ConcurrentLinkedDeque<String> queue = new ConcurrentLinkedDeque<>();
		Thread t = new Thread(() -> {
			try {
				InputStream in = sock.getInputStream();
				byte[] buf = new byte[1024];
				while (true) {
					int read = in.read(buf, 0, 1024);
					if (read == -1) {
						break;
					}
					String resp = new String(buf, 0, read);
					logger.debug("QUEUE OFFER: " + resp);
					queue.offer(resp);
				}
			} catch (Exception e) {
			}
		});
		t.start();
		return queue;
	}

	private Boolean testConditionForQueue(ConcurrentLinkedDeque<String> q, Integer condition, String regex) {
		Stream<String> list = Arrays.asList(q.stream().reduce((e, s) -> e + s).get().split("\r\n")).stream()
				.filter(s -> s.matches(regex));
		return list.count() == condition;
	}

}
