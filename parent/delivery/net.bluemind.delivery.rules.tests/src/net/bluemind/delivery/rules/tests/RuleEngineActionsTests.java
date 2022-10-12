package net.bluemind.delivery.rules.tests;

import static net.bluemind.backend.mail.api.flags.MailboxItemFlag.System.Deleted;
import static net.bluemind.backend.mail.api.flags.MailboxItemFlag.System.Flagged;
import static net.bluemind.backend.mail.api.flags.MailboxItemFlag.System.Seen;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.james.mime4j.dom.Entity;
import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.Multipart;
import org.junit.Test;

import net.bluemind.backend.mail.replica.api.IDbMailboxRecords;
import net.bluemind.backend.mail.replica.api.MailboxRecord;
import net.bluemind.backend.mail.replica.api.MailboxReplica;
import net.bluemind.core.container.model.ItemValue;
import net.bluemind.core.sendmail.testhelper.TestMail;
import net.bluemind.delivery.lmtp.common.DeliveryContent;
import net.bluemind.mailbox.api.rules.actions.MailFilterRuleActionFollowUp.DueDate;
import net.bluemind.mailbox.api.rules.actions.MailFilterRuleActionFollowUp.FollowUpAction;
import net.bluemind.mime4j.common.AddressableEntity;
import net.bluemind.mime4j.common.Mime4JHelper;

public class RuleEngineActionsTests extends AbstractRuleEngineTests {

	@Test
	public void testAddHeader() {
		var message = new MessageBuilder("Subject").build();
		var engine = engineOn(message);
		var rules = rulesMatchingSubjectOf(message, rule -> rule.addAddHeader("X-HEADER", "header-value"));

		DeliveryContent result = engine.apply(rules);

		assertNotNull(result.message().getHeader().getField("X-HEADER"));
		assertEquals("header-value", result.message().getHeader().getField("X-HEADER").getBody());
	}

	@Test
	public void testCategorize() {
		var message = new MessageBuilder("Subject").build();
		var engine = engineOn(message);
		var rules = rulesMatchingSubjectOf(message, rule -> rule.addCategorize(Arrays.asList("one", "two")));

		DeliveryContent result = engine.apply(rules);

		assertNotNull(result.message().getHeader().getField("X-Bm-Otlk-Name-Keywords"));
		assertEquals("one,two", result.message().getHeader().getField("X-Bm-Otlk-Name-Keywords").getBody());
	}

	@Test
	public void testCopy() {
		var message = new MessageBuilder("Subject").build();
		var engine = engineOn(message);
		var rules = rulesMatchingSubjectOf(message, rule -> rule.addCopy("Trash"));

		DeliveryContent result = engine.apply(rules);

		assertNotNull(result.message());

		ItemValue<MailboxReplica> trash = folder(boxUser1, "Trash");
		IDbMailboxRecords recordsApi = provider.instance(IDbMailboxRecords.class, trash.uid);
		List<ItemValue<MailboxRecord>> records = recordsApi.all();
		assertEquals(1, records.size());
	}

	@Test
	public void testDiscard() {
		var message = new MessageBuilder("Subject").build();
		var engine = engineOn(message);
		var rules = rulesMatchingSubjectOf(message, rule -> rule.addDiscard());

		DeliveryContent result = engine.apply(rules);

		assertNull(result.message());
	}

	@Test
	public void testFollowUp() {
		var message = new MessageBuilder("Subject").build();
		var engine = engineOn(message);
		var rules = rulesMatchingSubjectOf(message,
				rule -> rule.addFollowUp(FollowUpAction.FOLLOW_UP, DueDate.NEXT_WEEK));

		DeliveryContent result = engine.apply(rules);
		// TODO
		assertNotNull(result.message());
	}

	@Test
	public void testMarkAsDeleted() {
		var message = new MessageBuilder("Subject").build();
		var engine = engineOn(message);
		var rules = rulesMatchingSubjectOf(message, rule -> rule.addMarkAsDeleted());

		DeliveryContent result = engine.apply(rules);

		assertNotNull(result.message());
		assertTrue(result.mailboxRecord().flags.contains(Deleted.value()));
	}

	@Test
	public void testMarkAsImportant() {
		var message = new MessageBuilder("Subject").build();
		var engine = engineOn(message);
		var rules = rulesMatchingSubjectOf(message, rule -> rule.addMarkAsImportant());

		DeliveryContent result = engine.apply(rules);

		assertNotNull(result.message());
		assertTrue(result.mailboxRecord().flags.contains(Flagged.value()));
	}

	@Test
	public void testMarkAsRead() {
		var message = new MessageBuilder("Subject").build();
		var engine = engineOn(message);
		var rules = rulesMatchingSubjectOf(message, rule -> rule.addMarkAsRead());

		DeliveryContent result = engine.apply(rules);

		assertNotNull(result.message());
		assertTrue(result.mailboxRecord().flags.contains(Seen.value()));
	}

