package net.bluemind.mailbox.api.rules.actions;

import java.util.Objects;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public class MailFilterRuleActionMove extends MailFilterRuleAction {

	public String folder;

	public MailFilterRuleActionMove() {
		this.name = MailFilterRuleActionName.MOVE;
	}

	public MailFilterRuleActionMove(String folder) {
		this();
		this.folder = folder;
	}

	public String folder() {
		return folder;
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
		MailFilterRuleActionMove other = (MailFilterRuleActionMove) obj;
		return Objects.equals(folder, other.folder);
	}

}
