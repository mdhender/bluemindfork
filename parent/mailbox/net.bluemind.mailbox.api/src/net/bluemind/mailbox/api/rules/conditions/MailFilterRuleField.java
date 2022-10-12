package net.bluemind.mailbox.api.rules.conditions;

import java.util.Objects;

public class MailFilterRuleField<T> {

	private final MailFilterRuleKnownField field;
	private final Class<T> type;
	private final String name;

	public MailFilterRuleField(MailFilterRuleKnownField field, Class<T> type, String name) {
		this.field = field;
		this.type = type;
		this.name = name;
	}

	public MailFilterRuleKnownField field() {
		return field;
	}

	public Class<T> type() {
		return type;
	}

	public String name() {
		return name;
	}

	@Override
	public int hashCode() {
		return Objects.hash(field, name);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MailFilterRuleField other = (MailFilterRuleField) obj;
		return field == other.field && Objects.equals(name, other.name);
	}

}
