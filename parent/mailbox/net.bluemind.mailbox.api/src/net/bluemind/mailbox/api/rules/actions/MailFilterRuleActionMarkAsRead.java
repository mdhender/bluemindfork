package net.bluemind.mailbox.api.rules.actions;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
@JsonIgnoreProperties({ "flags" })
public class MailFilterRuleActionMarkAsRead extends MailFilterRuleActionSetFlags {

	public MailFilterRuleActionMarkAsRead() {
		super(Arrays.asList("\\Seen"));
		this.name = MailFilterRuleActionName.MARK_AS_READ;
	}
}
