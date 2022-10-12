package net.bluemind.delivery.rules.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.apache.james.mime4j.dom.Message;
import org.junit.Test;

import com.google.common.io.CountingInputStream;
import com.google.common.io.FileBackedOutputStream;

import net.bluemind.delivery.rules.FieldValueMessageProvider;
import net.bluemind.mailbox.api.rules.conditions.MailFilterRuleField;
import net.bluemind.mailbox.api.rules.conditions.MailFilterRuleKnownField;
import net.bluemind.mime4j.common.Mime4JHelper;

public class MessageFieldValueProviderTests {

	private static final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	@Test
	public void testMessageWithNullCharInTextPlainBody() {
		FieldValueMessageProvider provider = load("bm-15193.eml");

		List<String> body = provider.provides(MailFilterRuleKnownField.PART_CONTENT.toField());
		assertTrue(body.get(0).contains("I am sending you this message to inform you"));

		List<String> subject = provider.provides(MailFilterRuleKnownField.SUBJECT.toField());
		assertEquals("RELAY: Hello, this is the mail server on mail3.fortdefrance.fr. ", subject.get(0));

		long size = provider.provides(MailFilterRuleKnownField.SIZE.toField());
		assertEquals(13200, size);

		List<String> from = provider.provides(MailFilterRuleKnownField.FROM_EMAIL.toField());
		assertEquals("postmaster@mail3.fortdefrance.fr", from.get(0));

		List<String> to = provider.provides(MailFilterRuleKnownField.TO_EMAIL.toField());
		assertEquals(1, to.size());
		assertEquals("lelamentin@laforet.com", to.get(0));

		long attachmentCount = provider.provides(MailFilterRuleKnownField.ATTACHMENTS_COUNT.toField());
		assertEquals(2, attachmentCount);

	}

	@Test
	public void testMessageWithTextHtmlBody() {
		FieldValueMessageProvider provider = load("BM-15245.eml");

		List<String> body = provider.provides(MailFilterRuleKnownField.PART_CONTENT.toField());
		assertTrue(body.get(0).contains("Pouvez vous m'envoyer la facture concernant cette intervention"));

		Date date = provider.provides(MailFilterRuleKnownField.DATE.toField());
		assertEquals(0, toDate("2019-10-01 14:44:09").compareTo(date));

		List<String> replyTo = provider.provides(headerField("reply-to"));
		assertEquals("Serge DAVEU <daveuls@orange.fr>", replyTo.get(0));
	}

	@Test
	public void testMessageWithMultipleRecipients() {
		FieldValueMessageProvider provider = load("bm-15237.eml");

		List<String> from = provider.provides(MailFilterRuleKnownField.FROM_EMAIL.toField());
		assertEquals("david.phan@bluemind.net", from.get(0));

		List<String> to = provider.provides(MailFilterRuleKnownField.TO_EMAIL.toField());
		assertEquals(2, to.size());
		assertEquals("nicolas.lascombes@bluemind.net", to.get(0));
		assertEquals("david.phan@bluemind.net", to.get(1));
	}

	@Test
	public void testMessageWithEmptyFromAddress() {
		FieldValueMessageProvider provider = load("empty_from_address.eml");

		List<String> from = provider.provides(MailFilterRuleKnownField.FROM_EMAIL.toField());
		assertEquals("Christian Bergere", from.get(0));
	}

	@Test
	public void testMessageWithTextPlainAndHtml() {
		FieldValueMessageProvider provider = load("encoded_contentType.eml");

		List<String> body = provider.provides(MailFilterRuleKnownField.PART_CONTENT.toField());
		assertTrue(body.get(0).replaceFirst("my body is a wonderland", "").contains("my body is a wonderland"));

		long attachmentCount = provider.provides(MailFilterRuleKnownField.ATTACHMENTS_COUNT.toField());
		assertEquals(1, attachmentCount);
	}

	@Test
	public void testMessageWithWrongMime() {
		FieldValueMessageProvider provider = load("wrong_part_mime.eml");

		List<String> body = provider.provides(MailFilterRuleKnownField.PART_CONTENT.toField());
		assertTrue(body.get(0).replaceFirst("again", "").contains("again"));

		long attachmentCount = provider.provides(MailFilterRuleKnownField.ATTACHMENTS_COUNT.toField());
		assertEquals(0, attachmentCount);
	}

	private MailFilterRuleField<List> headerField(String headerName) {
		return new MailFilterRuleField<List>(MailFilterRuleKnownField.HEADERS, List.class, "headers." + headerName);
	}

	private Date toDate(String date) {
		try {
			return formatter.parse(date);
		} catch (ParseException e) {
			return null;
		}
	}

	private FieldValueMessageProvider load(String eml) {
		ClassLoader cl = MessageFieldValueProviderTests.class.getClassLoader();
		try (InputStream inputStream = cl.getResourceAsStream("data/" + eml);
				CountingInputStream countedInput = new CountingInputStream(inputStream);
				FileBackedOutputStream fbos = new FileBackedOutputStream(32000)) {
			countedInput.transferTo(fbos);
			Message message = Mime4JHelper.parse(fbos.asByteSource().openBufferedStream());
			return new FieldValueMessageProvider(message, countedInput.getCount());
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}
