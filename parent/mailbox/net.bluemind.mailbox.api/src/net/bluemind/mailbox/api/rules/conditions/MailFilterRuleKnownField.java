package net.bluemind.mailbox.api.rules.conditions;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;

public enum MailFilterRuleKnownField {
	FROM_EMAIL("from.email", List.class), //
	FROM_RAW("from.raw", List.class), //
	TO_EMAIL("to.email", List.class), //
	TO_RAW("to.raw", List.class), //
	CC_EMAIL("cc.email", List.class), //
	CC_RAW("cc.raw", List.class), //
	BCC_EMAIL("bcc.email", List.class), //
	BCC_RAW("bcc.raw", List.class), //
	HEADERS("headers", List.class), //
	SUBJECT("subject", List.class), //
	PART_CONTENT("part.content", List.class), //
	SIZE("size", Long.class), //
	DATE("date", Date.class), //
	ATTACHMENTS_COUNT("attachments.count", Long.class);

	private String text;
	private Class<?> type;

	private MailFilterRuleKnownField(String text, Class<?> type) {
		this.text = text;
		this.type = type;
	}

	public String text() {
		return text;
	}

	public <T> MailFilterRuleField<T> toField() {
		return new MailFilterRuleField<>(this, (Class<T>) this.type, this.text);
	}

	@SuppressWarnings("unchecked")
	public static <T> Optional<MailFilterRuleField<T>> from(String name) {
		return Arrays.stream(MailFilterRuleKnownField.values()) //
				.filter(field -> name.startsWith(field.text)) //
				.findFirst() //
				.map(field -> new MailFilterRuleField<>(field, (Class<T>) field.type, name));
	}
}