/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
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
package net.bluemind.backend.cyrus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.mail.internet.MimeMessage;

import org.apache.jsieve.CommandManager;
import org.apache.jsieve.CommandManagerImpl;
import org.apache.jsieve.ConfigurationManager;
import org.apache.jsieve.SieveFactory;
import org.apache.jsieve.TestManager;
import org.apache.jsieve.mail.ActionFileInto;
import org.apache.jsieve.mail.ActionKeep;
import org.apache.jsieve.mail.ActionRedirect;
import org.apache.jsieve.util.check.ScriptChecker.Results;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.io.Files;

import freemarker.template.TemplateException;
import net.bluemind.backend.cyrus.internal.ScriptCheckMailAdapter;
import net.bluemind.backend.cyrus.internal.SieveWriter;
import net.bluemind.backend.cyrus.internal.SieveWriter.Type;
import net.bluemind.backend.cyrus.utils.Copy;
import net.bluemind.backend.cyrus.utils.Discard;
import net.bluemind.backend.cyrus.utils.DiscardAction;
import net.bluemind.backend.cyrus.utils.ImapFlags;
import net.bluemind.backend.cyrus.utils.Include;
import net.bluemind.backend.cyrus.utils.Redirect;
import net.bluemind.config.Token;
import net.bluemind.core.api.Email;
import net.bluemind.core.api.fault.ServerFault;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.domain.api.Domain;
import net.bluemind.imap.AnnotationList;
import net.bluemind.imap.StoreClient;
import net.bluemind.imap.sieve.SieveClient;
import net.bluemind.imap.sieve.SieveClient.SieveConnectionData;
import net.bluemind.imap.sieve.SieveScript;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.MailFilter;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.node.api.NodeActivator;
import net.bluemind.pool.impl.BmConfIni;
import net.bluemind.server.api.Server;
import net.bluemind.tests.defaultdata.PopulateHelper;

public class SieveWriterTests {

	private ItemValue<Mailbox> mbox;

	private SieveWriter writer;

	private byte[] mail;

	private Server imapServer;
	private CyrusServiceForTests service;

	@Before
	public void setup() throws Exception {
		JdbcTestHelper.getInstance().beforeTest();

		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());

		VertxPlatform.spawnBlocking(30, TimeUnit.SECONDS);

		String imapServerAddress = new BmConfIni().get("imap-role");
		assertNotNull(imapServerAddress);
		imapServer = new Server();
		imapServer.ip = imapServerAddress;
		imapServer.tags = Lists.newArrayList("mail/imap");

		PopulateHelper.initGlobalVirt(imapServer);
		service = new CyrusServiceForTests(imapServerAddress);

		mail = Files.toByteArray(new File("data/test.eml"));
		mbox = new ItemValue<>();
		mbox.uid = "checkThat";
		mbox.value = new Mailbox();
		mbox.value.emails = Arrays.asList(Email.create("test@test.com", true));

