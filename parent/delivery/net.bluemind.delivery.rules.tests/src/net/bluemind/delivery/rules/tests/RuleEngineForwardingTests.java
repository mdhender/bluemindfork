package net.bluemind.delivery.rules.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.Test;

import net.bluemind.core.sendmail.testhelper.TestMail;
import net.bluemind.delivery.lmtp.common.DeliveryContent;
import net.bluemind.mailbox.api.rules.MailFilterRule;

public class RuleEngineForwardingTests extends AbstractRuleEngineTests {

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
				.apply(Arrays.asList(forward("one@bluemind.net", "two@bluemind.net")));

		assertNotNull(result.message());
		assertHasForward();
	}

	private void assertHasForward() {
		assertTrue(mailer.mailSent);
		List<TestMail> messages = mailer.messages;
		assertEquals(1, messages.size());
		TestMail forward = messages.get(0);
		assertEquals(2, forward.to.size());
		assertTrue(forward.to.contains("one@bluemind.net"));
		assertTrue(forward.to.contains("two@bluemind.net"));
	}

	private MailFilterRule forward(String... emails) throws ParseException {
		MailFilterRule rule = new MailFilterRule();
		rule.type = MailFilterRule.Type.FORWARD;
		rule.trigger = MailFilterRule.Trigger.IN;
		rule.active = true;
		rule.name = "";
		rule.conditions = Collections.emptyList();
		rule.addRedirect(Arrays.asList(emails), true);
		rule.stop = false;
		return rule;
	}
}
