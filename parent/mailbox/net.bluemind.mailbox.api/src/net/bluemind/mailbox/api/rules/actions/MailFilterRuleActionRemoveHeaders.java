package net.bluemind.mailbox.api.rules.actions;

import java.util.List;
import java.util.Objects;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public class MailFilterRuleActionRemoveHeaders extends MailFilterRuleAction {

	public List<String> headerNames;

	public MailFilterRuleActionRemoveHeaders() {
		this.name = MailFilterRuleActionName.REMOVE_HEADERS;
	}

	public MailFilterRuleActionRemoveHeaders(List<String> headerNames) {
		this();
		this.headerNames = headerNames;
	}

	public List<String> headerNames() {
		return headerNames;
	}

	@Override
	public int hashCode() {
		return Objects.hash(headerNames);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MailFilterRuleActionRemoveHeaders other = (MailFilterRuleActionRemoveHeaders) obj;
		return Objects.equals(headerNames, other.headerNames);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("MailFilterRuleActionRemoveHeaders [headerNames=");
		builder.append(headerNames);
		builder.append(", name=");
		builder.append(name);
		builder.append("]");
		return builder.toString();
	}

}
