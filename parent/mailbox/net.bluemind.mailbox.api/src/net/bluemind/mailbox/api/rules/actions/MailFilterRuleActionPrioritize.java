package net.bluemind.mailbox.api.rules.actions;

import java.util.Collections;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public class MailFilterRuleActionPrioritize extends MailFilterRuleActionAddHeaders {

	public MailFilterRuleActionPrioritize() {
		this.name = MailFilterRuleActionName.PRIORITIZE;
	}

	public MailFilterRuleActionPrioritize(String priority) {
		super(Collections.singletonMap("X-Priority", priority));
		this.name = MailFilterRuleActionName.PRIORITIZE;
	}

}
