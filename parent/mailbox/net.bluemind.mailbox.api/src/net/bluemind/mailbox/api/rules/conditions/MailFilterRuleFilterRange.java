package net.bluemind.mailbox.api.rules.conditions;

import java.util.Arrays;
import java.util.List;

import net.bluemind.core.api.BMApi;
import net.bluemind.mailbox.api.rules.FieldValueProvider;
import net.bluemind.mailbox.api.rules.ParameterValueProvider;
import net.bluemind.mailbox.api.rules.conditions.MailFilterRuleOperators.RangeOperator;

@BMApi(version = "3")
public class MailFilterRuleFilterRange extends MailFilterRuleFilter {

	public String lowerBound;
	public String upperBound;

	public MailFilterRuleFilterRange() {
		this.operator = MailFilterRuleOperatorName.RANGE;
	}

	public MailFilterRuleFilterRange(List<String> fields, String lowerBound, String upperBound) {
		this();
		this.fields = fields;
		this.lowerBound = lowerBound;
		this.upperBound = upperBound;
	}

	public MailFilterRuleFilterRange(String field, String lowerBound, String upperBound) {
		this(Arrays.asList(field), lowerBound, upperBound);
	}

	@Override
	protected <F> boolean match(String fieldName, FieldValueProvider fieldProvider,
			ParameterValueProvider parameterProvider) {
		return MailFilterRuleKnownField.<F>from(fieldName).map(field -> {
			RangeOperator<F> ruleOperator = MailFilterRuleOperators.range(field);
			String providedLowerBound = parameterProvider.provides(lowerBound);
			String providedUpperBound = parameterProvider.provides(upperBound);
			F fieldValue = fieldProvider.provides(field);
			return ruleOperator.match(fieldValue, providedLowerBound, providedUpperBound);
		}).orElse(false);

	}
}
