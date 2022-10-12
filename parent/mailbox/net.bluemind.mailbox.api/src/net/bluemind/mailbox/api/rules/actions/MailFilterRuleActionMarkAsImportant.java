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
}
