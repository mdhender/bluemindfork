package net.bluemind.mailbox.api.rules.conditions;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public enum MailFilterRuleOperatorName {
	EXISTS, EQUALS, CONTAINS, MATCHES, RANGE;
}