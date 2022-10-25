package net.bluemind.mailbox.api.rules.actions;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
@JsonIgnoreProperties({ "flags" })
public class MailFilterRuleActionMarkAsImportant extends MailFilterRuleActionSetFlags {

	public MailFilterRuleActionMarkAsImportant() {
		super(Arrays.asList("\\Flagged"));
		this.name = MailFilterRuleActionName.MARK_AS_IMPORTANT;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("MailFilterRuleActionMarkAsImportant [flags=");
		builder.append(flags);
		builder.append(", internalFlags=");
		builder.append(internalFlags);
		builder.append(", name=");
		builder.append(name);
		builder.append("]");
		return builder.toString();
	}
}
