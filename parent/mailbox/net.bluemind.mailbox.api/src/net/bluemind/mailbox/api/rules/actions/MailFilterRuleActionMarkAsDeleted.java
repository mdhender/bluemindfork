package net.bluemind.mailbox.api.rules.actions;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
@JsonIgnoreProperties({ "flags" })
public class MailFilterRuleActionMarkAsDeleted extends MailFilterRuleActionSetFlags {

	public MailFilterRuleActionMarkAsDeleted() {
		super(Arrays.asList("\\Deleted", "\\Seen"), Arrays.asList("\\Expunged"));
		this.name = MailFilterRuleActionName.MARK_AS_DELETED;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("MailFilterRuleActionMarkAsDeleted [flags=");
		builder.append(flags);
		builder.append(", internalFlags=");
		builder.append(internalFlags);
		builder.append(", name=");
		builder.append(name);
		builder.append("]");
		return builder.toString();
	}
}
