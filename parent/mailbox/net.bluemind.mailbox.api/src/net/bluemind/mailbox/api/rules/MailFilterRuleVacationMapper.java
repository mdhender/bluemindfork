package net.bluemind.mailbox.api.rules;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Date;
import java.util.Optional;
import java.util.function.Supplier;

import net.bluemind.mailbox.api.MailFilter;
import net.bluemind.mailbox.api.rules.conditions.MailFilterRuleCondition;
import net.bluemind.mailbox.api.rules.conditions.MailFilterRuleFilterRange;
import net.bluemind.mailbox.api.rules.conditions.MailFilterRuleOperatorName;

public class MailFilterRuleVacationMapper implements MailFilterRuleTypeMapper<MailFilter.Vacation> {

	private Supplier<DateTimeFormatter> dtfSupplier = () -> DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	public Optional<MailFilterRule> map(MailFilter.Vacation vacation) {
		if (vacation == null) {
			return Optional.empty();
		}

		MailFilterRule rule = new MailFilterRule();
		rule.type = MailFilterRule.Type.VACATION;
		rule.trigger = MailFilterRule.Trigger.IN;
		rule.active = vacation.enabled;
		rule.client = "bluemind";
		rule.name = "";
		DateTimeFormatter formatter = dtfSupplier.get();
		rule.conditions = new ArrayList<>();
		if (vacation.start != null || vacation.end != null) {
			String startDate = (vacation.start != null) ? formatter.format(vacation.start.toInstant()) : null;
			String endDate = (vacation.end != null) ? formatter.format(vacation.end.toInstant()) : null;
			rule.conditions.add(MailFilterRuleCondition.between("date", startDate, endDate));
		}
		if (vacation.subject != null && (vacation.text != null || vacation.textHtml != null)) {
			rule.addReply(vacation.subject, vacation.text, vacation.textHtml);
		}
		rule.stop = false;
		return Optional.of(rule);
	}

	public MailFilter.Vacation map(MailFilterRule rule) {
		MailFilter.Vacation vacation = new MailFilter.Vacation();
		vacation.enabled = rule.active;
		rule.reply().ifPresentOrElse(reply -> {
			vacation.subject = reply.subject;
			vacation.text = reply.plainBody;
			vacation.textHtml = reply.htmlBody;
		}, () -> {
			vacation.subject = "";
			vacation.text = "";
			vacation.textHtml = "";
		});
		rule.conditions.stream()
				.filter(condition -> condition.filter != null && condition.filter.fields.contains("date")
						&& MailFilterRuleOperatorName.RANGE.equals(condition.filter.operator))
				.findFirst() //
				.map(condition -> (MailFilterRuleFilterRange) condition.filter) //
				.ifPresent(range -> {
					vacation.start = parse(range.lowerBound);
					vacation.end = parse(range.upperBound);
				});
		return vacation;
	}

	private Date parse(String parameter) {
		if (parameter == null) {
			return null;
		}
		try {
			TemporalAccessor parsed = dtfSupplier.get().parse(parameter);
			if (parsed == null) {
				return null;
			}
			return Date.from(Instant.from(parsed));
		} catch (DateTimeParseException e) {
			return null;
		}
	}

}