		writer = new SieveWriter();
	}

	@Test
	public void write_sieveMailshare() throws Exception {
		String domainUid = "sieve-mailshare-" + System.currentTimeMillis() + ".tld";
		Domain d = Domain.create(domainUid, domainUid, domainUid, Collections.emptySet());
		PopulateHelper.createTestDomain(domainUid, d, imapServer);

		String mailboxUid = UUID.randomUUID().toString();
		Mailbox m = new Mailbox();
		m.name = "mailshare-" + System.currentTimeMillis();
		m.type = net.bluemind.mailbox.api.Mailbox.Type.mailshare;
		m.dataLocation = imapServer.ip;
		m.routing = Routing.internal;
		m.emails = Arrays.asList(Email.create(m.name + "@" + domainUid, true));

		String admins = "admins: admin0 bmhiddensysadmin@" + domainUid;
		NodeActivator.get(imapServer.ip).writeFile("/etc/cyrus-admins", new ByteArrayInputStream(admins.getBytes()));

		service.createPartition(domainUid);
		service.refreshPartitions(Arrays.asList(domainUid));
		service.reload();
		service.createBox(m.name + "@" + domainUid, domainUid);

		writer.write(ItemValue.create(mailboxUid, m), null, ItemValue.create(domainUid, d), MailFilter.create());

		try (StoreClient storeClient = new StoreClient(imapServer.ip, 1143, "admin0", Token.admin0())) {
			assertTrue(storeClient.login());

			AnnotationList annotations = storeClient.getAnnotation(m.name + "@" + domainUid);
			assertNotNull(annotations);
			assertFalse(annotations.isEmpty());

			assertTrue(annotations.containsKey("/vendor/cmu/cyrus-imapd/sieve"));
			assertNull(annotations.get("/vendor/cmu/cyrus-imapd/sieve").valuePriv);
			assertEquals(mailboxUid + ".sieve", annotations.get("/vendor/cmu/cyrus-imapd/sieve").valueShared);
		}
	}

	@Test
	public void testVacation() throws Exception {

		MailFilter filter = new MailFilter();
		filter.vacation = new MailFilter.Vacation();
		filter.vacation.enabled = true;
		Calendar c = Calendar.getInstance();
		c.add(Calendar.DAY_OF_YEAR, -1);
		filter.vacation.start = new Date(c.getTimeInMillis());

		c.add(Calendar.DAY_OF_YEAR, 3);
		filter.vacation.end = new Date(c.getTimeInMillis());
		filter.vacation.subject = "blabla";
		filter.vacation.text = "blabla\n jfgkj \nsfdkjfdk";
		mbox.value.emails = Arrays.asList(Email.create("test@test.com", true), Email.create("toto@test.com", false),
				Email.create("alias@test.com", false, true));

		ItemValue<Domain> domain = getTestDomain();
		domain.value.aliases = new HashSet<>(Arrays.asList("test.fr", "test.net"));
		String sieveScript = writer.generateSieveScript(Type.USER, mbox, "Mr Test", domain, filter);
		Results r = check(sieveScript);
		assertActionKeep(r);

		String sub = sieveScript.substring(sieveScript.indexOf(":addresses"));
		String[] addresses = sub.substring(sub.indexOf("[") + 1, sub.indexOf("]")).replace("\"", "").split(",");
		assertEquals(5, addresses.length);
		assertEquals(0, Sets.difference(new HashSet<>(
				Arrays.asList("test@test.com", "toto@test.com", "alias@test.com", "alias@test.fr", "alias@test.net")),
				new HashSet<>(Arrays.asList(addresses))).size());
	}

	private ItemValue<Domain> getTestDomain() {
		return ItemValue.create("test.com", Domain.create("test.com", "test.com", "test.com", Collections.emptySet()));
	}

	@Test
	public void testForward() throws Exception {

		MailFilter filter = new MailFilter();
		filter.forwarding = new MailFilter.Forwarding();
		filter.forwarding.enabled = true;
		filter.forwarding.emails = new HashSet<>(Arrays.asList("test@toto.com"));

		mbox.value.emails = Arrays.asList(Email.create("test@test.com", true));

		System.out.println(writer.generateSieveScript(Type.USER, mbox, null, getTestDomain(), filter));
	}

	@Test
	public void testBreakOnFirstMatch() throws Exception {
		MailFilter.Rule rule = new MailFilter.Rule();
		rule.active = true;
		rule.criteria = "FROM:IS: sid@pinkfloyd.net";
		rule.deliver = "test";

		// This rule will match and use a redirect action
		MailFilter.Rule rule2 = new MailFilter.Rule();
		rule2.active = true;
		rule2.criteria = "SUBJECT:IS: SubjectTest";
		rule2.forward.emails.add("toto@gmail.com");

		MailFilter.Rule rule3 = new MailFilter.Rule();
		rule3.active = true;
		rule3.criteria = "FROM:IS: roger.water@pinkfloyd.net";
		rule3.deliver = "toto";

		Results r = check(writer.generateSieveScript(Type.USER, mbox, null, getTestDomain(),
				MailFilter.create(rule, rule2, rule3)));
		assertAction(ActionRedirect.class, r);

	}

	@Test
	public void testContinueAfterMatch() throws Exception {
		MailFilter.Rule rule = new MailFilter.Rule();
		rule.active = true;
		rule.criteria = "FROM:IS: sid@pinkfloyd.net";
		rule.discard = true;

		// This rule will match and use a redirect action
		MailFilter.Rule rule2 = new MailFilter.Rule();
		rule2.active = true;
		rule2.stop = false;
		rule2.criteria = "SUBJECT:IS: SubjectTest";
		rule2.forward.emails.add("toto@gmail.com");

		// This rule will match and use a fileinto action
		MailFilter.Rule rule3 = new MailFilter.Rule();
		rule3.active = true;
		rule3.criteria = "FROM:IS: roger.water@pinkfloyd.net";
		rule3.deliver = "test";

		Results r = check(writer.generateSieveScript(Type.USER, mbox, null, getTestDomain(),
				MailFilter.create(rule, rule2, rule3)));
		assertAction(Arrays.<Class<?>>asList(ActionRedirect.class, ActionFileInto.class), r);

	}

	@Test
	public void testFrom() throws Exception {
		MailFilter.Rule rule = new MailFilter.Rule();
		rule.active = true;
		rule.criteria = "FROM:IS: roger.water@pinkfloyd.net";
		rule.deliver = "test";
		Results r = check(writer.generateSieveScript(Type.USER, mbox, null, getTestDomain(), MailFilter.create(rule)));
		assertAction(ActionFileInto.class, r);

		rule.criteria = "FROM:IS: sid.barrett@pinkfloyd.net";
		r = check(writer.generateSieveScript(Type.USER, mbox, null, getTestDomain(), MailFilter.create(rule)));
		assertActionKeep(r);
	}

	@Test
	public void testSubject() throws Exception {

		MailFilter.Rule rule = new MailFilter.Rule();
		rule.active = true;
		rule.criteria = "SUBJECT:IS: SubjectTest";
		rule.deliver = "test";

		Results r = check(writer.generateSieveScript(Type.USER, mbox, null, getTestDomain(), MailFilter.create(rule)));

		assertAction(ActionFileInto.class, r);

		rule.criteria = "SUBJECT:IS: FalseSub";
		r = check(writer.generateSieveScript(Type.USER, mbox, null, getTestDomain(), MailFilter.create(rule)));
		assertActionKeep(r);

	}

	@Test
	public void testMatchAll() throws IOException, TemplateException, ServerFault, Exception {
		MailFilter.Rule rule = new MailFilter.Rule();
		rule.active = true;
		rule.criteria = "MATCHALL";
		rule.discard = true;

		Results r = check(writer.generateSieveScript(Type.USER, mbox, null, getTestDomain(), MailFilter.create(rule)));

		assertAction(DiscardAction.class, r);
	}

	@Test
	public void testPutScript() throws Exception {
		String name = "test." + System.currentTimeMillis() + ".sieve";
		final String content = "" // test script
				+ "require [ \"fileinto\", \"imapflags\", "
				// +"\"body\", " // cyrus 2.3 extensions ?!
				+ "\"vacation\" ];\n" // extensions
				// +"if body :text :contains \"viagra\"{\n discard;\n}\n"
				+ "if size :over 500K {\n   setflag \"\\\\Flagged\";\n}\n" + "fileinto \"INBOX\";\n";
		InputStream contentStream = new ByteArrayInputStream(content.getBytes());

		SieveConnectionData connectionData = new SieveConnectionData("admin0", Token.admin0(),
				new BmConfIni().get("imap-role"));
		try (SieveClient sc = new SieveClient(connectionData)) {
			assertTrue(sc.login());

			boolean res = sc.putscript(name, contentStream);
			assertTrue(res);

			List<SieveScript> list = sc.listscripts();
			assertTrue(list.size() > 0);

			boolean found = false;
			for (SieveScript script : list) {
				if (script.getName().equals(name)) {
					found = true;
					String v = sc.getScript(script.getName());
					assertEquals(content, v.replace("\r", ""));
				}
			}

			assertTrue(found);
		}

	}

	@Test
	public void testPutVacation() throws Exception {

		MailFilter filter = new MailFilter();
		filter.vacation = new MailFilter.Vacation();
		filter.vacation.enabled = true;
		Calendar c = Calendar.getInstance();
		c.add(Calendar.DAY_OF_YEAR, -1);
		filter.vacation.start = new Date(c.getTimeInMillis());

		c.add(Calendar.DAY_OF_YEAR, 3);
		filter.vacation.end = new Date(c.getTimeInMillis());
		filter.vacation.subject = "blabla";
		filter.vacation.textHtml = "<html><body><b>test content<b></body></html>";
		filter.vacation.text = "test content";
		mbox.value.emails = Arrays.asList(Email.create("test@test.com", true), Email.create("toto@test.com", false),
				Email.create("alias@test.com", false, true));

		ItemValue<Domain> domain = getTestDomain();
		domain.value.aliases = new HashSet<>(Arrays.asList("test.fr", "test.net"));
		String sieveName = mbox.uid + ".sieve";
		String sieveScriptContent = writer.generateSieveScript(Type.USER, mbox, "Test", domain, filter);

		InputStream contentStream = new ByteArrayInputStream(sieveScriptContent.getBytes());

		SieveConnectionData connectionData = new SieveConnectionData("admin0", Token.admin0(),
				new BmConfIni().get("imap-role"));
		try (SieveClient sc = new SieveClient(connectionData)) {
			assertTrue(sc.login());

			boolean res = sc.putscript(sieveName, contentStream);
			assertTrue(res);

			Optional<String> maybeSieveContent = sc.listscripts().stream()
					.filter(script -> script.getName().equals(sieveName))
					.map(sieveScript -> sc.getScript(sieveScript.getName())).findFirst();

			assertTrue(maybeSieveContent.isPresent());

			maybeSieveContent.ifPresent(
					savedContent -> assertEquals(sieveScriptContent.replace("\r", ""), savedContent.replace("\r", "")));
		}
	}

	@Test
	public void testFailPutScript() {
		String name = "test." + System.currentTimeMillis() + ".sieve";
		final String content = "bang";
		InputStream contentStream = new ByteArrayInputStream(content.getBytes());

		SieveConnectionData connectionData = new SieveConnectionData("admin0", Token.admin0(),
				new BmConfIni().get("imap-role"));
		try (SieveClient sc = new SieveClient(connectionData)) {
			assertTrue(sc.login());

			boolean res = sc.putscript(name, contentStream);
			assertFalse(res);
		}

	}

	private void assertActionKeep(Results results) {
		assertAction(Arrays.<Class<?>>asList(ActionKeep.class), results);
	}

	private void assertAction(Class<?> klass, Results results) {
		assertAction(Arrays.<Class<?>>asList(klass), results);
	}

	private void assertAction(List<Class<?>> classes, Results results) {
		Assert.assertEquals(classes.size(), results.getActionsExecuted().size());
		for (Object o : results.getActionsExecuted()) {
			boolean ok = false;
			for (Class<?> k : classes) {
				if (k == o.getClass()) {
					ok = true;
					break;
				}
			}
			Assert.assertTrue("l'action " + o.getClass().getSimpleName() + " n'est pas attendu", ok);
		}
	}

	private Results check(String script) throws Exception {
		System.out.println(script);

		// on ajout les commandes et test manquant
		ConfigurationManager confM = new ConfigurationManager() {

			@Override
			public CommandManager getCommandManager() {
				getCommandMap().put("include", Include.class.getName());
				getCommandMap().put("copy", Copy.class.getName());
				getCommandMap().put("redirect", Redirect.class.getName());
				getCommandMap().put("vacation", net.bluemind.backend.cyrus.utils.Vacation.class.getName());
				getCommandMap().put("discard", Discard.class.getName());
				return new CommandManagerImpl(getCommandMap());

			}

			@Override
			public TestManager getTestManager() {
				getTestMap().put("imapflags", ImapFlags.class.getName());
				return super.getTestManager();
			}

		};
		SieveFactory sieveFactory = confM.build();

		ScriptCheckMailAdapter adapter = new ScriptCheckMailAdapter();
		MimeMessage mimeMessage = new MimeMessage(null, new ByteArrayInputStream(mail));
		adapter.setMail(mimeMessage);
		sieveFactory.interpret(adapter, new ByteArrayInputStream(script.getBytes()));
		return new Results(adapter.getExecutedActions());
	}

	@Test
	public void nullOrEmptyForward() throws Exception {
		MailFilter.Rule rule = new MailFilter.Rule();
		rule.active = true;
		rule.criteria = "FROM:IS: sid@pinkfloyd.net";
		rule.deliver = "test";
		assertFalse(writer.generateSieveScript(Type.USER, mbox, null, getTestDomain(), MailFilter.create(rule))
				.contains("redirect"));

		assertFalse(writer.generateSieveScript(Type.SHARED, mbox, null, getTestDomain(), MailFilter.create(rule))
				.contains("redirect"));

		assertFalse(writer.generateSieveScript(Type.DOMAIN, mbox, null, getTestDomain(), MailFilter.create(rule))
				.contains("redirect"));
	}

	@Test
	public void testDeliver() throws Exception {
		MailFilter.Rule rule = new MailFilter.Rule();
		rule.active = true;
		rule.criteria = "FROM:IS: david.gilmour@pinkfloyd.net";
		rule.deliver = "test";
		assertTrue(writer.generateSieveScript(Type.USER, mbox, null, getTestDomain(), MailFilter.create(rule))
				.contains("fileinto \"test\""));

		rule.deliver = null;
		assertFalse(writer.generateSieveScript(Type.USER, mbox, null, getTestDomain(), MailFilter.create(rule))
				.contains("fileinto \"\""));

		rule.deliver = "";
		assertFalse(writer.generateSieveScript(Type.USER, mbox, null, getTestDomain(), MailFilter.create(rule))
				.contains("fileinto \"\""));

		rule.deliver = "   ";
		assertFalse(writer.generateSieveScript(Type.USER, mbox, null, getTestDomain(), MailFilter.create(rule))
				.contains("fileinto \"\""));

	}

	@Test
	public void testMailForwardLocalCopy() throws Exception {
		MailFilter.Rule rule = new MailFilter.Rule();
		rule.active = true;
		rule.criteria = "SUBJECT:IS: fwd";
		rule.forward.localCopy = true;
		rule.forward.emails.add("fwd@bm.com");

		String s = writer.generateSieveScript(Type.USER, mbox, null, getTestDomain(), MailFilter.create(rule));
		assertTrue(s.contains("redirect :copy \"fwd@bm.com\";"));

		rule.forward.localCopy = false;
		s = writer.generateSieveScript(Type.USER, mbox, null, getTestDomain(), MailFilter.create(rule));
		assertTrue(s.contains("redirect  \"fwd@bm.com\";"));
	}

	@Test
	public void testActivateScriptDoesNotExist() {
		SieveConnectionData connectionData = new SieveConnectionData("admin0", Token.admin0(),
				new BmConfIni().get("imap-role"));
		try (SieveClient sc = new SieveClient(connectionData)) {
			assertTrue(sc.login());
			boolean res = sc.activate("blabla.sieve");
			assertFalse(res);
		}
	}

	@Test
	public void testActivate() {

		String name = "test." + System.currentTimeMillis() + ".sieve";
		final String content = "" // test script
				+ "require [ \"fileinto\", \"imapflags\", "
				// +"\"body\", " // cyrus 2.3 extensions ?!
				+ "\"vacation\" ];\n" // extensions
				// +"if body :text :contains \"viagra\"{\n discard;\n}\n"
				+ "if size :over 500K {\n   setflag \"\\\\Flagged\";\n}\n" + "fileinto \"INBOX\";\n";
		InputStream contentStream = new ByteArrayInputStream(content.getBytes());

		SieveConnectionData connectionData = new SieveConnectionData("admin0", Token.admin0(),
				new BmConfIni().get("imap-role"));
		try (SieveClient sc = new SieveClient(connectionData)) {
			assertTrue(sc.login());

			boolean res = sc.putscript(name, contentStream);
			assertTrue(res);

			res = sc.activate(name);
			assertTrue(res);
		}
	}
}