	@Test
	public void testMove() {
		var message = new MessageBuilder("Subject").build();
		var engine = engineOn(message);
		var rules = rulesMatchingSubjectOf(message, rule -> rule.addMove("Trash"));

		DeliveryContent result = engine.apply(rules);

		assertNotNull(result.message());
		assertEquals("Trash", result.folderItem().value.name);
	}

	@Test
	public void testPrioritize() {
		var message = new MessageBuilder("Subject").build();
		var engine = engineOn(message);
		var rules = rulesMatchingSubjectOf(message, rule -> rule.addPrioritize(3));

		DeliveryContent result = engine.apply(rules);

		assertNotNull(result.message());
		assertNotNull(result.message().getHeader().getField("X-Priority"));
		assertEquals("3", result.message().getHeader().getField("X-Priority").getBody());
	}

	@Test
	public void testRedirectWithKeepCopy() {
		var message = new MessageBuilder("Subject") //
				.from(emailUser2).to(emailUser1) //
				.content("Original text content", "Original html content") //
				.build();
		var engine = engineOn(message);
		var rules = rulesMatchingSubjectOf(message, rule -> rule.addRedirect(Arrays.asList(emailUser2), true));

		DeliveryContent result = engine.apply(rules);

		assertNotNull(result.message());

		assertTrue(mailer.mailSent);
		List<TestMail> messages = mailer.messages;
		assertEquals(1, messages.size());
		TestMail reply = messages.get(0);
		assertEquals(1, reply.to.size());
		assertEquals(emailUser2, reply.to.iterator().next());
		assertEquals(emailUser2, reply.from);

		Message redirectedMessage = reply.message;
		assertTrue(redirectedMessage.isMultipart());
		List<Entity> parts = ((Multipart) redirectedMessage.getBody()).getBodyParts();
		List<AddressableEntity> addressableParts = Mime4JHelper.expandTree(parts);
		Map<String, List<AddressableEntity>> partsByMimeType = addressableParts.stream() //
				.filter(part -> part.getMimeType() != null && !Mime4JHelper.isAttachment(part))
				.collect(Collectors.groupingBy(part -> part.getMimeType()));

		assertEquals(1, partsByMimeType.get("text/html").size());
		String htmlContent = extractContent(partsByMimeType.get("text/html").get(0));
		assertTrue(htmlContent.contains("Original html content"));

		assertEquals(1, partsByMimeType.get("text/plain").size());
		String textContent = extractContent(partsByMimeType.get("text/plain").get(0));
		assertTrue(textContent.contains("Original text content"));
	}

	@Test
	public void testRemoveHeaders() {
		var message = new MessageBuilder("Subject").header("X-HEADER", "header-value").build();
		var engine = engineOn(message);
		var rules = rulesMatchingSubjectOf(message, rule -> rule.addRemoveHeader("X-HEADER"));

		DeliveryContent result = engine.apply(rules);

		assertNotNull(result.message());
		assertNull(result.message().getHeader().getField("X-HEADER"));
	}

	@Test
	public void testReply() {
		var message = new MessageBuilder("Subject") //
				.from(emailUser2).to(emailUser1) //
				.content(null, "Original html content") //
				.build();
		var engine = engineOn(message);
		var rules = rulesMatchingSubjectOf(message, rule -> rule.addReply("subject", "body", "<b>htmlBody</b>"));

		DeliveryContent result = engine.apply(rules);

		assertNotNull(result.message());

		assertTrue(mailer.mailSent);
		List<TestMail> messages = mailer.messages;
		assertEquals(1, messages.size());
		TestMail reply = messages.get(0);
		assertEquals(1, reply.to.size());
		assertEquals(emailUser2, reply.to.iterator().next());
		assertEquals(emailUser1, reply.from);
	}

	@Test
	public void testSetFlags() {
		var message = new MessageBuilder("Subject").build();
		var engine = engineOn(message);
		var rules = rulesMatchingSubjectOf(message,
				rule -> rule.addSetFlags(Deleted.value().flag, Flagged.value().flag));

		DeliveryContent result = engine.apply(rules);

		assertTrue(result.mailboxRecord().flags.contains(Deleted.value()));
		assertTrue(result.mailboxRecord().flags.contains(Flagged.value()));
	}

	@Test
	public void testTransferWithKeepCopy() {
		var message = new MessageBuilder("Subject") //
				.from(emailUser2).to(emailUser1) //
				.content(null, "Original message content") //
				.build();
		var engine = engineOn(message);
		var rules = rulesMatchingSubjectOf(message, rule -> rule.addTransfer(Arrays.asList(emailUser2), false, true));

		DeliveryContent result = engine.apply(rules);

		assertNotNull(result.message());

		assertTrue(mailer.mailSent);
		List<TestMail> messages = mailer.messages;
		assertEquals(1, messages.size());
		TestMail reply = messages.get(0);
		assertEquals(1, reply.to.size());
		assertEquals(emailUser2, reply.to.iterator().next());
		assertEquals(emailUser1, reply.from);
	}

