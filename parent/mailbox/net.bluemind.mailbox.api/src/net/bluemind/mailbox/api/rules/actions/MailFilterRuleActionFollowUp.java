package net.bluemind.mailbox.api.rules.actions;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public class MailFilterRuleActionFollowUp extends MailFilterRuleAction {

	public FollowUpAction action = FollowUpAction.FOLLOW_UP;
	public DueDate dueDate = DueDate.NONE;

	@BMApi(version = "3")
	public enum FollowUpAction {
		FOLLOW_UP, FOR_YOUR_INFORMATION, TRANSFER, NO_RESPONSE_NEEDED, READ, RESPOND
	}

	@BMApi(version = "3")
	public enum DueDate {
		TODAY, TOMORROW, THIS_WEEK, NEXT_WEEK, NONE, OVER
	}

	public MailFilterRuleActionFollowUp() {
		this.name = MailFilterRuleActionName.FOLLOW_UP;
	}

	public MailFilterRuleActionFollowUp(FollowUpAction action, DueDate dueDate) {
		this();
		this.action = action;
		this.dueDate = dueDate;
	}
}
