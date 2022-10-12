package net.bluemind.mailbox.api.rules;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import net.bluemind.mailbox.api.MailFilter;

public class MailFilterRuleForwardingMapper implements MailFilterRuleTypeMapper<MailFilter.Forwarding> {

	public MailFilterRuleForwardingMapper() {

	}

	public Optional<MailFilterRule> map(MailFilter.Forwarding forwarding) {
		if (forwarding == null) {
			return Optional.empty();
		}

		MailFilterRule rule = new MailFilterRule();
		rule.type = MailFilterRule.Type.FORWARD;
		rule.trigger = MailFilterRule.Trigger.IN;
		rule.active = forwarding.enabled;
		rule.client = "bluemind";
		rule.name = "";
		rule.conditions = new ArrayList<>();
		if (forwarding.emails != null) {
			List<String> emails = new ArrayList<>(forwarding.emails);
			rule.addRedirect(emails, forwarding.localCopy);
		}
		rule.stop = false;
		return Optional.of(rule);
	}

	public MailFilter.Forwarding map(MailFilterRule rule) {
		MailFilter.Forwarding forwarding = new MailFilter.Forwarding();
		forwarding.enabled = rule.active;
		rule.redirect().ifPresentOrElse(redirect -> {
			forwarding.emails = new HashSet<>(redirect.emails());
			forwarding.localCopy = redirect.keepCopy;
		}, () -> {
			forwarding.emails = new HashSet<>();
			forwarding.localCopy = false;
		});
		return forwarding;
	}

}
