package net.bluemind.mailbox.api.rules.conditions;

import java.util.Arrays;
import java.util.List;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.GwtIncompatible;
import net.bluemind.mailbox.api.rules.FieldValueProvider;
import net.bluemind.mailbox.api.rules.ParameterValueProvider;
import net.bluemind.mailbox.api.rules.conditions.MailFilterRuleOperators.MatchesOperator;

@BMApi(version = "3")
public class MailFilterRuleFilterMatches extends MailFilterRuleFilter {

	public List<String> values;

	public MailFilterRuleFilterMatches() {
		this.operator = MailFilterRuleOperatorName.MATCHES;
	}

	public MailFilterRuleFilterMatches(List<String> fields, List<String> values) {
		this();
		this.fields = fields;
		this.values = values;
	}

	public MailFilterRuleFilterMatches(List<String> fields, String value) {
		this(fields, Arrays.asList(value));
	}

	public MailFilterRuleFilterMatches(String field, List<String> values) {
		this(Arrays.asList(field), values);
	}

	public MailFilterRuleFilterMatches(String field, String value) {
		this(Arrays.asList(field), Arrays.asList(value));
	}

	@GwtIncompatible
	@Override
	protected <F> boolean match(String fieldName, FieldValueProvider fieldProvider,
			ParameterValueProvider parameterProvider) {
		return MailFilterRuleKnownField.<F>from(fieldName).map(field -> {
			MatchesOperator<F> ruleOperator = MailFilterRuleOperators.matches(field);
			List<String> parameterValues = parameterProvider.provides(values);
			F fieldValue = fieldProvider.provides(field);
			return ruleOperator.match(fieldValue, parameterValues);
		}).orElse(false);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("MailFilterRuleFilterContains [values=");
		builder.append(values);
		builder.append(", fields=");
		builder.append(fields);
		builder.append(", operator=");
		builder.append(operator);
		builder.append("]");
		return builder.toString();
	}
}
