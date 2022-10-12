package net.bluemind.mailbox.api.rules.actions;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public class MailFilterRuleActionDiscard extends MailFilterRuleAction {

	public MailFilterRuleActionDiscard() {
		this.name = MailFilterRuleActionName.DISCARD;
	}
}
