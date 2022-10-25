package net.bluemind.mailbox.api.rules.actions;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public class MailFilterRuleActionDeferredAction extends MailFilterRuleAction {

	public MailFilterRuleActionDeferredAction() {
		this.name = MailFilterRuleActionName.DEFERRED_ACTION;
	}

}
