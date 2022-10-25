package net.bluemind.mailbox.api.rules.actions;

import java.util.Arrays;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public class MailFilterRuleActionUncategorize extends MailFilterRuleActionRemoveHeaders {

	public MailFilterRuleActionUncategorize() {
		super(Arrays.asList("X-Bm-Otlk-Name-Keywords"));
		this.name = MailFilterRuleActionName.UNCATEGORIZE;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("MailFilterRuleActionUncategorize [headerNames=");
		builder.append(headerNames);
		builder.append(", name=");
		builder.append(name);
		builder.append("]");
		return builder.toString();
	}

}
