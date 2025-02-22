package net.bluemind.mailbox.api.rules.actions;

import java.util.Objects;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public class MailFilterRuleActionCopy extends MailFilterRuleAction {
	public String subtree;
	public Long id;
	public String folder;

	public MailFilterRuleActionCopy() {
		this.name = MailFilterRuleActionName.COPY;
	}

	public MailFilterRuleActionCopy(String folder) {
		this("user", null, folder);
	}

	public MailFilterRuleActionCopy(String subtree, Long id, String folder) {
		this();
		this.subtree = subtree;
		this.id = id;
		this.folder = folder;
	}

	public String subtree() {
		return subtree;
	}

	public Long id() {
		return id;
	}

	public String folder() {
		return this.folder;
	}

	public String asString() {
		return (id == null) ? subtree + ":" + folder : subtree + ":" + id + ":" + folder;
	}

	public static MailFilterRuleActionCopy fromString(String value) {
		String[] tokens = value.split(":");
		return (tokens.length == 2) //
				? new MailFilterRuleActionCopy(tokens[0], null, tokens[1]) //
				: new MailFilterRuleActionCopy(tokens[0], Long.parseLong(tokens[1]), tokens[2]);
	}

	@Override
	public int hashCode() {
		return Objects.hash(folder, id, subtree);
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
		return Objects.equals(folder, other.folder) && Objects.equals(id, other.id)
				&& Objects.equals(subtree, other.subtree);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("MailFilterRuleActionCopy [subtree=");
		builder.append(subtree);
		builder.append(", id=");
		builder.append(id);
		builder.append(", folder=");
		builder.append(folder);
		builder.append(", name=");
		builder.append(name);
		builder.append("]");
		return builder.toString();
	}

}
