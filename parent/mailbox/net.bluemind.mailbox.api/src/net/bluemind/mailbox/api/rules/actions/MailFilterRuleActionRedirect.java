package net.bluemind.mailbox.api.rules.actions;

import java.util.List;
import java.util.Objects;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public class MailFilterRuleActionRedirect extends MailFilterRuleAction {

	public List<String> emails;
	public boolean keepCopy;

	public MailFilterRuleActionRedirect() {
		this.name = MailFilterRuleActionName.REDIRECT;
	}

	public MailFilterRuleActionRedirect(List<String> emails, boolean keepCopy) {
		this();
		this.emails = emails;
		this.keepCopy = keepCopy;
	}

	public List<String> emails() {
		return emails;
	}

	public boolean keepCopy() {
		return keepCopy;
	}

	@Override
	public int hashCode() {
		return Objects.hash(emails, keepCopy);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MailFilterRuleActionRedirect other = (MailFilterRuleActionRedirect) obj;
		return Objects.equals(emails, other.emails) && keepCopy == other.keepCopy;
	}

}
