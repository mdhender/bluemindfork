package net.bluemind.mailbox.api.rules.conditions;

import java.util.Arrays;
import java.util.List;

import net.bluemind.core.api.BMApi;
import net.bluemind.core.api.GwtIncompatible;
import net.bluemind.mailbox.api.rules.FieldValueProvider;
import net.bluemind.mailbox.api.rules.ParameterValueProvider;
import net.bluemind.mailbox.api.rules.conditions.MailFilterRuleOperators.ExistsOperator;

@BMApi(version = "3")
public class MailFilterRuleFilterExists extends MailFilterRuleFilter {

	public MailFilterRuleFilterExists() {
		this.operator = MailFilterRuleOperatorName.EXISTS;
	}

	public MailFilterRuleFilterExists(List<String> fields) {
		this();
		this.fields = fields;
	}

	public MailFilterRuleFilterExists(String field) {
		this(Arrays.asList(field));
	}

	@GwtIncompatible
	@Override
	protected <F> boolean match(String fieldName, FieldValueProvider fieldProvider,
			ParameterValueProvider parameterProvider) {
		return MailFilterRuleKnownField.<F>from(fieldName).map(field -> {
			ExistsOperator<F> ruleOperator = MailFilterRuleOperators.exists(field);
			F fieldValue = fieldProvider.provides(field);
			return ruleOperator.match(fieldValue);
		}).orElse(false);

	}
}
