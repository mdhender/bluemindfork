package net.bluemind.mailbox.api.rules.actions;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public class MailFilterRuleActionDiscard extends MailFilterRuleAction {

	public MailFilterRuleActionDiscard() {
		this.name = MailFilterRuleActionName.DISCARD;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("MailFilterRuleActionDiscard [name=");
		builder.append(name);
		builder.append("]");
		return builder.toString();
	}
}
