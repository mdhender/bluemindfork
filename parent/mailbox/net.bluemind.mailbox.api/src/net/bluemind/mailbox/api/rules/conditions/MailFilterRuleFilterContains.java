package net.bluemind.mailbox.api.rules.conditions;

import java.util.Arrays;
import java.util.List;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.GwtIncompatible;
import net.bluemind.mailbox.api.rules.FieldValueProvider;
import net.bluemind.mailbox.api.rules.ParameterValueProvider;
import net.bluemind.mailbox.api.rules.conditions.MailFilterRuleOperators.ContainsOperator;

@BMApi(version = "3")
public class MailFilterRuleFilterContains extends MailFilterRuleFilter {

	@BMApi(version = "3")
	public enum Comparator {
		FULLSTRING, SUBSTRING, PREFIX
	}

	@BMApi(version = "3")
	public enum Modifier {
		NONE, CASE_INSENSITIVE, IGNORE_NONSPACING_MARK, LOOSE
	}

	public Comparator comparator = Comparator.SUBSTRING;
	public Modifier modifier = Modifier.NONE;
	public List<String> values;

	public MailFilterRuleFilterContains() {
		this.operator = MailFilterRuleOperatorName.CONTAINS;
	}

	public MailFilterRuleFilterContains(List<String> fields, List<String> values, Comparator comparator,
			Modifier modifier) {
		this();
		this.fields = fields;
		this.values = values;
		this.comparator = comparator;
		this.modifier = modifier;
	}

	public MailFilterRuleFilterContains(List<String> fields, List<String> values) {
		this(fields, values, Comparator.SUBSTRING, Modifier.NONE);
	}

	public MailFilterRuleFilterContains(List<String> fields, String value, Comparator comparator, Modifier modifier) {
		this(fields, Arrays.asList(value), comparator, modifier);
	}

	public MailFilterRuleFilterContains(List<String> fields, String value) {
		this(fields, Arrays.asList(value));
	}

	public MailFilterRuleFilterContains(String field, List<String> values, Comparator comparator, Modifier modifier) {
		this(Arrays.asList(field), values, comparator, modifier);
	}

	public MailFilterRuleFilterContains(String field, List<String> values) {
		this(Arrays.asList(field), values);
	}

	public MailFilterRuleFilterContains(String field, String value, Comparator comparator, Modifier modifier) {
		this(Arrays.asList(field), Arrays.asList(value), comparator, modifier);
	}

	public MailFilterRuleFilterContains(String field, String value) {
		this(Arrays.asList(field), Arrays.asList(value));
	}

	@GwtIncompatible
	@Override
	protected <F> boolean match(String fieldName, FieldValueProvider fieldProvider,
			ParameterValueProvider parameterProvider) {
		return MailFilterRuleKnownField.<F>from(fieldName).map(field -> {
			ContainsOperator<F> ruleOperator = MailFilterRuleOperators.contains(field);
			List<String> parameterValues = parameterProvider.provides(values);
			F fieldValue = fieldProvider.provides(field);
			return ruleOperator.match(fieldValue, parameterValues, comparator, modifier);
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
