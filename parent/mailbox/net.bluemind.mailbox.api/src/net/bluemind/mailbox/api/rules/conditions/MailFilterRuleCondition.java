package net.bluemind.mailbox.api.rules.conditions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import net.bluemind.core.api.BMApi;
import net.bluemind.mailbox.api.rules.FieldValueProvider;
import net.bluemind.mailbox.api.rules.ParameterValueProvider;

@BMApi(version = "3")
public class MailFilterRuleCondition {

	public interface OperatorFunction extends BiFunction<Boolean, Boolean, Boolean> {

	}

	@BMApi(version = "3")
	public enum Operator {
		OR(Boolean::logicalOr), AND(Boolean::logicalAnd);

		OperatorFunction func;

		private Operator(OperatorFunction func) {
			this.func = func;
		}
	}

	public Operator operator = Operator.AND;
	public MailFilterRuleFilter filter = null;
	public List<MailFilterRuleCondition> conditions = new ArrayList<>();
	public boolean negate;

	public MailFilterRuleCondition() {

	}

	@JsonCreator
	public MailFilterRuleCondition(@JsonProperty("operator") Operator operator,
			@JsonProperty("filter") MailFilterRuleFilter filter,
			@JsonProperty("conditions") List<MailFilterRuleCondition> conditions,
			@JsonProperty("negate") boolean negate) {
		this.operator = operator;
		this.filter = filter;
		this.conditions = conditions;
		this.negate = negate;
	}

	public MailFilterRuleCondition(MailFilterRuleFilter filter, boolean negate) {
		this(Operator.AND, filter, Collections.emptyList(), negate);
	}

	public MailFilterRuleCondition(Operator operator, MailFilterRuleFilter filter, boolean negate) {
		this(operator, filter, Collections.emptyList(), negate);
	}

	public MailFilterRuleFilter filter() {
		return filter;
	}

	public Stream<MailFilterRuleFilter> filterStream() {
		return filter != null ? Stream.of(filter) : conditions.stream().flatMap(MailFilterRuleCondition::filterStream);
	}

	public boolean match(FieldValueProvider fieldProvider, ParameterValueProvider parameterProvider) {
		boolean result = filter != null //
				? filter.match(fieldProvider, parameterProvider) //
				: match(conditions, fieldProvider, parameterProvider);
		return (negate) ? !result : result;
	}

	public static boolean match(List<MailFilterRuleCondition> conditions, FieldValueProvider fieldProvider,
			ParameterValueProvider parameterProvider) {
		Boolean result = null;
		for (MailFilterRuleCondition condition : conditions) {
			OperatorFunction operator = condition.operator.func;
			result = (result == null) //
					? condition.match(fieldProvider, parameterProvider) //
					: operator.apply(result, condition.match(fieldProvider, parameterProvider));
		}
		return result == null || result;
	}

	public MailFilterRuleCondition or(MailFilterRuleCondition condition) {
		condition.operator = Operator.OR;
		return new MailFilterRuleCondition(Operator.AND, null, Arrays.asList(this, condition), false);
	}

	public MailFilterRuleCondition and(MailFilterRuleCondition condition) {
		return new MailFilterRuleCondition(Operator.AND, null, Arrays.asList(this, condition), false);
	}

	public MailFilterRuleCondition not() {
		negate = !negate;
		return this;
	}

	public static MailFilterRuleCondition not(MailFilterRuleCondition condition) {
		condition.negate = !condition.negate;
		return condition;
	}

	public static MailFilterRuleCondition exists(List<String> fields) {
		MailFilterRuleFilter filter = new MailFilterRuleFilterExists(fields);
		return new MailFilterRuleCondition(filter, false);
	}

	public static MailFilterRuleCondition exists(String field) {
		return exists(Arrays.asList(field));
	}

	public static MailFilterRuleCondition equal(List<String> fields, List<String> parameters) {
		MailFilterRuleFilter filter = new MailFilterRuleFilterEquals(fields, parameters);
		return new MailFilterRuleCondition(filter, false);
	}

	public static MailFilterRuleCondition equal(List<String> fields, String parameter) {
		return equal(fields, Arrays.asList(parameter));
	}

	public static MailFilterRuleCondition equal(String field, List<String> parameters) {
		return equal(Arrays.asList(field), parameters);
	}

	public static MailFilterRuleCondition equal(String field, String parameter) {
		return equal(Arrays.asList(field), Arrays.asList(parameter));
	}

	public static MailFilterRuleCondition contains(List<String> fields, List<String> parameters) {
		MailFilterRuleFilter filter = new MailFilterRuleFilterContains(fields, parameters);
		return new MailFilterRuleCondition(filter, false);
	}

	public static MailFilterRuleCondition contains(List<String> fields, String parameter) {
		return contains(fields, Arrays.asList(parameter));
	}

	public static MailFilterRuleCondition contains(String field, List<String> parameters) {
		return contains(Arrays.asList(field), parameters);
	}

	public static MailFilterRuleCondition matches(List<String> fields, List<String> parameters) {
		MailFilterRuleFilter filter = new MailFilterRuleFilterMatches(fields, parameters);
		return new MailFilterRuleCondition(filter, false);
	}

	public static MailFilterRuleCondition matches(List<String> fields, String parameter) {
		return matches(fields, Arrays.asList(parameter));
	}

	public static MailFilterRuleCondition matches(String field, List<String> parameters) {
		return matches(Arrays.asList(field), parameters);
	}

	public static MailFilterRuleCondition contains(String field, String parameter) {
		return contains(Arrays.asList(field), Arrays.asList(parameter));
	}

	public static MailFilterRuleCondition between(List<String> fields, String lowerBound, String upperBound) {
		MailFilterRuleFilter filter = new MailFilterRuleFilterRange(fields, lowerBound, upperBound);
		return new MailFilterRuleCondition(filter, false);
	}

	public static MailFilterRuleCondition between(String field, String lowerBound, String upperBound) {
		return between(Arrays.asList(field), lowerBound, upperBound);
	}

	public static MailFilterRuleCondition greaterThan(List<String> fields, String lowerBound) {
		MailFilterRuleFilter filter = new MailFilterRuleFilterRange(fields, lowerBound, null);
		return new MailFilterRuleCondition(filter, false);
	}

	public static MailFilterRuleCondition greaterThan(String field, String lowerBound) {
		return greaterThan(Arrays.asList(field), lowerBound);
	}

	public static MailFilterRuleCondition lowerThan(List<String> fields, String upperBound) {
		MailFilterRuleFilter filter = new MailFilterRuleFilterRange(fields, null, upperBound);
		return new MailFilterRuleCondition(filter, false);
	}

	public static MailFilterRuleCondition lowerThan(String field, String upperBound) {
		return lowerThan(Arrays.asList(field), upperBound);
	}

	@Override
	public int hashCode() {
		return Objects.hash(conditions, filter, negate, operator);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MailFilterRuleCondition other = (MailFilterRuleCondition) obj;
		return Objects.equals(conditions, other.conditions) && Objects.equals(filter, other.filter)
				&& negate == other.negate && operator == other.operator;
	}

}
