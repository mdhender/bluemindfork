package net.bluemind.mailbox.api.rules.conditions;

import java.util.Arrays;
import java.util.List;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.GwtIncompatible;
import net.bluemind.mailbox.api.rules.FieldValueProvider;
import net.bluemind.mailbox.api.rules.ParameterValueProvider;
import net.bluemind.mailbox.api.rules.conditions.MailFilterRuleOperators.EqualsOperator;

@BMApi(version = "3")
public class MailFilterRuleFilterEquals extends MailFilterRuleFilter {

	public List<String> values;

	public MailFilterRuleFilterEquals() {
		this.operator = MailFilterRuleOperatorName.EQUALS;
	}

	public MailFilterRuleFilterEquals(List<String> fields, List<String> values) {
		this();
		this.fields = fields;
		this.values = values;
	}

	public MailFilterRuleFilterEquals(List<String> fields, String value) {
		this(fields, Arrays.asList(value));
	}

	public MailFilterRuleFilterEquals(String field, List<String> values) {
		this(Arrays.asList(field), values);
	}

	public MailFilterRuleFilterEquals(String field, String value) {
		this(Arrays.asList(field), Arrays.asList(value));
	}

	@GwtIncompatible
	@Override
	protected <F> boolean match(String fieldName, FieldValueProvider fieldProvider,
			ParameterValueProvider parameterProvider) {
		return MailFilterRuleKnownField.<F>from(fieldName).map(field -> {
			EqualsOperator<F> ruleOperator = MailFilterRuleOperators.equals(field);
			List<String> parameterValues = parameterProvider.provides(values);
			F fieldValue = fieldProvider.provides(field);
			return ruleOperator.match(fieldValue, parameterValues);
		}).orElse(false);

	}
}
