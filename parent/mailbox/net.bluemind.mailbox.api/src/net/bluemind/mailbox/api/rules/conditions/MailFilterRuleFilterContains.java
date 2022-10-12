package net.bluemind.mailbox.api.rules.conditions;

import java.util.Arrays;
import java.util.List;

import net.bluemind.core.api.BMApi;
import net.bluemind.mailbox.api.rules.FieldValueProvider;
import net.bluemind.mailbox.api.rules.ParameterValueProvider;
import net.bluemind.mailbox.api.rules.conditions.MailFilterRuleOperators.ContainsOperator;

@BMApi(version = "3")
public class MailFilterRuleFilterContains extends MailFilterRuleFilter {

	public List<String> values;

	public MailFilterRuleFilterContains() {
		this.operator = MailFilterRuleOperatorName.CONTAINS;

	}

	public MailFilterRuleFilterContains(List<String> fields, List<String> values) {
		this();
		this.fields = fields;
		this.values = values;
	}

	public MailFilterRuleFilterContains(List<String> fields, String value) {
		this(fields, Arrays.asList(value));
	}

	public MailFilterRuleFilterContains(String field, List<String> values) {
		this(Arrays.asList(field), values);
	}

	public MailFilterRuleFilterContains(String field, String value) {
		this(Arrays.asList(field), Arrays.asList(value));
	}

	@Override
	protected <F> boolean match(String fieldName, FieldValueProvider fieldProvider,
			ParameterValueProvider parameterProvider) {
		return MailFilterRuleKnownField.<F>from(fieldName).map(field -> {
			ContainsOperator<F> ruleOperator = MailFilterRuleOperators.contains(field);
			List<String> parameterValues = parameterProvider.provides(values);
			F fieldValue = fieldProvider.provides(field);
			return ruleOperator.match(fieldValue, parameterValues);
		}).orElse(false);

	}
}
