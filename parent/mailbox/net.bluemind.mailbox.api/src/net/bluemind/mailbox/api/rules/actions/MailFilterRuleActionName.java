package net.bluemind.mailbox.api.rules.actions;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public enum MailFilterRuleActionName {
	ADD_HEADER, CATEGORIZE, COPY, DISCARD, FOLLOW_UP, MARK_AS_DELETED, MARK_AS_IMPORTANT, MARK_AS_READ, MOVE,
	PRIORITIZE, REDIRECT, REMOVE_HEADERS, REPLY, SET_FLAGS, TRANSFER, UNCATEGORIZE, UNFOLLOW
}
