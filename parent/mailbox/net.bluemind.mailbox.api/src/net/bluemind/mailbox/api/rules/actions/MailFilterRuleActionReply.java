package net.bluemind.mailbox.api.rules.actions;

import java.util.Objects;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public class MailFilterRuleActionReply extends MailFilterRuleAction {

	public String subject;
	public String plainBody;
	public String htmlBody;

	public MailFilterRuleActionReply() {
		this.name = MailFilterRuleActionName.REPLY;
	}

	public MailFilterRuleActionReply(String subject, String plainBody, String htmlBody) {
		this();
		this.subject = subject;
		this.plainBody = plainBody;
		this.htmlBody = htmlBody;
	}

	public String subject() {
		return subject;
	}

	public String plainBody() {
		return plainBody;
	}

	public String htmlBody() {
		return htmlBody;
	}

	@Override
	public int hashCode() {
		return Objects.hash(htmlBody, plainBody, subject);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("MailFilterRuleActionReply [subject=");
		builder.append(subject);
		builder.append(", plainBody=");
		builder.append(plainBody);
		builder.append(", htmlBody=");
		builder.append(htmlBody);
		builder.append(", name=");
		builder.append(name);
		builder.append("]");
		return builder.toString();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MailFilterRuleActionReply other = (MailFilterRuleActionReply) obj;
		return Objects.equals(htmlBody, other.htmlBody) && Objects.equals(plainBody, other.plainBody)
				&& Objects.equals(subject, other.subject);
	}

}
