package net.bluemind.mailbox.api.rules.actions;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "name")
@JsonSubTypes({ //
		@Type(value = MailFilterRuleActionAddHeaders.class, name = "ADD_HEADER"),
		@Type(value = MailFilterRuleActionCategorize.class, name = "CATEGORIZE"),
		@Type(value = MailFilterRuleActionCopy.class, name = "COPY"),
		@Type(value = MailFilterRuleActionDiscard.class, name = "DISCARD"),
		@Type(value = MailFilterRuleActionFollowUp.class, name = "FOLLOW_UP"),
		@Type(value = MailFilterRuleActionMarkAsDeleted.class, name = "MARK_AS_DELETED"),
		@Type(value = MailFilterRuleActionMarkAsImportant.class, name = "MARK_AS_IMPORTANT"),
		@Type(value = MailFilterRuleActionMarkAsRead.class, name = "MARK_AS_READ"),
		@Type(value = MailFilterRuleActionMove.class, name = "MOVE"),
		@Type(value = MailFilterRuleActionPrioritize.class, name = "PRIORITIZE"),
		@Type(value = MailFilterRuleActionRedirect.class, name = "REDIRECT"),
		@Type(value = MailFilterRuleActionRemoveHeaders.class, name = "REMOVE_HEADERS"),
		@Type(value = MailFilterRuleActionReply.class, name = "REPLY"),
		@Type(value = MailFilterRuleActionSetFlags.class, name = "SET_FLAGS"),
		@Type(value = MailFilterRuleActionTransfer.class, name = "TRANSFER"),
		@Type(value = MailFilterRuleActionUncategorize.class, name = "UNCATEGORIZE"),
		@Type(value = MailFilterRuleActionUnfollow.class, name = "UNFOLLOW"),
		@Type(value = MailFilterRuleActionCustom.class, name = "CUSTOM") })
public class MailFilterRuleAction {

	public MailFilterRuleActionName name;
}
