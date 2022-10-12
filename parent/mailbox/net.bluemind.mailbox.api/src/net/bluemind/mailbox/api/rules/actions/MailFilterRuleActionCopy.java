package net.bluemind.mailbox.api.rules.actions;

import java.util.Objects;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public class MailFilterRuleActionCopy extends MailFilterRuleAction {
	public String folder;

	public MailFilterRuleActionCopy() {
		this.name = MailFilterRuleActionName.COPY;
	}

	public MailFilterRuleActionCopy(String folder) {
		this();
		this.folder = folder;
	}

	public String folder() {
		return this.folder;
	}

	@Override
	public int hashCode() {
		return Objects.hash(folder);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MailFilterRuleActionCopy other = (MailFilterRuleActionCopy) obj;
		return Objects.equals(folder, other.folder);
	}

}
