package net.bluemind.delivery.rules.tests;

import static net.bluemind.mailbox.api.rules.conditions.MailFilterRuleKnownField.FROM_EMAIL;
import static net.bluemind.mailbox.api.rules.conditions.MailFilterRuleKnownField.TO_EMAIL;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Arrays;

import org.junit.Test;

import net.bluemind.delivery.lmtp.common.DeliveryContent;
import net.bluemind.mailbox.api.rules.MailFilterRule;
import net.bluemind.mailbox.api.rules.conditions.MailFilterRuleCondition;

public class RuleEngineParameterValueProviderTests extends AbstractRuleEngineTests {

	@Test
	public void testParameterBmDynamicAddressesMe() {
		var message = new MessageBuilder("Subject") //
				.from(emailUser2).to(emailUser1) //
				.content(null, "Original message content") //
				.build();
		var engine = engineOn(message);
		var rule = new MailFilterRule();
		rule.stop = false;
		rule.conditions.add(MailFilterRuleCondition.equal(TO_EMAIL.text(), "BM_DYNAMIC_ADDRESSES_ME"));
		rule.addDiscard();

		DeliveryContent result = engine.apply(Arrays.asList(rule));

		assertNull(result.message());

		rule = new MailFilterRule();
		rule.stop = false;
		rule.conditions.add(MailFilterRuleCondition.equal(FROM_EMAIL.text(), "BM_DYNAMIC_ADDRESSES_ME"));
		rule.addDiscard();

		result = engine.apply(Arrays.asList(rule));

		assertNotNull(result.message());
	}
}
