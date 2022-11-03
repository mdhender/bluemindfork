package net.bluemind.mailbox.api.rules.actions;

import java.util.Objects;

import net.bluemind.core.api.BMApi;

@BMApi(version = "3")
public class MailFilterRuleActionMove extends MailFilterRuleAction {

	public String subtree;
	public Long id;
	public String folder;

	public MailFilterRuleActionMove() {
		this.name = MailFilterRuleActionName.MOVE;
	}

	public MailFilterRuleActionMove(String folder) {
		this("user", null, folder);
	}

	public MailFilterRuleActionMove(String subtree, Long id, String folder) {
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
		return folder;
	}
	
	public String asString() {
		return (id == null) ? subtree + ":" + folder : subtree +":" + id + ":" + folder;
	}
	
	public static MailFilterRuleActionMove fromString(String value) {
		String[] tokens = value.split(":");
		return (tokens.length == 2) // 
				? new MailFilterRuleActionMove(tokens[0], null, tokens[1]) //
				: new MailFilterRuleActionMove(tokens[0], Long.parseLong(tokens[1]), tokens[2]);
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
		MailFilterRuleActionMove other = (MailFilterRuleActionMove) obj;
		return Objects.equals(folder, other.folder) && Objects.equals(id, other.id)
				&& Objects.equals(subtree, other.subtree);
	}
	
	

}
