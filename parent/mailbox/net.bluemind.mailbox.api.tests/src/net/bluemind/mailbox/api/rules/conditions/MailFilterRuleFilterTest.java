package net.bluemind.mailbox.api.rules.conditions;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import net.bluemind.mailbox.api.rules.FieldValueProvider;
import net.bluemind.mailbox.api.rules.ParameterValueProvider;
import net.bluemind.mailbox.api.rules.conditions.MailFilterRuleFilterContains.Comparator;
import net.bluemind.mailbox.api.rules.conditions.MailFilterRuleFilterContains.Modifier;

public class MailFilterRuleFilterTest {
	private static final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	private static final String SUBJECT_VALUE = "Subject";
	private static final Long SIZE_VALUE = 42l;
	private static final List<String> TO_EMAIL_VALUES = Arrays.asList("one@bm.net", "two@bm.net");
	private static final String DATE_VALUE = "2022-09-19 16:26:33";

	private FieldValueProvider fieldValueProvider = new FieldValueProvider() {
		@Override
		@SuppressWarnings("unchecked")
		public <T> T provides(MailFilterRuleField<T> field) {
			switch (field.field()) {
			case SUBJECT:
				return (T) Arrays.asList(SUBJECT_VALUE);
			case SIZE:
				return (T) SIZE_VALUE;
			case TO_EMAIL:
				return (T) TO_EMAIL_VALUES;
			case DATE:
				try {
					return (T) formatter.parse(DATE_VALUE);
				} catch (ParseException e) {
				}
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
	public void testStringFieldEquals() {
		var filter = new MailFilterRuleFilterEquals("subject", SUBJECT_VALUE);
		var match = filter.match(fieldValueProvider, parameterProvider);
		assertTrue(match);

		filter = new MailFilterRuleFilterEquals("subject", "not subject");
		match = filter.match(fieldValueProvider, parameterProvider);
		assertFalse(match);
	}

	@Test
	public void testStringFieldContains() {
		var filter = new MailFilterRuleFilterContains("subject", SUBJECT_VALUE.substring(1, 3));
		var match = filter.match(fieldValueProvider, parameterProvider);
		assertTrue(match);

		filter = new MailFilterRuleFilterContains("subject",
				Arrays.asList(SUBJECT_VALUE.substring(0, 2), SUBJECT_VALUE.substring(3, 7)));
		match = filter.match(fieldValueProvider, parameterProvider);
		assertTrue(match);

		filter = new MailFilterRuleFilterContains("subject",
				Arrays.asList(SUBJECT_VALUE.substring(0, 2), "not subject"));
		match = filter.match(fieldValueProvider, parameterProvider);
		assertTrue(match);

		filter = new MailFilterRuleFilterContains("subject", "not subject");
		match = filter.match(fieldValueProvider, parameterProvider);
		assertFalse(match);
	}

	@Test
	public void testStringFieldMatches() {
		var filter = new MailFilterRuleFilterMatches("subject", SUBJECT_VALUE.substring(1, 3));
		var match = filter.match(fieldValueProvider, parameterProvider);
		assertFalse(match);

		filter = new MailFilterRuleFilterMatches("subject", SUBJECT_VALUE.substring(0, 3) + "*");
		match = filter.match(fieldValueProvider, parameterProvider);
		assertTrue(match);

		filter = new MailFilterRuleFilterMatches("subject", SUBJECT_VALUE.replace("u", "?"));
		match = filter.match(fieldValueProvider, parameterProvider);
		assertTrue(match);

		filter = new MailFilterRuleFilterMatches("subject", SUBJECT_VALUE.replace("u", "?").substring(0, 3) + "*");
		match = filter.match(fieldValueProvider, parameterProvider);
		assertTrue(match);

		filter = new MailFilterRuleFilterMatches("subject",
				Arrays.asList(SUBJECT_VALUE.substring(0, 2), "not subject*"));
		match = filter.match(fieldValueProvider, parameterProvider);
		assertFalse(match);
	}

	@Test
	public void testStringFieldExists() {
		var filter = new MailFilterRuleFilterExists("subject");
		var match = filter.match(fieldValueProvider, parameterProvider);
		assertTrue(match);

		filter = new MailFilterRuleFilterExists("headers.X-BM-UNDEFINED");
		match = filter.match(fieldValueProvider, parameterProvider);
		assertFalse(match);
	}

	@Test
	public void testListFieldContainsSubstring() {
		var filter = new MailFilterRuleFilterContains("to.email", "one@bm.net");
		var match = filter.match(fieldValueProvider, parameterProvider);
		assertTrue(match);

		filter = new MailFilterRuleFilterContains("to.email", TO_EMAIL_VALUES);
		match = filter.match(fieldValueProvider, parameterProvider);
		assertTrue(match);

		filter = new MailFilterRuleFilterContains("to.email", Arrays.asList("one@bm.net", "three@bm.net"));
		match = filter.match(fieldValueProvider, parameterProvider);
		assertTrue(match);

		filter = new MailFilterRuleFilterContains("to.email", "bm.net");
		match = filter.match(fieldValueProvider, parameterProvider);
		assertTrue(match);

		filter = new MailFilterRuleFilterContains("to.email", "three@bm.net");
		match = filter.match(fieldValueProvider, parameterProvider);
		assertFalse(match);
	}

	@Test
	public void testListFieldContainsPrefix() {
		var filter = new MailFilterRuleFilterContains("to.email", "one", Comparator.PREFIX, Modifier.NONE);
		var match = filter.match(fieldValueProvider, parameterProvider);
		assertTrue(match);

		filter = new MailFilterRuleFilterContains("to.email", Arrays.asList("one", "bm.net"), Comparator.PREFIX,
				Modifier.NONE);
		match = filter.match(fieldValueProvider, parameterProvider);
		assertTrue(match);

		filter = new MailFilterRuleFilterContains("to.email", "bm.net", Comparator.PREFIX, Modifier.NONE);
		match = filter.match(fieldValueProvider, parameterProvider);
		assertFalse(match);
	}

	@Test
	public void testListFieldContainsCaseInsensitive() {
		var filter = new MailFilterRuleFilterContains("subject", SUBJECT_VALUE.toLowerCase());
		var match = filter.match(fieldValueProvider, parameterProvider);
		assertFalse(match);

		filter = new MailFilterRuleFilterContains("subject", SUBJECT_VALUE.toLowerCase(), Comparator.SUBSTRING,
				Modifier.CASE_INSENSITIVE);
		match = filter.match(fieldValueProvider, parameterProvider);
		assertTrue(match);
	}

	@Test
	public void testListFieldContainsIgnoreNonspacingMark() {
		var filter = new MailFilterRuleFilterContains("subject", "Sûbjëct");
		var match = filter.match(fieldValueProvider, parameterProvider);
		assertFalse(match);

		filter = new MailFilterRuleFilterContains("subject", "Sûbjëct", Comparator.SUBSTRING,
				Modifier.IGNORE_NONSPACING_MARK);
		match = filter.match(fieldValueProvider, parameterProvider);
		assertTrue(match);

		filter = new MailFilterRuleFilterContains("subject", "sûbjëct", Comparator.SUBSTRING,
				Modifier.IGNORE_NONSPACING_MARK);
		match = filter.match(fieldValueProvider, parameterProvider);
		assertFalse(match);
	}

	@Test
	public void testListFieldContainsLoose() {
		var filter = new MailFilterRuleFilterContains("subject", "sûbjëct");
		var match = filter.match(fieldValueProvider, parameterProvider);
		assertFalse(match);

		filter = new MailFilterRuleFilterContains("subject", "sûbjëct", Comparator.SUBSTRING, Modifier.LOOSE);
		match = filter.match(fieldValueProvider, parameterProvider);
		assertTrue(match);

		filter = new MailFilterRuleFilterContains("subject", "Sûbjëct", Comparator.SUBSTRING, Modifier.LOOSE);
		match = filter.match(fieldValueProvider, parameterProvider);
		assertTrue(match);
	}

	@Test
	public void testLongFieldBetween() {
		var filter = new MailFilterRuleFilterRange("size", String.valueOf(SIZE_VALUE - 1),
				String.valueOf(SIZE_VALUE + 1));
		var match = filter.match(fieldValueProvider, parameterProvider);
		assertTrue(match);

		filter = new MailFilterRuleFilterRange("size", String.valueOf(SIZE_VALUE + 1), String.valueOf(SIZE_VALUE + 3));
		match = filter.match(fieldValueProvider, parameterProvider);
		assertFalse(match);

		filter = new MailFilterRuleFilterRange("size", String.valueOf(SIZE_VALUE), null);
		match = filter.match(fieldValueProvider, parameterProvider);
		assertFalse(match);

		filter = new MailFilterRuleFilterRange("size", String.valueOf(SIZE_VALUE), null, true);
		match = filter.match(fieldValueProvider, parameterProvider);
		assertTrue(match);

		filter = new MailFilterRuleFilterRange("size", null, String.valueOf(SIZE_VALUE));
		match = filter.match(fieldValueProvider, parameterProvider);
		assertFalse(match);

		filter = new MailFilterRuleFilterRange("size", null, String.valueOf(SIZE_VALUE), true);
		match = filter.match(fieldValueProvider, parameterProvider);
		assertTrue(match);
	}

	@Test
	public void testDateFieldBetween() {
		// field date value = "2022-09-19 16:26:33"
		var filter = new MailFilterRuleFilterRange("date", "2022-09-18 16:26:33", "2022-09-20 16:26:33");
		boolean match = filter.match(fieldValueProvider, parameterProvider);
		assertTrue(match);

		filter = new MailFilterRuleFilterRange("date", "2022-09-20 16:26:33", "2022-09-21 16:26:33");
		match = filter.match(fieldValueProvider, parameterProvider);
		assertFalse(match);

		filter = new MailFilterRuleFilterRange("date", DATE_VALUE, null);
		match = filter.match(fieldValueProvider, parameterProvider);
		assertFalse(match);

		filter = new MailFilterRuleFilterRange("date", DATE_VALUE, null, true);
		match = filter.match(fieldValueProvider, parameterProvider);
		assertTrue(match);

		filter = new MailFilterRuleFilterRange("date", null, DATE_VALUE);
		match = filter.match(fieldValueProvider, parameterProvider);
		assertFalse(match);

		filter = new MailFilterRuleFilterRange("date", null, DATE_VALUE, true);
		match = filter.match(fieldValueProvider, parameterProvider);
		assertTrue(match);
	}
}
