package net.bluemind.mailbox.api.rules.conditions;

import static net.bluemind.mailbox.api.rules.conditions.MailFilterRuleCondition.equal;
import static net.bluemind.mailbox.api.rules.conditions.MailFilterRuleCondition.not;
import static net.bluemind.mailbox.api.rules.conditions.MailFilterRuleKnownField.PART_CONTENT;
import static net.bluemind.mailbox.api.rules.conditions.MailFilterRuleKnownField.SUBJECT;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.junit.Test;

import net.bluemind.mailbox.api.rules.FieldValueProvider;
import net.bluemind.mailbox.api.rules.ParameterValueProvider;

public class MailFilterRuleConditionTest {
	private static final String SUBJECT_VALUE = "Subject";
	private static final String CONTENT_VALUE = "Content";

	private FieldValueProvider fieldValueProvider = new FieldValueProvider() {
		@Override
		@SuppressWarnings("unchecked")
		public <T> T provides(MailFilterRuleField<T> field) {
			switch (field.field()) {
			case SUBJECT:
				return (T) Arrays.asList(SUBJECT_VALUE);
			case PART_CONTENT:
				return (T) Arrays.asList(CONTENT_VALUE);
			default:
				return null;
			}
		}
	};

	private ParameterValueProvider parameterProvider = new ParameterValueProvider() {
		@Override
		public List<String> provides(List<String> parameters) {
			return parameters;
		}
	};

	@Test
	public void testOrCondition() {
		Stream.of(SUBJECT_VALUE, "not subject").forEach(subject -> {
			Stream.of(CONTENT_VALUE, "not content").forEach(content -> {
				MailFilterRuleCondition condition = equal(SUBJECT.text(), subject) //
						.or(equal(PART_CONTENT.text(), content));
				var exceptedResult = subject.equals(SUBJECT_VALUE) || content.equals(CONTENT_VALUE);
				assertEquals(exceptedResult, condition.match(fieldValueProvider, parameterProvider));
			});
		});
	}

	@Test
	public void testNegateOrCondition() {
		Stream.of(SUBJECT_VALUE, "not subject").forEach(subject -> {
			Stream.of(CONTENT_VALUE, "not content").forEach(content -> {
				MailFilterRuleCondition condition = not(equal(SUBJECT.text(), subject) //
						.or(equal(PART_CONTENT.text(), content)));
				var exceptedResult = !(subject.equals(SUBJECT_VALUE) || content.equals(CONTENT_VALUE));
				assertEquals(exceptedResult, condition.match(fieldValueProvider, parameterProvider));
			});
		});
	}

	@Test
	public void testAndCondition() {
		Stream.of(SUBJECT_VALUE, "not subject").forEach(subject -> {
			Stream.of(CONTENT_VALUE, "not content").forEach(content -> {
				MailFilterRuleCondition condition = equal(SUBJECT.text(), subject) //
						.and(equal(PART_CONTENT.text(), content));
				var exceptedResult = subject.equals(SUBJECT_VALUE) && content.equals(CONTENT_VALUE);
				assertEquals(exceptedResult, condition.match(fieldValueProvider, parameterProvider));
			});
		});
	}

	@Test
	public void testNegateAndCondition() {
		Stream.of(SUBJECT_VALUE, "not subject").forEach(subject -> {
			Stream.of(CONTENT_VALUE, "not content").forEach(content -> {
				MailFilterRuleCondition condition = not(equal(SUBJECT.text(), subject) //
						.and(equal(PART_CONTENT.text(), content)));
				var exceptedResult = !(subject.equals(SUBJECT_VALUE) && content.equals(CONTENT_VALUE));
				assertEquals(exceptedResult, condition.match(fieldValueProvider, parameterProvider));
			});
		});
	}

}
