package net.bluemind.mailbox.api.rules.actions;

import java.util.Collections;
import java.util.List;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public class MailFilterRuleActionSetFlags extends MailFilterRuleAction {

	public List<String> flags;
	public List<String> internalFlags;

	public MailFilterRuleActionSetFlags() {
		this.name = MailFilterRuleActionName.SET_FLAGS;
	}

	public MailFilterRuleActionSetFlags(List<String> flags, List<String> internalFlags) {
		this();
		this.flags = flags;
		this.internalFlags = internalFlags;
	}

	public MailFilterRuleActionSetFlags(List<String> flags) {
		this(flags, Collections.emptyList());
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("MailFilterRuleActionSetFlags [flags=");
		builder.append(flags);
		builder.append(", internalFlags=");
		builder.append(internalFlags);
		builder.append(", name=");
		builder.append(name);
		builder.append("]");
		return builder.toString();
	}

}
