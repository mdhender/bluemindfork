package net.bluemind.mailbox.api.rules.actions;

import java.util.Arrays;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public class MailFilterRuleActionUnfollow extends MailFilterRuleActionRemoveHeaders {

	public MailFilterRuleActionUnfollow() {
		super(Arrays.asList("X-Bm-Otlk-Reminder-Set", "X-Bm-Otlk-Flag-Request", "X-Bm-Otlk-Flag-Status",
				"X-Bm-Otlk-Flag-Color", "X-Bm-Otlk-Task-Ordinal-Date", "X-Bm-Otlk-Common-Start",
				"X-Bm-Otlk-Task-Due-Date"));
		this.name = MailFilterRuleActionName.UNFOLLOW;
	}

}
