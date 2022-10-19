package net.bluemind.delivery.rules.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.Test;

import net.bluemind.core.sendmail.testhelper.TestMail;
import net.bluemind.delivery.lmtp.common.DeliveryContent;
import net.bluemind.mailbox.api.rules.MailFilterRule;
import net.bluemind.mailbox.api.rules.conditions.MailFilterRuleCondition;

public class RuleEngineVacationTests extends AbstractRuleEngineTests {

	private static final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	@Test
	public void testMessageWithDateWithinClosedRange() throws ParseException {
		Date date = formatter.parse("2022-01-01 00:00:00");
		var message = new MessageBuilder("Subject") //
				.from(emailUser2).to(emailUser1) //
				.date(date) //
				.content(null, "Original message content") //
				.build();

		DeliveryContent result = engineOn(message)
				.apply(Arrays.asList(vacation("2021-01-01 00:00:00", "2022-01-01 00:00:01")));

		assertNotNull(result.message());
		assertHasReplied();
	}

	@Test
	public void testMessageWithDateWithinRangeWithNoUpperBound() throws ParseException {
		Date date = formatter.parse("2022-01-01 00:00:00");
		var message = new MessageBuilder("Subject") //
				.from(emailUser2).to(emailUser1) //
				.date(date) //
				.content(null, "Original message content") //
				.build();

		DeliveryContent result = engineOn(message).apply(Arrays.asList(vacation("2021-01-01 00:00:00", null)));

		assertNotNull(result.message());
		assertHasReplied();
	}

	@Test
	public void testMessageWithDateWithinRangeWithNoLowerBound() throws ParseException {
		Date date = formatter.parse("2019-01-01 00:00:00");
		var message = new MessageBuilder("Subject") //
				.from(emailUser2).to(emailUser1) //
				.date(date) //
				.content(null, "Original message content") //
				.build();

		DeliveryContent result = engineOn(message).apply(Arrays.asList(vacation(null, "2022-01-01 00:00:01")));

		assertNotNull(result.message());
		assertHasReplied();
	}

	@Test
	public void testMessageWithDateOutsideRange() throws ParseException {
		Date date = formatter.parse("2019-01-01 00:00:00");
		var message = new MessageBuilder("Subject") //
				.from(emailUser2).to(emailUser1) //
				.date(date) //
				.content(null, "Original message content") //
				.build();

		DeliveryContent result = engineOn(message)
				.apply(Arrays.asList(vacation("2021-01-01 00:00:00", "2022-01-01 00:00:01")));

		assertNotNull(result.message());
		assertFalse(mailer.mailSent);
	}

	@Test
	public void testMessageWithNoReplySender() throws ParseException {
		Date date = formatter.parse("2022-01-01 00:00:00");
		var message = new MessageBuilder("Subject") //
				.from("noreply@bluemind.net").to(emailUser1) //
				.date(date) //
				.content(null, "Original message content") //
				.build();

		DeliveryContent result = engineOn(message)
				.apply(Arrays.asList(vacation("2021-01-01 00:00:00", "2023-01-01 00:00:01")));

		assertNotNull(result.message());
		assertFalse(mailer.mailSent);
	}

	@Test
	public void testMessageWithXDSPAMResultHeader() throws ParseException {
		Date date = formatter.parse("2022-01-01 00:00:00");
		var message = new MessageBuilder("Subject") //
				.from(emailUser2).to(emailUser1) //
				.date(date) //
				.header("X-DSPAM-Result", "Spamxx") //
				.content(null, "Original message content") //
				.build();

		DeliveryContent result = engineOn(message)
				.apply(Arrays.asList(vacation("2021-01-01 00:00:00", "2023-01-01 00:00:01")));

		assertNotNull(result.message());
		assertFalse(mailer.mailSent);
	}

	@Test
	public void testMessageWithSpamHeader() throws ParseException {
		Date date = formatter.parse("2022-01-01 00:00:00");
		var message = new MessageBuilder("Subject") //
				.from(emailUser2).to(emailUser1) //
				.date(date) //
				.header("X-Spam-Flag", "YESxx") //
				.content(null, "Original message content") //
				.build();

		DeliveryContent result = engineOn(message)
				.apply(Arrays.asList(vacation("2021-01-01 00:00:00", "2023-01-01 00:00:01")));

		assertNotNull(result.message());
		assertFalse(mailer.mailSent);
	}

	@Test
	public void testMessageWithPrecedenceList() throws ParseException {
		Date date = formatter.parse("2022-01-01 00:00:00");
		var message = new MessageBuilder("Subject") //
				.from(emailUser2).to(emailUser1) //
				.date(date) //
				.header("Precedence", "list") //
				.content(null, "Original message content") //
				.build();

		DeliveryContent result = engineOn(message)
				.apply(Arrays.asList(vacation("2021-01-01 00:00:00", "2023-01-01 00:00:01")));

		assertNotNull(result.message());
		assertFalse(mailer.mailSent);
	}

	@Test
	public void testMessageWithPrecedenceBulk() throws ParseException {
		Date date = formatter.parse("2022-01-01 00:00:00");
		var message = new MessageBuilder("Subject") //
				.from(emailUser2).to(emailUser1) //
				.date(date) //
				.header("Precedence", "bulk") //
				.content(null, "Original message content") //
				.build();

		DeliveryContent result = engineOn(message)
				.apply(Arrays.asList(vacation("2021-01-01 00:00:00", "2023-01-01 00:00:01")));

		assertNotNull(result.message());
		assertFalse(mailer.mailSent);
	}

	@Test
	public void testMessageWithPrecedenceJunk() throws ParseException {
		Date date = formatter.parse("2022-01-01 00:00:00");
		var message = new MessageBuilder("Subject") //
				.from(emailUser2).to(emailUser1) //
				.date(date) //
				.header("Precedence", "junk") //
				.content(null, "Original message content") //
				.build();

		DeliveryContent result = engineOn(message)
				.apply(Arrays.asList(vacation("2021-01-01 00:00:00", "2023-01-01 00:00:01")));

		assertNotNull(result.message());
		assertHasReplied();
	}

	private void assertHasReplied() {
		assertTrue(mailer.mailSent);
		List<TestMail> messages = mailer.messages;
		assertEquals(1, messages.size());
		TestMail reply = messages.get(0);
		assertEquals(1, reply.to.size());
		assertEquals(emailUser2, reply.to.iterator().next());
		assertEquals(emailUser1, reply.from);
	}

	private MailFilterRule vacation(String start, String end) throws ParseException {
		MailFilterRule rule = new MailFilterRule();
		rule.type = MailFilterRule.Type.VACATION;
		rule.trigger = MailFilterRule.Trigger.IN;
		rule.active = true;
		rule.name = "";
		rule.conditions = new ArrayList<>();
		rule.conditions.add(MailFilterRuleCondition.between("date", start, end));
		rule.addReply("On vacation", "Coming back later", null);
		return rule;
	}
}
