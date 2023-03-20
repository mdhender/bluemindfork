package net.bluemind.mailbox.api.rules;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import net.bluemind.mailbox.api.rules.MailFilterRule.Type;

public class MailFilterRuleTest {
	
	@Test
	public void testOrder() {
		List<MailFilterRule> rules = new ArrayList<>();
		MailFilterRule rule2 = new MailFilterRule();
		MailFilterRule rule1 = new MailFilterRule();
		rule1.type = Type.VACATION;
		rule1.name = "1";
		rules.add(rule1);
		MailFilterRule rule4 = new MailFilterRule();
		rule4.type = Type.GENERIC;
		rule4.name = "3";
		rules.add(rule4);
		MailFilterRule rule5 = new MailFilterRule();
		rule5.type = Type.GENERIC;
		rule5.name = "4";
		rules.add(rule5);
		rule2.type = Type.FORWARD;
		rule2.name = "2";
		rules.add(rule2);
		
		List<MailFilterRule> sorted = MailFilterRule.sort(rules);
		
		int index = 1;
		for (MailFilterRule rule: sorted) {
			assertEquals(index, Integer.parseInt(rule.name));
			index++;
		}
	}
}