	@Test
	public void testTransferWithoutKeepCopy() {
		var message = new MessageBuilder("Subject") //
				.from(emailUser2).to(emailUser1) //
				.content(null, "Original message content") //
				.build();
		var engine = engineOn(message);
		var rules = rulesMatchingSubjectOf(message, rule -> rule.addTransfer(Arrays.asList(emailUser2), false, false));

		DeliveryContent result = engine.apply(rules);

		assertNull(result.message());

		assertTrue(mailer.mailSent);
		List<TestMail> messages = mailer.messages;
		assertEquals(1, messages.size());
		TestMail reply = messages.get(0);
		assertEquals(1, reply.to.size());
		assertEquals(emailUser2, reply.to.iterator().next());
		assertEquals(emailUser1, reply.from);

		Message transferedMessage = reply.message;
		assertTrue(transferedMessage.isMultipart());
		List<Entity> parts = ((Multipart) transferedMessage.getBody()).getBodyParts();
		List<AddressableEntity> addressableParts = Mime4JHelper.expandTree(parts);
		Map<String, List<AddressableEntity>> partsByMimeType = addressableParts.stream() //
				.filter(part -> part.getMimeType() != null && !Mime4JHelper.isAttachment(part))
				.collect(Collectors.groupingBy(part -> part.getMimeType()));
		assertEquals(1, partsByMimeType.get("text/html").size());
		assertNull(partsByMimeType.get("text/plain"));
		String htmlContent = extractContent(partsByMimeType.get("text/html").get(0));
		assertTrue(htmlContent.contains("Original message content"));
	}

	@Test
	public void testTransferWithAsAttachment() {
		var message = new MessageBuilder("Subject") //
				.from(emailUser2).to(emailUser1) //
				.content(null, "Original message content") //
				.build();
		var engine = engineOn(message);
		var rules = rulesMatchingSubjectOf(message, rule -> rule.addTransfer(Arrays.asList(emailUser2), true, false));

		DeliveryContent result = engine.apply(rules);

		assertNull(result.message());

		assertTrue(mailer.mailSent);
		List<TestMail> messages = mailer.messages;
		assertEquals(1, messages.size());
		TestMail reply = messages.get(0);
		assertEquals(1, reply.to.size());
		assertEquals(emailUser2, reply.to.iterator().next());
		assertEquals(emailUser1, reply.from);

		Message transferedMessage = reply.message;
		assertTrue(transferedMessage.isMultipart());
		List<Entity> parts = ((Multipart) transferedMessage.getBody()).getBodyParts();
		List<AddressableEntity> addressableParts = Mime4JHelper.expandTree(parts);
		assertEquals(1, addressableParts.size());
		assertTrue(Mime4JHelper.isAttachment(addressableParts.get(0)));
	}

	@Test
	public void testUncategorize() {
		var message = new MessageBuilder("Subject").header("X-Bm-Otlk-Name-Keywords", "one,two").build();
		var engine = engineOn(message);
		var rules = rulesMatchingSubjectOf(message, rule -> rule.addUncategorize());

		DeliveryContent result = engine.apply(rules);

		assertNull(result.message().getHeader().getField("X-Bm-Otlk-Name-Keywords"));
	}

	@Test
	public void testUnfollow() {
		var message = new MessageBuilder("Subject").header("X-Bm-Otlk-Flag-Request", FollowUpAction.FOLLOW_UP.name())
				.build();
		var engine = engineOn(message);
		var rules = rulesMatchingSubjectOf(message, rule -> rule.addUncategorize());

		DeliveryContent result = engine.apply(rules);
		// TODO
	}

	@Test
	public void testMultipleRules() {
		var message = new MessageBuilder("Subject").build();
		var engine = engineOn(message);
		var rules = rulesMatchingSubjectOf(message, //
				rule -> rule.addMarkAsRead(), //
				rule -> rule.addAddHeader("X-HEADER", "header-value"));

		DeliveryContent result = engine.apply(rules);

		assertTrue(result.mailboxRecord().flags.contains(Seen.value()));
		assertNotNull(result.message().getHeader().getField("X-HEADER"));
		assertEquals("header-value", result.message().getHeader().getField("X-HEADER").getBody());
	}

	@Test
	public void testMultipleRulesStop() {
		var message = new MessageBuilder("Subject").build();
		var engine = engineOn(message);
		var rules = rulesMatchingSubjectOf(message, //
				rule -> rule.addMarkAsRead(), //
				rule -> {
					rule.addMarkAsImportant();
					rule.stop = true;
				}, //
				rule -> rule.addAddHeader("X-HEADER", "header-value"));

		DeliveryContent result = engine.apply(rules);

		assertTrue(result.mailboxRecord().flags.contains(Seen.value()));
		assertTrue(result.mailboxRecord().flags.contains(Flagged.value()));
		assertNull(result.message().getHeader().getField("X-HEADER"));
	}
}
