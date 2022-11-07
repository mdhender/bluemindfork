package net.bluemind.mailbox.api.rules.conditions;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.GwtIncompatible;
import net.bluemind.mailbox.api.rules.FieldValueProvider;
import net.bluemind.mailbox.api.rules.ParameterValueProvider;

@BMApi(version = "3")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "operator")
@JsonSubTypes({ //
		@Type(value = MailFilterRuleFilterExists.class, name = "EXISTS"),
		@Type(value = MailFilterRuleFilterEquals.class, name = "EQUALS"),
		@Type(value = MailFilterRuleFilterContains.class, name = "CONTAINS"),
		@Type(value = MailFilterRuleFilterMatches.class, name = "MATCHES"),
		@Type(value = MailFilterRuleFilterRange.class, name = "RANGE"), })
public abstract class MailFilterRuleFilter {

	public List<String> fields;
	public MailFilterRuleOperatorName operator;

	public MailFilterRuleFilter() {

	}

	@GwtIncompatible
	public boolean match(FieldValueProvider fieldProvider, ParameterValueProvider parameterProvider) {
		return fields.stream() //
				.map(field -> match(field, fieldProvider, parameterProvider)) //
				.reduce(false, Boolean::logicalOr);
	}

	@GwtIncompatible
	protected abstract <F> boolean match(String fieldName, FieldValueProvider fieldProvider,
			ParameterValueProvider parameterProvider);
}
