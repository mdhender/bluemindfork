package net.bluemind.mailbox.api.rules.actions;

import java.util.List;
import java.util.Objects;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public class MailFilterRuleActionTransfer extends MailFilterRuleAction {
	public List<String> emails;
	public boolean asAttachment;
	public boolean keepCopy;

	public MailFilterRuleActionTransfer() {
		this.name = MailFilterRuleActionName.TRANSFER;
	}

	public MailFilterRuleActionTransfer(List<String> emails, boolean asAttachment, boolean keepCopy) {
		this();
		this.emails = emails;
		this.asAttachment = asAttachment;
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
		MailFilterRuleActionTransfer other = (MailFilterRuleActionTransfer) obj;
		return Objects.equals(emails, other.emails) && keepCopy == other.keepCopy;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("MailFilterRuleActionTransfer [emails=");
		builder.append(emails);
		builder.append(", asAttachment=");
		builder.append(asAttachment);
		builder.append(", keepCopy=");
		builder.append(keepCopy);
		builder.append(", name=");
		builder.append(name);
		builder.append("]");
		return builder.toString();
	}

}
