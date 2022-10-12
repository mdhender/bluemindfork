package net.bluemind.mailbox.api.rules.actions;

import java.util.Map;
import java.util.Objects;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public class MailFilterRuleActionAddHeaders extends MailFilterRuleAction {

	public Map<String, String> headers;

	public MailFilterRuleActionAddHeaders() {
		this.name = MailFilterRuleActionName.ADD_HEADER;
	}

	public MailFilterRuleActionAddHeaders(Map<String, String> headers) {
		this();
		this.headers = headers;
	}

	public String header(String headerName) {
		return headers.get(headerName);
	}

	@Override
	public int hashCode() {
		return Objects.hash(headers);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MailFilterRuleActionAddHeaders other = (MailFilterRuleActionAddHeaders) obj;
		return Objects.equals(headers, other.headers);
	}

}
