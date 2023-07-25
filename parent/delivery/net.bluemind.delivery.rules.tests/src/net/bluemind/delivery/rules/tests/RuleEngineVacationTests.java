package net.bluemind.delivery.rules.tests;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;

import net.bluemind.core.sendmail.testhelper.TestMail;
import net.bluemind.delivery.lmtp.common.DeliveryContent;
import net.bluemind.delivery.rules.MailboxVacationSendersCache;
import net.bluemind.mailbox.api.rules.MailFilterRule;
import net.bluemind.mailbox.api.rules.conditions.MailFilterRuleCondition;

public class RuleEngineVacationTests extends AbstractRuleEngineTests {

	private static final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	private static final String originalBody = "Original message content\nOriginal message content";
	private static final String originalBodyHtml = "<html><body><p>Original message content<br/>Original message content</p></body></html>";
	private static final String replyMessage = "Coming back later\nComing back later";
	private static final String replyMessageHtml = "<html><body><p>Coming back later<br/>Coming back later</p></body></html>";

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

		assertThat(result.message()).isNotNull();
		assertHasReplied();
	}

	@Test
	public void testReplyTxt_bodyTxt() throws ParseException {
		// reply text & body text
		Date date = formatter.parse("2022-01-01 00:00:00");
		var message = new MessageBuilder("Subject") //
				.from(emailUser2).to(emailUser1) //
				.date(date) //
				.content(originalBody, null) //
				.build();

		DeliveryContent result = engineOn(message)
				.apply(Arrays.asList(vacationWithoutHtml("2021-01-01 00:00:00", "2022-01-10 00:00:01")));

		assertThat(result.message()).isNotNull();
		;
		TestMail mail = assertHasReplied();

		final String expected = """
				Coming back later
				Coming back later
				>Original message content
				>Original message content
				""";

		assertThat(mail.message.isMultipart()).isTrue();
		String extractContent = extractMsgBody(mail.message, "plain");
		assertThat(extractContent.trim()).isEqualTo(expected.trim());
		;
	}

	@Test
	public void testReplyHtml_bodyHtml() throws ParseException {
		// reply html & body html
		Date date = formatter.parse("2022-01-01 00:00:00");
		var message = new MessageBuilder("Subject") //
				.from(emailUser2).to(emailUser1) //
				.date(date) //
				.content(null, originalBodyHtml) //
				.build();

		DeliveryContent result = engineOn(message)
				.apply(Arrays.asList(vacation("2021-01-01 00:00:00", "2022-01-10 00:00:01")));

		assertThat(result.message()).isNotNull();
		;
		TestMail mail = assertHasReplied();

		final String expected = """
				<html>
				 <head></head>
				 <body>
				  <p>Coming back later<br>Coming back later</p>
				  <blockquote type="cite" style="padding-left:5px; border-left:2px solid #1010ff; margin-left:5px">
				   <p>Original message content<br>Original message content</p>
				  </blockquote>
				 </body>
				</html>""";

		assertThat(mail.message.isMultipart()).isTrue();
		;
		String extractContent = extractMsgBody(mail.message, "html");
		assertThat(extractContent).isEqualTo(expected);
		;
	}

	@Test
	public void testReplyHtml_bodyTxt() throws ParseException {
		// reply html & body txt
		Date date = formatter.parse("2022-01-01 00:00:00");
		var message = new MessageBuilder("Subject") //
				.from(emailUser2).to(emailUser1) //
				.date(date) //
				.content(originalBody, null) //
				.build();

		DeliveryContent result = engineOn(message)
				.apply(Arrays.asList(vacation("2021-01-01 00:00:00", "2022-01-10 00:00:01")));

		assertThat(result.message()).isNotNull();
		;
		TestMail mail = assertHasReplied();

		final String expected = """
				Coming back later
				Coming back later
				>Original message content
				>Original message content
				""";

		assertThat(mail.message.isMultipart()).isTrue();
		;
		String extractContent = extractMsgBody(mail.message, "plain");
		assertThat(extractContent.trim()).isEqualTo(expected.trim());
		;
	}

	@Test
	public void testReplyTxt_bodyHtml() throws ParseException {
		// reply text & body html
		Date date = formatter.parse("2022-01-01 00:00:00");
		var message = new MessageBuilder("Subject") //
				.from(emailUser2).to(emailUser1) //
				.date(date) //
				.content(null, originalBodyHtml) //
				.build();

		DeliveryContent result = engineOn(message)
				.apply(Arrays.asList(vacationWithoutHtml("2021-01-01 00:00:00", "2022-01-10 00:00:01")));

		assertThat(result.message()).isNotNull();
		;
		TestMail mail = assertHasReplied();

		final String expected = """
				<html>
				 <head></head>
				 <body>
				  <p>Coming back later<br>Coming back later</p>
				  <blockquote type="cite" style="padding-left:5px; border-left:2px solid #1010ff; margin-left:5px">
				   <p>Original message content<br>Original message content</p>
				  </blockquote>
				 </body>
				</html>""";

		assertThat(mail.message.isMultipart()).isTrue();
		String extractContent = extractMsgBody(mail.message, "html");
		assertThat(extractContent).isEqualTo(expected);
	}

	@Test
	public void testMessageWithDateWithinRangeWithNoUpperBound() throws ParseException {
		Date date = formatter.parse("2022-01-01 00:00:00");
		var message = new MessageBuilder("Subject") //
				.from(emailUser2).to(emailUser1) //
				.date(date) //
				.content(null, "Original message content") //
				.build();

		DeliveryContent result = engineOn(message)
				.apply(Arrays.asList(vacationWithoutHtml("2021-01-01 00:00:00", null)));

		assertThat(result.message()).isNotNull();
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

		DeliveryContent result = engineOn(message)
				.apply(Arrays.asList(vacationWithoutHtml(null, "2022-01-01 00:00:01")));

		assertThat(result.message()).isNotNull();
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
				.apply(Arrays.asList(vacationWithoutHtml("2021-01-01 00:00:00", "2022-01-01 00:00:01")));

		assertThat(result.message()).isNotNull();
		assertThat(mailer.mailSent).isFalse();
	}

	@Test
	public void testMessageWithNoReplySender() throws ParseException {
		// from noreply@bluemind.net
		Date date = formatter.parse("2022-01-01 00:00:00");
		var message = new MessageBuilder("Subject") //
				.from("noreply@bluemind.net").to(emailUser1) //
				.date(date) //
				.content(null, "Original message content") //
				.build();

		DeliveryContent result = engineOn(message)
				.apply(Arrays.asList(vacationWithoutHtml("2021-01-01 00:00:00", "2023-01-01 00:00:01")));

		assertThat(result.message()).isNotNull();
		assertThat(mailer.mailSent).isFalse();

		// from noreplytoto@bluemind.net
		date = formatter.parse("2022-01-01 00:00:00");
		message = new MessageBuilder("Subject") //
				.from("noreplytoto@bluemind.net").to(emailUser1) //
				.date(date) //
				.content(null, "Original message content") //
				.build();

		result = engineOn(message)
				.apply(Arrays.asList(vacationWithoutHtml("2021-01-01 00:00:00", "2023-01-01 00:00:01")));

		assertThat(result.message()).isNotNull();
		assertThat(mailer.mailSent).isFalse();

		// from no-reply@bluemind.net
		date = formatter.parse("2022-01-01 00:00:00");
		message = new MessageBuilder("Subject") //
				.from("noreplytoto@bluemind.net").to(emailUser1) //
				.date(date) //
				.content(null, "Original message content") //
				.build();

		result = engineOn(message)
				.apply(Arrays.asList(vacationWithoutHtml("2021-01-01 00:00:00", "2023-01-01 00:00:01")));

		assertThat(result.message()).isNotNull();
		assertThat(mailer.mailSent).isFalse();
	}

	@Test
	public void testRecipientCache() throws ParseException {
		MailboxVacationSendersCache.Factory vacationCacheFactory = MailboxVacationSendersCache.Factory.build("/tmp");
		Date date = formatter.parse("2022-01-01 00:00:00");
		var message = new MessageBuilder("Subject") //
				.from(emailUser2).to(emailUser1) //
				.date(date) //
				.content(null, "Original message content") //
				.build();

		var result = engineOn(message, vacationCacheFactory)
				.apply(Arrays.asList(vacation("2021-01-01 00:00:00", "2022-01-01 00:00:01")));

		assertThat(result.message()).isNotNull();
		assertHasReplied();

		mailer.reset();
		result = engineOn(message, vacationCacheFactory)
				.apply(Arrays.asList(vacation("2021-01-01 00:00:00", "2022-01-01 00:00:01")));

		assertThat(result.message()).isNotNull();
		assertWithMessage("no autoreply to the second mail from the same sender").that(mailer.mailSent).isFalse();

		var message2 = new MessageBuilder("Subject") //
				.from("noreply@bluemind.net").to(emailUser1) //
				.date(date) //
				.content(null, "Original message content") //
				.build();

		mailer.reset();
		result = engineOn(message2, vacationCacheFactory)
				.apply(Arrays.asList(vacationWithoutHtml("2021-01-01 00:00:00", "2023-01-01 00:00:01")));

		assertThat(result.message()).isNotNull();
		assertWithMessage("no autoreply to sender like noreply*").that(mailer.mailSent).isFalse();

		mailer.reset();
		result = engineOn(message, vacationCacheFactory)
				.apply(Arrays.asList(vacation("2021-01-01 00:00:00", "2022-01-01 00:00:01")));

		assertThat(result.message()).isNotNull();
		assertWithMessage("still no autoreply to the third mail from the same sender").that(mailer.mailSent).isFalse();

		mailer.reset();
		result = engineOn(message, vacationCacheFactory)
				.apply(Arrays.asList(vacationUnactive(originalBodyHtml, originalBody)));

		assertThat(result.message()).isNotNull();
		assertWithMessage("no autoreply on disabled vacation, reset recipient cache").that(mailer.mailSent).isFalse();

		mailer.reset();
		result = engineOn(message, vacationCacheFactory)
				.apply(Arrays.asList(vacation("2021-01-01 00:00:00", "2022-01-01 00:00:01")));

		assertThat(result.message()).isNotNull();
		assertWithMessage("autoreply after recipient cache have been reset").that(mailer.mailSent).isTrue();
		assertHasReplied();
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
				.apply(Arrays.asList(vacationWithoutHtml("2021-01-01 00:00:00", "2023-01-01 00:00:01")));

		assertThat(result.message()).isNotNull();
		assertThat(mailer.mailSent).isFalse();
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
				.apply(Arrays.asList(vacationWithoutHtml("2021-01-01 00:00:00", "2023-01-01 00:00:01")));

		assertThat(result.message()).isNotNull();
		assertThat(mailer.mailSent).isFalse();
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
				.apply(Arrays.asList(vacationWithoutHtml("2021-01-01 00:00:00", "2023-01-01 00:00:01")));

		assertThat(result.message()).isNotNull();
		assertThat(mailer.mailSent).isFalse();
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
				.apply(Arrays.asList(vacationWithoutHtml("2021-01-01 00:00:00", "2023-01-01 00:00:01")));

		assertThat(result.message()).isNotNull();
		assertThat(mailer.mailSent).isFalse();
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
				.apply(Arrays.asList(vacationWithoutHtml("2021-01-01 00:00:00", "2023-01-01 00:00:01")));

		assertThat(result.message()).isNotNull();
		assertHasReplied();
	}

	private TestMail assertHasReplied() {
		assertThat(mailer.mailSent).isTrue();
		List<TestMail> messages = mailer.messages;
		assertThat(messages).hasSize(1);
		TestMail reply = messages.get(0);
		assertThat(reply.to).hasSize(1);
		assertThat(reply.to.iterator().next()).isEqualTo(emailUser2);
		assertThat(reply.from).isEqualTo(emailUser1);
		return reply;
	}

	private MailFilterRule vacationWithoutHtml(String start, String end) throws ParseException {
		MailFilterRule rule = new MailFilterRule();
		rule.type = MailFilterRule.Type.VACATION;
		rule.trigger = MailFilterRule.Trigger.IN;
		rule.active = true;
		rule.name = "";
		rule.conditions = new ArrayList<>();
		rule.conditions.add(MailFilterRuleCondition.between("date", start, end));
		rule.addReply("On vacation", replyMessage, null);
		return rule;
	}

	private MailFilterRule vacation(String start, String end) throws ParseException {
		MailFilterRule rule = new MailFilterRule();
		rule.type = MailFilterRule.Type.VACATION;
		rule.trigger = MailFilterRule.Trigger.IN;
		rule.active = true;
		rule.name = "";
		rule.conditions = new ArrayList<>();
		rule.conditions.add(MailFilterRuleCondition.between("date", start, end));
		rule.addReply("On vacation", replyMessage, replyMessageHtml);
		return rule;
	}

	private MailFilterRule vacationUnactive(String start, String end) throws ParseException {
		MailFilterRule rule = vacation(start, end);
		rule.active = false;
		return rule;
	}
}
