package net.bluemind.mailbox.api.rules.conditions;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import net.bluemind.mailbox.api.utils.WildcardMatcher;

public class MailFilterRuleOperators {

	private static final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	private MailFilterRuleOperators() {

	}

	@SuppressWarnings("unchecked")
	public static <T> ExistsOperator<T> exists(MailFilterRuleField<T> field) {
		if (field.type().equals(Long.class)) {
			return (ExistsOperator<T>) MailFilterRuleOperators.LongOperator.EXISTS_OPERATOR;
		} else if (field.type().equals(List.class)) {
			return (ExistsOperator<T>) MailFilterRuleOperators.ListOperator.EXISTS_OPERATOR;
		} else if (field.type().equals(Date.class)) {
			return (ExistsOperator<T>) MailFilterRuleOperators.DateOperator.EXISTS_OPERATOR;
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public static <T> EqualsOperator<T> equals(MailFilterRuleField<T> field) {
		if (field.type().equals(Long.class)) {
			return (EqualsOperator<T>) MailFilterRuleOperators.LongOperator.EQUALS_OPERATOR;
		} else if (field.type().equals(List.class)) {
			return (EqualsOperator<T>) MailFilterRuleOperators.ListOperator.EQUALS_OPERATOR;
		} else if (field.type().equals(Date.class)) {
			return (EqualsOperator<T>) MailFilterRuleOperators.DateOperator.EQUALS_OPERATOR;
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public static <T> ContainsOperator<T> contains(MailFilterRuleField<T> field) {
		if (field.type().equals(List.class)) {
			return (ContainsOperator<T>) MailFilterRuleOperators.ListOperator.CONTAINS_ANY_OPERATOR;
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public static <T> MatchesOperator<T> matches(MailFilterRuleField<T> field) {
		if (field.type().equals(List.class)) {
			return (MatchesOperator<T>) MailFilterRuleOperators.ListOperator.MATCHES_ANY_OPERATOR;
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public static <T> RangeOperator<T> range(MailFilterRuleField<T> field) {
		if (field.type().equals(Long.class)) {
			return (RangeOperator<T>) MailFilterRuleOperators.LongOperator.RANGE_OPERATOR;
		} else if (field.type().equals(Date.class)) {
			return (RangeOperator<T>) MailFilterRuleOperators.DateOperator.RANGE_OPERATOR;
		}
		return null;
	}

	private abstract static class MailboxRuleAbstractOperator<T> implements MailFilterRuleOperator<T> {
		@Override
		public int hashCode() {
			return Objects.hash(operator());
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			MailboxRuleAbstractOperator<?> other = (MailboxRuleAbstractOperator<?>) obj;
			return Objects.equals(operator(), other.operator());
		}
	}

	public abstract static class ExistsOperator<T> extends MailboxRuleAbstractOperator<T> {
		public MailFilterRuleOperatorName operator() {
			return MailFilterRuleOperatorName.EXISTS;
		}

		public abstract boolean match(T value);
	}

	public abstract static class EqualsOperator<T> extends MailboxRuleAbstractOperator<T> {
		public MailFilterRuleOperatorName operator() {
			return MailFilterRuleOperatorName.EQUALS;
		}

		public abstract boolean match(T value, List<String> parameters);
	}

	public abstract static class ContainsOperator<T> extends MailboxRuleAbstractOperator<T> {
		public MailFilterRuleOperatorName operator() {
			return MailFilterRuleOperatorName.CONTAINS;
		}

		public abstract boolean match(T value, List<String> parameters);
	}

	public abstract static class MatchesOperator<T> extends MailboxRuleAbstractOperator<T> {
		public MailFilterRuleOperatorName operator() {
			return MailFilterRuleOperatorName.MATCHES;
		}

		public abstract boolean match(T value, List<String> parameters);
	}

	public abstract static class RangeOperator<T> extends MailboxRuleAbstractOperator<T> {
		public MailFilterRuleOperatorName operator() {
			return MailFilterRuleOperatorName.RANGE;
		}

		public abstract boolean match(T value, String lowerBound, String upperBound);
	}

	public static final class ListOperator {
		private ListOperator() {

		}

		public static final ExistsOperator<List<String>> EXISTS_OPERATOR = new ExistsOperator<List<String>>() {
			@Override
			public boolean match(List<String> values) {
				return values != null && !values.isEmpty();
			}
		};

		public static final EqualsOperator<List<String>> EQUALS_OPERATOR = new EqualsOperator<List<String>>() {
			@Override
			public boolean match(List<String> values, List<String> parameters) {
				return values != null && parameters.stream().anyMatch(values::contains);
			}
		};

		public static final ContainsOperator<List<String>> CONTAINS_ANY_OPERATOR = new ContainsOperator<List<String>>() {
			@Override
			public boolean match(List<String> values, List<String> parameters) {
				return values != null && parameters.stream()
						.anyMatch(parameter -> values.stream().anyMatch(value -> value.contains(parameter)));
			}
		};

		public static final MatchesOperator<List<String>> MATCHES_ANY_OPERATOR = new MatchesOperator<List<String>>() {
			@Override
			public boolean match(List<String> values, List<String> parameters) {
				return values != null && parameters.stream().anyMatch(
						parameter -> values.stream().anyMatch(value -> WildcardMatcher.match(value, parameter)));
			}
		};
	}

	public static final class LongOperator {
		private LongOperator() {

		}

		public static final ExistsOperator<Integer> EXISTS_OPERATOR = new ExistsOperator<Integer>() {
			@Override
			public boolean match(Integer value) {
				return value != null;
			}
		};

		public static final EqualsOperator<Integer> EQUALS_OPERATOR = new EqualsOperator<Integer>() {
			@Override
			public boolean match(Integer value, List<String> parameters) {
				return value != null && parse(parameters).anyMatch(value::equals);
			}
		};

		public static final RangeOperator<Integer> RANGE_OPERATOR = new RangeOperator<Integer>() {
			@Override
			public boolean match(Integer value, String lowerBoundParameter, String upperBoundParameter) {
				Long lowerBound = (lowerBoundParameter != null) ? parse(lowerBoundParameter) : null;
				Long upperBound = (upperBoundParameter != null) ? parse(upperBoundParameter) : null;
				if ((lowerBound == null && upperBound == null) || value == null) {
					return false;
				} else if (lowerBound == null) {
					return value <= upperBound;
				} else if (upperBound == null) {
					return value >= lowerBound;
				} else {
					return value >= lowerBound && value <= upperBound;
				}
			}
		};

		private static Stream<Long> parse(List<String> parameters) {
			return parameters.stream().map(LongOperator::parse).filter(Objects::nonNull);
		}

		private static Long parse(String parameter) {
			try {
				return Long.parseLong(parameter);
			} catch (NumberFormatException e) {
				return null;
			}
		}
	}

	public static final class DateOperator {
		private DateOperator() {

		}

		public static final ExistsOperator<Date> EXISTS_OPERATOR = new ExistsOperator<Date>() {
			@Override
			public boolean match(Date value) {
				return value != null;
			}
		};

		public static final EqualsOperator<Date> EQUALS_OPERATOR = new EqualsOperator<Date>() {
			@Override
			public boolean match(Date value, List<String> parameters) {
				return value != null && parse(parameters).anyMatch(value::equals);
			}
		};

		public static final MailFilterRuleOperator<Date> RANGE_OPERATOR = new RangeOperator<Date>() {
			@Override
			public boolean match(Date value, String lowerBoundParameter, String upperBoundParameter) {
				Date lowerBound = (lowerBoundParameter != null) ? parse(lowerBoundParameter) : null;
				Date upperBound = (upperBoundParameter != null) ? parse(upperBoundParameter) : null;
				if ((lowerBound == null && upperBound == null) || value == null) {
					return false;
				} else if (lowerBound == null) {
					return value.before(upperBound);
				} else if (upperBound == null) {
					return value.after(lowerBound);
				} else {
					return value.after(lowerBound) && value.before(upperBound);
				}
			}
		};

		private static Stream<Date> parse(List<String> parameters) {
			return parameters.stream().map(DateOperator::parse).filter(Objects::nonNull);
		}

		private static Date parse(String parameter) {
			try {
				return formatter.parse(parameter);
			} catch (ParseException e) {
				return null;
			}
		}

	}

}