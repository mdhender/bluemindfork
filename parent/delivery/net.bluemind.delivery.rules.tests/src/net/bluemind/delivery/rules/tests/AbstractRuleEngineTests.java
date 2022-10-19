package net.bluemind.delivery.rules.tests;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.apache.james.mime4j.dom.Entity;
import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.TextBody;
import org.junit.Before;

import net.bluemind.backend.mail.replica.api.IDbByContainerReplicatedMailboxes;
import net.bluemind.backend.mail.replica.api.IDbReplicatedMailboxes;
import net.bluemind.backend.mail.replica.api.IMailReplicaUids;
import net.bluemind.backend.mail.replica.api.MailboxRecord;
import net.bluemind.backend.mail.replica.api.MailboxReplica;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.context.SecurityContext;
import net.bluemind.core.jdbc.JdbcActivator;
import net.bluemind.core.jdbc.JdbcTestHelper;
import net.bluemind.core.rest.IServiceProvider;
import net.bluemind.core.rest.ServerSideServiceProvider;
import net.bluemind.core.sendmail.testhelper.FakeSendmail;
import net.bluemind.delivery.lmtp.MailboxLookup;
import net.bluemind.delivery.lmtp.common.DeliveryContent;
import net.bluemind.delivery.lmtp.common.ResolvedBox;
import net.bluemind.delivery.rules.MailboxVacationSendersCache;
import net.bluemind.delivery.rules.RuleEngine;
import net.bluemind.lib.vertx.VertxPlatform;
import net.bluemind.mailbox.api.IMailboxes;
import net.bluemind.mailbox.api.Mailbox;
import net.bluemind.mailbox.api.Mailbox.Routing;
import net.bluemind.mailbox.api.rules.MailFilterRule;
import net.bluemind.mailbox.api.rules.conditions.MailFilterRuleCondition;
import net.bluemind.mailbox.api.rules.conditions.MailFilterRuleKnownField;
import net.bluemind.server.api.Server;
import net.bluemind.tests.defaultdata.PopulateHelper;
import net.bluemind.utils.FileUtils;

public class AbstractRuleEngineTests {

	protected IServiceProvider provider;
	protected MailboxLookup lookup;
	protected FakeSendmail mailer;

	protected ItemValue<Mailbox> mboxUser1;
	protected String emailUser1;
	protected ResolvedBox boxUser1;
	protected ItemValue<MailboxReplica> rootFolderUser1;

	protected ItemValue<Mailbox> mboxUser2;
	protected String emailUser2;

	@Before
	public void before() throws Exception {
		var domainUid = "test" + System.currentTimeMillis() + ".lab";
		JdbcTestHelper.getInstance().beforeTest();
		JdbcActivator.getInstance().setDataSource(JdbcTestHelper.getInstance().getDataSource());

		var pipo = new Server();
		pipo.ip = PopulateHelper.FAKE_CYRUS_IP;
		pipo.tags = Collections.singletonList("mail/imap");

		VertxPlatform.spawnBlocking(25, TimeUnit.SECONDS);

		PopulateHelper.initGlobalVirt(pipo);
		PopulateHelper.addDomain(domainUid, Routing.none);

		this.provider = ServerSideServiceProvider.getProvider(SecurityContext.SYSTEM);
		this.lookup = new MailboxLookup(key -> this.provider);
		this.mailer = new FakeSendmail();

		var user1Uid = PopulateHelper.addUser("user1", domainUid, Routing.internal);
		this.mboxUser1 = provider.instance(IMailboxes.class, domainUid).getComplete(user1Uid);
		this.emailUser1 = mboxUser1.value.defaultEmail().address;
		this.boxUser1 = lookup.lookupEmail(mboxUser1.value.defaultEmail().address);
		var subtree = IMailReplicaUids.subtreeUid(boxUser1.dom.uid, boxUser1.mbox);
		var treeApi = provider.instance(IDbByContainerReplicatedMailboxes.class, subtree);
		this.rootFolderUser1 = treeApi.byReplicaName("INBOX");

		String user2Uid = PopulateHelper.addUser("user2", domainUid, Routing.internal);
		this.mboxUser2 = provider.instance(IMailboxes.class, domainUid).getComplete(user2Uid);
		this.emailUser2 = mboxUser2.value.defaultEmail().address;
	}

	protected RuleEngine engineOn(Message message) {
		var mailboxRecord = new MailboxRecord();
		mailboxRecord.messageBody = "42";
		String from = (message.getFrom() != null)
				? message.getFrom().stream().findFirst().map(m -> m.getAddress()).orElse(null)
				: null;
		DeliveryContent content = new DeliveryContent(from, boxUser1, rootFolderUser1, message, mailboxRecord);
		MailboxVacationSendersCache.Factory vacationCacheFactory = MailboxVacationSendersCache.Factory.build("/tmp");
		return new RuleEngine(provider, mailer, content, vacationCacheFactory);
	}

	protected List<MailFilterRule> rulesMatchingSubjectOf(Message message, Consumer<MailFilterRule>... actionSetters) {
		return Arrays.stream(actionSetters).map(actionSetter -> {
			var rule = new MailFilterRule();
			rule.stop = false;
			rule.conditions
					.add(MailFilterRuleCondition.equal(MailFilterRuleKnownField.SUBJECT.text(), message.getSubject()));
			actionSetter.accept(rule);
			return rule;
		}).toList();
	}

	protected ItemValue<MailboxReplica> folder(ResolvedBox box, String name) {
		String subtree = IMailReplicaUids.subtreeUid(box.dom.uid, box.mbox);
		IDbReplicatedMailboxes treeApi = provider.instance(IDbByContainerReplicatedMailboxes.class, subtree);
		return treeApi.byReplicaName(name);
	}

	protected String extractContent(Entity entity) {
		TextBody body = (TextBody) entity.getBody();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			FileUtils.transfer(body.getInputStream(), out, true);
			String charset = entity.getCharset();
			return new String(out.toByteArray(), charset != null ? charset : "utf-8");
		} catch (IOException ioe) {
			return null;
		}
	}
}
