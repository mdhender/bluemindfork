package net.bluemind.mailbox.api.rules.actions;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public class MailFilterRuleActionCategorize extends MailFilterRuleActionAddHeaders {

	public static final String LIST_SEPARATOR = ",";

	public MailFilterRuleActionCategorize() {
		this.name = MailFilterRuleActionName.CATEGORIZE;

	}

	public MailFilterRuleActionCategorize(List<String> categories) {
		super(Collections.singletonMap("X-Bm-Otlk-Name-Keywords",
				categories.stream().collect(Collectors.joining(LIST_SEPARATOR))));
		this.name = MailFilterRuleActionName.CATEGORIZE;
	}

}