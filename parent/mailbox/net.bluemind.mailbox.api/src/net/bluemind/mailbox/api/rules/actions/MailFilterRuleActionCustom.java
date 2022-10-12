package net.bluemind.mailbox.api.rules.actions;

import java.util.HashMap;
import java.util.Map;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public class MailFilterRuleActionCustom extends MailFilterRuleAction {
	public String kind;
	public Map<String, String> parameters = new HashMap<>();

	public MailFilterRuleActionCustom() {
		this.name = MailFilterRuleActionName.CUSTOM;
	}

	public MailFilterRuleActionCustom(String kind, Map<String, String> parameters) {
		this();
		this.kind = kind;
		this.parameters = parameters;
	}

}
